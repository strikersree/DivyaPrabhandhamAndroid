package com.srinivaskannan.divyaprabhandham.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * About & acknowledgements. Mirrors the iOS AboutView: an intro and version,
 * then reciters, sources, and a gratitude list of named people, then testers
 * and the verse-rights note. Names live in Credits.kt (as on iOS) rather than
 * the localisation table so they are easy to add to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val context = LocalContext.current
    val script = appState.scriptChoice

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appState.ui(Ui.ABOUT)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = appState.ui(Ui.DIVYA_PRABANDHAM),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = appState.ui(Ui.ABOUT_BLURB),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${appState.ui(Ui.VERSION)} $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${repository.totalPasurams} ${appState.ui(Ui.PASURAMS)} · " +
                    "${repository.allWorks.size} ${appState.ui(Ui.WORKS)} · " +
                    "${repository.divyaDesams.size} ${appState.ui(Ui.DIVYA_DESAMS)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CreditSection(appState.ui(Ui.CREDITS_RECITATION), Credits.reciters, script)
            CreditSection(appState.ui(Ui.CREDITS_SOURCES), Credits.sources, script)
            CreditSection(appState.ui(Ui.CREDITS_GRATITUDE), Credits.gratitude, script)

            HorizontalDivider()

            Text(
                text = appState.ui(Ui.CREDITS_TESTERS),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = appState.ui(Ui.TESTERS_NOTE),
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text(
                text = appState.ui(Ui.VERSE_RIGHTS_NOTE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreditSection(title: String, people: List<Credit>, script: ScriptChoice) {
    HorizontalDivider()
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        people.forEach { person ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = person.name(script),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = person.role(script),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
