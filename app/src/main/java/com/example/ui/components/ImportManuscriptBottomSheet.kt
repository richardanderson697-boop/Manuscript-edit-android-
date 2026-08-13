package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportManuscriptBottomSheet(
    onDismiss: () -> Unit,
    onImportText: (title: String, author: String, text: String) -> Unit,
    onImportGoogleDocs: (url: String, title: String, author: String) -> Unit,
    onCreateNewBook: (title: String, author: String, chapterTitle: String, chapterContent: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: File / Import, 1: New Book, 2: Google Docs

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var firstChapterTitle by remember { mutableStateOf("Chapter 1") }
    var firstChapterText by remember { mutableStateOf("") }
    var googleDocsUrl by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    rawText = content
                    val fileName = fileUri.lastPathSegment?.substringAfterLast('/') ?: "Imported Manuscript"
                    selectedFileName = fileName.removeSuffix(".txt").removeSuffix(".md")
                    if (title.isBlank()) {
                        title = selectedFileName.replace('_', ' ').replace('-', ' ').capitalize()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C182A),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        modifier = modifier.testTag("import_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add or Import Manuscript",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF12101C),
                contentColor = Color(0xFFE2B563),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(".txt File / Paste", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Book", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Docs", fontSize = 12.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Manuscript Title") },
                placeholder = { Text("e.g. Chronicles of Eldoria") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_title_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE2B563),
                    unfocusedBorderColor = Color(0xFF3B3454),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFE2B563),
                    unfocusedLabelColor = Color(0xFFA099B8)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author Name") },
                placeholder = { Text("e.g. Jane Doe") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_author_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE2B563),
                    unfocusedBorderColor = Color(0xFF3B3454),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFE2B563),
                    unfocusedLabelColor = Color(0xFFA099B8)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Tab 0: File Pick or Paste multi-chapter text
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE2B563)
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedFileName.isNotEmpty()) "Selected: $selectedFileName.txt"
                        else "Pick .txt File from Device Storage"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Manuscript Text (Multi-Chapter or Single)") },
                    placeholder = { Text("Paste manuscript text here... (Headings like 'Chapter 1', 'Chapter 2' will be auto-split)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("import_raw_text_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2B563),
                        unfocusedBorderColor = Color(0xFF3B3454),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE2B563),
                        unfocusedLabelColor = Color(0xFFA099B8)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (rawText.isNotBlank()) {
                            onImportText(title, author, rawText)
                            onDismiss()
                        }
                    },
                    enabled = rawText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_import_text_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2B563),
                        contentColor = Color(0xFF12101C)
                    )
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import & Auto-Split Chapters", fontWeight = FontWeight.Bold)
                }
            } else if (selectedTab == 1) {
                // Tab 1: Create New Book from scratch
                OutlinedTextField(
                    value = firstChapterTitle,
                    onValueChange = { firstChapterTitle = it },
                    label = { Text("First Chapter Title") },
                    placeholder = { Text("e.g. Chapter 1: The Departure") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2B563),
                        unfocusedBorderColor = Color(0xFF3B3454),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE2B563),
                        unfocusedLabelColor = Color(0xFFA099B8)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstChapterText,
                    onValueChange = { firstChapterText = it },
                    label = { Text("First Chapter Text") },
                    placeholder = { Text("Write your chapter content here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2B563),
                        unfocusedBorderColor = Color(0xFF3B3454),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE2B563),
                        unfocusedLabelColor = Color(0xFFA099B8)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onCreateNewBook(title, author, firstChapterTitle, firstChapterText)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_create_new_book_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2B563),
                        contentColor = Color(0xFF12101C)
                    )
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Book & Open Workspace", fontWeight = FontWeight.Bold)
                }
            } else {
                // Tab 2: Google Docs Sync
                OutlinedTextField(
                    value = googleDocsUrl,
                    onValueChange = { googleDocsUrl = it },
                    label = { Text("Google Docs Document URL or ID") },
                    placeholder = { Text("https://docs.google.com/document/d/1ABC.../edit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_gdocs_url_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2B563),
                        unfocusedBorderColor = Color(0xFF3B3454),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE2B563),
                        unfocusedLabelColor = Color(0xFFA099B8)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262138), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Docs Sharing Setup", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure your document in Google Docs is set to 'Anyone with the link can view' so the proofreader engine can fetch the latest manuscript text.",
                            color = Color(0xFFD0C8E8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (googleDocsUrl.isNotBlank()) {
                            onImportGoogleDocs(googleDocsUrl, title, author)
                            onDismiss()
                        }
                    },
                    enabled = googleDocsUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_import_gdocs_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2B563),
                        contentColor = Color(0xFF12101C)
                    )
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync & Proofread Google Doc", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


