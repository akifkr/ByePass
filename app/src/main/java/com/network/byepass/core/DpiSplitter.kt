package com.network.byepass.core

import java.io.OutputStream
import java.nio.charset.StandardCharsets

object DpiSplitter {

    fun isClientHello(data: ByteArray): Boolean {
        // TLS Handshake (0x16), TLS Sürümü (0x03, 0x01..0x03), ClientHello (0x01)
        return data.size > 5 && data[0] == 0x16.toByte() && data[5] == 0x01.toByte()
    }

    fun isPlainHttp(data: ByteArray): Boolean {
        val head = String(data, 0, minOf(data.size, 10), StandardCharsets.US_ASCII)
        return head.startsWith("GET ") ||
                head.startsWith("POST ") ||
                head.startsWith("HEAD ") ||
                head.startsWith("PUT ") ||
                head.startsWith("DELETE ") ||
                head.startsWith("OPTIONS ")
    }

    // HTTP DPI filtrelerini şaşırtmak için Host başlığının harf büyüklüğünü değiştirir
    fun mutateHttpHost(data: ByteArray): ByteArray {
        val text = String(data, StandardCharsets.ISO_8859_1)
        val modified = text.replaceFirst("Host:", "hOSt:", ignoreCase = true)
        return modified.toByteArray(StandardCharsets.ISO_8859_1)
    }

    // İlk TCP segmentini 2. bayttan bölerek DPI cihazlarının reassembly yapmasını engeller
    fun forwardFragmented(data: ByteArray, out: OutputStream, splitAt: Int = 2) {
        if (data.size <= splitAt) {
            out.write(data)
            out.flush()
            return
        }

        out.write(data, 0, splitAt)
        out.flush()

        try {
            Thread.sleep(2)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        out.write(data, splitAt, data.size - splitAt)
        out.flush()
    }
}