package com.cagan.iptv.data

import android.content.Context

class PlaylistStorage(context: Context) {
    private val prefs = context.getSharedPreferences("cagan_iptv", Context.MODE_PRIVATE)

    var userPlaylistUrl: String?
        get() = prefs.getString("user_playlist_url", null)?.takeIf { it.isNotBlank() }
        set(value) { prefs.edit().putString("user_playlist_url", value.orEmpty()).apply() }

    var cachedPlaylistText: String?
        get() = prefs.getString("cached_playlist_text", null)?.takeIf { it.isNotBlank() }
        set(value) { prefs.edit().putString("cached_playlist_text", value.orEmpty()).apply() }

    var cachedSourceName: String?
        get() = prefs.getString("cached_source_name", null)
        set(value) { prefs.edit().putString("cached_source_name", value).apply() }

    fun clearUserPlaylist() {
        prefs.edit().remove("user_playlist_url").apply()
    }
}
