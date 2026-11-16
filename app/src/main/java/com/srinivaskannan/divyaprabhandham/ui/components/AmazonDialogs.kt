package com.srinivaskannan.divyaprabhandham.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.media.AmazonMusicLauncher
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * Shown when a recitation is asked for and Amazon Music is not on the phone.
 * It says why the Play Store is about to open, and what else listening needs,
 * before leaving the app — rather than dropping the listener onto a store
 * page with no explanation.
 */
@Composable
fun AmazonInstallDialog(onDismiss: () -> Unit) {
    val appState = LocalAppState.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appState.ui(Ui.AMAZON_NEEDED_TITLE)) },
        text = { Text(appState.ui(Ui.AMAZON_NEEDED_BODY)) },
        confirmButton = {
            TextButton(
                onClick = {
                    AmazonMusicLauncher.openStore(context)
                    onDismiss()
                },
            ) { Text(appState.ui(Ui.AMAZON_INSTALL)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appState.ui(Ui.NOT_NOW)) }
        },
    )
}

/**
 * Asks for the one permission the in-app player needs, in plain words, before
 * the system screen that words it alarmingly. [onDecline] is the other button
 * — labelled [declineLabel], since what "no" leads to depends on where the
 * question was asked — and is not called when the dialog is merely dismissed.
 */
@Composable
fun PlayerAccessDialog(
    declineLabel: String,
    onTurnOn: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appState = LocalAppState.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appState.ui(Ui.PLAYER_ACCESS_TITLE)) },
        text = { Text(appState.ui(Ui.PLAYER_ACCESS_BODY)) },
        confirmButton = {
            TextButton(onClick = onTurnOn) { Text(appState.ui(Ui.PLAYER_ACCESS_TURN_ON)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(declineLabel) }
        },
    )
}
