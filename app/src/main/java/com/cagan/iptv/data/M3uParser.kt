package com.cagan.iptv.data

import com.cagan.iptv.model.Channel

object M3uParser {

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        var currentName = ""
        var currentGroup = "Diğer"
        var currentLogo: String? = null

        text.lineSequence().forEach { raw ->
            val line = raw.trim()

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    currentName = line.substringAfterLast(",").trim().ifBlank { "Kanal" }

                    currentGroup =
                        Regex("""group-title="([^"]*)"""")
                            .find(line)
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.ifBlank { "Diğer" }
                            ?: "Diğer"

                    currentLogo =
                        Regex("""tvg-logo="([^"]*)"""")
                            .find(line)
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.ifBlank { null }
                }

                line.isNotBlank() && !line.startsWith("#") -> {
                    channels += Channel(
                        name = currentName.ifBlank { "Kanal" },
                        url = line,
                        group = currentGroup,
                        logoUrl = currentLogo
                    )

                    currentName = ""
                    currentGroup = "Diğer"
                    currentLogo = null
                }
            }
        }

        return channels
    }
}
