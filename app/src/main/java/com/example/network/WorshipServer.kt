package com.example.network

import android.util.Log
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorshipServer(
    private val port: Int = 9876,
    private val onClientCountChanged: (Int) -> Unit
) {
    private val TAG = "WorshipServer"
    private var serverSocket: ServerSocket? = null
    private val clients = Collections.synchronizedList(mutableListOf<ClientHandler>())
    private var executorService: ExecutorService? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        executorService = Executors.newCachedThreadPool()
        
        executorService?.execute {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Server started on port $port")
                
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    socket.keepAlive = true // Enable TCP Keep-Alive
                    Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                    
                    val handler = ClientHandler(socket)
                    clients.add(handler)
                    updateClientCount()
                    executorService?.execute(handler)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            } finally {
                stop()
            }
        }
    }

    fun broadcast(message: String) {
        val executor = executorService
        if (executor != null && !executor.isShutdown) {
            executor.execute {
                synchronized(clients) {
                    val iterator = clients.iterator()
                    while (iterator.hasNext()) {
                        val client = iterator.next()
                        if (client.sendMessage(message)) {
                            Log.d(TAG, "Sent message to client: $message")
                        } else {
                            Log.d(TAG, "Failed to send, removing client")
                            client.close()
                            iterator.remove()
                        }
                    }
                }
                updateClientCount()
            }
        } else {
            Log.w(TAG, "Cannot broadcast, executorService is null or shutdown")
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        Log.d(TAG, "Stopping server...")
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        
        synchronized(clients) {
            for (client in clients) {
                client.close()
            }
            clients.clear()
        }
        updateClientCount()
        
        executorService?.shutdownNow()
        executorService = null
        Log.d(TAG, "Server stopped.")
    }

    private fun updateClientCount() {
        onClientCountChanged(clients.size)
    }

    private inner class ClientHandler(private val socket: Socket) : Runnable {
        private var writer: PrintWriter? = null
        private var isClosed = false

        override fun run() {
            try {
                writer = PrintWriter(socket.getOutputStream(), true)
                val reader = socket.getInputStream().bufferedReader()
                
                // Keep reading from client just to detect disconnect or handle keep-alives
                while (isRunning && !isClosed) {
                    val line = reader.readLine() ?: break
                    Log.d(TAG, "Received from client: $line")
                    if (line == "PING") {
                        sendMessage("PONG")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client connection error: ${e.message}")
            } finally {
                close()
                synchronized(clients) {
                    clients.remove(this)
                }
                updateClientCount()
            }
        }

        fun sendMessage(msg: String): Boolean {
            val w = writer ?: return false
            return try {
                synchronized(w) {
                    w.println(msg)
                    w.flush()
                    !w.checkError()
                }
            } catch (e: Exception) {
                false
            }
        }

        fun close() {
            if (isClosed) return
            isClosed = true
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
