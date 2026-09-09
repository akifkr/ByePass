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
        var upstream: Socket? = null
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

            if (firstLine.startsWith("CONNECT ")) {
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
            }
        } catch (e: Exception) {
            TunnelService.log("Hata: ${e.message}")
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { upstream?.close() } catch (_: Exception) {}
        }
    }

    private fun resolve(host: String): InetAddress {
        dnsCache[host]?.let { return it }

        return try {
            val query = createDnsQuery(host)
            val socket = DatagramSocket()
            socket.soTimeout = 2500

            val packet = DatagramPacket(query, query.size, InetAddress.getByName("77.88.8.8"), 1253)
            socket.send(packet)

            val respBuf = ByteArray(512)
            val respPacket = DatagramPacket(respBuf, respBuf.size)
            socket.receive(respPacket)
            socket.close()

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