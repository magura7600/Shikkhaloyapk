package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.HttpURLConnection

object NetworkUtils {

    /**
     * Checks if the device has a network interface active (WiFi, Cellular, Ethernet etc.).
     * This is a quick local check.
     */
    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = cm.activeNetwork ?: return false
                    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    @Suppress("DEPRECATION")
                    val activeNetworkInfo = cm.activeNetworkInfo
                    activeNetworkInfo != null && activeNetworkInfo.isConnected
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the device has actual, functional Internet access by opening a lightweight socket connection.
     * This avoids false-positives where the user is connected to a WiFi/router but has no internet service.
     */
    suspend fun hasActualInternetAccess(context: Context): Boolean {
        if (!isNetworkConnected(context)) return false
        return withContext(Dispatchers.IO) {
            try {
                // Try connecting to Google DNS (8.8.8.8) on port 53 (DNS) with a short timeout.
                // This is extremely fast and reliable.
                val timeoutMs = 2500
                val socket = Socket()
                val socketAddress = InetSocketAddress("8.8.8.8", 53)
                socket.connect(socketAddress, timeoutMs)
                socket.close()
                true
            } catch (e: Exception) {
                try {
                    // Fallback to lightweight HTTP HEAD request
                    val url = URL("https://www.google.com")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 2500
                    connection.readTimeout = 2500
                    connection.requestMethod = "HEAD"
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    responseCode in 200..399
                } catch (ex: Exception) {
                    false
                }
            }
        }
    }
}
