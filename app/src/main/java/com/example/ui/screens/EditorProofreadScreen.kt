package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InconsistencyType
import com.example.ui.components.InconsistencyCard
import com.example.ui.viewmodel.ManuscriptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorProofreadScreen(
    viewModel: ManuscriptViewModel,
    modifier: Modifier = Modifier
) {
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val chapters by viewModel.activeChapters.collectAsState()
    val inconsistencies by viewModel.activeInconsistencies.collectAsState()
    val selectedChapterIdx by viewModel.selectedChapterIndex.collectAsState()
    val filterType by viewModel.inconsistencyFilterType.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Inconsistencies Inspector, 1: Live Manuscript Reader / Manual Edit
    var chapterDropdownExpanded by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var newChapterContent by remember { mutableStateOf("") }

    val filteredInconsistencies = inconsistencies.filter { inc ->
        (selectedChapterIdx == 0 || inc.chapterIndex == selectedChapterIdx) &&
                (filterType == null || inc.type == filterType)
    }

    val activeChapterObj = if (selectedChapterIdx > 0) chapters.find { it.chapterIndex == selectedChapterIdx } else null

    var editableChapterText by remember(activeChapterObj) {
        mutableStateOf(activeChapterObj?.modifiedContent ?: "")
    }

    if (showAddChapterDialog) {
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            containerColor = Color(0xFF1C182A),
            title = {
                Text(
                    text = "Add Chapter to '${activeManuscript?.title ?: "Manuscript"}'",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your next chapter text below. It will be linked as Chapter ${(chapters.maxOfOrNull { it.chapterIndex } ?: 0) + 1} and scanned for cross-chapter continuity.",
                        color = Color(0xFFC3BBDC),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newChapterContent,
                        onValueChange = { newChapterContent = it },
                        placeholder = { Text("Paste chapter text here... (Supports 'Chapter 2', 'Chapter 3' headings)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeMsId = activeManuscript?.id
                        if (activeMsId != null && newChapterContent.isNotBlank()) {
                            viewModel.appendChaptersToManuscript(activeMsId, newChapterContent)
                            newChapterContent = ""
                            showAddChapterDialog = false
                        }
                    },
                    enabled = newChapterContent.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B563), contentColor = Color(0xFF12101C))
                ) {
                    Text("Add & Cross-Scan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) {
                    Text("Cancel", color = Color(0xFFA099B8))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12101C))
    ) {
        // Top Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C182A))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeManuscript?.title ?: "No Manuscript Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Chapter Dropdown Selector
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { chapterDropdownExpanded = true }
                                .padding(vertical = 2.dp)
                                .testTag("chapter_selector_dropdown")
                        ) {
                            Text(
                                text = if (selectedChapterIdx == 0) "Full Manuscript (${chapters.size} Chapters)"
                                else activeChapterObj?.title ?: "Chapter $selectedChapterIdx",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2B563),
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFFE2B563)
                            )
                        }

                        DropdownMenu(
                            expanded = chapterDropdownExpanded,
                            onDismissRequest = { chapterDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF26213A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Full Manuscript (All Chapters)", color = Color.White) },
                                onClick = {
                                    viewModel.selectChapterIndex(0)
                                    chapterDropdownExpanded = false
                                }
                            )
                            chapters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("Ch ${ch.chapterIndex}: ${ch.title}", color = Color.White) },
                                    onClick = {
                                        viewModel.selectChapterIndex(ch.chapterIndex)
                                        chapterDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFF3B3454))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFE2B563), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Add Next Chapter", color = Color(0xFFE2B563), fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    chapterDropdownExpanded = false
                                    showAddChapterDialog = true
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.scanActiveManuscript() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2B563),
                        contentColor = Color(0xFF12101C)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("rescan_ai_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color(0xFF12101C),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isScanning) "Scanning..." else "AI Re-Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // View Mode Switcher Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFF161322),
            contentColor = Color(0xFFE2B563)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Issues & Fixes (${filteredInconsistencies.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manuscript Editor", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (activeTab == 0) {
            // Filter Pills Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { viewModel.setInconsistencyFilter(null) },
                        label = { Text("All (${inconsistencies.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE2B563),
                            selectedLabelColor = Color(0xFF12101C),
                            containerColor = Color(0xFF1C182A),
                            labelColor = Color(0xFFC3BBDC)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == InconsistencyType.NAME_MISMATCH,
                        onClick = { viewModel.setInconsistencyFilter(InconsistencyType.NAME_MISMATCH) },
                        label = { Text("Names") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64B5F6),
                            selectedLabelColor = Color(0xFF12101C),
                            containerColor = Color(0xFF1C182A),
                            labelColor = Color(0xFFC3BBDC)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == InconsistencyType.DATE_CHRONOLOGY,
                        onClick = { viewModel.setInconsistencyFilter(InconsistencyType.DATE_CHRONOLOGY) },
                        label = { Text("Dates & Chronology") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB74D),
                            selectedLabelColor = Color(0xFF12101C),
                            containerColor = Color(0xFF1C182A),
                            labelColor = Color(0xFFC3BBDC)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == InconsistencyType.PLOT_TIMELINE,
                        onClick = { viewModel.setInconsistencyFilter(InconsistencyType.PLOT_TIMELINE) },
                        label = { Text("Plot Continuity") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE57373),
                            selectedLabelColor = Color(0xFF12101C),
                            containerColor = Color(0xFF1C182A),
                            labelColor = Color(0xFFC3BBDC)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == InconsistencyType.CHARACTER_TRAIT,
                        onClick = { viewModel.setInconsistencyFilter(InconsistencyType.CHARACTER_TRAIT) },
                        label = { Text("Character Traits") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFA1887F),
                            selectedLabelColor = Color(0xFF12101C),
                            containerColor = Color(0xFF1C182A),
                            labelColor = Color(0xFFC3BBDC)
                        )
                    )
                }
            }

            // Inconsistency List
            if (filteredInconsistencies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Inconsistencies Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your manuscript looks clean for this filter! Tap 'AI Re-Scan' to re-verify.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA099B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredInconsistencies, key = { it.id }) { inc ->
                        InconsistencyCard(
                            inconsistency = inc,
                            onAccept = { viewModel.acceptInconsistency(inc) },
                            onReject = { viewModel.rejectInconsistency(inc) },
                            onCustomFix = { custom -> viewModel.applyCustomFix(inc, custom) },
                            onAiAdjust = {
                                viewModel.aiGenerateAlternativeFix(inc) { altFix ->
                                    viewModel.applyCustomFix(inc, altFix)
                                }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        } else {
            // Live Manuscript Editor View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (selectedChapterIdx == 0) {
                    Text(
                        text = "Viewing Full Manuscript. Select an individual chapter from the top dropdown to enable direct live editing.",
                        color = Color(0xFFFFB020),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val fullText = chapters.joinToString("\n\n") { "--- ${it.title} ---\n\n${it.modifiedContent}" }
                    OutlinedTextField(
                        value = fullText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("full_manuscript_text_view"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B3454),
                            unfocusedBorderColor = Color(0xFF2E2942),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                } else if (activeChapterObj != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Manuscript Chapter Editor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Button(
                            onClick = {
                                viewModel.updateChapterContentManually(activeChapterObj.id, editableChapterText)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B563), contentColor = Color(0xFF12101C)),
                            modifier = Modifier.testTag("save_chapter_manual_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editableChapterText,
                        onValueChange = { editableChapterText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chapter_live_editor_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }
    }
}
