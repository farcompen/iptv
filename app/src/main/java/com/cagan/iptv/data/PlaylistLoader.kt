package com.cagan.iptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object PlaylistLoader {
    suspend fun load(url: String): String = withContext(Dispatchers.IO) { loadBlocking(url) }

    fun loadBlocking(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Cagan-IP-TV/0.2")
            connection.setRequestProperty("Accept", "application/json, application/x-mpegURL, text/plain, */*")
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
