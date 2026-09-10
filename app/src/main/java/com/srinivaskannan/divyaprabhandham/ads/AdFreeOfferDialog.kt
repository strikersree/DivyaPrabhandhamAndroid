package com.srinivaskannan.divyaprabhandham.ads

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * The ad-free upsell shown from [AdBanner] — either the permanent "Remove
 * ads" label, or the throttled prompt after returning from a tapped ad. One
 * shared dialog for all three placements rather than three copies, so the
 * offer reads identically everywhere it appears.
 */
@Composable
fun AdFreeOfferDialog(tipJar: TipJar, onDismiss: () -> Unit) {
    val appState = LocalAppState.current
    val product = tipJar.adFreeProduct

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appState.ui(Ui.UNLOCK_AD_FREE)) },
        text = {
            Text(
                if (product != null) {
                    "${appState.ui(Ui.UNLOCK_AD_FREE_DETAIL)} — ${product.price}"
                } else {
                    appState.ui(Ui.UNLOCK_AD_FREE_DETAIL)
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = product != null,
                onClick = {
                    product?.let { tipJar.purchase(it) }
                    onDismiss()
                },
            ) { Text(appState.ui(Ui.UNLOCK_AD_FREE)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appState.ui(Ui.CLOSE)) }
        },
    )
}
