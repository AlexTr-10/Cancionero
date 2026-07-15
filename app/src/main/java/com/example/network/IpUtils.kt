package com.example.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object IpUtils {
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                // Ignore loopback and inactive interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Desconocida (Sin Wi-Fi)"
    }
}
