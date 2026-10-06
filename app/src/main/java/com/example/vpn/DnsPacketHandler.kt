package com.example.vpn

import java.nio.ByteBuffer

/**
 * Handles DNS packet parsing, resolved IP extraction, and synthetic response generation.
 */
object DnsPacketHandler {

    /**
     * Extracts requested domain name from raw DNS payload (skipping IP/UDP header).
     */
    fun extractDomainName(dnsPayload: ByteArray): String? {
        if (dnsPayload.size < 12) return null
        try {
            var index = 12 // Skip 12-byte DNS header
            val sb = StringBuilder()

            while (index < dnsPayload.size) {
                val labelLength = dnsPayload[index].toInt() and 0xFF
                if (labelLength == 0) break
                // Check if it's a pointer (compression)
                if ((labelLength and 0xC0) == 0xC0) {
                    index += 2
                    break
                }
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
     * Attempts to parse the first resolved IPv4 address from an upstream DNS response.
     */
    fun extractResolvedIp(dnsResponse: ByteArray): String? {
        if (dnsResponse.size < 12) return null
        try {
            val anCount = ((dnsResponse[6].toInt() and 0xFF) shl 8) or (dnsResponse[7].toInt() and 0xFF)
            if (anCount <= 0) return null

            // Skip Question section
            var index = 12
            while (index < dnsResponse.size) {
                val len = dnsResponse[index].toInt() and 0xFF
                if (len == 0) {
                    index += 5 // 0x00 + 2 bytes QTYPE + 2 bytes QCLASS
                    break
                }
                if ((len and 0xC0) == 0xC0) {
                    index += 6 // 2 bytes pointer + 2 bytes QTYPE + 2 bytes QCLASS
                    break
                }
                index += len + 1
            }

            // Iterate through Answer records
            for (i in 0 until anCount) {
                if (index + 10 > dnsResponse.size) break

                // Skip Name
                if ((dnsResponse[index].toInt() and 0xC0) == 0xC0) {
                    index += 2
                } else {
                    while (index < dnsResponse.size && dnsResponse[index].toInt() != 0) {
                        index += (dnsResponse[index].toInt() and 0xFF) + 1
                    }
                    index++
                }

                if (index + 10 > dnsResponse.size) break
                val type = ((dnsResponse[index].toInt() and 0xFF) shl 8) or (dnsResponse[index + 1].toInt() and 0xFF)
                val rdLength = ((dnsResponse[index + 8].toInt() and 0xFF) shl 8) or (dnsResponse[index + 9].toInt() and 0xFF)
                index += 10

                // Type 1 = A (IPv4)
                if (type == 1 && rdLength == 4 && index + 4 <= dnsResponse.size) {
                    val ip0 = dnsResponse[index].toInt() and 0xFF
                    val ip1 = dnsResponse[index + 1].toInt() and 0xFF
                    val ip2 = dnsResponse[index + 2].toInt() and 0xFF
                    val ip3 = dnsResponse[index + 3].toInt() and 0xFF
                    return "$ip0.$ip1.$ip2.$ip3"
                }

                index += rdLength
            }
            return null
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
            // QR=1 (response), AA=1, RD=1, RA=1, RCODE=3 (NXDOMAIN)
            response[2] = 0x85.toByte()
            response[3] = 0x83.toByte()
            // Clear Answer / Authority / Additional counts
            response[6] = 0
            response[7] = 0
            response[8] = 0
            response[9] = 0
            response[10] = 0
            response[11] = 0
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
            // QR=1, RA=1, RCODE=0, ANCOUNT=1
            buffer.put(2, 0x81.toByte())
            buffer.put(3, 0x80.toByte())
            buffer.put(6, 0x00.toByte())
            buffer.put(7, 0x01.toByte())

            // Answer: Name pointer (0xC00C), Type A (0x0001), Class IN (0x0001), TTL 60s, Len 4, IP 0.0.0.0
            buffer.putShort(0xC00C.toShort())
            buffer.putShort(1.toShort())
            buffer.putShort(1.toShort())
            buffer.putInt(60)
            buffer.putShort(4.toShort())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
            buffer.put(0.toByte())
        }
        val result = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(result)
        return result
    }
}
