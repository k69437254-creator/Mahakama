package com.aram.mahakama.audio

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

object AudarConnectionTest {
    fun run() {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL("http://127.0.0.1:18765/health")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val status = connection.responseCode
                val body = connection.inputStream.bufferedReader().use { it.readText() }

                Log.i("AUDAR_BRIDGE_TEST", "HTTP $status | $body")
            } catch (error: Exception) {
                Log.e("AUDAR_BRIDGE_TEST", "CONNECTION FAILED", error)
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
