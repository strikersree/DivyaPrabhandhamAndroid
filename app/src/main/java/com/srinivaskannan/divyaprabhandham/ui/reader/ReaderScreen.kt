package com.srinivaskannan.divyaprabhandham.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.data.BookSection
import com.srinivaskannan.divyaprabhandham.data.Essence
import com.srinivaskannan.divyaprabhandham.data.Stanza
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.LastRead
import com.srinivaskannan.divyaprabhandham.prefs.ReaderThemeChoice
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.ui.components.shareText
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import com.srinivaskannan.divyaprabhandham.ui.theme.ReadingFonts
import com.srinivaskannan.divyaprabhandham.ui.theme.currentReaderTheme
import com.srinivaskannan.divyaprabhandham.ui.theme.ReaderPalette
import com.srinivaskannan.divyaprabhandham.ui.theme.readerPalette
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The reader.
 *
 * A few decisions carried over from iOS because they were hard-won there:
 *
 *  - Verse text is selectable, and the essence action lives on the pasuram
 *    number badge rather than on the card. Putting it on the card meant a long
 *    press anywhere in a verse opened a menu instead of selecting text, which
 *    is the wrong trade in a book.
 *  - Progress tracks the furthest verse currently on screen, so scrolling back
 *    up winds the bar down rather than leaving it stuck at the end.
 *  - Adjacent-section cards top and bottom let a reciter move through the text
 *    continuously, and they push onto the back stack so Back retraces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    section: BookSection,
    work: Work?,
    initialStanzaKey: String?,
    onBack: () -> Unit,
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val theme = currentReaderTheme(appState)
    val palette = readerPalette(theme)
    val accent = MaterialTheme.colorScheme.primary

    val stanzas = remember(section.id, appState.scriptChoice) {
        section.stanzas(appState.scriptChoice)
    }
    val verseCount = remember(stanzas) {
        stanzas.count { !it.isHeading && !it.isDescription }
    }

    val listState = rememberLazyListState()
    val previous = remember(section.id) { repository.previousSection(section.id) }
    val next = remember(section.id) { repository.nextSection(section.id) }
    val decadEssence = remember(section.id) { repository.decadEssence(section.id) }

    var essenceSheet by remember { mutableStateOf<EssenceTarget?>(null) }
    var fontBarExpanded by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }

    // Header rows sit above the verses, so the index of a verse in the list is
    // offset by however many of them are present.
    val leadingRows = remember(decadEssence, previous) {
        1 + (if (decadEssence != null) 1 else 0) + (if (previous != null) 1 else 0)
    }

    /** Furthest verse currently on screen, 0..1. */
    val progress by remember {
        derivedStateOf {
            if (verseCount == 0) return@derivedStateOf 0f
            val furthest = listState.layoutInfo.visibleItemsInfo
                .maxOfOrNull { it.index } ?: return@derivedStateOf 0f
            ((furthest - leadingRows + 1).coerceIn(0, verseCount)).toFloat() / verseCount
        }
    }
    val animatedProgress by animateFloatAsState(progress, label = "readingProgress")

    // Record recency once per section, and restore the scroll position.
    LaunchedEffect(section.id) {
        appState.noteVisited(section.id)
        val key = initialStanzaKey
            ?: appState.lastRead?.takeIf { it.sectionId == section.id }?.stanzaKey
        if (key != null) {
            val index = stanzas.indexOfFirst { section.key(it) == key }
            if (index >= 0) listState.scrollToItem(index + leadingRows)
        }
    }

    // Remember where the reader got to. Debounced: without it every verse that
    // scrolls past writes to DataStore and schedules a sync push.
    LaunchedEffect(section.id, stanzas) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(400)
            .distinctUntilChanged()
            .collect { index ->
                val stanza = stanzas.getOrNull(index - leadingRows) ?: return@collect
                appState.setLastRead(LastRead(section.id, section.key(stanza)))
            }
    }

    Scaffold(
        modifier = modifier,
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        section.title(appState.scriptChoice),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appState.ui(Ui.BACK),
                        )
                    }
                },
                actions = {
                    // While the global high-contrast appearance is active the
                    // reader palette is fixed, so the picker is hidden rather
                    // than sitting there appearing to do nothing.
                    if (!appState.isHighContrast) {
                        Box {
                            IconButton(onClick = { themeMenuOpen = true }) {
                                Icon(
                                    themeIcon(theme),
                                    contentDescription = appState.ui(Ui.CHANGE_THEME),
                                )
                            }
                            DropdownMenu(
                                expanded = themeMenuOpen,
                                onDismissRequest = { themeMenuOpen = false },
                            ) {
                                ReaderThemeChoice.pickable.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(themeLabel(appState, option)) },
                                        leadingIcon = {
                                            Icon(themeIcon(option), contentDescription = null)
                                        },
                                        onClick = {
                                            appState.setTheme(option)
                                            themeMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                    titleContentColor = palette.text,
                    actionIconContentColor = palette.secondaryText,
                    navigationIconContentColor = palette.text,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "header") {
                    ReaderHeader(work = work, palette = palette)
                }

                if (decadEssence != null) {
                    item(key = "decadEssence") {
                        DecadEssenceCard(
                            accent = accent,
                            palette = palette,
                            onClick = {
                                essenceSheet = EssenceTarget(null, decadEssence)
                            },
                        )
                    }
                }

                if (previous != null) {
                    item(key = "prev") {
                        AdjacentSectionCard(
                            target = previous,
                            isNext = false,
                            palette = palette,
                            accent = accent,
                            onClick = { onOpenSection(previous.id, null) },
                        )
                    }
                }

                items(
                    count = stanzas.size,
                    key = { section.key(stanzas[it]) },
                ) { index ->
                    val stanza = stanzas[index]
                    when {
                        stanza.isDescription -> DecadeDescription(stanza.text, palette, accent)
                        stanza.isHeading -> AttributionHeader(stanza.text, accent)
                        else -> StanzaCard(
                            stanza = stanza,
                            section = section,
                            work = work,
                            palette = palette,
                            accent = accent,
                            onShowEssence = { essence ->
                                essenceSheet = EssenceTarget(stanza.number, essence)
                            },
                        )
                    }
                }

                if (next != null) {
                    item(key = "next") {
                        AdjacentSectionCard(
                            target = next,
                            isNext = true,
                            palette = palette,
                            accent = accent,
                            onClick = { onOpenSection(next.id, null) },
                        )
                    }
                }
            }

            // A hairline under the app bar showing how far through the section
            // the reader has got.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(palette.secondaryText.copy(alpha = 0.15f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(3.dp)
                    .background(accent)
                    .semantics {
                        contentDescription = appState.ui(Ui.READING_PROGRESS)
                    },
            )

            FontSizeControl(
                expanded = fontBarExpanded,
                onExpandedChange = { fontBarExpanded = it },
                palette = palette,
                appState = appState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    essenceSheet?.let { target ->
        EssenceSheet(
            number = target.number,
            essence = target.essence,
            palette = palette,
            accent = accent,
            onDismiss = { essenceSheet = null },
        )
    }
}

private data class EssenceTarget(val number: Int?, val essence: Essence?)

@Composable
private fun ReaderHeader(
    work: Work?,
    palette: ReaderPalette,
) {
    val appState = LocalAppState.current
    if (work == null) return
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = work.title(appState.scriptChoice),
            style = MaterialTheme.typography.headlineSmall,
            color = palette.text,
        )
        // Tamil puts the attribution after the author's name, English before.
        val author = work.author(appState.scriptChoice)
        val composed = appState.ui(Ui.COMPOSED_BY)
        Text(
            text = if (appState.scriptChoice == ScriptChoice.TAMIL) {
                "$author $composed"
            } else {
                "$composed $author"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
        )
    }
}

