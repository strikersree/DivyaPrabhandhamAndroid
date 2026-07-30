package com.srinivaskannan.divyaprabhandham.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.notify.ReminderScheduler
import com.srinivaskannan.divyaprabhandham.prefs.AccentChoice
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.AppearanceChoice
import com.srinivaskannan.divyaprabhandham.prefs.FontChoice
import com.srinivaskannan.divyaprabhandham.prefs.ReminderTime
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.prefs.WidgetAayiram
import com.srinivaskannan.divyaprabhandham.sync.GoogleSyncManager
import com.srinivaskannan.divyaprabhandham.sync.SyncStatus
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.components.GroupFooter
import com.srinivaskannan.divyaprabhandham.ui.components.GroupHeader
import com.srinivaskannan.divyaprabhandham.ui.components.ListRow
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.color
import com.srinivaskannan.divyaprabhandham.ui.theme.supportsDynamicColor

/**
 * Settings, as a master list with detail panes.
 *
 * The panes are held in local state rather than as navigation routes: they are
 * leaves with no deep links into them and no state worth restoring, so putting
 * them on the back stack would only mean seven more routes to keep in step with
 * the widget's URI scheme for no gain.
 */
private enum class Pane { NONE, SYNC, SCRIPT, APPEARANCE, FONT, ACCENT, NOTIFICATIONS, WIDGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sync: GoogleSyncManager,
    onOpenAbout: () -> Unit,
    onOpenTipJar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    var pane by remember { mutableStateOf(Pane.NONE) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(paneTitle(appState, pane)) },
                navigationIcon = {
                    if (pane != Pane.NONE) BackButton { pane = Pane.NONE }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (pane) {
                Pane.NONE -> RootList(
                    appState = appState,
                    sync = sync,
                    onPane = { pane = it },
                    onOpenAbout = onOpenAbout,
                    onOpenTipJar = onOpenTipJar,
                )
                Pane.SYNC -> SyncPane(appState, sync)
                Pane.SCRIPT -> ScriptPane(appState)
                Pane.APPEARANCE -> AppearancePane(appState)
                Pane.FONT -> FontPane(appState)
                Pane.ACCENT -> AccentPane(appState)
                Pane.NOTIFICATIONS -> NotificationsPane(appState)
                Pane.WIDGET -> WidgetPane(appState)
            }
        }
    }
}

@Composable
private fun RootList(
    appState: AppState,
    sync: GoogleSyncManager,
    onPane: (Pane) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenTipJar: () -> Unit,
) {
    ListRow(
        title = appState.ui(Ui.SYNC_TITLE),
        subtitle = if (appState.syncEnabled) appState.ui(Ui.SYNC_ON) else null,
        leading = Icons.Filled.CloudSync,
        onClick = { onPane(Pane.SYNC) },
    )
    HorizontalDivider()

    GroupHeader(appState.ui(Ui.APPEARANCE_HEADER))
    ListRow(
        title = appState.ui(Ui.SCRIPT_HEADER),
        leading = Icons.Filled.Translate,
        trailingText = appState.scriptChoice.label,
        onClick = { onPane(Pane.SCRIPT) },
    )
    ListRow(
        title = appState.ui(Ui.APPEARANCE_HEADER),
        leading = Icons.Filled.Contrast,
        trailingText = appearanceLabel(appState, appState.appearance),
        onClick = { onPane(Pane.APPEARANCE) },
    )
    ListRow(
        title = appState.ui(Ui.FONT_HEADER),
        leading = Icons.Filled.FormatSize,
        trailingText = fontLabel(appState, appState.fontChoice),
        onClick = { onPane(Pane.FONT) },
    )
    ListRow(
        title = appState.ui(Ui.ACCENT_HEADER),
        leading = Icons.Filled.Palette,
        trailingText = accentLabel(appState, appState.accentChoice),
        onClick = { onPane(Pane.ACCENT) },
    )

    GroupHeader(appState.ui(Ui.NOTIFICATIONS_HEADER))
    ListRow(
        title = appState.ui(Ui.DAILY_REMINDERS),
        leading = Icons.Filled.Notifications,
        trailingText = if (appState.notificationsEnabled && appState.reminderTimes.isNotEmpty()) {
            appState.reminderTimes.joinToString(", ") { it.label() }
        } else {
            appState.ui(Ui.REMINDERS_OFF)
        },
        onClick = { onPane(Pane.NOTIFICATIONS) },
    )
    ListRow(
        title = appState.ui(Ui.WIDGET_HEADER),
        leading = Icons.Filled.Widgets,
        onClick = { onPane(Pane.WIDGET) },
    )

    GroupHeader(appState.ui(Ui.ABOUT))
    ListRow(
        title = appState.ui(Ui.TIP_JAR),
        subtitle = if (appState.isSupporter) appState.ui(Ui.SUPPORTER_SINCE) else null,
        leading = Icons.Filled.VolunteerActivism,
        onClick = onOpenTipJar,
    )
    ListRow(
        title = appState.ui(Ui.ABOUT),
        leading = Icons.Filled.Info,
        onClick = onOpenAbout,
    )
}

