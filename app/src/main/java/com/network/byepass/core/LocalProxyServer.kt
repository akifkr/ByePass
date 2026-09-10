package com.network.byepass.core

import com.network.byepass.service.TunnelService
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.random.Random

class LocalProxyServer(
    private val port: Int = 10808
) {
    private var serverSocket: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()
    @Volatile private var active = false

    private val dnsCache = ConcurrentHashMap<String, InetAddress>()

    companion object {
        // Yandex genuinely runs a DNS resolver on this non-standard port
        // specifically so users can dodge ISPs that transparently
        // intercept/poison anything sent to UDP port 53, regardless of
        // destination IP. This is the same technique documented by the
        // GoodbyeDPI-Turkey fork (`--dns-addr 77.88.8.8 --dns-port 1253`).
        // Do not "fix" this to port 53 — on networks that hijack port 53,
        // that gets you the ISP's spoofed answer instead of Yandex's real
        // one (which is exactly what caused the cert-mismatch bug earlier).
        private const val UPSTREAM_DNS_IP = "77.88.8.8"
        private const val UPSTREAM_DNS_PORT = 1253
        private const val DNS_TIMEOUT_MS = 2500
    }

    fun start() {
        active = true
        dnsCache.clear()
        serverSocket = ServerSocket(port, 128)
        pool.execute {
            while (active) {
                try {
                    val client = serverSocket?.accept() ?: break
                    pool.execute { handle(client) }
                } catch (_: SocketException) {
                    break
                }
            }
        }
    }

    fun stop() {
        active = false
        try { serverSocket?.close() } catch (_: Exception) {}
        pool.shutdownNow()
    }

    private fun handle(client: Socket) {
        try {
            client.tcpNoDelay = true
            val cin = client.getInputStream()
            val cout = client.getOutputStream()

            val headerBuf = ByteArray(4096)
            val n = cin.read(headerBuf)
            if (n <= 0) {
                client.close()
                return
            }

            val request = String(headerBuf, 0, n, Charsets.ISO_8859_1)
            val firstLine = request.lines().firstOrNull() ?: ""

            when {
                firstLine.startsWith("CONNECT ") -> handleConnect(firstLine, cin, cout, client)
                DpiSplitter.isPlainHttp(headerBuf, n) -> handlePlainHttp(request, firstLine, cin, cout, client)
                else -> client.close()
            }
        } catch (e: Exception) {
            TunnelService.log("Hata: ${e.message}")
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun handleConnect(firstLine: String, cin: InputStream, cout: OutputStream, client: Socket) {
        var upstream: Socket? = null
        try {
            val hostPort = firstLine.split(" ")[1]
            val targetHost = hostPort.split(":")[0]
            val targetPort = hostPort.split(":").getOrNull(1)?.toIntOrNull() ?: 443

            TunnelService.log("İstek: $targetHost")

            val targetIp = resolve(targetHost)
            TunnelService.log("DNS: ${targetIp.hostAddress}")

            upstream = Socket()
            upstream.tcpNoDelay = true
            upstream.connect(InetSocketAddress(targetIp, targetPort), 5000)

            cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
            cout.flush()

            val uout = upstream.getOutputStream()
            val uin = upstream.getInputStream()

            val tlsBuf = ByteArray(8192)
            val tlsLen = cin.read(tlsBuf)
            if (tlsLen > 0) {
                val tlsData = tlsBuf.copyOf(tlsLen)
                if (DpiSplitter.isClientHello(tlsData)) {
                    TunnelService.log("Bypass: $targetHost")
                    DpiSplitter.forwardFragmented(tlsData, uout)
                } else {
                    uout.write(tlsData)
                    uout.flush()
                }
            }

            pipe(client, upstream, cin, uout, uin, cout)
        } catch (e: Exception) {
            TunnelService.log("Hata: ${e.message}")
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { upstream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Handles plain-text HTTP (port 80) proxying. Previously missing: with
     * only CONNECT supported, any absolute-form HTTP request the system
     * proxy forwarded here (as Android does for non-HTTPS destinations)
     * just hung until timeout because nothing ever wrote a response.
     */
    private fun handlePlainHttp(
        request: String,
        firstLine: String,
        cin: InputStream,
        cout: OutputStream,
        client: Socket
    ) {
        var upstream: Socket? = null
        try {
            val lines = request.split("\r\n")
            val hostHeader = lines.firstOrNull { it.startsWith("Host:", ignoreCase = true) }
                ?.substringAfter(":")?.trim()

            val parts = firstLine.split(" ")
            if (parts.size < 3) {
                client.close()
                return
            }
            val method = parts[0]
            val rawUri = parts[1]
            val httpVersion = parts[2]

            val targetHost: String
            val targetPort: Int
            val path: String

            if (rawUri.startsWith("http://", ignoreCase = true)) {
                val withoutScheme = rawUri.substring(7)
                val slashIdx = withoutScheme.indexOf('/')
                val authority = if (slashIdx >= 0) withoutScheme.substring(0, slashIdx) else withoutScheme
                path = if (slashIdx >= 0) withoutScheme.substring(slashIdx) else "/"
                val hp = authority.split(":")
                targetHost = hp[0]
                targetPort = hp.getOrNull(1)?.toIntOrNull() ?: 80
            } else {
                val hp = (hostHeader ?: "").split(":")
                targetHost = hp.getOrNull(0) ?: ""
                targetPort = hp.getOrNull(1)?.toIntOrNull() ?: 80
                path = rawUri
            }

            if (targetHost.isEmpty()) {
                client.close()
                return
            }

            TunnelService.log("HTTP İstek: $targetHost")
            val targetIp = resolve(targetHost)
            TunnelService.log("DNS: ${targetIp.hostAddress}")

            upstream = Socket()
            upstream.tcpNoDelay = true
            upstream.connect(InetSocketAddress(targetIp, targetPort), 5000)

            // Rewrite absolute-form request line ("GET http://host/path ...")
            // to origin-form ("GET /path ..."), since the target is the
            // origin server itself, not another proxy — some servers don't
            // handle absolute-form requests correctly.
            val restHeaders = lines.drop(1).joinToString("\r\n")
            val rewritten = "$method $path $httpVersion\r\n$restHeaders"
            val mutated = DpiSplitter.mutateHttpHost(rewritten.toByteArray(Charsets.ISO_8859_1))

            val uout = upstream.getOutputStream()
            val uin = upstream.getInputStream()

            TunnelService.log("Bypass (HTTP): $targetHost")
            DpiSplitter.forwardHttpFragmented(mutated, uout)

            pipe(client, upstream, cin, uout, uin, cout)
        } catch (e: Exception) {
            TunnelService.log("HTTP Hata: ${e.message}")
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { upstream?.close() } catch (_: Exception) {}
        }
    }

    private fun resolve(host: String): InetAddress {
        dnsCache[host]?.let { return it }

        var socket: DatagramSocket? = null
        return try {
            val txId = Random.nextInt(0x0000, 0x10000)
            val query = createDnsQuery(host, txId)
            socket = DatagramSocket()
            socket.soTimeout = DNS_TIMEOUT_MS

            val packet = DatagramPacket(
                query, query.size,
                InetAddress.getByName(UPSTREAM_DNS_IP), UPSTREAM_DNS_PORT
            )
            socket.send(packet)

            val respBuf = ByteArray(512)
            val respPacket = DatagramPacket(respBuf, respBuf.size)
            socket.receive(respPacket)

            val ip = parseDnsResponse(respBuf, respPacket.length, txId) ?: InetAddress.getByName(host)
            dnsCache[host] = ip
            ip
        } catch (_: Exception) {
            val fallback = InetAddress.getByName(host)
            dnsCache[host] = fallback
            TunnelService.log("Uyarı: $host için Yandex DNS'e ulaşılamadı, sistem DNS'ine düşüldü")
            fallback
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun createDnsQuery(host: String, txId: Int): ByteArray {
        val buffer = ByteBuffer.allocate(512)
        buffer.putShort(txId.toShort())
        buffer.putShort(0x0100.toShort())
        buffer.putShort(1.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())

        for (part in host.split(".")) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            buffer.put(bytes.size.toByte())
            buffer.put(bytes)
        }
        buffer.put(0.toByte())
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }

    private fun parseDnsResponse(data: ByteArray, length: Int, expectedTxId: Int): InetAddress? {
        try {
            if (length < 12) return null

            val respTxId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            if (respTxId != expectedTxId) return null // guards against stray/spoofed replies

            var pos = 12
            while (pos < length && data[pos] != 0.toByte()) {
                pos += (data[pos].toInt() and 0xFF) + 1
            }
            pos += 5

            while (pos + 12 <= length) {
                if ((data[pos].toInt() and 0xC0) == 0xC0) {
                    pos += 2
                } else {
                    while (pos < length && data[pos] != 0.toByte()) pos++
                    pos++
                }

                val type = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                val dataLen = ((data[pos + 8].toInt() and 0xFF) shl 8) or (data[pos + 9].toInt() and 0xFF)
                pos += 10

                if (type == 1 && dataLen == 4 && pos + 4 <= length) {
                    val ipBytes = ByteArray(4)
                    System.arraycopy(data, pos, ipBytes, 0, 4)
                    return InetAddress.getByAddress(ipBytes)
                }
                pos += dataLen
            }
        } catch (_: Exception) {}
        return null
    }

    private fun pipe(
        client: Socket,
        upstream: Socket,
        cin: InputStream,
        uout: OutputStream,
        uin: InputStream,
        cout: OutputStream
    ) {
        val f1 = pool.submit {
            val buf = ByteArray(32768)
            try {
                var len: Int
                while (cin.read(buf).also { len = it } != -1) {
                    uout.write(buf, 0, len)
                    uout.flush()
                }
            } catch (_: Exception) {}
            try { upstream.shutdownOutput() } catch (_: Exception) {}
        }

        val f2 = pool.submit {
            val buf = ByteArray(32768)
            try {
                var len: Int
                while (uin.read(buf).also { len = it } != -1) {
                    cout.write(buf, 0, len)
                    cout.flush()
                }
            } catch (_: Exception) {}
            try { client.shutdownOutput() } catch (_: Exception) {}
        }

        try {
            f1.get()
            f2.get()
        } catch (_: Exception) {}
    }
}