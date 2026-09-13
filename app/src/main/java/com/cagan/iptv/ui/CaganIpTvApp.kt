package com.cagan.iptv.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cagan.iptv.data.M3uParser
import com.cagan.iptv.data.PlaylistRepository
import com.cagan.iptv.data.PlaylistResult
import com.cagan.iptv.model.Channel
import kotlinx.coroutines.launch

private val Bg = Color(0xFF050A12)
private val Panel = Color(0xFF0D1520)
private val Panel2 = Color(0xFF111D2B)
private val Accent = Color(0xFF56D6FF)
private val Soft = Color(0xFF8FA1AF)

@Composable
fun CaganIpTvApp() {
    val context = LocalContext.current
    val repository = remember { PlaylistRepository(context) }
    val scope = rememberCoroutineScope()

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var addMode by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var sourceName by remember { mutableStateOf("") }

    fun applyResult(result: PlaylistResult) {
        channels = result.channels
        sourceName = result.sourceName + if (result.fromCache) " (önbellek)" else ""
        addMode = false
        startupError = null
    }

    LaunchedEffect(Unit) {
        try {
            applyResult(repository.loadStartup())
        } catch (e: Exception) {
            startupError = e.message ?: "Playlist yüklenemedi."
            addMode = true
        } finally {
            initialLoading = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent)
    ) {
        when {
            initialLoading -> LoadingScreen("Playlist hazırlanıyor...")
            selectedChannel != null -> PlayerScreen(selectedChannel!!, onBack = { selectedChannel = null })
            addMode -> AddPlaylistScreen(
                canGoBack = channels.isNotEmpty(),
                initialError = startupError,
                onLoadUser = { url -> repository.loadUserPlaylist(url) },
                onUseDefault = { repository.useRemoteDefault() },
                onLoaded = { applyResult(it) },
                onBack = { addMode = false }
            )
            else -> HomeScreen(
                channels = channels,
                sourceName = sourceName,
                onPlay = { selectedChannel = it },
                onChangePlaylist = { addMode = true }
            )
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text(message, color = Soft)
        }
    }
}

@Composable
private fun AddPlaylistScreen(
    canGoBack: Boolean,
    initialError: String?,
    onLoadUser: suspend (String) -> PlaylistResult,
    onUseDefault: suspend () -> PlaylistResult,
    onLoaded: (PlaylistResult) -> Unit,
    onBack: () -> Unit
) {
    var playlistUrl by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember(initialError) { mutableStateOf(initialError) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(720.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(Modifier.padding(36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LiveTv, null, tint = Accent, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Cagan IP TV", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Android TV IPTV Player", color = Soft)
                    }
                }

                Text("Playlist Kaynağı", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text("Uygulama açılışta kullanıcı listenizi, sonra repo varsayılanlarını, son olarak önbelleği dener.", color = Soft, fontSize = 13.sp)

                OutlinedTextField(
                    value = playlistUrl,
                    onValueChange = { playlistUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Manuel M3U / M3U8 URL") },
                    placeholder = { Text("https://.../playlist.m3u") }
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = playlistUrl.isNotBlank() && !loading,
                        onClick = {
                            scope.launch {
                                loading = true; error = null
                                try { onLoaded(onLoadUser(playlistUrl.trim())) }
                                catch (e: Exception) { error = "Playlist yüklenemedi: ${e.message ?: "Bilinmeyen hata"}" }
                                loading = false
                            }
                        }
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "Yükleniyor..." else "Manuel Listeyi Kullan")
                    }

                    FilledTonalButton(
                        enabled = !loading,
                        onClick = {
                            scope.launch {
                                loading = true; error = null
                                try { onLoaded(onUseDefault()) }
                                catch (e: Exception) { error = "Varsayılan liste yüklenemedi: ${e.message ?: "Bilinmeyen hata"}" }
                                loading = false
                            }
                        }
                    ) { Text("Varsayılan Liste") }

                    if (canGoBack) TextButton(onClick = onBack) { Text("Geri") }
                }

                Text("Yalnızca erişim/yayın hakkınız bulunan listeleri kullanın.", color = Soft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    channels: List<Channel>,
    sourceName: String,
    onPlay: (Channel) -> Unit,
    onChangePlaylist: () -> Unit
) {

    val groups =
        remember(channels) {
            channels
                .groupBy { it.group }
                .toSortedMap()
        }

    var selectedGroup by remember(channels) {
        mutableStateOf("Tüm Kanallar")
    }

    val visibleChannels =
        if (selectedGroup == "Tüm Kanallar")
            channels
        else
            groups[selectedGroup].orEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {

        Column(
            modifier = Modifier
                .width(270.dp)
                .fillMaxHeight()
                .background(Panel)
                .padding(20.dp)
        ) {

            Text(
                "Cagan",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "IP TV",
                fontSize = 20.sp,
                color = Accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(28.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    selectedGroup = "Tüm Kanallar"
                }
            ) {
                Text("Tüm Kanallar")
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                groups.keys.forEach { group ->

                    item {

                        FilledTonalButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                selectedGroup = group
                            }
                        ) {
                            Text(
                                group,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onChangePlaylist
            ) {

                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Playlist Değiştir")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {

            Text(
                selectedGroup,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "${visibleChannels.size} kanal" + if (sourceName.isNotBlank()) " • $sourceName" else "",
                color = Soft
            )

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(230.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                items(
                    items = visibleChannels,
                    key = { it.url }
                ) { channel ->

                    Card(
                        onClick = {
                            onPlay(channel)
                        },
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Panel2
                            ),
                        shape =
                            RoundedCornerShape(18.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .height(95.dp),
                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            Icon(
                                Icons.Rounded.LiveTv,
                                contentDescription = null,
                                tint = Accent
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                channel.name,
                                fontWeight =
                                    FontWeight.SemiBold,
                                maxLines = 1
                            )

                            Text(
                                channel.group,
                                color = Soft,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val player =
        remember(channel.url) {

            ExoPlayer
                .Builder(context)
                .build()
                .apply {

                    setMediaItem(
                        MediaItem.fromUri(
                            channel.url
                        )
                    )

                    prepare()
                    playWhenReady = true
                }
        }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            modifier =
                Modifier.fillMaxSize(),

            factory = {

                PlayerView(it).apply {

                    this.player = player

                    useController = true

                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                }
            }
        )

        Button(
            onClick = onBack,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
        ) {
            Text("← ${channel.name}")
        }
    }
}
