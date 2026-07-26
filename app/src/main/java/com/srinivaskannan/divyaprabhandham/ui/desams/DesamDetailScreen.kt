package com.srinivaskannan.divyaprabhandham.ui.desams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.data.DivyaDesam
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import com.srinivaskannan.divyaprabhandham.ui.theme.ReadingFonts

/**
 * One temple, and every pasuram that performs its mangalasasanam.
 *
 * The verse cards here are read-only previews — tapping one opens it in the
 * reader, at that exact verse, so a visitor can go from "which verses are about
 * this temple" to reading them in place without losing where they were.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesamDetailScreen(
    desam: DivyaDesam,
    onBack: () -> Unit,
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val context = LocalContext.current
    val script = appState.scriptChoice

    val verses = remember(desam.id, script) {
        desam.pasurams.mapNotNull { number ->
            repository.location(number)?.let { (sectionId, key) ->
                repository.stanzaForKey(key, script)?.let { (section, stanza) ->
                    Triple(number, section, stanza)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(desam.name(script), maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = desam.place(script),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    desam.perumal(script)?.let {
                        Text(
                            text = "${appState.ui(Ui.PERUMAL)}: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    desam.thaayar(script)?.let {
                        Text(
                            text = "${appState.ui(Ui.THAAYAR)}: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${desam.pasurams.size} ${appState.ui(Ui.DESAM_VERSES)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(verses.size, key = { "v-${verses[it].first}" }) { index ->
                val (number, section, stanza) = verses[index]
                Surface(
                    onClick = { onOpenSection(section.id, section.key(stanza)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${appState.ui(Ui.PASURAM)} $number",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stanza.text,
                            style = TextStyle(
                                fontFamily = ReadingFonts.family(
                                    context, appState.fontChoice, script,
                                ),
                                fontSize = 17.sp,
                                lineHeight = (17 * ReadingFonts.lineHeightMultiplier(script)).sp,
                            ),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
