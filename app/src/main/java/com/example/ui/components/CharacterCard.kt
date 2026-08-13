package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterProfile

@Composable
fun CharacterCard(
    character: CharacterProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = when (character.role.lowercase()) {
        "protagonist" -> Color(0xFFE2B563)
        "antagonist" -> Color(0xFFFF5252)
        else -> Color(0xFF64B5F6)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("character_card_${character.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C182A)),
        border = BorderStroke(
            width = 1.dp,
            color = if (character.conflictCount > 0) Color(0xFFFFB020) else Color(0xFF2E2942)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.2f))
                            .border(1.dp, roleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = character.primaryName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }

                    Column {
                        Text(
                            text = character.primaryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(roleColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = character.role,
                                    color = roleColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (character.conflictCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33FFB020))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${character.conflictCount} Conflicts",
                                            color = Color(0xFFFFC107),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit profile",
                            tint = Color(0xFFE2B563),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete character",
                            tint = Color(0xFF8E88A8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (character.aliases.isNotBlank()) {
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "Aliases / Spellings: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA099B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = character.aliases,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD4CEE8)
                    )
                }
            }

            if (character.physicalAttributes.isNotBlank()) {
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "Physical Traits: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA099B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = character.physicalAttributes,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD4CEE8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (character.ageTimeline.isNotBlank()) {
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "Timeline / Age: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA099B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = character.ageTimeline,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE2B563)
                    )
                }
            }

            if (character.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = character.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0A8CC),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
