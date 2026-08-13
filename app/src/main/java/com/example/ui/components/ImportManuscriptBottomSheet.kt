package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportManuscriptBottomSheet(
    onDismiss: () -> Unit,
    onImportText: (title: String, author: String, text: String) -> Unit,
    onImportGoogleDocs: (url: String, title: String, author: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Text Paste / File, 1: Google Docs

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var googleDocsUrl by remember { mutableStateOf("") }

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
                text = "Import Manuscript",
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
                            Text("Text / .txt File")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Docs Link")
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
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Manuscript Text or Paste Content") },
                    placeholder = { Text("Paste chapter text here... (Supports 'Chapter 1', 'Chapter 2' splitting)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                    Text("Import & Parse Chapters", fontWeight = FontWeight.Bold)
                }
            } else {
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
