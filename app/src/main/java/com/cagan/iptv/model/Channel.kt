package com.cagan.iptv.model

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Diğer",
    val logoUrl: String? = null
)
