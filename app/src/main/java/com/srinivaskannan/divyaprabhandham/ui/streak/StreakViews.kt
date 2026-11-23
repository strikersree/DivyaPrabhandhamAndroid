package com.srinivaskannan.divyaprabhandham.ui.streak

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.notify.ReminderScheduler
import com.srinivaskannan.divyaprabhandham.prefs.ReadingLog
import com.srinivaskannan.divyaprabhandham.prefs.ReadingPlace
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** The streak's own colour: a flame, whatever the accent. */
private val FlameTop = Color(0xFFFF9E29)
private val FlameBottom = Color(0xFFED4D21)
private val FlameSolid = Color(0xFFF57324)
private val Flame = Brush.verticalGradient(listOf(FlameTop, FlameBottom))
private val Missed = Color(0xFFD9363E)

/** Weekday and month names follow the menu language, as on iOS. */
private fun chromeLocale(tamil: Boolean): Locale =
    if (tamil) Locale.forLanguageTag("ta-IN") else Locale.US

// MARK: - Timing a reading screen

/**
 * Counts the time this screen is on screen with the app in front toward the
 * day's reading. [place] null means not counted.
 */
@Composable
fun ReadingTimeEffect(place: ReadingPlace?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateAsState()
    val reading = place != null && state.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(reading, place?.key) {
        if (!reading) return@LaunchedEffect
        val id = ReadingLog.begin(place ?: return@LaunchedEffect)
        try {
            while (true) {
                delay(10_000)
                ReadingLog.flush()
            }
        } finally {
            ReadingLog.end(id)
        }
    }
}

// MARK: - Home card

/** Home's reading summary: the streak, this week at a glance, total time and
 *  the favourite division. Opens the reading calendar. */
