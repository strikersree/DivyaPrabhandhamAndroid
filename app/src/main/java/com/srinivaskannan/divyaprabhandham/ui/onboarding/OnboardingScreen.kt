package com.srinivaskannan.divyaprabhandham.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.UiText
import com.srinivaskannan.divyaprabhandham.notify.ReminderScheduler
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.ReminderTime
import com.srinivaskannan.divyaprabhandham.prefs.ReaderThemeChoice
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.readerPalette

/**
 * First-run onboarding. A short guided flow that lets the person shape the app
 * to themselves before they begin — font size, script and menu language, theme,
 * and daily reminders — each with a live preview that updates as they choose.
 *
 * Every setting is applied to the real app state as it is chosen, so the preview
 * *is* the app; there is no separate confirm. "Skip now" leaves the flow at any
 * point (keeping whatever was already adjusted), and a footer note reassures
 * that all of it is changeable later in Settings. The app-icon and sign-in steps
 * are intentionally not here yet (icon comes next; sign-in waits for sync).
 */
@Composable
fun OnboardingHost(onFinish: () -> Unit) {
    val steps = remember { listOf(Step.WELCOME, Step.FONT, Step.SCRIPT, Step.THEME, Step.ICON, Step.REMINDER) }
    var index by remember { mutableIntStateOf(0) }
    val appState = LocalAppState.current
    val step = steps[index]
    val isLast = index == steps.lastIndex

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding-step",
                ) { s ->
                    when (s) {
                        Step.WELCOME -> WelcomeStep()
                        Step.FONT -> FontStep(appState)
                        Step.SCRIPT -> ScriptStep(appState)
                        Step.THEME -> ThemeStep(appState)
                        Step.ICON -> IconStep(appState)
                        Step.REMINDER -> ReminderStep(appState)
                    }
                }
            }

            // Progress dots sit at the bottom, clear of the camera cutout.
            StepDots(current = index, total = steps.size)
            Spacer(Modifier.height(12.dp))

            // Reassurance that nothing here is permanent.
            Text(
                appState.ui(Ui.ONB_SETTINGS_NOTE),
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    TextButton(onClick = { index-- }) { Text(appState.ui(Ui.ONB_BACK)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFinish) { Text(appState.ui(Ui.ONB_SKIP)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (isLast) onFinish() else index++ }) {
                    Text(
                        when {
                            step == Step.WELCOME -> appState.ui(Ui.ONB_GET_STARTED)
                            isLast -> appState.ui(Ui.ONB_DONE)
                            else -> appState.ui(Ui.ONB_NEXT)
                        },
                    )
                }
            }
        }
    }
}

