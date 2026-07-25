package com.srinivaskannan.divyaprabhandham.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.data.Essence
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.ReaderPalette
import com.srinivaskannan.divyaprabhandham.ui.theme.ReadingFonts

/**
 * The essence of a pasuram or of a whole decad: a short, precomputed thematic
 * summary in the reader's chosen language.
 *
 * A bottom sheet rather than a dialog, matching the iOS half-height sheet — it
 * keeps the verse visible above, which is the point: the essence is a gloss on
 * something you are still looking at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssenceSheet(
    number: Int?,
    essence: Essence?,
    palette: ReaderPalette,
    accent: Color,
    onDismiss: () -> Unit,
) {
    val appState = LocalAppState.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = appState.ui(Ui.ESSENCE_TITLE),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            )
            if (number != null) {
                Text(
                    text = "${appState.ui(Ui.PASURAM)} $number",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
            if (essence != null) {
                SelectionContainer {
                    Text(
                        text = essence.text(appState.scriptChoice),
                        style = TextStyle(
                            fontFamily = ReadingFonts.family(
                                context, appState.fontChoice, appState.scriptChoice,
                            ),
                            fontSize = 19.sp,
                            lineHeight = (19 * ReadingFonts
                                .lineHeightMultiplier(appState.scriptChoice)).sp,
                            color = palette.text,
                        ),
                    )
                }
            } else {
                Text(
                    text = appState.ui(Ui.ESSENCE_UNAVAILABLE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryText,
                )
            }
        }
    }
}
