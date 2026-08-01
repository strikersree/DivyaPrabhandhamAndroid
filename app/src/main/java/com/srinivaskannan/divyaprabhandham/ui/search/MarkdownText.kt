package com.srinivaskannan.divyaprabhandham.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders the light Markdown the model returns — headers (#/##/###), bold
 * (**...**), and bullet lists (* / -) — as real Compose text, so answers read
 * cleanly instead of showing raw ** and ## the way an unformatted Text does.
 *
 * Deliberately small: it handles the constructs the Ask model actually emits,
 * not a full CommonMark parser. Inline bold is parsed within each line;
 * block-level structure (headers, bullets) is handled line by line.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    inlineBold(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                is MdBlock.Bullet -> Row(Modifier.padding(vertical = 2.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(inlineBold(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MdBlock.Paragraph -> if (block.text.isNotBlank()) {
                    Text(
                        inlineBold(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                } else {
                    Spacer(Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
}

private fun parseBlocks(text: String): List<MdBlock> =
    text.replace("\r\n", "\n").split("\n").map { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("### ") -> MdBlock.Heading(3, trimmed.removePrefix("### ").trim())
            trimmed.startsWith("## ") -> MdBlock.Heading(2, trimmed.removePrefix("## ").trim())
            trimmed.startsWith("# ") -> MdBlock.Heading(1, trimmed.removePrefix("# ").trim())
            trimmed.startsWith("* ") -> MdBlock.Bullet(trimmed.removePrefix("* ").trim())
            trimmed.startsWith("- ") -> MdBlock.Bullet(trimmed.removePrefix("- ").trim())
            else -> MdBlock.Paragraph(line)
        }
    }

/**
 * Parses inline **bold** within a line into an AnnotatedString. Also strips any
 * stray leading/trailing bold markers a heading line may carry (the model often
 * writes `### **Title**`).
 */
private fun inlineBold(input: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = input
    while (i < s.length) {
        val start = s.indexOf("**", i)
        if (start < 0) {
            append(s.substring(i))
            break
        }
        append(s.substring(i, start))
        val end = s.indexOf("**", start + 2)
        if (end < 0) {
            // Unmatched marker: drop it rather than print raw **.
            append(s.substring(start + 2))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(s.substring(start + 2, end))
        }
        i = end + 2
    }
}
