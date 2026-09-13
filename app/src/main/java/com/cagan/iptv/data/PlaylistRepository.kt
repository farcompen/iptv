package com.cagan.iptv.data

import android.content.Context
import com.cagan.iptv.BuildConfig
import com.cagan.iptv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PlaylistResult(
    val channels: List<Channel>,
    val sourceName: String,
    val sourceUrl: String? = null,
    val fromCache: Boolean = false
)

class PlaylistRepository(context: Context) {
    private val storage = PlaylistStorage(context.applicationContext)

    suspend fun loadStartup(): PlaylistResult = withContext(Dispatchers.IO) {
        val userUrl = storage.userPlaylistUrl
        if (!userUrl.isNullOrBlank()) {
            runCatching { loadAndCache(userUrl, "Kullanıcı Listesi") }.getOrNull()?.let { return@withContext it }
        }

        val configUrl = BuildConfig.DEFAULT_CONFIG_URL.trim()
        if (configUrl.isNotBlank()) {
            runCatching { loadRemoteDefaults(configUrl) }.getOrNull()?.let { return@withContext it }
        }

        loadCache() ?: throw IllegalStateException(
            if (configUrl.isBlank())
                "Varsayılan repo adresi yapılandırılmamış. Manuel M3U ekleyin veya CAGAN_CONFIG_URL tanımlayın."
            else "Kullanılabilir playlist bulunamadı."
        )
    }

    suspend fun loadUserPlaylist(url: String): PlaylistResult = withContext(Dispatchers.IO) {
        val result = loadAndCache(url.trim(), "Kullanıcı Listesi")
        storage.userPlaylistUrl = url.trim()
        result
    }

    suspend fun useRemoteDefault(): PlaylistResult = withContext(Dispatchers.IO) {
        storage.clearUserPlaylist()
        val configUrl = BuildConfig.DEFAULT_CONFIG_URL.trim()
        if (configUrl.isBlank()) throw IllegalStateException("Varsayılan repo adresi yapılandırılmamış.")
        runCatching { loadRemoteDefaults(configUrl) }.getOrElse {
            loadCache() ?: throw it
        }
    }

    private fun loadRemoteDefaults(configUrl: String): PlaylistResult {
        val json = PlaylistLoader.loadBlocking(configUrl)
        val config = parseConfig(json)
        val enabled = config.playlists.filter { it.enabled && it.url.isNotBlank() }
        if (enabled.isEmpty()) throw IllegalStateException("Config içinde aktif playlist yok.")

        var lastError: Throwable? = null
        for (playlist in enabled) {
            try {
                return loadAndCacheBlocking(playlist.url, playlist.name.ifBlank { "Varsayılan Liste" })
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Varsayılan playlistler yüklenemedi.")
    }

    private suspend fun loadAndCache(url: String, name: String): PlaylistResult = withContext(Dispatchers.IO) {
        loadAndCacheBlocking(url, name)
    }

    private fun loadAndCacheBlocking(url: String, name: String): PlaylistResult {
        val text = PlaylistLoader.loadBlocking(url)
        val channels = M3uParser.parse(text)
        if (channels.isEmpty()) throw IllegalStateException("Playlist içinde kanal bulunamadı.")
        storage.cachedPlaylistText = text
        storage.cachedSourceName = name
        return PlaylistResult(channels, name, url, false)
    }

    private fun loadCache(): PlaylistResult? {
        val text = storage.cachedPlaylistText ?: return null
        val channels = M3uParser.parse(text)
        if (channels.isEmpty()) return null
        return PlaylistResult(channels, storage.cachedSourceName ?: "Önbellek", fromCache = true)
    }

    private fun parseConfig(text: String): PlaylistConfig {
        val root = JSONObject(text)
        val array = root.optJSONArray("playlists") ?: throw IllegalStateException("Config playlists alanı bulunamadı.")
        val items = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(RemotePlaylist(
                    name = item.optString("name", "Varsayılan Liste"),
                    url = item.optString("url", ""),
                    enabled = item.optBoolean("enabled", true)
                ))
            }
        }
        return PlaylistConfig(root.optInt("version", 1), items)
    }
}
