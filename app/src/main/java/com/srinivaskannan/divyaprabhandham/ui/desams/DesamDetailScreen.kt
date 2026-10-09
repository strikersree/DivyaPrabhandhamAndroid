package com.srinivaskannan.divyaprabhandham.ui.desams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.DivyaDesam
import com.srinivaskannan.divyaprabhandham.data.Essence
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.reader.EssenceSheet
import com.srinivaskannan.divyaprabhandham.ui.reader.StanzaCard
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import com.srinivaskannan.divyaprabhandham.ui.theme.currentReaderTheme
import com.srinivaskannan.divyaprabhandham.ui.theme.readerPalette

/**
 * One temple, and every pasuram that performs its mangalasasanam.
 *
 * Each pasuram is shown in full as the reader's own card -- reading font and
 * size, bookmark, share, Explain and long-press Add to Collection -- on the
 * reader's palette, with its work and section underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesamDetailScreen(
    desam: DivyaDesam,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val script = appState.scriptChoice
    val palette = readerPalette(currentReaderTheme(appState))
    val accent = MaterialTheme.colorScheme.primary

    var essenceSheet by remember { mutableStateOf<Pair<Int?, Essence?>?>(null) }

    val verses = remember(desam.id, script) {
        desam.verseIdentifiers.mapNotNull { identifier ->
            repository.stanzaKeyForIdentifier(identifier)?.let { key ->
                repository.stanzaForKey(key, script)?.let { (section, stanza) ->
                    Triple(identifier, section, stanza)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = { Text(desam.name(script), maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                    titleContentColor = palette.text,
                    navigationIconContentColor = palette.text,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = desam.place(script),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.secondaryText,
                    )
                    desam.perumal(script)?.let {
                        Text(
                            text = "${appState.ui(Ui.PERUMAL)}: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.text,
                        )
                    }
                    desam.thaayar(script)?.let {
                        Text(
                            text = "${appState.ui(Ui.THAAYAR)}: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.text,
                        )
                    }
                    Text(
                        text = "${desam.verseIdentifiers.size} ${appState.ui(Ui.DESAM_VERSES)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                    )
                }
            }

            items(verses.size, key = { "v-${verses[it].first}" }) { index ->
                val (_, section, stanza) = verses[index]
                val work = remember(section.id) { repository.workContaining(section.id) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StanzaCard(
                        stanza = stanza,
                        section = section,
                        work = work,
                        palette = palette,
                        accent = accent,
                        onShowEssence = { essence -> essenceSheet = stanza.number to essence },
                    )
                    Text(
                        text = listOfNotNull(work?.title(script), section.title(script))
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }

    essenceSheet?.let { (number, essence) ->
        EssenceSheet(
            number = number,
            essence = essence,
            palette = palette,
            accent = accent,
            onDismiss = { essenceSheet = null },
        )
    }
}
