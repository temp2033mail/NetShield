package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.NetShieldDatabase
import com.example.data.model.AppFirewallRule
import com.example.data.model.NetworkLogEntity
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatLevel
import com.example.threat.ThreatIntelligence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class NetShieldVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var workerJob: Job? = null
    private var rulesCollectorJob: Job? = null

    // In-memory cache of firewall rules for zero-latency packet filtering
    private val activeFirewallRules = ConcurrentHashMap<String, AppFirewallRule>()

    // Upstream DNS socket protected from VPN routing
    private var upstreamDnsSocket: DatagramSocket? = null
    private val primaryDns = InetSocketAddress("1.1.1.1", 53)
    private val secondaryDns = InetSocketAddress("8.8.8.8", 53)

    companion object {
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        private const val NOTIFICATION_CHANNEL_ID = "netshield_protection_channel"
        private const val NOTIFICATION_ID = 1001

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive = _isVpnActive.asStateFlow()

        private val _blockedThreatsSessionCount = MutableStateFlow(0)
        val blockedThreatsSessionCount = _blockedThreatsSessionCount.asStateFlow()

        private val _queriesSessionCount = MutableStateFlow(0)
        val queriesSessionCount = _queriesSessionCount.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, NetShieldVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetShieldVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_isVpnActive.value) return

        serviceScope.launch {
            try {
                val database = NetShieldDatabase.getInstance(applicationContext)
                val dao = database.netShieldDao()

                // Collect latest firewall rules in background continuously
                rulesCollectorJob?.cancel()
                rulesCollectorJob = launch {
                    dao.getAllFirewallRules().collect { rulesList ->
                        activeFirewallRules.clear()
                        for (rule in rulesList) {
                            activeFirewallRules[rule.packageName] = rule
                        }
                    }
                }

                // Initialize protected upstream DNS socket
                try {
                    upstreamDnsSocket?.close()
                    val socket = DatagramSocket()
                    protect(socket) // CRITICAL: socket traffic bypasses VPN interface
                    socket.soTimeout = 2500
                    upstreamDnsSocket = socket
                } catch (e: Exception) {
                    Log.e("NetShieldVPN", "Failed to create upstream socket", e)
                }

                val builder = Builder()
                    .setSession("NetShield Sentinel")
                    .setMtu(1500)
                    // Set local VPN IP
                    .addAddress("10.200.1.1", 32)
                    // Intercept system DNS by pointing DNS server to our VPN interface
                    .addDnsServer("10.200.1.1")
                    // Route DNS traffic through TUN
                    .addRoute("10.200.1.1", 32)
                    // Intercept common hardcoded public DNS servers (8.8.8.8, 1.1.1.1, etc.)
                    .addRoute("1.1.1.1", 32)
                    .addRoute("1.0.0.1", 32)
                    .addRoute("8.8.8.8", 32)
                    .addRoute("8.8.4.4", 32)
                    .addRoute("9.9.9.9", 32)
                    .addRoute("208.67.222.222", 32)

                // Configure IPv6 addresses and routes
                try {
                    builder.addAddress("fd00:1:fd00:1::1", 128)
                    builder.addDnsServer("fd00:1:fd00:1::1")
                    builder.addRoute("fd00:1:fd00:1::1", 128)
                    builder.addRoute("2606:4700:4700::1111", 128)
                    builder.addRoute("2001:4860:4860::8888", 128)
                } catch (e: Exception) {
                    Log.w("NetShieldVPN", "IPv6 not supported: ${e.message}")
                }

                // Disallow ONLY NetShield itself so our upstream DNS forwarder doesn't loop into the VPN
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w("NetShieldVPN", "Cannot disallow self: ${e.message}")
                }

                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    Log.e("NetShieldVPN", "Failed to establish VPN interface")
                    stopSelf()
                    return@launch
                }

                _isVpnActive.value = true
                updateNotification()

                // Start packet processing loop
                workerJob = launch(Dispatchers.IO) {
                    processPackets(vpnInterface!!)
                }

            } catch (e: Exception) {
                Log.e("NetShieldVPN", "Error starting VPN: ${e.message}", e)
                stopVpn()
            }
        }
    }

    private suspend fun processPackets(descriptor: ParcelFileDescriptor) {
        val database = NetShieldDatabase.getInstance(applicationContext)
        val dao = database.netShieldDao()
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)

        val packet = ByteBuffer.allocate(32767)

        while (serviceScope.isActive && _isVpnActive.value) {
            try {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    packet.position(0)

                    val isWifi = isCurrentNetworkWifi()
                    val networkType = if (isWifi) "WIFI" else "CELLULAR"

                    // Check if IPv4 packet
                    if (packet.remaining() > 20) {
                        val versionAndIHL = packet.get(0).toInt() and 0xFF
                        val ipVersion = versionAndIHL shr 4

                        if (ipVersion == 4) {
                            handleIpv4Packet(
                                packet = packet,
                                length = length,
                                isWifi = isWifi,
                                networkType = networkType,
                                dao = dao,
                                outputStream = outputStream
                            )
                        } else if (ipVersion == 6 && packet.remaining() >= 48) {
                            handleIpv6Packet(
                                packet = packet,
                                length = length,
                                isWifi = isWifi,
                                networkType = networkType,
                                dao = dao,
                                outputStream = outputStream
                            )
                        }
                    }

                    packet.clear()
                }
            } catch (e: Exception) {
                if (!_isVpnActive.value) break
            }
        }
    }

    private suspend fun handleIpv4Packet(
        packet: ByteBuffer,
        length: Int,
        isWifi: Boolean,
        networkType: String,
        dao: com.example.data.db.NetShieldDao,
        outputStream: FileOutputStream
    ) {
        val versionAndIHL = packet.get(0).toInt() and 0xFF
        val ihl = (versionAndIHL and 0x0F) * 4

        if (packet.remaining() < ihl + 8) return
        val protocol = packet.get(9).toInt() and 0xFF // 17 = UDP

        if (protocol == 17) {
            val srcPort = packet.getShort(ihl).toInt() and 0xFFFF
            val destPort = packet.getShort(ihl + 2).toInt() and 0xFFFF

            if (destPort == 53) { // DNS Query Intercepted!
                _queriesSessionCount.value += 1
                val udpHeaderLen = 8
                val dnsPayloadOffset = ihl + udpHeaderLen
                val dnsPayloadLen = length - dnsPayloadOffset

                if (dnsPayloadLen > 12) {
                    val dnsPayload = ByteArray(dnsPayloadLen)
                    System.arraycopy(packet.array(), dnsPayloadOffset, dnsPayload, 0, dnsPayloadLen)

                    val domain = DnsPacketHandler.extractDomainName(dnsPayload)
                    if (domain != null) {
                        val srcIpBytes = ByteArray(4)
                        val dstIpBytes = ByteArray(4)
                        System.arraycopy(packet.array(), 12, srcIpBytes, 0, 4)
                        System.arraycopy(packet.array(), 16, dstIpBytes, 0, 4)

                        // 1. Identify requesting application (PCAPdroid UID resolution)
                        val (appPackage, appLabel) = resolveAppForConnection(
                            protocol = OsConstants.IPPROTO_UDP,
                            srcIpBytes = srcIpBytes,
                            srcPort = srcPort,
                            dstIpBytes = dstIpBytes,
                            dstPort = destPort
                        )

                        // 2. Check per-app Wi-Fi and Cellular Firewall Rules
                        val appRule = activeFirewallRules[appPackage] ?: dao.getRuleForPackage(appPackage)
                        val isAppBlockedByFirewall = if (isWifi) {
                            appRule?.blockWifi == true
                        } else {
                            appRule?.blockCellular == true
                        }

                        if (isAppBlockedByFirewall) {
                            // APP FIREWALL BLOCK TRIGGERED
                            _blockedThreatsSessionCount.value += 1
                            val reason = if (isWifi) {
                                "$appLabel is blocked from accessing Wi-Fi networks"
                            } else {
                                "$appLabel is blocked from accessing Cellular data networks"
                            }

                            dao.insertLog(
                                NetworkLogEntity(
                                    domain = domain,
                                    ipAddress = "Blocked by App Firewall",
                                    appName = appLabel,
                                    packageName = appPackage,
                                    protocol = "DNS",
                                    networkType = networkType,
                                    bytesTransferred = length.toLong(),
                                    isBlocked = true,
                                    blockReason = reason,
                                    threatCategory = ThreatCategory.APP_FIREWALL.name,
                                    threatLevel = ThreatLevel.HIGH.name
                                )
                            )
                            updateNotification()

                            // Return immediate NXDOMAIN response to stop connection
                            val nxDomainResponse = DnsPacketHandler.createNxDomainResponse(dnsPayload, dnsPayloadLen)
                            val responseIpPacket = createUdpIpResponse(
                                requestPacket = packet.array(),
                                ihl = ihl,
                                udpPayload = nxDomainResponse
                            )
                            outputStream.write(responseIpPacket)
                            outputStream.flush()
                            return
                        }

                        // 3. Check Malicious Domain Intelligence & User Custom Rules
                        val customRule = dao.getCustomDomainRule(domain)
                        val isCustomWhitelisted = customRule != null && !customRule.isBlocked
                        val isCustomBlocked = customRule?.isBlocked == true

                        val threatResult = if (isCustomWhitelisted) {
                            null
                        } else if (isCustomBlocked) {
                            ThreatIntelligence.checkDomain(domain).copy(
                                isThreat = true,
                                category = ThreatCategory.CUSTOM_BLOCKED,
                                reason = "Blocked by user custom domain blacklist"
                            )
                        } else {
                            val check = ThreatIntelligence.checkDomain(domain)
                            if (check.isThreat) check else null
                        }

                        if (threatResult != null && threatResult.isThreat) {
                            // MALICIOUS DOMAIN DETECTED -> BLOCK IMMEDIATELY
                            _blockedThreatsSessionCount.value += 1

                            dao.insertLog(
                                NetworkLogEntity(
                                    domain = domain,
                                    ipAddress = "0.0.0.0 (Intercepted)",
                                    appName = appLabel,
                                    packageName = appPackage,
                                    protocol = "DNS",
                                    networkType = networkType,
                                    bytesTransferred = length.toLong(),
                                    isBlocked = true,
                                    blockReason = threatResult.reason,
                                    threatCategory = threatResult.category.name,
                                    threatLevel = threatResult.level.name
                                )
                            )
                            updateNotification()

                            // Synthesize Blackhole / NXDOMAIN response
                            val blackholeDnsResponse = DnsPacketHandler.createBlackholeResponse(dnsPayload, dnsPayloadLen)
                            val responseIpPacket = createUdpIpResponse(
                                requestPacket = packet.array(),
                                ihl = ihl,
                                udpPayload = blackholeDnsResponse
                            )
                            outputStream.write(responseIpPacket)
                            outputStream.flush()
                            return
                        }

                        // 4. CLEAN & ALLOWED QUERY -> FORWARD TO UPSTREAM DNS
                        val upstreamResponse = forwardDnsQueryUpstream(dnsPayload)
                        if (upstreamResponse != null) {
                            val resolvedIp = DnsPacketHandler.extractResolvedIp(upstreamResponse) ?: "Resolved"

                            dao.insertLog(
                                NetworkLogEntity(
                                    domain = domain,
                                    ipAddress = resolvedIp,
                                    appName = appLabel,
                                    packageName = appPackage,
                                    protocol = "DNS",
                                    networkType = networkType,
                                    bytesTransferred = length.toLong(),
                                    isBlocked = false,
                                    blockReason = "Verified safe connection",
                                    threatCategory = ThreatCategory.NONE.name,
                                    threatLevel = ThreatLevel.SAFE.name
                                )
                            )

                            // Wrap and return the real DNS response to the calling application
                            val responseIpPacket = createUdpIpResponse(
                                requestPacket = packet.array(),
                                ihl = ihl,
                                udpPayload = upstreamResponse
                            )
                            outputStream.write(responseIpPacket)
                            outputStream.flush()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleIpv6Packet(
        packet: ByteBuffer,
        length: Int,
        isWifi: Boolean,
        networkType: String,
        dao: com.example.data.db.NetShieldDao,
        outputStream: FileOutputStream
    ) {
        val nextHeader = packet.get(6).toInt() and 0xFF
        if (nextHeader == 17) { // UDP
            val headerLen = 40
            val srcPort = packet.getShort(headerLen).toInt() and 0xFFFF
            val destPort = packet.getShort(headerLen + 2).toInt() and 0xFFFF

            if (destPort == 53) {
                _queriesSessionCount.value += 1
                val udpHeaderLen = 8
                val dnsPayloadOffset = headerLen + udpHeaderLen
                val dnsPayloadLen = length - dnsPayloadOffset

                if (dnsPayloadLen > 12) {
                    val dnsPayload = ByteArray(dnsPayloadLen)
                    System.arraycopy(packet.array(), dnsPayloadOffset, dnsPayload, 0, dnsPayloadLen)

                    val domain = DnsPacketHandler.extractDomainName(dnsPayload)
                    if (domain != null) {
                        val check = ThreatIntelligence.checkDomain(domain)
                        if (check.isThreat) {
                            _blockedThreatsSessionCount.value += 1
                            dao.insertLog(
                                NetworkLogEntity(
                                    domain = domain,
                                    ipAddress = ":: (Blocked IPv6)",
                                    appName = "Network Client",
                                    packageName = "android",
                                    protocol = "DNS (IPv6)",
                                    networkType = networkType,
                                    bytesTransferred = length.toLong(),
                                    isBlocked = true,
                                    blockReason = check.reason,
                                    threatCategory = check.category.name,
                                    threatLevel = check.level.name
                                )
                            )
                            updateNotification()
                        } else {
                            dao.insertLog(
                                NetworkLogEntity(
                                    domain = domain,
                                    ipAddress = "Resolved IPv6",
                                    appName = "Network Client",
                                    packageName = "android",
                                    protocol = "DNS (IPv6)",
                                    networkType = networkType,
                                    bytesTransferred = length.toLong(),
                                    isBlocked = false,
                                    blockReason = "Verified safe connection",
                                    threatCategory = ThreatCategory.NONE.name,
                                    threatLevel = ThreatLevel.SAFE.name
                                )
                            )
                            forwardDnsQueryUpstream(dnsPayload)
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves the calling application package and label using connection owner UID (Android 10+).
     */
    private fun resolveAppForConnection(
        protocol: Int,
        srcIpBytes: ByteArray,
        srcPort: Int,
        dstIpBytes: ByteArray,
        dstPort: Int
    ): Pair<String, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cm = getSystemService(ConnectivityManager::class.java)
                val srcAddress = InetAddress.getByAddress(srcIpBytes)
                val dstAddress = InetAddress.getByAddress(dstIpBytes)
                val uid = cm.getConnectionOwnerUid(
                    protocol,
                    InetSocketAddress(srcAddress, srcPort),
                    InetSocketAddress(dstAddress, dstPort)
                )

                if (uid > 0) {
                    val packages = packageManager.getPackagesForUid(uid)
                    if (!packages.isNullOrEmpty()) {
                        val pkg = packages[0]
                        val appName = try {
                            val appInfo = packageManager.getApplicationInfo(pkg, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            pkg
                        }
                        return Pair(pkg, appName)
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        return Pair("android", "Android System")
    }

    /**
     * Forwards a DNS query to upstream public DNS server via a protected socket.
     */
    private fun forwardDnsQueryUpstream(queryPayload: ByteArray): ByteArray? {
        val socket = upstreamDnsSocket ?: return null
        return try {
            val sendPacket = DatagramPacket(queryPayload, queryPayload.size, primaryDns)
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)

            val result = ByteArray(receivePacket.length)
            System.arraycopy(receiveBuffer, 0, result, 0, receivePacket.length)
            result
        } catch (e: Exception) {
            // Retry with secondary DNS
            try {
                val sendPacket = DatagramPacket(queryPayload, queryPayload.size, secondaryDns)
                socket.send(sendPacket)

                val receiveBuffer = ByteArray(2048)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                val result = ByteArray(receivePacket.length)
                System.arraycopy(receiveBuffer, 0, result, 0, receivePacket.length)
                result
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun createUdpIpResponse(
        requestPacket: ByteArray,
        ihl: Int,
        udpPayload: ByteArray
    ): ByteArray {
        val udpLen = 8 + udpPayload.size
        val totalLen = ihl + udpLen
        val response = ByteArray(totalLen)

        // Swap IP src and dst
        System.arraycopy(requestPacket, 0, response, 0, ihl)
        System.arraycopy(requestPacket, 16, response, 12, 4) // dst IP -> src IP
        System.arraycopy(requestPacket, 12, response, 16, 4) // src IP -> dst IP

        // IP Total Length
        response[2] = (totalLen shr 8).toByte()
        response[3] = (totalLen and 0xFF).toByte()
        response[10] = 0 // Clear IP checksum
        response[11] = 0

        // Swap UDP src and dst port
        val srcPort = ((requestPacket[ihl].toInt() and 0xFF) shl 8) or (requestPacket[ihl + 1].toInt() and 0xFF)
        val dstPort = ((requestPacket[ihl + 2].toInt() and 0xFF) shl 8) or (requestPacket[ihl + 3].toInt() and 0xFF)

        response[ihl] = (dstPort shr 8).toByte()
        response[ihl + 1] = (dstPort and 0xFF).toByte()
        response[ihl + 2] = (srcPort shr 8).toByte()
        response[ihl + 3] = (srcPort and 0xFF).toByte()

        // UDP Length
        response[ihl + 4] = (udpLen shr 8).toByte()
        response[ihl + 5] = (udpLen and 0xFF).toByte()
        response[ihl + 6] = 0 // Checksum optional in IPv4 UDP
        response[ihl + 7] = 0

        // Copy DNS payload
        System.arraycopy(udpPayload, 0, response, ihl + 8, udpPayload.size)

        // Calculate IPv4 Header Checksum
        var sum = 0L
        for (i in 0 until ihl step 2) {
            val word = ((response[i].toInt() and 0xFF) shl 8) or (response[i + 1].toInt() and 0xFF)
            sum += word
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = (sum.inv() and 0xFFFF).toInt()
        response[10] = (checksum shr 8).toByte()
        response[11] = (checksum and 0xFF).toByte()

        return response
    }

    private fun isCurrentNetworkWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun stopVpn() {
        _isVpnActive.value = false
        workerJob?.cancel()
        rulesCollectorJob?.cancel()
        try {
            upstreamDnsSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        upstreamDnsSocket = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "NetShield Active Sentinel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time firewall and malicious domain blocking status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NetShieldVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val blockedCount = _blockedThreatsSessionCount.value
        val queriesCount = _queriesSessionCount.value

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("NetShield Active Sentinel")
            .setContentText("Protected • $blockedCount threats blocked • $queriesCount inspected")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Shield", pendingStopIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
