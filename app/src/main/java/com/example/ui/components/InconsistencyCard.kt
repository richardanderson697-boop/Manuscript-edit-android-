package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Inconsistency
import com.example.data.model.InconsistencySeverity
import com.example.data.model.InconsistencyStatus
import com.example.data.model.InconsistencyType

@Composable
fun InconsistencyCard(
    inconsistency: Inconsistency,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCustomFix: (String) -> Unit,
    onAiAdjust: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showCustomFixDialog by remember { mutableStateOf(false) }
    var customFixInput by remember { mutableStateOf(inconsistency.suggestedFix) }
    var isExpanded by remember { mutableStateOf(true) }

    val typeLabel = when (inconsistency.type) {
        InconsistencyType.NAME_MISMATCH -> "Name Mismatch"
        InconsistencyType.DATE_CHRONOLOGY -> "Date & Timeline"
        InconsistencyType.PLOT_TIMELINE -> "Plot Continuity"
        InconsistencyType.CHARACTER_TRAIT -> "Character Trait"
    }

    val typeColor = when (inconsistency.type) {
        InconsistencyType.NAME_MISMATCH -> Color(0xFF64B5F6)
        InconsistencyType.DATE_CHRONOLOGY -> Color(0xFFFFB74D)
        InconsistencyType.PLOT_TIMELINE -> Color(0xFFE57373)
        InconsistencyType.CHARACTER_TRAIT -> Color(0xFFA1887F)
    }

    val severityBg = when (inconsistency.severity) {
        InconsistencySeverity.HIGH -> Color(0x33FF5252)
        InconsistencySeverity.MEDIUM -> Color(0x33FFB020)
        InconsistencySeverity.LOW -> Color(0x3342A5F5)
    }

    val severityText = when (inconsistency.severity) {
        InconsistencySeverity.HIGH -> Color(0xFFFF5252)
        InconsistencySeverity.MEDIUM -> Color(0xFFFFC107)
        InconsistencySeverity.LOW -> Color(0xFF64B5F6)
    }

    val statusBg = when (inconsistency.status) {
        InconsistencyStatus.ACCEPTED -> Color(0x334CAF50)
        InconsistencyStatus.CUSTOM_FIX -> Color(0x332196F3)
        InconsistencyStatus.REJECTED -> Color(0x339E9E9E)
        InconsistencyStatus.PENDING -> Color(0x22FFFFFF)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inconsistency_card_${inconsistency.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211D32)),
        border = BorderStroke(
            width = 1.dp,
            color = if (inconsistency.status == InconsistencyStatus.ACCEPTED) Color(0xFF4CAF50)
            else if (inconsistency.status == InconsistencyStatus.REJECTED) Color(0xFF616161)
            else typeColor.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(severityBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = inconsistency.severity.name,
                            color = severityText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ch ${inconsistency.chapterIndex} : Line ${inconsistency.lineNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA099B8)
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle expand",
                            tint = Color(0xFFB0A8CC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original Text Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161322))
                    .border(1.dp, Color(0xFF332E4A), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "ORIGINAL TEXT IN MANUSCRIPT:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8A80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${inconsistency.originalText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Context & Explanation
                    Text(
                        text = inconsistency.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4CEE8),
                        lineHeight = 18.sp
                    )

                    if (inconsistency.contextSnippet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Context: ${inconsistency.contextSnippet}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9E97B8),
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Suggested Fix Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF13281E))
                            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (inconsistency.status == InconsistencyStatus.CUSTOM_FIX) "AUTHOR CUSTOM FIX:" else "SUGGESTED CORRECTION:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF81C784),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (onAiAdjust != null && inconsistency.status == InconsistencyStatus.PENDING) {
                                    TextButton(
                                        onClick = onAiAdjust,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .height(24.dp)
                                            .testTag("ai_rethink_button_${inconsistency.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFE2B563),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "AI Rethink",
                                            fontSize = 10.sp,
                                            color = Color(0xFFE2B563),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = inconsistency.userCustomFix ?: inconsistency.suggestedFix,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA5D6A7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (inconsistency.status == InconsistencyStatus.PENDING) {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("accept_fix_button_${inconsistency.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF388E3C),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showCustomFixDialog = true },
                                modifier = Modifier.testTag("custom_edit_button_${inconsistency.id}"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2B563)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2B563)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adjust", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = onReject,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("reject_fix_button_${inconsistency.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFFB0A8CC)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (inconsistency.status) {
                                        InconsistencyStatus.ACCEPTED -> "FIX APPLIED TO CHAPTER"
                                        InconsistencyStatus.CUSTOM_FIX -> "CUSTOM FIX APPLIED"
                                        InconsistencyStatus.REJECTED -> "DISMISSED BY AUTHOR"
                                        InconsistencyStatus.PENDING -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (inconsistency.status) {
                                        InconsistencyStatus.ACCEPTED -> Color(0xFF81C784)
                                        InconsistencyStatus.CUSTOM_FIX -> Color(0xFF64B5F6)
                                        InconsistencyStatus.REJECTED -> Color(0xFF9E9E9E)
                                        InconsistencyStatus.PENDING -> Color.White
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomFixDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFixDialog = false },
            title = {
                Text(
                    text = "Manual Fix Adjustment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your preferred text to replace '${inconsistency.originalText}':",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD0C8E8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customFixInput,
                        onValueChange = { customFixInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_fix_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF443D5E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomFixDialog = false
                        onCustomFix(customFixInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B563), contentColor = Color(0xFF12101C)),
                    modifier = Modifier.testTag("save_custom_fix_button")
                ) {
                    Text("Apply Custom Fix", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFixDialog = false }) {
                    Text("Cancel", color = Color(0xFFB0A8CC))
                }
            },
            containerColor = Color(0xFF26213A)
        )
    }
}
