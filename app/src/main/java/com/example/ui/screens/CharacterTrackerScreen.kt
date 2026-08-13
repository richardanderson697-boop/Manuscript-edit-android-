package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterProfile
import com.example.ui.components.CharacterCard
import com.example.ui.viewmodel.ManuscriptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterTrackerScreen(
    viewModel: ManuscriptViewModel,
    modifier: Modifier = Modifier
) {
    val characters by viewModel.activeCharacters.collectAsState()
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var showCharacterModal by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<CharacterProfile?>(null) }

    var primaryNameInput by remember { mutableStateOf("") }
    var aliasesInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("Protagonist") }
    var ageTimelineInput by remember { mutableStateOf("") }
    var physicalInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    fun openEditModal(character: CharacterProfile?) {
        editingCharacter = character
        if (character != null) {
            primaryNameInput = character.primaryName
            aliasesInput = character.aliases
            roleInput = character.role
            ageTimelineInput = character.ageTimeline
            physicalInput = character.physicalAttributes
            notesInput = character.notes
        } else {
            primaryNameInput = ""
            aliasesInput = ""
            roleInput = "Supporting"
            ageTimelineInput = ""
            physicalInput = ""
            notesInput = ""
        }
        showCharacterModal = true
    }

    Scaffold(
        containerColor = Color(0xFF12101C),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openEditModal(null) },
                containerColor = Color(0xFFE2B563),
                contentColor = Color(0xFF12101C),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Character", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_character")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Character Consistency Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${characters.size} Characters tracked in '${activeManuscript?.title ?: "Manuscript"}'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA099B8)
                    )
                }

                Button(
                    onClick = { viewModel.scanActiveManuscript() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262138), contentColor = Color(0xFFE2B563)),
                    modifier = Modifier.testTag("ai_auto_scan_roster_button")
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (characters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Characters Tracked Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Auto-Scan' to let AI extract characters from your manuscript, or add manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA099B8)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(characters, key = { it.id }) { char ->
                        CharacterCard(
                            character = char,
                            onEdit = { openEditModal(char) },
                            onDelete = { viewModel.deleteCharacterProfile(char.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCharacterModal) {
        AlertDialog(
            onDismissRequest = { showCharacterModal = false },
            title = {
                Text(
                    text = if (editingCharacter == null) "Add Character Profile" else "Edit Character Profile",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = primaryNameInput,
                        onValueChange = { primaryNameInput = it },
                        label = { Text("Primary Name") },
                        placeholder = { Text("e.g. Lord Richard Sterling") },
                        modifier = Modifier.fillMaxWidth().testTag("character_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = aliasesInput,
                        onValueChange = { aliasesInput = it },
                        label = { Text("Aliases & Name Variations") },
                        placeholder = { Text("e.g. Richard, Lord Stirling") },
                        modifier = Modifier.fillMaxWidth().testTag("character_aliases_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("Role") },
                        placeholder = { Text("Protagonist, Antagonist, Supporting") },
                        modifier = Modifier.fillMaxWidth().testTag("character_role_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = physicalInput,
                        onValueChange = { physicalInput = it },
                        label = { Text("Physical Attributes") },
                        placeholder = { Text("e.g. Amber eyes, dark coat, silver key") },
                        modifier = Modifier.fillMaxWidth().testTag("character_physical_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = ageTimelineInput,
                        onValueChange = { ageTimelineInput = it },
                        label = { Text("Age & Timeline Consistency Notes") },
                        placeholder = { Text("e.g. Age 28 in Ch 1") },
                        modifier = Modifier.fillMaxWidth().testTag("character_timeline_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2B563),
                            unfocusedBorderColor = Color(0xFF3B3454),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Backstory & Plot Notes") },
                        modifier = Modifier.fillMaxWidth().testTag("character_notes_input"),
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
                        val msId = activeManuscript?.id ?: 1L
                        val newChar = CharacterProfile(
                            id = editingCharacter?.id ?: 0L,
                            manuscriptId = msId,
                            primaryName = primaryNameInput.ifBlank { "Unknown Character" },
                            aliases = aliasesInput,
                            role = roleInput.ifBlank { "Supporting" },
                            ageTimeline = ageTimelineInput,
                            physicalAttributes = physicalInput,
                            notes = notesInput,
                            conflictCount = editingCharacter?.conflictCount ?: 0
                        )
                        viewModel.saveCharacterProfile(newChar)
                        showCharacterModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B563), contentColor = Color(0xFF12101C)),
                    modifier = Modifier.testTag("save_character_modal_button")
                ) {
                    Text("Save Character", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCharacterModal = false }) {
                    Text("Cancel", color = Color(0xFFB0A8CC))
                }
            },
            containerColor = Color(0xFF211D32)
        )
    }
}
