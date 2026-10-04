package com.example.vpn

import java.nio.ByteBuffer

/**
 * Handles basic DNS packet parsing and synthetic response generation for loopback VPN blocking.
 */
object DnsPacketHandler {

    /**
     * Extracts requested domain name from raw DNS payload (skipping IP/UDP header if already stripped).
     */
    fun extractDomainName(dnsPayload: ByteArray): String? {
        if (dnsPayload.size < 12) return null
        try {
            var index = 12 // Skip 12-byte DNS header
            val sb = StringBuilder()

            while (index < dnsPayload.size) {
                val labelLength = dnsPayload[index].toInt() and 0xFF
                if (labelLength == 0) break
                index++
                if (index + labelLength > dnsPayload.size) return null

                if (sb.isNotEmpty()) sb.append(".")
                for (i in 0 until labelLength) {
                    sb.append(dnsPayload[index + i].toInt().toChar())
                }
                index += labelLength
            }

            return if (sb.isNotEmpty()) sb.toString().lowercase() else null
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Creates a synthetic DNS NXDOMAIN (non-existent domain) response to block the request immediately.
     */
    fun createNxDomainResponse(requestBytes: ByteArray, length: Int): ByteArray {
        val response = requestBytes.copyOf(length)
        if (length >= 12) {
            // Set QR bit = 1 (response), Opcode = 0, RCODE = 3 (Name Error / NXDOMAIN)
            response[2] = 0x81.toByte() // QR=1, RD=1
            response[3] = 0x83.toByte() // RA=1, RCODE=3 (NXDOMAIN)
        }
        return response
    }

    /**
     * Creates a synthetic DNS response pointing to 0.0.0.0 (Blackhole IP).
     */
    fun createBlackholeResponse(requestBytes: ByteArray, length: Int): ByteArray {
        val buffer = ByteBuffer.allocate(length + 16)
        buffer.put(requestBytes, 0, length)
        if (length >= 12) {
            // Set QR bit = 1, RA = 1, RCODE = 0 (No error), ANCOUNT = 1
            buffer.put(2, 0x81.toByte())
            buffer.put(3, 0x80.toByte())
            buffer.put(6, 0x00.toByte())
            buffer.put(7, 0x01.toByte()) // ANCOUNT = 1

            // Append Answer: Name pointer (0xC00C), Type A (0x0001), Class IN (0x0001), TTL 60s, Len 4, IP 0.0.0.0
            buffer.putShort(0xC00C.toShort()) // Pointer to Question Name
            buffer.putShort(1.toShort())      // Type A
            buffer.putShort(1.toShort())      // Class IN
            buffer.putInt(60)                 // TTL 60 seconds
            buffer.putShort(4.toShort())      // Data length = 4
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())            // 0.0.0.0
        }
        val result = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(result)
        return result
    }
}
