package com.srinivaskannan.divyaprabhandham.remote

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.DivyaPrabhandhamApp
import com.srinivaskannan.divyaprabhandham.data.AmazonWork
import com.srinivaskannan.divyaprabhandham.media.AmazonMusicLauncher
import kotlinx.coroutines.delay

/**
 * One recitation to try starting. [title] is the track's title exactly as
 * Amazon Music's session reported it on the first run of this test.
 */
private class TestTrack(val label: String, val album: String, val asin: String, val title: String) {
    /** The form Amazon Music's own session uses for a track ID. */
    val asinUri: String get() = "asin://$asin/"

    /** A voice-search style request for exactly this song, as an assistant would send it. */
    val exactSearch: Bundle get() = songSearch(title, ARTIST, ALBUM)

    val request: PlayRequest get() = PlayRequest(label, asin, title, exactSearch)
}

private const val ARTIST = "N S Ranganathan"
private const val ALBUM = "Nalayira Divyaprabandham: Mudalaayiram"

private fun songSearch(title: String, artist: String? = null, album: String? = null) = Bundle().apply {
    putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
    putString(MediaStore.EXTRA_MEDIA_TITLE, title)
    artist?.let { putString(MediaStore.EXTRA_MEDIA_ARTIST, it) }
    album?.let { putString(MediaStore.EXTRA_MEDIA_ALBUM, it) }
}

/** The same kind of request, for a whole album. */
private val albumSearch: Bundle get() = albumSearch(ALBUM, ARTIST)

private fun albumSearch(album: String, artist: String? = null) = Bundle().apply {
    putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
    putString(MediaStore.EXTRA_MEDIA_ALBUM, album)
    artist?.let { putString(MediaStore.EXTRA_MEDIA_ARTIST, it) }
}

/** As the queue was titled after a hand-off to it; its artist has not been seen. */
private const val THIRUVAAYMOZHI_ALBUM = "Nalayira Divyaprabandham: Thiruvaimozhi"

private val testTracks = listOf(
    TestTrack("Thiruppaavai", "B00DHETYEO", "B00DHEU1ZA", "Thiruppavai"),
    TestTrack("Periyaazhvaar 1st paththu", "B00DHETYEO", "B00DHETYUS", "Periyazhwar Thirumozhi-Mudal Pathu"),
    TestTrack("Periyaazhvaar 2nd paththu", "B00DHETYEO", "B00DHETZA2", "Periyazhwar Thirumozhi-Irndam Pathu"),
    TestTrack("Periyaazhvaar 3rd paththu", "B00DHETYEO", "B00DHETZWA", "Periyazhwar Thirumozhi-Moondram Pathu"),
    TestTrack("Periyaazhvaar 4th paththu", "B00DHETYEO", "B00DHEU0CY", "Periyazhwar Thirumozhi-Nankam Pathu"),
    TestTrack("Periyaazhvaar 5th paththu", "B00DHETYEO", "B00DHEU1E6", "Periyazhwar Thirumozhi-Aindam Pathu"),
)

/** The work the play-in-order test walks through. */
private val paththus = testTracks.drop(1)

/** The one title a search got wrong on the phone: it played the 1st paththu instead. */
private val thirdPaththu = testTracks[3]

/** The rest of the album, so the title check covers every track in it. */
private val albumTitles = testTracks + listOf(
    TestTrack("Naachchiyaar Thirumozhi", "B00DHETYEO", "B00DHEU2LS", "Nachiaar Thirumozhi"),
    TestTrack("Perumaal Thirumozhi", "B00DHETYEO", "B00DHEU308", "Perumaal Thirumozhi"),
    TestTrack("Thiruchchandha Viruththam", "B00DHETYEO", "B00DHEU3DU", "Thiruchanda Virutham"),
    TestTrack("Thirumaalai", "B00DHETYEO", "B00DHEU3V2", "Thirumaalai"),
    TestTrack("Thiruppalliyezhuchchi", "B00DHETYEO", "B00DHEU4A2", "Thirupalliyezhi"),
    TestTrack("Amalanaadhipiraan", "B00DHETYEO", "B00DHEU592", "Amalaathipiran"),
    TestTrack("Kanninun Siruththaambu", "B00DHETYEO", "B00DHEU5PQ", "Kanninum Siruthaambu"),
)

