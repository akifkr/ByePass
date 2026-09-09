package com.network.byepass.core

import com.network.byepass.service.TunnelService
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class LocalProxyServer(
    private val port: Int = 10808
) {
    private var serverSocket: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()
    @Volatile private var active = false

    private val dnsCache = ConcurrentHashMap<String, InetAddress>()

    fun start() {
        active = true
        dnsCache.clear()
        serverSocket = ServerSocket(port, 128)
        pool.execute {
            while (active) {
                try {
                    val client = serverSocket?.accept() ?: break
                    pool.execute { handleConnection(client) }
                } catch (_: SocketException) {
                    break
                } catch (_: Exception) {
                    // Genel dinleyici hatası
                }
            }
        }
    }

    fun stop() {
        active = false
        closeQuietly(serverSocket)
        pool.shutdownNow()
    }

    private fun handleConnection(client: Socket) {
        var upstream: Socket? = null
        try {
            client.tcpNoDelay = true
            val cin = client.getInputStream()
            val cout = client.getOutputStream()

            val headerBuf = ByteArray(8192)
            val n = cin.read(headerBuf)
            if (n <= 0) {
                closeQuietly(client)
                return
            }

            val request = String(headerBuf, 0, n, Charsets.ISO_8859_1)
            val firstLine = request.lines().firstOrNull()?.trim() ?: ""

            if (firstLine.startsWith("CONNECT ")) {
                // --- HTTPS (CONNECT Tüneli) ---
                val rawTarget = firstLine.split(" ").getOrNull(1) ?: return
                val (targetHost, targetPort) = parseHostPort(rawTarget, defaultPort = 443)

                TunnelService.log("HTTPS İstek: $targetHost:$targetPort")
                val targetIp = resolve(targetHost)

                upstream = Socket()
                upstream.tcpNoDelay = true
                upstream.connect(InetSocketAddress(targetIp, targetPort), 5000)

                cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
                cout.flush()

                val uout = upstream.getOutputStream()
                val uin = upstream.getInputStream()

                // İlk el sıkışma (TLS ClientHello) kontrolü
                val handshakeBuf = ByteArray(8192)
                val hLen = cin.read(handshakeBuf)
                if (hLen > 0) {
                    val data = handshakeBuf.copyOf(hLen)
                    if (DpiSplitter.isClientHello(data)) {
                        TunnelService.log("Bypass (TLS Parçalama): $targetHost")
                        DpiSplitter.forwardFragmented(data, uout)
                    } else {
                        uout.write(data)
                        uout.flush()
                    }
                }

                pipeBidirectional(client, upstream, cin, cout, uin, uout)
            } else if (DpiSplitter.isPlainHttp(headerBuf)) {
                // --- Standart Düz HTTP İstekleri (GET, POST vs.) ---
                val rawUrl = firstLine.split(" ").getOrNull(1) ?: return
                val (targetHost, targetPort, relativePath) = parseHttpUrl(rawUrl, request)

                TunnelService.log("HTTP İstek: $targetHost:$targetPort")
                val targetIp = resolve(targetHost)

                upstream = Socket()
                upstream.tcpNoDelay = true
                upstream.connect(InetSocketAddress(targetIp, targetPort), 5000)

                val uout = upstream.getOutputStream()
                val uin = upstream.getInputStream()

                // İstek satırındaki mutlak URL'i göreli yola çevirip Host başlığını mutasyona uğratıyoruz
                val method = firstLine.substringBefore(" ")
                val protocol = firstLine.substringAfterLast(" ")
                val rewrittenFirstLine = "$method $relativePath $protocol"
                val lines = request.lines().toMutableList()
                lines[0] = rewrittenFirstLine
                val fullRawRequest = lines.joinToString("\r\n").toByteArray(Charsets.ISO_8859_1)

                val mutatedPayload = DpiSplitter.mutateHttpHost(fullRawRequest)
                TunnelService.log("Bypass (HTTP Host Mutasyonu): $targetHost")
                DpiSplitter.forwardFragmented(mutatedPayload, uout)

                pipeBidirectional(client, upstream, cin, cout, uin, uout)
            } else {
                // Tanınmayan protokol
                closeQuietly(client)
            }
        } catch (e: Exception) {
            TunnelService.log("Hata: ${e.message}")
        } finally {
            closeQuietly(client)
            closeQuietly(upstream)
        }
    }

    private fun parseHostPort(raw: String, defaultPort: Int): Pair<String, Int> {
        val clean = raw.trim()
        return if (clean.startsWith("[")) {
            val endBracket = clean.indexOf(']')
            val host = clean.substring(1, endBracket)
            val portPart = clean.substring(endBracket + 1)
            val port = if (portPart.startsWith(":")) portPart.substring(1).toIntOrNull() ?: defaultPort else defaultPort
            Pair(host, port)
        } else {
            val host = clean.substringBeforeLast(":")
            val port = clean.substringAfterLast(":", defaultPort.toString()).toIntOrNull() ?: defaultPort
            Pair(host, port)
        }
    }

    private fun parseHttpUrl(rawUrl: String, fullHeader: String): Triple<String, Int, String> {
        return try {
            val uri = URI(rawUrl)
            if (uri.host != null) {
                val port = if (uri.port != -1) uri.port else 80
                val path = if (uri.rawPath.isNullOrEmpty()) "/" else uri.rawPath + (if (uri.rawQuery != null) "?${uri.rawQuery}" else "")
                Triple(uri.host, port, path)
            } else {
                throw IllegalArgumentException()
            }
        } catch (_: Exception) {
            // URL parse edilemezse Host: başlığından al
            val hostLine = fullHeader.lines().firstOrNull { it.startsWith("Host:", ignoreCase = true) }
            val hostVal = hostLine?.substringAfter(":")?.trim() ?: "unknown"
            val (h, p) = parseHostPort(hostVal, 80)
            Triple(h, p, rawUrl)
        }
    }

    // Thread patlamasını engelleyen ve çift yönlü I/O sağlayan optimize boru hattı
    private fun pipeBidirectional(
        client: Socket,
        upstream: Socket,
        cin: InputStream,
        cout: OutputStream,
        uin: InputStream,
        uout: OutputStream
    ) {
        // İstemciden gelen veriyi yukarı sunucuya ileten tekil arka plan iş parçacığı
        val uploadTask = pool.submit {
            val buf = ByteArray(16384)
            try {
                var len: Int
                while (cin.read(buf).also { len = it } != -1) {
                    uout.write(buf, 0, len)
                    uout.flush()
                }
            } catch (_: Exception) {}
            finally {
                try { upstream.shutdownOutput() } catch (_: Exception) {}
            }
        }

        // Ana thread indirme (upstream -> client) için kullanılır (Ekstra thread açılmaz)
        val downloadBuf = ByteArray(16384)
        try {
            var len: Int
            while (uin.read(downloadBuf).also { len = it } != -1) {
                cout.write(downloadBuf, 0, len)
                cout.flush()
            }
        } catch (_: Exception) {}
        finally {
            try { client.shutdownOutput() } catch (_: Exception) {}
        }

        uploadTask.cancel(true)
    }

    private fun resolve(host: String): InetAddress {
        dnsCache[host]?.let { return it }

        // IP literal ise doğrudan dön
        try {
            return InetAddress.getByName(host).also { dnsCache[host] = it }
        } catch (_: Exception) {}

        // Soket sızıntısını önlemek için .use {} kullanıldı
        return try {
            val query = createDnsQuery(host)
            val respBuf = ByteArray(512)
            val respPacket = DatagramPacket(respBuf, respBuf.size)

            DatagramSocket().use { socket ->
                socket.soTimeout = 2500
                val targetServer = InetAddress.getByAddress(byteArrayOf(77, 88, 8, 8)) // 77.88.8.8
                val packet = DatagramPacket(query, query.size, targetServer, 1253)
                socket.send(packet)
                socket.receive(respPacket)
            }

            val ip = parseDnsResponse(respBuf, respPacket.length) ?: InetAddress.getByName(host)
            dnsCache[host] = ip
            ip
        } catch (_: Exception) {
            val fallback = InetAddress.getByName(host)
            dnsCache[host] = fallback
            fallback
        }
    }

    private fun createDnsQuery(host: String): ByteArray {
        val buffer = ByteBuffer.allocate(512)
        buffer.putShort(0x1337.toShort())
        buffer.putShort(0x0100.toShort()) // Standart sorgu
        buffer.putShort(1.toShort())      // 1 Soru
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())

        for (part in host.split(".")) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            buffer.put(bytes.size.toByte())
            buffer.put(bytes)
        }
        buffer.put(0.toByte())
        buffer.putShort(1.toShort()) // Type A
        buffer.putShort(1.toShort()) // Class IN

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }

    private fun parseDnsResponse(data: ByteArray, length: Int): InetAddress? {
        try {
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

    private fun closeQuietly(closeable: Closeable?) {
        try { closeable?.close() } catch (_: Exception) {}
    }
}