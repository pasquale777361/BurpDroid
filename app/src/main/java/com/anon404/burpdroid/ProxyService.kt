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
            val clientInput = clientSocket.getInputStream().bufferedReader()
            val clientOutput = clientSocket.getOutputStream()

            val requestLine = clientInput.readLine()
            if (requestLine.isNullOrEmpty()) {
                clientSocket.close()
                return
            }

            RequestLogger.addLog(">> $requestLine")

            var host: String? = null
            var port = 80
            val requestBuilder = StringBuilder("$requestLine\r\n")
            var header: String?
            while (clientInput.readLine().also { header = it } != null && header!!.isNotEmpty()) {
                requestBuilder.append("$header\r\n")
                if (header!!.lowercase().startsWith("host:")) {
                    val hostParts = header!!.substring(5).trim().split(":")
                    host = hostParts[0]
                    if (hostParts.size > 1) {
                        port = hostParts[1].toIntOrNull() ?: 80
                    }
                }
            }
            requestBuilder.append("\r\n")

            if (host == null) {
                clientSocket.close()
                return
            }

            val targetSocket = Socket(host, port)
            val targetOutput = targetSocket.getOutputStream()
            val targetInput = targetSocket.getInputStream()

            targetOutput.write(requestBuilder.toString().toByteArray())
            targetOutput.flush()

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (targetInput.read(buffer).also { bytesRead = it } != -1) {
                clientOutput.write(buffer, 0, bytesRead)
                clientOutput.flush()
            }

            targetSocket.close()
            clientSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            RequestLogger.addLog("Error handling client: ${e.message}")
        }
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