/** Whole albums to open, so their queues can be walked for track IDs. */
private val testAlbums = listOf(
    "Mudalaayiram" to "B00DHETYEO",
    "Periya Thirumozhi" to "B00DIM066U",
    "Thiruvaaymozhi" to "B00DHTZVV4",
    // Not yet driven from the pill: their names, artists and track order are unread.
    "Thiruppallaandu" to "B005HRCYNW",
    // The Iyarpaa album to use (2026-11-16); B00DIYWZ8U was the wrong one.
    "Iyarpaa" to "B00DHGE2RQ",
    "Desika Prabandham" to "B0766455DJ",
)

/**
 * Debug-only test screen for the remote-control pill. Has its own launcher
 * icon ("DP Remote Test"); see [RemoteProbe] for what it is finding out.
 */
class RemoteProbeActivity : ComponentActivity() {

    private lateinit var probe: RemoteProbe
    private var standIn: StandInPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        probe = RemoteProbe(applicationContext)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ProbeScreen(
                        probe = probe,
                        onGrant = ::openAccessSettings,
                        onStandIn = ::startStandIn,
                        onAppStandIn = ::toggleAppStandIn,
                        onHandOff = ::handOff,
                        onAppInfo = ::openAmazonAppInfo,
                        onShare = ::shareReport,
                        onCopy = ::copyReport,
                    )
                }
            }
        }
    }

    // The grant happens in Settings and playback starts in Amazon Music, so
    // coming back is when there is something new to read.
    override fun onResume() {
        super.onResume()
        probe.refresh()
    }

    override fun onDestroy() {
        standIn?.release()
        probe.release()
        super.onDestroy()
    }

    private fun openAccessSettings() {
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                probe.listenerComponent.flattenToString(),
            )
        } else null
        val opened = detail != null && runCatching { startActivity(detail) }.isSuccess
        if (!opened) startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun startStandIn() {
        if (standIn == null) {
            standIn = StandInPlayer(
                this,
                album = ALBUM,
                ids = albumTitles.associate { it.title to it.asin },
                // What Amazon Music did on the phone.
                mistakes = mapOf(thirdPaththu.title to testTracks[1].title),
            )
        }
        probe.note("stand-in player started")
        probe.refresh()
    }

    /** For trying the app's own pill on an emulator: see [AppStandIn]. */
    private fun toggleAppStandIn(shuffled: Boolean) {
        if (AppStandIn.isOn) {
            AppStandIn.stop()
            probe.note("app stand-in stopped")
        } else {
            val ready = (application as DivyaPrabhandhamApp).startup as? DivyaPrabhandhamApp.Startup.Ready
            AppStandIn.start(this, ready?.repository?.amazonAlbums.orEmpty(), shuffled)
            probe.note(
                if (AppStandIn.isOn) "app stand-in started${if (shuffled) ", shuffled" else ""}"
                else "app stand-in: the app has not loaded yet",
            )
        }
        probe.refresh()
    }

    private fun handOff(label: String, album: String, asin: String?) {
        val result = AmazonMusicLauncher.launch(this, AmazonWork(album = album, track = asin))
        probe.note("hand-off $label ($album, $asin) → $result")
    }

    /** Where Force stop lives, for the cold-start test. */
    private fun openAmazonAppInfo() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${RemoteProbe.AMAZON}")),
            )
        }.onFailure { probe.note("could not open Amazon Music's app info: ${it.message}") }
    }

    private fun shareReport() {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "DP remote-control test")
            .putExtra(Intent.EXTRA_TEXT, probe.report())
        startActivity(Intent.createChooser(send, "Share report"))
    }

    private fun copyReport() {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("DP remote-control test", probe.report()))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProbeScreen(
    probe: RemoteProbe,
    onGrant: () -> Unit,
    onStandIn: () -> Unit,
    onAppStandIn: (shuffled: Boolean) -> Unit,
    onHandOff: (label: String, album: String, asin: String?) -> Unit,
    onAppInfo: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Remote-control pill test", style = MaterialTheme.typography.headlineSmall)

        Section("1. Notification access") {
            if (probe.hasAccess) {
                Text("Granted. Sessions: ${probe.sessions.ifEmpty { listOf("none playing") }.joinToString()}")
                if (!probe.isInstalled(RemoteProbe.AMAZON)) {
                    Text(
                        "Amazon Music is not installed, so any other player stands in.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = onStandIn) { Text("Start a stand-in player") }
                }
            } else {
                Text(
                    "Not granted. Android only shows other apps' players to an app with " +
                        "Notification access. This test reads no notifications.",
                )
                Button(onClick = onGrant) { Text("Open Settings to grant") }
            }
            // Offered before the grant too, so the app's own way of asking can be tried.
            if (!probe.isInstalled(RemoteProbe.AMAZON)) {
                OutlinedButton(onClick = { onAppStandIn(false) }) {
                    Text("Stand in for Amazon Music in the app (on/off)")
                }
                OutlinedButton(onClick = { onAppStandIn(true) }) {
                    Text("The same, playing albums out of order (on/off)")
                }
            }
        }

        Section("2. The pill") {
            Pill(probe)
        }

        Section("3. Start a track") {
            Text(
                "Hand-off opens Amazon Music, as the app does today. Search exact asks " +
                    "Amazon Music to start the track while you stay here.",
                style = MaterialTheme.typography.bodySmall,
            )
            testTracks.forEach { track ->
                Text(track.label, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onHandOff(track.label, track.album, track.asin) }) { Text("Hand-off") }
                    OutlinedButton(onClick = { probe.playFromSearch(track.title, track.exactSearch) }) {
                        Text("Search exact")
                    }
                }
            }
        }

        Section("4. The album as a queue") {
            Text(
                "Asks Amazon Music for the whole album instead of one track. If a queue " +
                    "appears in section 9, the paththus can follow each other by themselves. " +
                    "The third button starts the album and then jumps to the 3rd paththu.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { probe.playFromSearch(ALBUM, albumSearch) }) { Text("Album exact") }
                OutlinedButton(onClick = { probe.playFromSearch("$ALBUM $ARTIST") }) { Text("Album by name") }
                Button(onClick = { probe.playInAlbum(thirdPaththu.request, ALBUM, albumSearch) }) {
                    Text("Album, then 3rd paththu")
                }
            }
        }

        Section("5. Reaching a track with next") {
            Text(
                "An album start plays in order but shows no queue, so the only way to a " +
                    "track inside it is pressing next. Each button starts the album and " +
                    "presses next until that track shows, checking its ID. Quiet pauses " +
                    "first, so the tracks passed should not be heard. Walk goes through " +
                    "the whole album and writes down its order. Afterwards, try the " +
                    "pill's own next and previous buttons.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (probe.stepping) {
                Button(onClick = probe::stopStepping) { Text("Stop stepping") }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { probe.stepThroughAlbum(ALBUM, albumSearch, thirdPaththu.request, false) }) {
                        Text("To 3rd paththu")
                    }
                    Button(onClick = { probe.stepThroughAlbum(ALBUM, albumSearch, testTracks[0].request, false) }) {
                        Text("To Thiruppaavai")
                    }
                    Button(onClick = { probe.stepThroughAlbum(ALBUM, albumSearch, testTracks[0].request, true) }) {
                        Text("Quiet: to Thiruppaavai")
                    }
                    Button(onClick = { probe.stepThroughAlbum(ALBUM, albumSearch, albumTitles.last().request, true) }) {
                        Text("Quiet: to the last track")
                    }
                    OutlinedButton(onClick = { probe.stepThroughAlbum(ALBUM, albumSearch, null, true) }) {
                        Text("Walk Mudalaayiram")
                    }
                    OutlinedButton(onClick = {
                        probe.stepThroughAlbum(THIRUVAAYMOZHI_ALBUM, albumSearch(THIRUVAAYMOZHI_ALBUM), null, true)
                    }) { Text("Walk Thiruvaaymozhi") }
                }
            }
        }

        Section("6. Check every title") {
            Text(
                "Search exact played the 1st paththu when asked for the 3rd. This asks for " +
                    "each of the album's ${albumTitles.size} tracks for five seconds and records which " +
                    "ones come back right. The last two buttons ask for the 3rd paththu with " +
                    "less detail.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (probe.checking) {
                    Button(onClick = probe::stopChecking) { Text("Stop checking") }
                } else {
                    Button(onClick = { probe.checkTitles(albumTitles.map { it.request }) }) {
                        Text("Check all ${albumTitles.size} titles")
                    }
                }
                OutlinedButton(onClick = {
                    probe.playFromSearch(thirdPaththu.title, songSearch(thirdPaththu.title))
                }) { Text("3rd: title only") }
                OutlinedButton(onClick = {
                    probe.playFromSearch(thirdPaththu.title, songSearch(thirdPaththu.title, ARTIST))
                }) { Text("3rd: title + artist") }
            }
        }

        Section("7. When a track ends") {
            Text(
                "Jump to last 15 s skips to the end of whatever is playing. After an album " +
                    "start, use it to see whether the next track follows by itself.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = probe::seekNearEnd) { Text("Jump to last 15 s") }
                if (probe.chain.isEmpty()) {
                    Button(onClick = { probe.playInOrder(paththus.map { it.request }) }) {
                        Text("Play the five paththus in order")
                    }
                } else {
                    Button(onClick = probe::stopChain) { Text("Stop playing in order") }
                }
            }
            if (probe.chain.isNotEmpty()) {
                Text("In order: " + probe.chain.joinToString(" → ") { it.label.substringAfter(' ') })
            }
        }

        Section("8. When Amazon Music is not running") {
            Text(
                "First swipe Amazon Music away from the recent apps, come back and tap " +
                    "Cold start. Then repeat after a Force stop from its app info page. " +
                    "Cold start tries each wake below in turn; the last one is a headset-style " +
                    "play press, which may briefly start another player.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { probe.coldStart(testTracks.first().request) }) {
                    Text("Cold start: ${testTracks.first().label}")
                }
                OutlinedButton(onClick = onAppInfo) { Text("Amazon Music app info") }
            }
            Text("One wake at a time", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = probe::probeBrowser) { Text("Wake: bind") }
                OutlinedButton(onClick = probe::wakeByBroadcast) { Text("Wake: broadcast") }
                OutlinedButton(onClick = probe::wakeByMediaKey) { Text("Wake: media key") }
            }
        }

        Section("9. The queue") {
            Text(
                "Tap an entry to jump to it without leaving this screen. Collect IDs plays " +
                    "each entry for four seconds and writes its track ID to the log. " +
                    "For a new album: open it, come back, collect, then search for it.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Open an album", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                testAlbums.forEach { (label, album) ->
                    Button(onClick = { onHandOff(label, album, null) }) { Text(label) }
                }
            }
            // Whether a search for the album as the session names it finds
            // that album again: what the app's pill would send.
            val album = probe.now?.album
            if (album != null) {
                OutlinedButton(onClick = {
                    probe.playFromSearch(album, albumSearch(album, probe.now?.artist))
                }) { Text("Search for this album") }
            }
            if (probe.queue.isEmpty()) {
                Text("Nothing queued.")
            } else {
                OutlinedButton(onClick = if (probe.collecting) probe::stopCollecting else probe::collectQueue) {
                    Text(if (probe.collecting) "Stop collecting" else "Collect IDs for these ${probe.queue.size}")
                }
                probe.queue.forEach { entry ->
                    val playing = entry.id == probe.now?.queueItemId
                    Text(
                        text = (if (playing) "▶ " else "") + entry.title,
                        fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { probe.skipTo(entry) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
            if (probe.seen.isNotEmpty()) {
                Text("Track IDs seen", fontWeight = FontWeight.SemiBold)
                SelectionContainer {
                    Text(
                        probe.seen.joinToString("\n") { (id, title) -> "$id  $title" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Section("10. Report") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onShare) { Text("Share report") }
                OutlinedButton(onClick = onCopy) { Text("Copy") }
                OutlinedButton(onClick = probe::dump) { Text("Dump now") }
                OutlinedButton(onClick = probe::clearLog) { Text("Clear log") }
            }
            SelectionContainer {
                Text(
                    text = probe.log.asReversed().take(60).joinToString("\n").ifEmpty { "Nothing logged yet." },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

/** A stand-in for the mini-player pill, wired to whatever session is followed. */
@Composable
private fun Pill(probe: RemoteProbe) {
    val now = probe.now
    if (now == null) {
        Text("No player session yet. Start something in Amazon Music, then come back.")
        return
    }
    var position by remember { mutableLongStateOf(now.livePositionMs()) }
    LaunchedEffect(now) {
        while (true) {
            position = now.livePositionMs()
            delay(500)
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                now.title ?: "(no title)",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(now.artist, now.album).joinToString(" — ").ifEmpty { now.packageName },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (now.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / now.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            Text(
                "${clock(position)} / ${if (now.durationMs > 0) clock(now.durationMs) else "--:--"}" +
                    "   id: ${now.mediaId ?: "none"}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(Icons.Filled.SkipPrevious, "Previous", probe::previous)
                PillButton(Icons.Filled.Replay30, "Back 30 seconds") { probe.seekBy(-30_000) }
                FilledIconButton(onClick = probe::playPause, modifier = Modifier.size(52.dp)) {
                    Icon(
                        if (now.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (now.isPlaying) "Pause" else "Play",
                    )
                }
                PillButton(Icons.Filled.Forward30, "Forward 30 seconds") { probe.seekBy(30_000) }
                PillButton(Icons.Filled.SkipNext, "Next", probe::next)
            }
        }
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
}

private fun clock(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    val (h, m, s) = Triple(total / 3600, total % 3600 / 60, total % 60)
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
