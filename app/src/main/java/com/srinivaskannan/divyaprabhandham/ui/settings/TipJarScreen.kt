package com.srinivaskannan.divyaprabhandham.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.billing.TipProduct
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * The tip jar.
 *
 * Every verse is free and stays free; this exists because some readers ask for
 * a way to say thank you. The tone is carried over from iOS deliberately — it
 * offers, it does not press, and once someone has given, or asked not to be
 * asked, it never comes back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipJarScreen(
    tipJar: TipJar,
    onBack: () -> Unit,
    isReminder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val products = tipJar.products

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appState.ui(Ui.TIP_JAR)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (appState.isSupporter) {
                Text(
                    text = appState.ui(Ui.TIP_THANKS),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = appState.ui(Ui.TIP_THANKS_BLURB),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            Text(
                text = appState.ui(Ui.TIP_JAR_BLURB),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            if (products.isEmpty()) {
                Text(
                    text = appState.ui(Ui.TIP_UNAVAILABLE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                products.forEach { product: TipProduct ->
                    Button(
                        onClick = { tipJar.purchase(product) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${product.title} · ${product.price}")
                    }
                }
            }

            if (isReminder) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(appState.ui(Ui.MAYBE_LATER)) }

                TextButton(
                    onClick = {
                        appState.silenceTipPrompt()
                        onBack()
                    },
                ) { Text(appState.ui(Ui.DONT_ASK_AGAIN)) }
            }
        }
    }
}
