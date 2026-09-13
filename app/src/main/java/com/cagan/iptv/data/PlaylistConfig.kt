package com.cagan.iptv.data

data class RemotePlaylist(
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

data class PlaylistConfig(
    val version: Int = 1,
    val playlists: List<RemotePlaylist> = emptyList()
)