/** One pasuram, with bookmark, share and the essence affordance. */
@Composable
private fun StanzaCard(
    stanza: Stanza,
    section: BookSection,
    work: Work?,
    palette: ReaderPalette,
    accent: Color,
    onShowEssence: (Essence?) -> Unit,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val context = LocalContext.current
    val key = section.key(stanza)
    val bookmarked = appState.isBookmarked(key)
    val essence = stanza.number?.let { repository.essence(it, section.id) }

    val fontFamily = ReadingFonts.family(context, appState.fontChoice, appState.scriptChoice)
    val size = appState.fontSize.sp
    val lineHeight = appState.fontSize * ReadingFonts.lineHeightMultiplier(appState.scriptChoice)

    var essenceMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = palette.card,
        border = palette.cardBorder?.let { border ->
            androidx.compose.foundation.BorderStroke(
                width = if (bookmarked) 2.5.dp else 1.5.dp,
                color = if (bookmarked) accent else border,
            )
        },
        tonalElevation = if (palette.cardBorder == null) 1.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stanza.number != null) {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.12f),
                            // The essence action hangs off the number badge,
                            // not the card, so a long press inside the verse
                            // still selects text.
                            modifier = Modifier.pointerInput(essence) {
                                if (essence == null) return@pointerInput
                                detectTapGestures(
                                    onLongPress = { essenceMenuOpen = true },
                                )
                            },
                        ) {
                            Text(
                                text = "${appState.ui(Ui.PASURAM)} ${stanza.number}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = essenceMenuOpen,
                            onDismissRequest = { essenceMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(appState.ui(Ui.EXPLAIN_DEFINE)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                },
                                onClick = {
                                    essenceMenuOpen = false
                                    onShowEssence(essence)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { appState.toggleBookmark(key) }) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = appState.ui(
                            if (bookmarked) Ui.REMOVE_BOOKMARK else Ui.ADD_BOOKMARK,
                        ),
                        tint = if (bookmarked) accent else palette.secondaryText,
                    )
                }
                IconButton(
                    onClick = {
                        shareText(context, buildShareText(appState, stanza, section, work))
                    },
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = appState.ui(Ui.SHARE),
                        tint = palette.secondaryText,
                    )
                }
            }

            SelectionContainer {
                Text(
                    text = stanza.text,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = size,
                        lineHeight = lineHeight.sp,
                        color = palette.text,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun buildShareText(
    appState: AppState,
    stanza: Stanza,
    section: BookSection,
    work: Work?,
): String = buildString {
    stanza.number?.let { appendLine("${appState.ui(Ui.PASURAM)} $it") }
    appendLine(stanza.text)
    appendLine()
    val source = work?.title(appState.scriptChoice) ?: section.title(appState.scriptChoice)
    append("— $source, ${appState.ui(Ui.FULL_BOOK_NAME)}")
}

/**
 * The decad-heading block that opens each thirumozhi section: reference line,
 * decad name, and an optional parenthetical theme.
 */
@Composable
private fun DecadeDescription(
    text: String,
    palette: ReaderPalette,
    accent: Color,
) {
    val lines = remember(text) {
        text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.firstOrNull()?.let { first ->
            if (first.startsWith("(")) {
                Text(
                    text = first,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = palette.secondaryText,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = first,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
        }
        if (lines.size > 1) {
            Text(
                text = lines[1],
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
        }
        if (lines.size > 2) {
            Text(
                text = lines.drop(2).joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = palette.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            Modifier
                .padding(top = 6.dp)
                .width(64.dp)
                .height(1.5.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.30f)),
        )
    }
}

/** Attribution lines like "நாதமுனிகள் அருளிச் செய்தது". */
@Composable
private fun AttributionHeader(text: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rule(accent, Modifier.weight(1f))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            textAlign = TextAlign.Center,
        )
        Rule(accent, Modifier.weight(1f))
    }
}

@Composable
private fun Rule(accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(1.5.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.35f)),
    )
}

@Composable
private fun DecadEssenceCard(
    accent: Color,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    val appState = LocalAppState.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = accent,
            )
            Text(
                text = appState.ui(Ui.DECAD_ESSENCE),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AdjacentSectionCard(
    target: BookSection,
    isNext: Boolean,
    palette: ReaderPalette,
    accent: Color,
    onClick: () -> Unit,
) {
    val appState = LocalAppState.current
    val caption = appState.ui(if (isNext) Ui.NEXT_SECTION else Ui.PREVIOUS_SECTION)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = palette.card,
        border = palette.cardBorder?.let {
            androidx.compose.foundation.BorderStroke(1.5.dp, it)
        },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = if (isNext) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText,
            )
            Text(
                text = target.title(appState.scriptChoice),
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
            )
        }
    }
}

