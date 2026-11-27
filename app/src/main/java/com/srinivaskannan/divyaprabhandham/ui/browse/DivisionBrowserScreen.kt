package com.srinivaskannan.divyaprabhandham.ui.browse

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.srinivaskannan.divyaprabhandham.data.AmazonWork
import com.srinivaskannan.divyaprabhandham.data.BookSection
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.Paththu
import com.srinivaskannan.divyaprabhandham.media.AmazonMusicLauncher
import com.srinivaskannan.divyaprabhandham.media.LocalAmazonRemote
import com.srinivaskannan.divyaprabhandham.media.LocalRecitationPosition
import com.srinivaskannan.divyaprabhandham.media.RecitationLauncher
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.ui.components.AmazonInstallDialog
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.components.PlayerAccessDialog
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.launch

/**
 * The works of one division, each expanding to show its sections.
 *
 * Long-pressing a work pins it to Home — the Android equivalent of the iOS
 * context menu, and the same gesture, so the muscle memory carries over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionBrowserScreen(
    division: Division,
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val context = LocalContext.current
    val repository = LocalRepository.current
    val works = repository.works(division)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A division with a single work reads better pre-expanded.
    val expanded = remember(division.id) {
        mutableStateListOf<String>().also { list ->
            works?.singleOrNull()?.let { list.add(it.id) }
        }
    }

    // The paththus opened, as "workId#paththu". All start closed, so a long
    // work reads as its paththus first.
    val openPaththus = remember(division.id) { mutableStateListOf<String>() }

    val remote = LocalAmazonRemote.current
    val recitationPosition = LocalRecitationPosition.current
    var showInstall by remember { mutableStateOf(false) }
    // The recitation waiting on an answer about player access: first to the
    // dialog, then — once "Turn on" has opened system Settings — to the switch.
    var askingAccessFor by remember { mutableStateOf<AmazonWork?>(null) }
    var awaitingAccessFor by remember { mutableStateOf<AmazonWork?>(null) }
    // The section heard first in what is waiting, to open when it plays.
    var pendingReader by remember { mutableStateOf<String?>(null) }

    // Opens a work, or one paththu of it, in Amazon Music itself.
    val handOff: (AmazonWork) -> Unit = { target ->
        when (AmazonMusicLauncher.launch(context, target)) {
            AmazonMusicLauncher.Result.OpenedApp -> Unit
            AmazonMusicLauncher.Result.NotInstalled -> showInstall = true
            AmazonMusicLauncher.Result.Failed -> scope.launch {
                snackbars.showSnackbar(appState.ui(Ui.LISTEN_UNAVAILABLE))
            }
        }
    }

    // Plays it from here where that is possible, with the pill for controls;
    // otherwise, or if that goes wrong, hands off.
    // While the reader follows the recitation, starting one here also opens
    // [readerSection], the section heard first, so the text is in front of
    // the listener as it begins.
    val listenOnAmazon: (AmazonWork, String?) -> Unit = { target, readerSection ->
        val start = repository.amazonStart(target)
        pendingReader = null
        when {
            !remote.isInstalled -> showInstall = true
            start == null -> handOff(target)
            remote.hasAccess -> {
                remote.start(start) { handOff(target) }
                if (appState.followRecitation && readerSection != null) onOpenSection(readerSection)
            }
            remote.declinedAccess -> handOff(target)
            else -> {
                pendingReader = readerSection
                askingAccessFor = target
            }
        }
    }

    // Back from system Settings: play what was asked for, if access was given.
    val lifecycleOwner = LocalLifecycleOwner.current
    val onReturn by rememberUpdatedState {
        awaitingAccessFor?.let { target ->
            awaitingAccessFor = null
            remote.refresh()
            if (remote.hasAccess) listenOnAmazon(target, pendingReader)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onReturn()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showInstall) {
        AmazonInstallDialog(onDismiss = { showInstall = false })
    }
    askingAccessFor?.let { target ->
        PlayerAccessDialog(
            declineLabel = appState.ui(Ui.OPEN_AMAZON_MUSIC),
            onTurnOn = {
                askingAccessFor = null
                if (remote.openAccessSettings()) awaitingAccessFor = target else handOff(target)
            },
            onDecline = {
                askingAccessFor = null
                remote.declineAccess()
                handOff(target)
            },
            onDismiss = { askingAccessFor = null },
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text(division.title(appState.scriptChoice)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (works == null) {
            EmptyState(
                title = division.title(appState.scriptChoice),
                message = appState.ui(Ui.COMING_SOON),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            works.forEach { work ->
                item(key = "work-${work.id}") {
                    WorkRow(
                        work = work,
                        isExpanded = work.id in expanded,
                        onToggle = {
                            if (work.id in expanded) expanded.remove(work.id)
                            else expanded.add(work.id)
                        },
                        onPin = {
                            val wasPinned = appState.isPinned(work.id)
                            // A long press with no visible result reads as a
                            // missed tap, so every outcome says something.
                            val message = when {
                                wasPinned -> appState.ui(Ui.UNPIN_FROM_HOME)
                                !appState.canPinMore -> appState.ui(Ui.PIN_LIMIT)
                                else -> appState.ui(Ui.PIN_TO_HOME)
                            }
                            if (wasPinned || appState.canPinMore) {
                                appState.togglePin(work.id)
                            }
                            scope.launch { snackbars.showSnackbar(message) }
                        },
                        hasRecitation = repository.hasRecitation(work.id),
                        onListen = {
                            // Amazon Music trial: where a work is mapped
                            // (currently Thiruppavai only), prefer it — it
                            // plays in the background properly, unlike the
                            // YouTube hand-off. Everything else keeps using
                            // the existing YouTube chain unchanged.
                            val amazonWork = repository.amazonTarget(work.id)
                            if (amazonWork != null) {
                                listenOnAmazon(
                                    amazonWork,
                                    amazonWork.marks(onTrack = 0).firstOrNull()?.section
                                        ?: work.sections.firstOrNull()?.id,
                                )
                            } else {
                                // Hand off to YouTube Music / YouTube / browser.
                                val target = repository.recitationTarget(work.id)
                                val ok = RecitationLauncher.launch(
                                    context = context,
                                    work = work,
                                    playlistId = target.playlistId,
                                    videoIds = target.videoIds,
                                    script = appState.scriptChoice,
                                )
                                if (!ok) scope.launch {
                                    snackbars.showSnackbar(appState.ui(Ui.LISTEN_UNAVAILABLE))
                                }
                            }
                        },
                    )
                }

                if (work.id in expanded) {
                    val amazonWork = repository.amazonTarget(work.id)
                    val groups = sectionGroups(work)
                    // A paththu plays on its own where the recording has a
                    // track for each one.
                    val paththuTracks = amazonWork?.tracks?.size == groups.count { it.paththu != null }
                    groups.forEach { group ->
                        val paththu = group.paththu
                        val name = paththu?.let { Paththu.name(it, appState.scriptChoice) }
                        val openKey = "${work.id}#$paththu"
                        if (paththu != null && name != null) {
                            val track = if (paththuTracks) amazonWork?.paththu(paththu - 1) else null
                            item(key = "p-${work.id}-$paththu") {
                                PaththuHeader(
                                    name = name,
                                    isOpen = openKey in openPaththus,
                                    onToggle = {
                                        if (openKey in openPaththus) openPaththus.remove(openKey)
                                        else openPaththus.add(openKey)
                                    },
                                    onListen = track?.let {
                                        {
                                            listenOnAmazon(
                                                it,
                                                amazonWork?.marks(onTrack = paththu - 1)?.firstOrNull()?.section
                                                    ?: group.sections.firstOrNull()?.id,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        if (paththu != null && openKey !in openPaththus) return@forEach
                        items(group.sections.size, key = { "s-${group.sections[it].id}" }) { index ->
                            val section = group.sections[index]
                            // A timed decade plays from its own start.
                            val decade = amazonWork?.decade(section.id)
                            Surface(
                                onClick = { onOpenSection(section.id) },
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 32.dp,
                                            end = if (decade != null) 0.dp else 20.dp,
                                            top = 10.dp,
                                            bottom = 10.dp,
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        text = section.title(appState.scriptChoice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    section.pasuramRange?.let { range ->
                                        Text(
                                            text = "${appState.ui(Ui.PASURAM)} ${range.first}–${range.last}" +
                                                " · ${range.last - range.first + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (decade != null) {
                                    DecadePlayButton(
                                        name = section.title(appState.scriptChoice),
                                        isPlaying = recitationPosition?.sectionId == section.id &&
                                            remote.now?.isPlaying == true,
                                        onPlay = { listenOnAmazon(decade, section.id) },
                                        onPause = remote::playPause,
                                    )
                                }
                              }
                            }
                        }
                    }
                }

                item(key = "divider-${work.id}") { HorizontalDivider() }
            }
        }
    }
}

/**
 * A run of a work's sections, under the paththu they belong to (null for
 * sections outside any paththu, such as the thaniyangal).
 */
