package com.example.network

import android.util.Log
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class WorshipClient(
    private val onMessageReceived: (String) -> Unit,
    private val onStatusChanged: (ConnectionStatus) -> Unit
) {
    private val TAG = "WorshipClient"
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var isRunning = false
    private val executor = Executors.newSingleThreadExecutor()

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    fun connect(host: String, port: Int = 9876) {
        if (isRunning) return
        isRunning = true
        onStatusChanged(ConnectionStatus.CONNECTING)

        executor.execute {
            try {
                socket = Socket().apply {
                    keepAlive = true
                }
                socket?.connect(InetSocketAddress(host, port), 5000)
                writer = PrintWriter(socket?.getOutputStream() ?: throw Exception("No output stream"), true)
                val reader = (socket?.getInputStream() ?: throw Exception("No input stream")).bufferedReader()
                
                onStatusChanged(ConnectionStatus.CONNECTED)
                Log.d(TAG, "Connected to $host:$port")

                while (isRunning) {
                    val line = reader.readLine() ?: break
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
        executor.execute {
            try {
                writer?.println("PING")
                writer?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        if (!isRunning) return
        isRunning = false
        Log.d(TAG, "Disconnecting client...")
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
        writer = null
        onStatusChanged(ConnectionStatus.DISCONNECTED)
        Log.d(TAG, "Client disconnected.")
    }
}