@Composable
private fun SyncPane(appState: AppState, sync: GoogleSyncManager) {
    GroupHeader(appState.ui(Ui.ACCOUNT_HEADER))
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = appState.ui(Ui.SYNC_TOGGLE),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = appState.syncEnabled,
                onCheckedChange = { enabled ->
                    appState.updateSyncEnabled(enabled)
                    // Turning the switch on is an explicit act, so this is the
                    // one place the account chooser may appear unprompted.
                    if (enabled) sync.pull(appState, interactive = true)
                    else sync.disconnect()
                },
            )
        }
    }
    GroupFooter(appState.ui(Ui.SYNC_FOOTER))

    if (appState.syncEnabled) {
        // A slow tick so "N minutes ago" advances while the screen is open,
        // without the user having to leave and come back.
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(sync.lastSyncedAt) {
            while (true) {
                now = System.currentTimeMillis()
                kotlinx.coroutines.delay(30_000)
            }
        }
        val syncedAt = sync.lastSyncedAt
        ListRow(
            title = appState.ui(Ui.SYNC_NOW),
            trailingText = when {
                sync.isSyncing -> appState.ui(Ui.SYNC_SYNCING)
                sync.status == SyncStatus.Failed -> appState.ui(Ui.SYNC_FAILED)
                sync.status == SyncStatus.NeedsConsent -> appState.ui(Ui.SYNC_NEEDS_CONSENT)
                syncedAt != null -> RelativeTime.syncedAgo(now, syncedAt, appState.scriptChoice)
                else -> appState.ui(Ui.SYNC_NEVER)
            },
            showChevron = false,
            onClick = { sync.pull(appState, interactive = true) },
        )
    }
    GroupFooter(appState.ui(Ui.ACCOUNT_FOOTER))
}

@Composable
private fun ScriptPane(appState: AppState) {
    ScriptChoice.entries.forEach { choice ->
        ChoiceRow(
            title = choice.label,
            subtitle = choice.detail,
            selected = appState.scriptChoice == choice,
            onClick = { appState.updateScript(choice) },
        )
    }
    GroupFooter(appState.ui(Ui.SCRIPT_FOOTER))
}

@Composable
private fun AppearancePane(appState: AppState) {
    AppearanceChoice.entries.forEach { choice ->
        ChoiceRow(
            title = appearanceLabel(appState, choice),
            selected = appState.appearance == choice,
            onClick = { appState.updateAppearance(choice) },
        )
    }
    GroupFooter(appState.ui(Ui.APPEARANCE_FOOTER))
}

@Composable
private fun FontPane(appState: AppState) {
    FontChoice.entries.forEach { choice ->
        ChoiceRow(
            title = fontLabel(appState, choice),
            subtitle = choice.preview,
            selected = appState.fontChoice == choice,
            onClick = { appState.updateFontChoice(choice) },
        )
    }
    GroupFooter(appState.ui(Ui.FONT_FOOTER))
}