@Composable
fun ReadingStatsCard(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val haptics = LocalHapticFeedback.current
    val tamil = !appState.uiEnglish
    var showSheet by remember { mutableStateOf(false) }

    // Read so the card recomposes as the log changes.
    val days = ReadingLog.days
    val today = LocalDate.now()
    val todaySeconds = days[ReadingLog.dayKey(today)]?.seconds ?: 0.0
    val met = todaySeconds >= ReadingLog.DAILY_GOAL
    val streak = remember(days) { ReadingLog.currentStreak(today) }

    if (showSheet) ReadingCalendarSheet(onDismiss = { showSheet = false })

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showSheet = true
        },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlameBadge(filled = met, size = 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$streak",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            appState.ui(Ui.DAY_STREAK),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    if (met) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                appState.ui(Ui.TODAY_GOAL_MET),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    } else {
                        val minutes = kotlin.math.ceil((ReadingLog.DAILY_GOAL - todaySeconds) / 60).toInt()
                        Text(
                            if (tamil) "தொடரைக் காக்க இன்று இன்னும் $minutes நிமிடம் படியுங்கள்"
                            else "Read $minutes more min today to keep it going",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LinearProgressIndicator(
                            progress = { (todaySeconds / ReadingLog.DAILY_GOAL).toFloat().coerceIn(0f, 1f) },
                            color = FlameSolid,
                            modifier = Modifier.widthIn(max = 160.dp).fillMaxWidth(),
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WeekStrip(tamil = tamil, today = today)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatColumn(
                    appState.ui(Ui.TOTAL_TIME),
                    ReadingLog.durationText(ReadingLog.totalSeconds, tamil),
                    Modifier.weight(1f),
                )
                val longest = ReadingLog.longestStreak
                StatColumn(
                    appState.ui(Ui.LONGEST_STREAK),
                    if (tamil) "$longest நாள்" else "$longest days",
                    Modifier.weight(1f),
                )
                val favourite = ReadingLog.favouriteDivisionId
                    ?.let { id -> Division.all.firstOrNull { it.id == id } }
                    ?.title(appState.scriptChoice) ?: "—"
                StatColumn(appState.ui(Ui.FAVOURITE_DIVISION), favourite, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlameBadge(filled: Boolean, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(Flame)
                else Modifier.background(FlameSolid.copy(alpha = 0.14f)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment, contentDescription = null,
            tint = if (filled) Color.White else FlameSolid,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** This week, one dot per day: a flame for each day the goal was met. */
@Composable
private fun WeekStrip(tamil: Boolean, today: LocalDate) {
    val locale = chromeLocale(tamil)
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
    Row(Modifier.fillMaxWidth()) {
        for (offset in 0 until 7) {
            val date = start.plusDays(offset.toLong())
            val isToday = date == today
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ReadingLog.metGoal(date)) {
                    FlameBadge(filled = true, size = 24)
                } else {
                    Box(
                        Modifier
                            .size(24.dp)
                            .border(
                                width = if (isToday) 2.dp else 1.5.dp,
                                color = if (isToday) FlameSolid
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

// MARK: - Calendar sheet

private enum class DayState { MET, SHORT, MISSED, TODAY, PLAIN }

/**
 * The reading calendar: which days were read and which were missed, a month at
 * a time. Opens half way; tapping a day expands it to show that day's minutes
 * and where they were spent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingCalendarSheet(onDismiss: () -> Unit) {
    val appState = LocalAppState.current
    val haptics = LocalHapticFeedback.current
    val tamil = !appState.uiEnglish
    val locale = chromeLocale(tamil)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollState = rememberScrollState()

    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = YearMonth.parse(month)
    val days = ReadingLog.days
    val firstDay = remember(days) { ReadingLog.firstDay }
    val earliest = YearMonth.from(firstDay ?: today)
    val current = YearMonth.from(today)

    // A day picked: open the sheet all the way and bring its detail into view,
    // once the detail has been laid out.
    LaunchedEffect(selected) {
        if (selected == null) return@LaunchedEffect
        delay(50)
        sheetState.expand()
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                appState.ui(Ui.READING_CALENDAR),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile(Icons.Filled.LocalFireDepartment, "${ReadingLog.currentStreak(today)}",
                    appState.ui(Ui.DAY_STREAK), Modifier.weight(1f))
                SummaryTile(Icons.Filled.EmojiEvents, "${ReadingLog.longestStreak}",
                    appState.ui(Ui.LONGEST_STREAK), Modifier.weight(1f))
                SummaryTile(Icons.Filled.CalendarMonth, "${ReadingLog.daysMeetingGoal}",
                    appState.ui(Ui.DAYS_READ), Modifier.weight(1f))
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { month = shown.minusMonths(1).toString() },
                            enabled = shown > earliest,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, appState.ui(Ui.BACK))
                        }
                        Text(
                            shown.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(
                            onClick = { month = shown.plusMonths(1).toString() },
                            enabled = shown < current,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, appState.ui(Ui.NEXT_SECTION))
                        }
                    }
                    MonthGrid(
                        month = shown,
                        today = today,
                        firstDay = firstDay,
                        locale = locale,
                        selected = selected?.let { LocalDate.parse(it) },
                        onSelect = { date ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selected = date.toString()
                        },
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    ) {
                        LegendItem(Modifier.background(Flame), appState.ui(Ui.LEGEND_READ))
                        LegendItem(Modifier.background(FlameSolid.copy(alpha = 0.22f)), appState.ui(Ui.LEGEND_SHORT))
                        LegendItem(Modifier.background(Missed.copy(alpha = 0.18f)), appState.ui(Ui.LEGEND_MISSED))
                    }
                }
            }

            val day = selected?.let { LocalDate.parse(it) }
            if (day != null) {
                DayDetail(day, tamil, locale)
            } else {
                Text(
                    appState.ui(Ui.TAP_DAY_HINT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = FlameSolid, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LegendItem(fill: Modifier, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).then(fill))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    firstDay: LocalDate?,
    locale: Locale,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val weekStart = WeekFields.of(locale).firstDayOfWeek
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - weekStart.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    weekStart.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        val date = week.getOrNull(i)
                        if (date != null) {
                            DayCell(
                                date = date,
                                state = dayState(date, today, firstDay),
                                isToday = date == today,
                                isSelected = date == selected,
                                isFuture = date.isAfter(today),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dayState(date: LocalDate, today: LocalDate, firstDay: LocalDate?): DayState = when {
    ReadingLog.metGoal(date) -> DayState.MET
    ReadingLog.seconds(date) > 0 -> DayState.SHORT
    date == today -> DayState.TODAY
    date.isBefore(today) && firstDay != null && !date.isBefore(firstDay) -> DayState.MISSED
    else -> DayState.PLAIN
}

@Composable
private fun DayCell(
    date: LocalDate,
    state: DayState,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val background = when (state) {
        DayState.MET -> Modifier.background(Flame)
        DayState.SHORT -> Modifier.background(FlameSolid.copy(alpha = 0.22f))
        DayState.MISSED -> Modifier.background(Missed.copy(alpha = 0.10f))
        else -> Modifier
    }
    val foreground = when (state) {
        DayState.MET -> Color.White
        DayState.MISSED -> Missed.copy(alpha = 0.8f)
        DayState.SHORT, DayState.TODAY -> onSurface
        DayState.PLAIN -> onSurface.copy(alpha = if (isFuture) 0.3f else 0.6f)
    }
    Box(
        Modifier
            .size(40.dp)
            .then(if (isSelected) Modifier.border(2.dp, onSurface, CircleShape) else Modifier)
            .padding(3.dp)
            .clip(CircleShape)
            .then(background)
            .then(if (isToday && state != DayState.MET) Modifier.border(2.dp, FlameSolid, CircleShape) else Modifier)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (state == DayState.MET || isToday) FontWeight.Bold else FontWeight.Normal,
            color = foreground,
        )
    }
}

// MARK: - One day

/** A day's minutes and where they were spent, longest first. */
@Composable
private fun DayDetail(date: LocalDate, tamil: Boolean, locale: Locale) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val script = appState.scriptChoice
    val day = ReadingLog.day(date)
    val total = day?.seconds ?: 0.0
    val places = day?.places.orEmpty().mapNotNull { (key, seconds) ->
        ReadingPlace.fromKey(key)?.let { it to seconds }
    }.sortedByDescending { it.second }

    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ReadingLog.durationText(total, tamil),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (total >= ReadingLog.DAILY_GOAL) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = FlameSolid)
                    }
                }
            }
            if (places.isEmpty()) {
                Text(
                    appState.ui(Ui.NO_READING_THAT_DAY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                places.forEach { (place, seconds) ->
                    val (icon, name, kind) = when (place) {
                        is ReadingPlace.Division -> Triple(
                            Icons.AutoMirrored.Filled.MenuBook,
                            Division.all.firstOrNull { it.id == place.id }?.title(script) ?: place.id,
                            appState.ui(Ui.BOOK),
                        )
                        is ReadingPlace.Desam -> Triple(
                            Icons.Filled.AccountBalance,
                            repository.divyaDesams.firstOrNull { it.id == place.id }?.name(script) ?: place.id,
                            appState.ui(Ui.DIVYA_DESAMS),
                        )
                        is ReadingPlace.Collection -> Triple(
                            Icons.Filled.CollectionsBookmark,
                            appState.collection(place.id)?.displayName(script) ?: place.id,
                            appState.ui(Ui.COLLECTIONS),
                        )
                    }
                    PlaceRow(icon, name, kind, seconds, total, tamil)
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(icon: ImageVector, name: String, kind: String, seconds: Double, total: Double, tamil: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = FlameSolid, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    kind,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                ReadingLog.durationText(seconds, tamil),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            Modifier
                .padding(start = 32.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((seconds / maxOf(total, 1.0)).toFloat().coerceIn(0.04f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Flame),
            )
        }
    }
}

// MARK: - Goal-met moment

/**
 * The brief "🔥 5-day streak!" banner the moment today's two minutes are in.
 * The first time, also when notification permission is asked for (Android 13+),
 * since the streak reminder is what it is for — as on iOS.
 */
@Composable
fun StreakCelebrationBanner(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var shown by remember { mutableStateOf<Int?>(null) }
    var visible by remember { mutableStateOf(false) }
    // Bumped per celebration, so the hide below is keyed on something this
    // composable owns. Keying it on ReadingLog.celebration cancelled it the
    // moment that was cleared, and the banner never went away.
    var showing by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) ReminderScheduler.reschedule(context, appState) }

    val celebration = ReadingLog.celebration
    LaunchedEffect(celebration) {
        val streak = celebration ?: return@LaunchedEffect
        ReadingLog.celebration = null
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        shown = streak
        visible = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReadingLog.askedNotificationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ReadingLog.askedNotificationPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        showing++
    }
    LaunchedEffect(showing) {
        if (showing == 0) return@LaunchedEffect
        delay(3_000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = FlameSolid)
                Spacer(Modifier.width(10.dp))
                Column {
                    val streak = shown ?: 0
                    Text(
                        if (appState.uiEnglish) "$streak-day streak!" else "$streak நாள் தொடர்!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        appState.ui(Ui.STREAK_CELEBRATION),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
