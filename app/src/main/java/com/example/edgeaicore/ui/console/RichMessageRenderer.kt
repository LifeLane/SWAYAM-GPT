package com.example.edgeaicore.ui.console

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAIGreen

/**
 * Rich message content renderer supporting:
 * - Markdown formatted text (Headers, Bold, Italic, Bullet lists, Numbered lists, Blockquotes)
 * - Code blocks with syntax badge and copy button
 * - JSON viewer with formatted tree structure and copy button
 * - Markdown tables formatted into clean Compose grids
 * - Key-value metric charts & progress bars
 */
@Composable
fun RichMessageContent(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isUser: Boolean = false
) {
    if (isUser) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = modifier
        )
        return
    }

    val blocks = remember(text) { parseMessageBlocks(text) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MessageBlock.Header -> {
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            2 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is MessageBlock.Code -> {
                    CodeBlockView(language = block.language, code = block.code)
                }
                is MessageBlock.JsonBlock -> {
                    JsonBlockView(json = block.rawJson)
                }
                is MessageBlock.Table -> {
                    TableView(headers = block.headers, rows = block.rows)
                }
                is MessageBlock.ChartData -> {
                    MetricChartView(title = block.title, items = block.items)
                }
                is MessageBlock.BlockQuote -> {
                    BlockQuoteView(text = block.text)
                }
                is MessageBlock.FormattedText -> {
                    FormattedTextView(text = block.text, textColor = textColor)
                }
            }
        }
    }
}

sealed class MessageBlock {
    data class Header(val level: Int, val text: String) : MessageBlock()
    data class Code(val language: String, val code: String) : MessageBlock()
    data class JsonBlock(val rawJson: String) : MessageBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MessageBlock()
    data class ChartData(val title: String, val items: List<Pair<String, Float>>) : MessageBlock()
    data class BlockQuote(val text: String) : MessageBlock()
    data class FormattedText(val text: String) : MessageBlock()
}

/**
 * Parses raw markdown into structured blocks.
 */
private fun parseMessageBlocks(rawText: String): List<MessageBlock> {
    val blocks = mutableListOf<MessageBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Code Block ```
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            val fullCode = codeLines.joinToString("\n")
            if (lang.equals("json", ignoreCase = true) || (fullCode.trimStart().startsWith("{") && fullCode.trimEnd().endsWith("}"))) {
                blocks.add(MessageBlock.JsonBlock(fullCode))
            } else {
                blocks.add(MessageBlock.Code(language = lang.ifBlank { "code" }, code = fullCode))
            }
            continue
        }

        // 2. Markdown Table (| col1 | col2 |)
        if (line.trim().startsWith("|") && line.trim().endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("---")) {
            val headers = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            i += 2 // skip header and separator
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val rowCells = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (rowCells.isNotEmpty()) {
                    rows.add(rowCells)
                }
                i++
            }
            blocks.add(MessageBlock.Table(headers, rows))
            continue
        }

        // 3. Headers (#, ##, ###)
        if (line.startsWith("### ")) {
            blocks.add(MessageBlock.Header(3, line.removePrefix("### ").trim()))
            i++
            continue
        } else if (line.startsWith("## ")) {
            blocks.add(MessageBlock.Header(2, line.removePrefix("## ").trim()))
            i++
            continue
        } else if (line.startsWith("# ")) {
            blocks.add(MessageBlock.Header(1, line.removePrefix("# ").trim()))
            i++
            continue
        }

        // 4. Blockquotes (> )
        if (line.startsWith("> ")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].startsWith("> ")) {
                quoteLines.add(lines[i].removePrefix("> ").trim())
                i++
            }
            blocks.add(MessageBlock.BlockQuote(quoteLines.joinToString(" ")))
            continue
        }

        // 5. Standard text / paragraph
        val textLines = mutableListOf<String>()
        while (i < lines.size &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].startsWith("# ") &&
            !lines[i].startsWith("## ") &&
            !lines[i].startsWith("### ") &&
            !lines[i].startsWith("> ") &&
            !(lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("---"))
        ) {
            textLines.add(lines[i])
            i++
        }

        val textContent = textLines.joinToString("\n").trim()
        if (textContent.isNotEmpty()) {
            blocks.add(MessageBlock.FormattedText(textContent))
        }
    }

    return blocks.ifEmpty { listOf(MessageBlock.FormattedText(rawText)) }
}

@Composable
fun CodeBlockView(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Header bar with language badge and copy action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4D4D4),
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code Snippet", code))
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) LocalAIGreen else Color(0xFFCCCCCC),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isCopied) "Copied!" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCopied) LocalAIGreen else Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFFDCDCAA)
                )
            }
        }
    }
}

@Composable
fun JsonBlockView(
    json: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E222B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3846)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF262C38))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = Color(0xFF79C0FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "JSON STRUCTURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF79C0FF),
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("JSON Payload", json))
                            isCopied = true
                            Toast.makeText(context, "JSON copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy JSON",
                        tint = if (isCopied) LocalAIGreen else Color(0xFFCCCCCC),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isCopied) "Copied!" else "Copy JSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCopied) LocalAIGreen else Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = json,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFF9CDCFE)
                )
            }
        }
    }
}

@Composable
fun TableView(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Headers
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 220.dp)
                            .padding(end = 12.dp)
                    )
                }
            }

            HorizontalDivider()

            // Rows
            rows.forEachIndexed { index, row ->
                val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 220.dp)
                                .padding(end = 12.dp)
                        )
                    }
                }
                if (index < rows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun MetricChartView(
    title: String,
    items: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items.forEach { (label, value) ->
                val progress = value.coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun BlockQuoteView(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FormattedTextView(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text) { formatMarkdownText(text) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = textColor,
        modifier = modifier
    )
}

/**
 * Converts inline bold (**), italic (*), and inline code (`) into AnnotatedString.
 */
private fun formatMarkdownText(input: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = input.length

        while (cursor < length) {
            // Bold **text**
            if (cursor + 1 < length && input[cursor] == '*' && input[cursor + 1] == '*') {
                val end = input.indexOf("**", cursor + 2)
                if (end != -1) {
                    val boldText = input.substring(cursor + 2, end)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldText)
                    }
                    cursor = end + 2
                    continue
                }
            }

            // Inline Code `text`
            if (input[cursor] == '`') {
                val end = input.indexOf('`', cursor + 1)
                if (end != -1) {
                    val codeText = input.substring(cursor + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22888888),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" $codeText ")
                    }
                    cursor = end + 1
                    continue
                }
            }

            // Bullet Point replacement
            if ((cursor == 0 || input[cursor - 1] == '\n') && (input.startsWith("• ", cursor) || input.startsWith("- ", cursor) || input.startsWith("* ", cursor))) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))) {
                    append(" • ")
                }
                cursor += 2
                continue
            }

            append(input[cursor])
            cursor++
        }
    }
}