@Composable
private fun AccentPane(appState: AppState) {
    AccentChoice.entries.forEach { choice ->
        // Wallpaper colour only exists on Android 12+; offering it below that
        // would be a row that visibly does nothing.
        if (choice == AccentChoice.DYNAMIC && !supportsDynamicColor) return@forEach
        Surface(
            onClick = { appState.updateAccent(choice) },
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(
                            color = if (choice == AccentChoice.DYNAMIC) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                choice.color
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = accentLabel(appState, choice),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(
                    selected = appState.accentChoice == choice,
                    onClick = { appState.updateAccent(choice) },
                )
            }
        }
    }
    GroupFooter(appState.ui(Ui.ACCENT_FOOTER))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsPane(appState: AppState) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            appState.updateNotificationsEnabled(true)
            ReminderScheduler.reschedule(context, appState)
        } else {
            permissionDenied = true
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = appState.ui(Ui.DAILY_REMINDERS),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = appState.notificationsEnabled,
                onCheckedChange = { wanted ->
                    when {
                        !wanted -> {
                            appState.updateNotificationsEnabled(false)
                            ReminderScheduler.cancelAll(context)
                        }
                        // Android 13 asks at runtime; below that the permission
                        // is granted at install time, so there is nothing to ask.
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                        else -> {
                            appState.updateNotificationsEnabled(true)
                            ReminderScheduler.reschedule(context, appState)
                        }
                    }
                },
            )
        }
    }
    GroupFooter(appState.ui(Ui.NOTIFICATIONS_FOOTER))

    if (permissionDenied) {
        GroupFooter(appState.ui(Ui.NOTIFICATIONS_DENIED))
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) { Text(appState.ui(Ui.OPEN_SETTINGS)) }
    }

    if (appState.notificationsEnabled) {
        appState.reminderTimes.forEach { time ->
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = time.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            appState.updateReminderTimes(appState.reminderTimes - time)
                            ReminderScheduler.reschedule(context, appState)
                        },
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = appState.ui(Ui.CANCEL))
                    }
                }
            }
        }

        if (appState.reminderTimes.size < AppState.MAX_REMINDERS) {
            TextButton(
                onClick = { showPicker = true },
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    text = appState.ui(Ui.ADD_TIME),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showPicker) {
        // 07:00 matches the iOS default: early enough for a morning reading,
        // late enough not to be an alarm.
        val state = rememberTimePickerState(initialHour = 7, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val times = appState.reminderTimes +
                            ReminderTime(state.hour, state.minute)
                        appState.updateReminderTimes(times.sortedWith(
                            compareBy({ it.hour }, { it.minute }),
                        ))
                        ReminderScheduler.reschedule(context, appState)
                        showPicker = false
                    },
                ) { Text(appState.ui(Ui.DONE)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(appState.ui(Ui.CANCEL))
                }
            },
            title = { Text(appState.ui(Ui.REMINDER_TIME)) },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun WidgetPane(appState: AppState) {
    val choices = buildList {
        add(WidgetAayiram.ALL)
        Division.all.forEach { division ->
            WidgetAayiram.entries.firstOrNull { it.key == division.id }?.let { add(it) }
        }
    }
    choices.forEach { choice ->
        ChoiceRow(
            title = when (choice) {
                WidgetAayiram.ALL -> appState.ui(Ui.ALL_AAYIRAMS)
                WidgetAayiram.FOLLOW_APP -> appState.ui(Ui.FOLLOW_APP_SETTING)
                else -> Division.byId(choice.key)?.title(appState.scriptChoice) ?: choice.key
            },
            selected = appState.widgetAayiram == choice,
            onClick = { appState.updateWidgetAayiram(choice) },
        )
    }
    GroupFooter(appState.ui(Ui.WIDGET_FOOTER))
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

// MARK: - Labels

private fun paneTitle(appState: AppState, pane: Pane): String = when (pane) {
    Pane.NONE -> appState.ui(Ui.SETTINGS)
    Pane.SYNC -> appState.ui(Ui.SYNC_TITLE)
    Pane.SCRIPT -> appState.ui(Ui.SCRIPT_HEADER)
    Pane.APPEARANCE -> appState.ui(Ui.APPEARANCE_HEADER)
    Pane.FONT -> appState.ui(Ui.FONT_HEADER)
    Pane.ACCENT -> appState.ui(Ui.ACCENT_HEADER)
    Pane.NOTIFICATIONS -> appState.ui(Ui.NOTIFICATIONS_HEADER)
    Pane.WIDGET -> appState.ui(Ui.WIDGET_HEADER)
}

private fun appearanceLabel(appState: AppState, choice: AppearanceChoice): String = when (choice) {
    AppearanceChoice.AUTO -> appState.ui(Ui.THEME_AUTO)
    AppearanceChoice.LIGHT -> appState.ui(Ui.THEME_LIGHT)
    AppearanceChoice.DARK -> appState.ui(Ui.THEME_DARK)
    AppearanceChoice.HIGH_CONTRAST -> appState.ui(Ui.THEME_HIGH_CONTRAST)
}

private fun fontLabel(appState: AppState, choice: FontChoice): String = when (choice) {
    FontChoice.TRADITIONAL -> appState.ui(Ui.FONT_TRADITIONAL)
    FontChoice.MODERN -> appState.ui(Ui.FONT_MODERN)
    FontChoice.CLASSIC -> appState.ui(Ui.FONT_CLASSIC)
    FontChoice.SANS -> appState.ui(Ui.FONT_SANS)
}

private fun accentLabel(appState: AppState, choice: AccentChoice): String = when (choice) {
    AccentChoice.VERMILION -> appState.ui(Ui.ACCENT_VERMILION)
    AccentChoice.GOLD -> appState.ui(Ui.ACCENT_GOLD)
    AccentChoice.PEACOCK -> appState.ui(Ui.ACCENT_PEACOCK)
    AccentChoice.LEAF -> appState.ui(Ui.ACCENT_GREEN)
    AccentChoice.MAROON -> appState.ui(Ui.ACCENT_CRIMSON)
    AccentChoice.DYNAMIC -> appState.ui(Ui.ACCENT_DYNAMIC)
}
