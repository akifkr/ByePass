package com.network.byepass.core

import java.io.OutputStream
import java.nio.charset.StandardCharsets

object DpiSplitter {

    fun isClientHello(data: ByteArray): Boolean {
        return data.size > 5 && data[0] == 0x16.toByte() && data[5] == 0x01.toByte()
    }

    fun isPlainHttp(data: ByteArray, length: Int = data.size): Boolean {
        val head = String(data, 0, minOf(length, 8), StandardCharsets.US_ASCII)
        return head.startsWith("GET ") || head.startsWith("POST ") || head.startsWith("HEAD ")
    }

    fun mutateHttpHost(data: ByteArray): ByteArray {
        val s = String(data, StandardCharsets.ISO_8859_1)
        val modified = s.replaceFirst("Host:", "hOSt:", ignoreCase = true)
        return modified.toByteArray(StandardCharsets.ISO_8859_1)
    }

    /**
     * Splits a TLS ClientHello right after the 5-byte record header so the
     * SNI extension lands in a second TCP write. Middleboxes that only
     * inspect the first packet of a flow won't see the full SNI in one go.
     */
    fun forwardFragmented(data: ByteArray, out: OutputStream) {
        forwardFragmentedAt(data, out, splitAt = 2)
    }

    /**
     * Same idea as [forwardFragmented] but for plain HTTP: splits right
     * before the "Host:" header (falling back to a fixed offset if the
     * header isn't found in the buffer) so the request line and the host
     * header don't travel in the same TCP segment.
     */
    fun forwardHttpFragmented(data: ByteArray, out: OutputStream) {
        val s = String(data, StandardCharsets.ISO_8859_1)
        val hostIdx = s.indexOf("Host:", ignoreCase = true)
        val splitAt = if (hostIdx > 0) hostIdx else 2
        forwardFragmentedAt(data, out, splitAt)
    }

    private fun forwardFragmentedAt(data: ByteArray, out: OutputStream, splitAt: Int) {
        if (data.size < 2) {
            out.write(data)
            out.flush()
            return
        }

        val cut = splitAt.coerceIn(1, data.size - 1)
        out.write(data, 0, cut)
        out.flush()

        try {
            Thread.sleep(2)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        out.write(data, cut, data.size - cut)
        out.flush()
    }
}