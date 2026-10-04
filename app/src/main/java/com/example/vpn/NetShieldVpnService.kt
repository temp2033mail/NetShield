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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.NetShieldDatabase
import com.example.data.model.NetworkLogEntity
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatLevel
import com.example.threat.ThreatIntelligence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class NetShieldVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var workerJob: Job? = null

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

                // Check active network type (Wi-Fi vs Cellular)
                val isWifi = isCurrentNetworkWifi()

                // Fetch firewall rules to block applications from this network
                val allRules = dao.getAllFirewallRules().first()
                val disallowedPackages = mutableSetOf<String>()

                for (rule in allRules) {
                    if (isWifi && rule.blockWifi) {
                        disallowedPackages.add(rule.packageName)
                    } else if (!isWifi && rule.blockCellular) {
                        disallowedPackages.add(rule.packageName)
                    }
                }

                val builder = Builder()
                    .setSession("NetShield Secure Firewall")
                    .addAddress("10.200.1.1", 32)
                    .addDnsServer("1.1.1.1") // Secure upstream DNS
                    .addRoute("10.200.1.0", 24)

                // Configure per-app network blocking via disallowed applications
                for (pkg in disallowedPackages) {
                    try {
                        builder.addDisallowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.w("NetShieldVPN", "Cannot disallow package $pkg: ${e.message}")
                    }
                }

                // Exclude ourselves so we can route DNS lookups
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    // Ignore
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

                    val networkType = if (isCurrentNetworkWifi()) "WIFI" else "CELLULAR"

                    // Inspect IPv4 Packet
                    if (packet.remaining() > 20) {
                        val versionAndIHL = packet.get(0).toInt() and 0xFF
                        val ipVersion = versionAndIHL shr 4
                        val ihl = (versionAndIHL and 0x0F) * 4

                        if (ipVersion == 4 && packet.remaining() >= ihl + 8) {
                            val protocol = packet.get(9).toInt() and 0xFF // 17 = UDP

                            if (protocol == 17) { // UDP
                                val srcPort = packet.getShort(ihl).toInt() and 0xFFFF
                                val destPort = packet.getShort(ihl + 2).toInt() and 0xFFFF

                                if (destPort == 53) { // DNS Query
                                    _queriesSessionCount.value += 1
                                    val udpHeaderLen = 8
                                    val dnsPayloadOffset = ihl + udpHeaderLen
                                    val dnsPayloadLen = length - dnsPayloadOffset

                                    if (dnsPayloadLen > 12) {
                                        val dnsPayload = ByteArray(dnsPayloadLen)
                                        System.arraycopy(packet.array(), dnsPayloadOffset, dnsPayload, 0, dnsPayloadLen)

                                        val domain = DnsPacketHandler.extractDomainName(dnsPayload)
                                        if (domain != null) {
                                            // Check custom blacklist/whitelist rules first
                                            val customRule = dao.getCustomDomainRule(domain)
                                            val isCustomBlocked = customRule?.isBlocked == true
                                            val isCustomWhitelisted = customRule != null && !customRule.isBlocked

                                            val threatResult = if (isCustomWhitelisted) {
                                                null
                                            } else if (isCustomBlocked) {
                                                ThreatIntelligence.checkDomain(domain).copy(
                                                    isThreat = true,
                                                    category = ThreatCategory.CUSTOM_BLOCKED,
                                                    reason = "Blocked by custom user blacklist rule"
                                                )
                                            } else {
                                                val check = ThreatIntelligence.checkDomain(domain)
                                                if (check.isThreat) check else null
                                            }

                                            if (threatResult != null && threatResult.isThreat) {
                                                // MALICIOUS DOMAIN DETECTED -> BLOCK IMMEDIATELY
                                                _blockedThreatsSessionCount.value += 1

                                                // Record in database
                                                dao.insertLog(
                                                    NetworkLogEntity(
                                                        domain = domain,
                                                        ipAddress = "0.0.0.0 (Blocked)",
                                                        appName = "Threat Sentinel",
                                                        packageName = "netshield.filter",
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

                                                // Build synthetic response packet with 0.0.0.0 / NXDOMAIN
                                                val syntheticDnsResponse = DnsPacketHandler.createBlackholeResponse(dnsPayload, dnsPayloadLen)
                                                val responseIpPacket = createUdpIpResponse(
                                                    requestPacket = packet.array(),
                                                    ihl = ihl,
                                                    udpPayload = syntheticDnsResponse
                                                )
                                                outputStream.write(responseIpPacket)
                                                outputStream.flush()
                                                packet.clear()
                                                continue
                                            } else {
                                                // Safe / Verified Domain
                                                dao.insertLog(
                                                    NetworkLogEntity(
                                                        domain = domain,
                                                        ipAddress = "Resolved via 1.1.1.1",
                                                        appName = "Network Client",
                                                        packageName = "android.system",
                                                        protocol = "DNS",
                                                        networkType = networkType,
                                                        bytesTransferred = length.toLong(),
                                                        isBlocked = false,
                                                        blockReason = "Passed all security filters",
                                                        threatCategory = ThreatCategory.NONE.name,
                                                        threatLevel = ThreatLevel.SAFE.name
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    packet.clear()
                }
            } catch (e: Exception) {
                if (!_isVpnActive.value) break
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

        // Copy payload
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
