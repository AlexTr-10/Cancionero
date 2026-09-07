package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class WorshipClient(
    private val onMessageReceived: (String) -> Unit,
    private val onStatusChanged: (ConnectionStatus) -> Unit
) {
    companion object {
        private const val TAG = "WorshipClient"
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var clientScope: CoroutineScope? = null

    @Volatile
    private var isRunning = false

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    fun connect(host: String, port: Int = 9876, memberName: String = "Integrante") {
        if (isRunning) return
        isRunning = true
        onStatusChanged(ConnectionStatus.CONNECTING)

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        clientScope = scope

        scope.launch {
            try {
                val newSocket = Socket().apply {
                    keepAlive = true
                    tcpNoDelay = true
                }
                socket = newSocket
                newSocket.connect(InetSocketAddress(host, port), 5000)

                val outputStream = newSocket.getOutputStream()
                val newWriter = PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)
                writer = newWriter

                val inputStream = newSocket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))

                onStatusChanged(ConnectionStatus.CONNECTED)
                Log.i(TAG, "Connected to leader at $host:$port")

                // Send initial greeting / identification packet to the Director
                val nameToSend = if (memberName.trim().isNotBlank()) memberName.trim() else "Integrante"
                val identifyPacket = JSONObject().apply {
                    put("type", "identify")
                    put("name", nameToSend)
                }.toString()

                synchronized(newWriter) {
                    newWriter.println(identifyPacket)
                    newWriter.flush()
                }

                while (isRunning && isActive && newSocket.isConnected && !newSocket.isClosed) {
                    val line = try {
                        reader.readLine()
                    } catch (e: Exception) {
                        Log.d(TAG, "Exception reading from server: ${e.message}")
                        null
                    } ?: break

                    Log.d(TAG, "Client received: $line")
                    try {
                        onMessageReceived(line)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error processing incoming message: ${ex.message}", ex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    fun sendPing() {
        val scope = clientScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                writer?.let { w ->
                    synchronized(w) {
                        w.println("PING")
                        w.flush()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send PING: ${e.message}")
            }
        }
    }

    fun disconnect() {
        if (!isRunning) return
        isRunning = false
        Log.i(TAG, "Disconnecting client...")

        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing client socket: ${e.message}")
        }
        socket = null
        writer = null

        clientScope?.cancel()
        clientScope = null

        onStatusChanged(ConnectionStatus.DISCONNECTED)
        Log.i(TAG, "Client disconnected.")
    }
}
