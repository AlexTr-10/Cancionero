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
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * High-concurrency TCP Server for Live Worship Sessions.
 *
 * Implements:
 * - Isolated per-client coroutines launched on Dispatchers.IO.
 * - Thread-safe ConcurrentHashMap storing active sockets keyed by remoteSocketAddress (IP:Port).
 * - Persistent read loops while (socket.isConnected && isActive && !socket.isClosed) without premature stream closures.
 * - Granular fault isolation so individual client disconnects never affect other connected members.
 * - Real-time active client count and connected member names updates dispatched cleanly.
 */
class WorshipServer(
    private val port: Int = 9876,
    private val onClientListChanged: ((count: Int, names: List<String>) -> Unit)? = null,
    private val onClientCountChanged: ((Int) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WorshipServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null

    // Thread-safe map of active clients keyed by unique socket.remoteSocketAddress.toString() (IP:Port)
    private val connectedClients = ConcurrentHashMap<String, ConnectedClient>()

    @Volatile
    private var isRunning = false

    private data class ConnectedClient(
        val socket: Socket,
        val writer: PrintWriter,
        @Volatile var memberName: String = "Integrante"
    )

    fun start() {
        if (isRunning) return
        isRunning = true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        serverScope = scope

        scope.launch {
            try {
                // Allow immediate reuse of the port if recently unbound
                val server = ServerSocket(port).apply {
                    reuseAddress = true
                }
                serverSocket = server
                Log.i(TAG, "WorshipServer listening on port $port")

                while (isRunning && isActive) {
                    try {
                        val clientSocket = server.accept()
                        clientSocket.keepAlive = true
                        clientSocket.tcpNoDelay = true

                        val clientKey = clientSocket.remoteSocketAddress.toString()
                        Log.i(TAG, "New connection accepted: $clientKey")

                        // Launch an isolated coroutine per client on Dispatchers.IO
                        scope.launch(Dispatchers.IO) {
                            handleClientConnection(clientSocket, clientKey)
                        }
                    } catch (se: SocketException) {
                        if (!isRunning) {
                            Log.d(TAG, "ServerSocket closed normally: ${se.message}")
                        } else {
                            Log.e(TAG, "SocketException in server accept loop: ${se.message}")
                        }
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Unexpected error accepting client: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ServerSocket on port $port: ${e.message}", e)
            } finally {
                if (isRunning) {
                    stop()
                }
            }
        }
    }

    private fun handleClientConnection(socket: Socket, clientKey: String) {
        var writer: PrintWriter? = null
        try {
            val outputStream = socket.getOutputStream()
            writer = PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)
            val inputStream = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))

            val client = ConnectedClient(socket = socket, writer = writer)
            connectedClients[clientKey] = client
            notifyClientListChanged()
            Log.i(TAG, "Client registered: $clientKey (Total active: ${connectedClients.size})")

            // Active persistent read loop - independent per client, without premature closures
            while (isRunning && socket.isConnected && !socket.isClosed) {
                val line = try {
                    reader.readLine()
                } catch (se: SocketException) {
                    Log.d(TAG, "Client $clientKey connection reset: ${se.message}")
                    null
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading from client $clientKey: ${e.message}")
                    null
                } ?: break // Null indicates end-of-stream (EOF / remote disconnect)

                Log.d(TAG, "Received from client [$clientKey]: $line")
                val trimmed = line.trim()
                if (trimmed.equals("PING", ignoreCase = true)) {
                    sendDirectMessage(client, "PONG")
                } else if (trimmed.startsWith("{")) {
                    try {
                        val json = JSONObject(trimmed)
                        if (json.optString("type") == "identify") {
                            val name = json.optString("name", "Integrante").trim()
                            if (name.isNotEmpty()) {
                                client.memberName = name
                                notifyClientListChanged()
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error parsing client JSON message: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client handler exception for $clientKey: ${e.message}")
        } finally {
            // Clean up ONLY this client without affecting any other member
            removeAndCloseClient(clientKey)
        }
    }

    private fun sendDirectMessage(client: ConnectedClient, message: String): Boolean {
        return try {
            synchronized(client.writer) {
                client.writer.println(message)
                client.writer.flush()
                !client.writer.checkError()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error sending direct message to client: ${e.message}")
            false
        }
    }

    fun broadcast(message: String) {
        val scope = serverScope
        if (!isRunning || scope == null || !scope.isActive) {
            Log.w(TAG, "Cannot broadcast: Server is not running")
            return
        }

        scope.launch(Dispatchers.IO) {
            if (connectedClients.isEmpty()) return@launch

            val failedClients = mutableListOf<String>()

            for ((clientKey, client) in connectedClients) {
                try {
                    val success = sendDirectMessage(client, message)
                    if (!success) {
                        Log.w(TAG, "Broadcast failed to $clientKey, marking for removal")
                        failedClients.add(clientKey)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception broadcasting to $clientKey: ${e.message}")
                    failedClients.add(clientKey)
                }
            }

            if (failedClients.isNotEmpty()) {
                for (failedKey in failedClients) {
                    removeAndCloseClient(failedKey)
                }
            }
        }
    }

    private fun removeAndCloseClient(clientKey: String) {
        val client = connectedClients.remove(clientKey) ?: return
        try {
            if (!client.socket.isClosed) {
                client.socket.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket for $clientKey: ${e.message}")
        }
        Log.i(TAG, "Client disconnected: $clientKey (Remaining: ${connectedClients.size})")
        notifyClientListChanged()
    }

    private fun notifyClientListChanged() {
        val count = connectedClients.size
        val names = connectedClients.values.map { it.memberName }
        try {
            onClientCountChanged?.invoke(count)
            onClientListChanged?.invoke(count, names)
        } catch (e: Exception) {
            Log.e(TAG, "Error in client callbacks: ${e.message}")
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        Log.i(TAG, "Stopping WorshipServer...")

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing serverSocket: ${e.message}")
        }
        serverSocket = null

        // Close all clients gracefully
        val clientsCopy = ArrayList(connectedClients.keys)
        for (clientKey in clientsCopy) {
            val client = connectedClients.remove(clientKey)
            try {
                if (client != null && !client.socket.isClosed) {
                    client.socket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client $clientKey: ${e.message}")
            }
        }
        connectedClients.clear()
        notifyClientListChanged()

        serverScope?.cancel()
        serverScope = null
        Log.i(TAG, "WorshipServer stopped completely.")
    }
}