private class SectionGroup(val paththu: Int?, val sections: List<BookSection>)

/**
 * The sections in reading order, split by paththu where the work is numbered
 * in paththus ("1-1.", "1-2.", … "2-1."), so each paththu gets a row of its
 * own that opens to its sections. Any other work stays one plain list.
 */
private fun sectionGroups(work: Work): List<SectionGroup> {
    val plain = listOf(SectionGroup(null, work.sections))

    val groups = mutableListOf<SectionGroup>()
    var run = mutableListOf<BookSection>()
    work.sections.forEachIndexed { index, section ->
        run.add(section)
        val next = work.sections.getOrNull(index + 1)
        if (next == null || next.paththuNumber != section.paththuNumber) {
            groups.add(SectionGroup(section.paththuNumber, run))
            run = mutableListOf()
        }
    }
    val paththus = groups.mapNotNull { it.paththu }
    return if (paththus.size > 1 && paththus == (1..paththus.size).toList()) groups else plain
}

/** Plays one decade from its start, or pauses it while it is the one playing. */
@Composable
private fun DecadePlayButton(name: String, isPlaying: Boolean, onPlay: () -> Unit, onPause: () -> Unit) {
    val appState = LocalAppState.current
    IconButton(onClick = if (isPlaying) onPause else onPlay, modifier = Modifier.padding(end = 8.dp)) {
        Icon(
            if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
            contentDescription = "${appState.ui(if (isPlaying) Ui.PLAYER_PAUSE else Ui.LISTEN)}: $name",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The name of one paththu, with a button that plays that paththu alone. */
@Composable
private fun PaththuHeader(
    name: String,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onListen: (() -> Unit)?,
) {
    val appState = LocalAppState.current
    Surface(onClick = onToggle, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 32.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (onListen != null) {
                IconButton(onClick = onListen) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = "${appState.ui(Ui.LISTEN)}: $name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Icon(
                if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun WorkRow(
    work: Work,
    isExpanded: Boolean,
    hasRecitation: Boolean,
    onToggle: () -> Unit,
    onPin: () -> Unit,
    onListen: () -> Unit,
) {
    val appState = LocalAppState.current
    val pinned = appState.isPinned(work.id)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.combinedClickable(
            onClick = onToggle,
            onLongClick = onPin,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = work.title(appState.scriptChoice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (pinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = appState.ui(Ui.PINNED),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Text(
                    text = work.author(appState.scriptChoice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                work.pasuramRange?.let { range ->
                    Text(
                        text = "${work.sections.size} ${appState.ui(Ui.SECTIONS)}" +
                            " · ${appState.ui(Ui.PASURAM)} ${range.first}–${range.last}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onListen) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = appState.ui(Ui.LISTEN),
                    // Dimmed when no recitation is mapped, so the affordance
                    // reads as unavailable rather than broken.
                    tint = if (hasRecitation) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
