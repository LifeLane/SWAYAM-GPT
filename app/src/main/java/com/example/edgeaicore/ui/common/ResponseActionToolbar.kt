package com.example.edgeaicore.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.analytics.ToolFunctionType
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.ui.theme.LocalAIGreen
import com.example.ui.theme.PrivateServerAmber

/**
 * Universal Icon-Based Toolset for SWAYAM AI Output Responses.
 * Provides:
 * - 📋 Copy (to system clipboard)
 * - 🌐 Translate (default Hindi & Bengali, plus Sanskrit, Spanish, etc.)
 * - ↗️ Share (native Android share sheet)
 * - 💾 Export (save as markdown / text / memory vault note)
 * - 🔄 Regenerate (re-run query through SWAYAM Core)
 * - ℹ️ Why this answer? (Provenance & Hardware Telemetry)
 */
@Composable
fun ResponseActionToolbar(
    responseText: String,
    translatedText: String? = null,
    activeLanguage: String? = null,
    onTranslate: (targetLanguage: String) -> Unit,
    onRevertTranslation: () -> Unit,
    onRegenerate: () -> Unit,
    onExport: ((text: String) -> Unit)? = null,
    explanation: ExplanationRecord? = null,
    onShowExplanation: ((ExplanationRecord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTranslateMenuOpen by remember { mutableStateOf(false) }
    var isExportMenuOpen by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    val currentTextToShareOrCopy = translatedText ?: responseText

    Column(modifier = modifier.fillMaxWidth()) {
        // Active translation pill indicator if translated
        if (activeLanguage != null && translatedText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable { onRevertTranslation() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    Text(
                        text = "Translated to $activeLanguage • Tap to revert to English",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Revert", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(12.dp))
                }
            }
        }

        // Icon-based Action Buttons Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. COPY ACTION
                ActionButtonIcon(
                    icon = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                    contentDescription = "Copy response",
                    tint = if (isCopied) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "action_copy_btn",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SWAYAM AI Response", currentTextToShareOrCopy)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        EdgeAICore.getInstance(context).analytics.trackToolUsage(
                            ToolFunctionType.COPY,
                            mapOf("length" to currentTextToShareOrCopy.length.toString())
                        )
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                // 2. TRANSLATE ACTION (Hindi & Bengali Default)
                Box {
                    ActionButtonIcon(
                        icon = Icons.Outlined.Translate,
                        contentDescription = "Translate response",
                        tint = if (activeLanguage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "action_translate_btn",
                        onClick = { isTranslateMenuOpen = true }
                    )

                    DropdownMenu(
                        expanded = isTranslateMenuOpen,
                        onDismissRequest = { isTranslateMenuOpen = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🇮🇳", fontSize = 16.sp)
                                    Column {
                                        Text("Hindi (हिन्दी)", fontWeight = FontWeight.Bold)
                                        Text("Default primary language", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "Hindi")
                                )
                                onTranslate("Hindi")
                            },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🇮🇳", fontSize = 16.sp)
                                    Column {
                                        Text("Bengali (বাংলা)", fontWeight = FontWeight.Bold)
                                        Text("Default primary language", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "Bengali")
                                )
                                onTranslate("Bengali")
                            },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = PrivateServerAmber) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("Sanskrit (संस्कृतम्)") },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "Sanskrit")
                                )
                                onTranslate("Sanskrit")
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Spanish (Español)") },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "Spanish")
                                )
                                onTranslate("Spanish")
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("French (Français)") },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "French")
                                )
                                onTranslate("French")
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("German (Deutsch)") },
                            onClick = {
                                isTranslateMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.TRANSLATE,
                                    mapOf("targetLanguage" to "German")
                                )
                                onTranslate("German")
                            }
                        )

                        if (activeLanguage != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Revert to Original (English)", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    isTranslateMenuOpen = false
                                    onRevertTranslation()
                                },
                                leadingIcon = { Icon(Icons.Default.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                // 3. SHARE ACTION
                ActionButtonIcon(
                    icon = Icons.Outlined.Share,
                    contentDescription = "Share response",
                    testTag = "action_share_btn",
                    onClick = {
                        EdgeAICore.getInstance(context).analytics.trackToolUsage(
                            ToolFunctionType.SHARE,
                            mapOf("length" to currentTextToShareOrCopy.length.toString())
                        )
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, currentTextToShareOrCopy)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share SWAYAM AI Response")
                        context.startActivity(shareIntent)
                    }
                )

                // 4. EXPORT ACTION
                Box {
                    ActionButtonIcon(
                        icon = Icons.Outlined.SaveAlt,
                        contentDescription = "Export response",
                        testTag = "action_export_btn",
                        onClick = {
                            if (onExport != null) {
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.EXPORT,
                                    mapOf("destination" to "Custom Callback")
                                )
                                onExport(currentTextToShareOrCopy)
                            } else {
                                isExportMenuOpen = true
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = isExportMenuOpen,
                        onDismissRequest = { isExportMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save to Personal Memory Vault") },
                            onClick = {
                                isExportMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.EXPORT,
                                    mapOf("destination" to "Memory Vault")
                                )
                                if (onExport != null) {
                                    onExport(currentTextToShareOrCopy)
                                } else {
                                    Toast.makeText(context, "Saved to Memory Vault", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Markdown (.md)") },
                            onClick = {
                                isExportMenuOpen = false
                                EdgeAICore.getInstance(context).analytics.trackToolUsage(
                                    ToolFunctionType.EXPORT,
                                    mapOf("destination" to "Markdown File")
                                )
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, currentTextToShareOrCopy)
                                    putExtra(Intent.EXTRA_SUBJECT, "SWAYAM_Response.md")
                                    type = "text/markdown"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Markdown"))
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )
                    }
                }

                // 5. REGENERATE ACTION
                ActionButtonIcon(
                    icon = Icons.Outlined.Refresh,
                    contentDescription = "Regenerate response",
                    testTag = "action_regenerate_btn",
                    onClick = {
                        EdgeAICore.getInstance(context).analytics.trackToolUsage(ToolFunctionType.REGENERATE)
                        onRegenerate()
                    }
                )
            }

            // 6. "WHY THIS ANSWER?" PROVENANCE LINK
            if (explanation != null && onShowExplanation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            EdgeAICore.getInstance(context).analytics.trackToolUsage(ToolFunctionType.PROVENANCE)
                            onShowExplanation(explanation)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Why this answer?",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testTag: String = ""
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}