/**
 * The text-size control: a small button that expands into a slider and folds
 * itself away again, so it is available without sitting on top of the verses.
 */
@Composable
private fun FontSizeControl(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    palette: ReaderPalette,
    appState: AppState,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        if (expanded) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("A", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = appState.fontSize,
                        onValueChange = { appState.setFontSize(it) },
                        valueRange = AppState.MIN_FONT_SIZE..AppState.MAX_FONT_SIZE,
                        steps = (AppState.MAX_FONT_SIZE - AppState.MIN_FONT_SIZE).toInt() - 1,
                        modifier = Modifier
                            .width(200.dp)
                            .semantics {
                                contentDescription = appState.ui(Ui.FONT_SIZE)
                            },
                    )
                    Text("A", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onExpandedChange(false) }) {
                        Icon(
                            Icons.Filled.FormatSize,
                            contentDescription = appState.ui(Ui.DONE),
                        )
                    }
                }
            }
        } else {
            FilledTonalIconButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Filled.FormatSize,
                    contentDescription = appState.ui(Ui.FONT_SIZE),
                )
            }
        }
    }
}

private fun themeLabel(appState: AppState, theme: ReaderThemeChoice): String = when (theme) {
    ReaderThemeChoice.LIGHT -> appState.ui(Ui.THEME_LIGHT_READER)
    ReaderThemeChoice.SEPIA -> appState.ui(Ui.THEME_SEPIA)
    ReaderThemeChoice.NIGHT -> appState.ui(Ui.THEME_NIGHT)
    ReaderThemeChoice.CONTRAST_LIGHT, ReaderThemeChoice.CONTRAST_DARK ->
        appState.ui(Ui.THEME_HIGH_CONTRAST)
}

private fun themeIcon(theme: ReaderThemeChoice) = when (theme) {
    ReaderThemeChoice.LIGHT -> Icons.Filled.LightMode
    ReaderThemeChoice.SEPIA -> Icons.AutoMirrored.Filled.MenuBook
    ReaderThemeChoice.NIGHT -> Icons.Filled.Bedtime
    ReaderThemeChoice.CONTRAST_LIGHT, ReaderThemeChoice.CONTRAST_DARK -> Icons.Filled.Visibility
}