private enum class Step { WELCOME, FONT, SCRIPT, THEME, ICON, REMINDER }

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 10.dp else 8.dp)
                    .background(
                        if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun StepHeader(title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(16.dp))
        // The welcome greets in both languages, whatever the UI language will be.
        Text(
            UiText.english(Ui.ONB_WELCOME_TITLE),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            UiText.tamil(Ui.ONB_WELCOME_TITLE),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            UiText.english(Ui.ONB_WELCOME_BODY),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            UiText.tamil(Ui.ONB_WELCOME_BODY),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

// A recognisable opening line, used as the live type sample across steps.
private const val SAMPLE_TA = "மார்கழித் திங்கள் மதிநிறைந்த நன்னாளால்"
private const val SAMPLE_EN = "Maargazhi thingal madhi niraindha nannaalaal"

@Composable
private fun PreviewCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun FontStep(appState: AppState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(appState.ui(Ui.ONB_FONT_TITLE), appState.ui(Ui.ONB_FONT_BODY))
        Spacer(Modifier.height(24.dp))
        PreviewCard {
            Text(
                if (appState.scriptChoice == ScriptChoice.TAMIL) SAMPLE_TA else SAMPLE_EN,
                fontSize = appState.fontSize.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Slider(
            value = appState.fontSize,
            onValueChange = { appState.updateFontSize(it) },
            valueRange = AppState.MIN_FONT_SIZE..AppState.MAX_FONT_SIZE,
        )
        Text("${appState.fontSize.toInt()} sp", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScriptStep(appState: AppState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(appState.ui(Ui.ONB_SCRIPT_TITLE), appState.ui(Ui.ONB_SCRIPT_BODY))
        Spacer(Modifier.height(20.dp))
        ScriptChoice.entries.forEach { choice ->
            val selected = appState.scriptChoice == choice
            Surface(
                onClick = { appState.updateScript(choice) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                border = if (selected) null
                else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(choice.label, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    Text(choice.detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ThemeStep(appState: AppState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(appState.ui(Ui.ONB_THEME_TITLE), appState.ui(Ui.ONB_THEME_BODY))
        Spacer(Modifier.height(20.dp))
        // Live reader preview in the chosen palette.
        val palette = readerPalette(appState.theme)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.background,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            border = palette.cardBorder?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        ) {
            Box(Modifier.padding(20.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    if (appState.scriptChoice == ScriptChoice.TAMIL) SAMPLE_TA else SAMPLE_EN,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReaderThemeChoice.pickable.forEach { theme ->
                val selected = appState.theme == theme
                val pal = readerPalette(theme)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(pal.background, CircleShape)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    ) {
                        Surface(
                            onClick = { appState.updateTheme(theme) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("அ", color = pal.text)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(themeLabel(appState, theme), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun themeLabel(appState: AppState, theme: ReaderThemeChoice): String = when (theme) {
    ReaderThemeChoice.SEPIA -> appState.ui(Ui.THEME_SEPIA)
    ReaderThemeChoice.NIGHT -> appState.ui(Ui.THEME_NIGHT)
    else -> appState.ui(Ui.THEME_LIGHT)
}

@Composable
private fun IconStep(appState: AppState) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(appState.ui(Ui.ONB_ICON_TITLE), appState.ui(Ui.ONB_ICON_BODY))
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconChoice(
                label = appState.ui(Ui.ONB_ICON_VADAKALAI),
                art = R.drawable.ic_icon_vadakalai,
                selected = appState.appIconKey == "vadakalai",
                onPick = {
                    appState.updateAppIconKey("vadakalai")
                    com.srinivaskannan.divyaprabhandham.ui.settings.AppIconSwitcher.apply(
                        context, com.srinivaskannan.divyaprabhandham.ui.settings.AppIcon.VADAKALAI,
                    )
                },
            )
            IconChoice(
                label = appState.ui(Ui.ONB_ICON_THENKALAI),
                art = R.drawable.ic_icon_thenkalai,
                selected = appState.appIconKey == "thenkalai",
                onPick = {
                    appState.updateAppIconKey("thenkalai")
                    com.srinivaskannan.divyaprabhandham.ui.settings.AppIconSwitcher.apply(
                        context, com.srinivaskannan.divyaprabhandham.ui.settings.AppIcon.THENKALAI,
                    )
                },
            )
        }
    }
}

@Composable
private fun IconChoice(label: String, art: Int, selected: Boolean, onPick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onPick,
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Image(
                painter = painterResource(art),
                contentDescription = label,
                modifier = Modifier.size(96.dp).padding(6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ReminderStep(appState: AppState) {
    val context = LocalContext.current
    val enabled = appState.notificationsEnabled

    fun enable() {
        // Give a sensible default time if none set, so there is something to fire.
        if (appState.reminderTimes.isEmpty()) {
            appState.updateReminderTimes(listOf(ReminderTime(6, 0)))
        }
        appState.updateNotificationsEnabled(true)
        ReminderScheduler.reschedule(context, appState)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) enable() }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(appState.ui(Ui.ONB_REMINDER_TITLE), appState.ui(Ui.ONB_REMINDER_BODY))
        Spacer(Modifier.height(24.dp))
        // A sample notification-style preview.
        PreviewCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(appState.ui(Ui.ONB_WELCOME_TITLE),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (enabled && appState.reminderTimes.isNotEmpty())
                            appState.reminderTimes.first().label()
                        else "06:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(appState.ui(Ui.ONB_REMINDER_ENABLE), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { wanted ->
                    when {
                        !wanted -> {
                            appState.updateNotificationsEnabled(false)
                            ReminderScheduler.cancelAll(context)
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> enable()
                    }
                },
            )
        }
    }
}
