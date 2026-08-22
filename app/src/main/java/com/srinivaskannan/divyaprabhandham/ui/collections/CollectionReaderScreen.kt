package com.srinivaskannan.divyaprabhandham.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.data.BookSection
import com.srinivaskannan.divyaprabhandham.data.Stanza
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.prefs.UserCollection
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import com.srinivaskannan.divyaprabhandham.ui.theme.ReaderPalette
import com.srinivaskannan.divyaprabhandham.ui.theme.ReadingFonts
import com.srinivaskannan.divyaprabhandham.ui.theme.readerPalette

/**
 * Reads a collection straight through as one continuous scroll, rather than
 * a list the person has to tap back into item by item — a collection like
 * Prabhandha Saaram or Desika Prabhandha Saaththumurai is a curated
 * sequence of highlights meant to be read start to finish, not a set of
 * independent pointers each requiring its own trip into the full reader.
 *
 * Every key in [UserCollection.pasuramKeys] is resolved once, in order,
 * and rendered here directly (not by opening the normal per-section
 * reader for each one) — since entries can come from entirely different
 * works, a small source header is inserted whenever the underlying work
 * or section changes from the previous entry, so the person always knows
 * what they're reading without it interrupting the scroll.
 */
private data class ResolvedEntry(val key: String, val section: BookSection, val stanza: Stanza, val work: Work?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionReaderScreen(collectionId: String, onBack: () -> Unit, onManage: () -> Unit) {
    val repository = LocalRepository.current
    val appState = LocalAppState.current
    val context = LocalContext.current
    val script = appState.scriptChoice
    val collection = appState.collection(collectionId)

    if (collection == null) {
        Scaffold(topBar = { TopAppBar(title = {}, navigationIcon = { BackButton(onBack) }) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Text(appState.ui(Ui.NO_RESULTS), modifier = Modifier.padding(24.dp))
            }
        }
        return
    }

    val entries = remember(collection.id, collection.pasuramKeys, script) {
        collection.pasuramKeys.mapNotNull { key ->
            val (section, stanza) = repository.stanzaForKey(key, script) ?: return@mapNotNull null
            ResolvedEntry(key, section, stanza, repository.workContaining(section.id))
        }
    }

    val fontFamily = ReadingFonts.family(context, appState.fontChoice, script)
    val fontSize = appState.fontSize.sp
    val lineHeight = appState.fontSize * ReadingFonts.lineHeightMultiplier(script)
    val palette = readerPalette(appState.theme)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collection.name) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (!collection.isBuiltIn) {
                        IconButton(onClick = onManage) {
                            Icon(Icons.Filled.Edit, contentDescription = appState.ui(Ui.MANAGE_COLLECTION))
                        }
                    }
                },
            )
        },
        containerColor = palette.background,
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                // An empty built-in collection (e.g. before its data lands)
                // or one whose keys no longer resolve -- rare, but silent
                // blankness would be worse than a plain explanatory line.
                Text(
                    appState.ui(Ui.NO_COLLECTIONS_HINT),
                    modifier = Modifier.padding(24.dp),
                    color = palette.text,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            var lastSectionId: String? = null
            items(entries, key = { it.key }) { entry ->
                Column {
                    if (entry.section.id != lastSectionId) {
                        SourceHeader(entry.work, entry.section, script, palette.text)
                        Spacer(Modifier.height(10.dp))
                    }
                    ContinuousStanzaCard(entry.stanza, fontFamily, fontSize, lineHeight, palette)
                }
                lastSectionId = entry.section.id
            }
        }
    }
}

@Composable
private fun SourceHeader(work: Work?, section: BookSection, script: ScriptChoice, textColor: Color) {
    val label = work?.title(script) ?: section.title(script)
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = textColor.copy(alpha = 0.7f),
    )
}

@Composable
private fun ContinuousStanzaCard(
    stanza: Stanza,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    lineHeight: Float,
    palette: ReaderPalette,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = palette.card,
        tonalElevation = if (palette.cardBorder == null) 1.dp else 0.dp,
    ) {
        SelectionContainer {
            Text(
                text = stanza.text,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    lineHeight = lineHeight.sp,
                    color = palette.text,
                ),
                modifier = Modifier.fillMaxWidth().padding(18.dp),
            )
        }
    }
}
