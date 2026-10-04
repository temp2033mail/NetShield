package com.example.tracker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import com.example.data.model.RealtimeSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkSpeedTracker(private val context: Context) {

    private val _speedState = MutableStateFlow(RealtimeSpeed())
    val speedState = _speedState.asStateFlow()

    // Circular buffer of speed history (in KB/s) for live visual chart (last 30 seconds)
    private val _downloadHistory = MutableStateFlow<List<Float>>(List(30) { 0f })
    val downloadHistory = _downloadHistory.asStateFlow()

    private val _uploadHistory = MutableStateFlow<List<Float>>(List(30) { 0f })
    val uploadHistory = _uploadHistory.asStateFlow()

    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastTimestamp = System.currentTimeMillis()

    private val initialRxBytes = if (lastRxBytes != TrafficStats.UNSUPPORTED.toLong()) lastRxBytes else 0L
    private val initialTxBytes = if (lastTxBytes != TrafficStats.UNSUPPORTED.toLong()) lastTxBytes else 0L

    fun startTracking(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000)

                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentMobileRx = TrafficStats.getMobileRxBytes()
                val currentMobileTx = TrafficStats.getMobileTxBytes()
                val now = System.currentTimeMillis()

                val timeDeltaSec = ((now - lastTimestamp) / 1000.0).coerceAtLeast(0.5)

                var downSpeed = 0L
                var upSpeed = 0L

                if (lastRxBytes != TrafficStats.UNSUPPORTED.toLong() && currentRx != TrafficStats.UNSUPPORTED.toLong()) {
                    if (currentRx >= lastRxBytes) {
                        downSpeed = ((currentRx - lastRxBytes) / timeDeltaSec).toLong()
                    }
                    lastRxBytes = currentRx
                }

                if (lastTxBytes != TrafficStats.UNSUPPORTED.toLong() && currentTx != TrafficStats.UNSUPPORTED.toLong()) {
                    if (currentTx >= lastTxBytes) {
                        upSpeed = ((currentTx - lastTxBytes) / timeDeltaSec).toLong()
                    }
                    lastTxBytes = currentTx
                }

                lastTimestamp = now

                val netType = detectNetworkType()
                val mobileTotal = (if (currentMobileRx != TrafficStats.UNSUPPORTED.toLong()) currentMobileRx else 0L) +
                        (if (currentMobileTx != TrafficStats.UNSUPPORTED.toLong()) currentMobileTx else 0L)
                val grandTotal = (if (currentRx != TrafficStats.UNSUPPORTED.toLong()) currentRx else 0L) +
                        (if (currentTx != TrafficStats.UNSUPPORTED.toLong()) currentTx else 0L)

                val wifiEstimated = (grandTotal - mobileTotal).coerceAtLeast(0L)

                val sessionRx = (currentRx - initialRxBytes).coerceAtLeast(0L)
                val sessionTx = (currentTx - initialTxBytes).coerceAtLeast(0L)

                _speedState.value = RealtimeSpeed(
                    downloadSpeedBps = downSpeed,
                    uploadSpeedBps = upSpeed,
                    currentNetworkType = netType,
                    isVpnActive = isVpnActive(),
                    totalRxSessionBytes = sessionRx,
                    totalTxSessionBytes = sessionTx,
                    totalWifiBytes = wifiEstimated,
                    totalCellularBytes = mobileTotal
                )

                // Update charts with speed in KB/s
                val downKb = (downSpeed / 1024f).coerceAtLeast(0f)
                val upKb = (upSpeed / 1024f).coerceAtLeast(0f)

                _downloadHistory.value = (_downloadHistory.value.drop(1) + downKb)
                _uploadHistory.value = (_uploadHistory.value.drop(1) + upKb)
            }
        }
    }

    private fun detectNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "NONE"
        val activeNet = cm.activeNetwork ?: return "NONE"
        val caps = cm.getNetworkCapabilities(activeNet) ?: return "NONE"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    private fun isVpnActive(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
