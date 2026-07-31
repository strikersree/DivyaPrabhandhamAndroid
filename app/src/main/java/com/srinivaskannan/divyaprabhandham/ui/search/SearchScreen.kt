package com.srinivaskannan.divyaprabhandham.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.ask.AskClient
import com.srinivaskannan.divyaprabhandham.ask.AskConversation
import com.srinivaskannan.divyaprabhandham.ask.AskError
import com.srinivaskannan.divyaprabhandham.ask.AskMessage
import com.srinivaskannan.divyaprabhandham.ask.AskResult
import com.srinivaskannan.divyaprabhandham.data.BookSection
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.launch

/**
 * The Ask tab: one input that does the cheap thing while typing and the smart
 * thing on submit.
 *
 * Typing runs the instant, offline, free local search — number-jump and text
 * matches over the corpus — surfaced as tappable cards. Submitting a question
 * sends it (with retrieved corpus context) to the Gemini proxy and adds a chat
 * answer. There is no Ask toggle: the single box behaves as search while typing
 * and as Ask on send, so nothing about the old fast search is lost.
 *
 * Layout is a conversation window — thread above, input pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()

    val conversation = remember { AskConversation() }
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()

    val jumpTarget = remember(trimmed) {
        trimmed.toIntOrNull()?.let { number -> repository.location(number)?.let { number to it } }
    }
    val liveMatches = remember(trimmed, appState.scriptChoice) {
        if (trimmed.isEmpty() || trimmed.toIntOrNull() != null) emptyList()
        else repository.filteredWorks(trimmed, appState.scriptChoice)
            .flatMap { work -> work.sections.map { work to it } }
            .take(8)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(conversation.messages.size, conversation.pending) {
        val count = conversation.messages.size + if (conversation.pending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun submit() {
        val q = trimmed
        if (q.isEmpty() || conversation.pending) return

        // A bare number is a jump, not a question — do it locally, for free.
        if (q.toIntOrNull() != null) {
            jumpTarget?.let { (_, loc) -> onOpenSection(loc.first, loc.second) }
            return
        }

        appState.noteSearch(q)
        conversation.addUser(q)

        val matchIds = repository.filteredWorks(q, appState.scriptChoice)
            .flatMap { it.sections }.map { it.id }.distinct().take(6)
        conversation.addLocalMatches(matchIds)

        val context = repository.askContext(q, appState.scriptChoice)
        query = ""
        conversation.setPending(true)
        scope.launch {
            val result = AskClient.ask(question = q, context = context)
            conversation.setPending(false)
            when (result) {
                is AskResult.Answer -> conversation.addAnswer(result.text)
                is AskResult.Error -> conversation.addFailure(result.kind)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (conversation.messages.isEmpty()) {
                AskIntro(
                    recents = appState.recentSearches,
                    liveMatches = liveMatches,
                    jumpChip = jumpTarget?.let { (n, _) -> n },
                    onPick = { query = it },
                    onOpenSection = onOpenSection,
                    onJump = { jumpTarget?.let { (_, loc) -> onOpenSection(loc.first, loc.second) } },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(conversation.messages, key = { it.id }) { message ->
                        when (message) {
                            is AskMessage.User -> UserBubble(message.text)
                            is AskMessage.Assistant -> AnswerBubble(message.text)
                            is AskMessage.Failure -> AnswerBubble(errorText(appState, message.kind), isError = true)
                            is AskMessage.LocalMatches -> LocalMatchCards(message.sectionIds, onOpenSection)
                        }
                    }
                    if (conversation.pending) {
                        item(key = "pending") { ThinkingBubble() }
                    }
                }
            }
        }

        if (conversation.messages.isNotEmpty() && liveMatches.isNotEmpty()) {
            LiveMatchStrip(liveMatches, onOpenSection)
        }

        InputBar(
            value = query,
            onValueChange = { query = it },
            onSend = ::submit,
            sending = conversation.pending,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AnswerBubble(text: String, isError: Boolean = false) {
    val appState = LocalAppState.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(Modifier.widthIn(max = 320.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AiSpark()
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (!isError) {
                Text(
                    appState.ui(Ui.ASK_DISCLAIMER),
                    Modifier.padding(start = 32.dp, top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    val appState = LocalAppState.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiSpark()
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(appState.ui(Ui.ASK_THINKING), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** The small gradient mark standing in for the assistant — a tasteful nod to a
 *  Gemini-style accent, not a copy of any brand. */
@Composable
private fun AiSpark() {
    Box(
        Modifier
            .size(24.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
                shape = RoundedCornerShape(50),
            ),
    )
}

@Composable
private fun LocalMatchCards(
    sectionIds: List<String>,
    onOpenSection: (String, String?) -> Unit,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    if (sectionIds.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            appState.ui(Ui.ASK_LOCAL_MATCHES),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        sectionIds.mapNotNull { repository.section(it) }.forEach { section ->
            Surface(
                onClick = { onOpenSection(section.id, null) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    section.title(appState.scriptChoice),
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LiveMatchStrip(
    matches: List<Pair<Work, BookSection>>,
    onOpenSection: (String, String?) -> Unit,
) {
    val appState = LocalAppState.current
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 180.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(matches, key = { it.second.id }) { (work, section) ->
                Surface(
                    onClick = { onOpenSection(section.id, null) },
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(section.title(appState.scriptChoice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text(work.title(appState.scriptChoice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AskIntro(
    recents: List<String>,
    liveMatches: List<Pair<Work, BookSection>>,
    jumpChip: Int?,
    onPick: (String) -> Unit,
    onOpenSection: (String, String?) -> Unit,
    onJump: () -> Unit,
) {
    val appState = LocalAppState.current
    if (jumpChip != null || liveMatches.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (jumpChip != null) {
                Surface(
                    onClick = onJump,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${appState.ui(Ui.GO_TO_PASURAM)} $jumpChip",
                        Modifier.padding(14.dp), fontWeight = FontWeight.Medium)
                }
            }
            LiveMatchStrip(liveMatches, onOpenSection)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AiSpark()
        Spacer(Modifier.size(12.dp))
        Text(appState.ui(Ui.ASK_INTRO_TITLE),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(appState.ui(Ui.ASK_INTRO_BODY),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (recents.isNotEmpty()) {
            Spacer(Modifier.size(20.dp))
            recents.take(5).forEach { term ->
                Surface(
                    onClick = { onPick(term) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(term, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    sending: Boolean,
) {
    val appState = LocalAppState.current
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(appState.ui(Ui.ASK_PLACEHOLDER), maxLines = 1) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !sending,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send,
                    contentDescription = appState.ui(Ui.SEARCH),
                    tint = if (value.isNotBlank() && !sending)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun errorText(appState: AppState, kind: AskError): String =
    when (kind) {
        AskError.OFFLINE -> appState.ui(Ui.ASK_ERR_OFFLINE)
        AskError.TIMEOUT -> appState.ui(Ui.ASK_ERR_TIMEOUT)
        AskError.RATE_LIMITED -> appState.ui(Ui.ASK_ERR_RATE)
        AskError.UNAUTHORIZED, AskError.SERVER, AskError.EMPTY -> appState.ui(Ui.ASK_ERR_SERVER)
    }
