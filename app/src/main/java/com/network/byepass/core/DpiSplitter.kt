package com.network.byepass.core

import java.io.OutputStream
import java.nio.charset.StandardCharsets

object DpiSplitter {

    fun isClientHello(data: ByteArray): Boolean {
        return data.size > 5 && data[0] == 0x16.toByte() && data[5] == 0x01.toByte()
    }

    fun isPlainHttp(data: ByteArray): Boolean {
        val head = String(data, 0, minOf(data.size, 8), StandardCharsets.US_ASCII)
        return head.startsWith("GET ") || head.startsWith("POST ") || head.startsWith("HEAD ")
    }

    fun mutateHttpHost(data: ByteArray): ByteArray {
        val s = String(data, StandardCharsets.ISO_8859_1)
        val modified = s.replaceFirst("Host:", "hOSt:", ignoreCase = true)
        return modified.toByteArray(StandardCharsets.ISO_8859_1)
    }

    fun forwardFragmented(data: ByteArray, out: OutputStream) {
        val splitAt = 2
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