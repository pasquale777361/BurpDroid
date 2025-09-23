package com.anon404.burpdroid

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class ProxyService : Service() {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val proxyPort = 8080

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            return START_STICKY
        }

        isRunning = true
        RequestLogger.addLog("Proxy service started on port $proxyPort")

        thread {
            try {
                serverSocket = ServerSocket(proxyPort)
                while (isRunning) {
                    val clientSocket = serverSocket!!.accept()
                    thread {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: IOException) {
                if (isRunning) { // Don't log error if socket was closed intentionally
                    e.printStackTrace()
                    RequestLogger.addLog("Proxy server error: ${e.message}")
                }
            }
        }

        return START_STICKY
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            val clientInputStream = clientSocket.getInputStream()
            val clientOutput = clientSocket.getOutputStream()

            // We can't use bufferedReader here as it might read past the headers
            val headers = mutableListOf<String>()
            var line: String
            val firstLine = readLine(clientInputStream)
            if (firstLine.isNullOrEmpty()) {
                clientSocket.close()
                return
            }
            headers.add(firstLine)
            RequestLogger.addLog(">> $firstLine")

            while (readLine(clientInputStream).also { line = it!! }.isNotEmpty()) {
                headers.add(line)
            }

            var host: String? = null
            var port = 80
            var contentLength = 0

            for (header in headers) {
                if (header.lowercase().startsWith("host:")) {
                    val hostParts = header.substring(5).trim().split(":")
                    host = hostParts[0]
                    if (hostParts.size > 1) {
                        port = hostParts[1].toIntOrNull() ?: 80
                    }
                } else if (header.lowercase().startsWith("content-length:")) {
                    contentLength = header.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            if (host == null) {
                clientSocket.close()
                return
            }

            val targetSocket = Socket(host, port)
            val targetOutput = targetSocket.getOutputStream()
            val targetInput = targetSocket.getInputStream()

            // Write headers to target
            for (header in headers) {
                targetOutput.write((header + "\r\n").toByteArray())
            }
            targetOutput.write("\r\n".toByteArray())
            targetOutput.flush()

            // Write request body if it exists
            if (contentLength > 0) {
                val bodyBuffer = ByteArray(contentLength)
                var bytesRead = 0
                while (bytesRead < contentLength) {
                    val read = clientInputStream.read(bodyBuffer, bytesRead, contentLength - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }
                targetOutput.write(bodyBuffer, 0, bytesRead)
                targetOutput.flush()
            }

            // Relay response back to client
            val responseBuffer = ByteArray(4096)
            var responseBytesRead: Int
            while (targetInput.read(responseBuffer).also { responseBytesRead = it } != -1) {
                clientOutput.write(responseBuffer, 0, responseBytesRead)
                clientOutput.flush()
            }

            targetSocket.close()
            clientSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            RequestLogger.addLog("Error handling client: ${e.message}")
        }
    }

    // Helper function to read a line from an InputStream
    private fun readLine(inputStream: java.io.InputStream): String? {
        val line = StringBuilder()
        while (true) {
            val nextByte = inputStream.read()
            if (nextByte == -1) {
                return if (line.isEmpty()) null else line.toString()
            }
            val c = nextByte.toChar()
            if (c == '\r') {
                // Handle CRLF, peek at the next character
                inputStream.mark(1)
                if (inputStream.read().toChar() != '\n') {
                    inputStream.reset()
                }
                break
            } else if (c == '\n') {
                break
            }
            line.append(c)
        }
        return line.toString()
    }


    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            serverSocket?.close()
            RequestLogger.addLog("Proxy service stopped.")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
