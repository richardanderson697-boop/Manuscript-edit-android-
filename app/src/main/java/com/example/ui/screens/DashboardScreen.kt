package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Manuscript
import com.example.ui.components.ImportManuscriptBottomSheet
import com.example.ui.components.ManuscriptCard
import com.example.ui.viewmodel.ManuscriptViewModel

@Composable
fun DashboardScreen(
    viewModel: ManuscriptViewModel,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manuscripts by viewModel.allManuscripts.collectAsState()
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val activeInconsistencies by viewModel.activeInconsistencies.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgressText by viewModel.scanProgressText.collectAsState()

    var showImportSheet by remember { mutableStateOf(false) }

    val pendingCount = activeInconsistencies.count { it.status == com.example.data.model.InconsistencyStatus.PENDING }
    val resolvedCount = activeInconsistencies.count { it.status != com.example.data.model.InconsistencyStatus.PENDING }

    Scaffold(
        containerColor = Color(0xFF12101C),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showImportSheet = true },
                containerColor = Color(0xFFE2B563),
                contentColor = Color(0xFF12101C),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Import Manuscript", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_import_manuscript")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF231B38), Color(0xFF382958))
                            )
                        )
                        .border(1.dp, Color(0xFF4C3A75), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2B563).copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFE2B563),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NARRATIVE SENTINEL AI",
                                        color = Color(0xFFE2B563),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Proofreading & Consistency Platform",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Automatically surface character name variations, date chronologies, plot gaps, and timeline errors across chapters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC3BBDC),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF191428), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB020),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("$pendingCount Pending", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("Issues Found", color = Color(0xFFA099B8), fontSize = 10.sp)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF191428), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("$resolvedCount Resolved", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("Fixes Applied", color = Color(0xFFA099B8), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isScanning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2142))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFE2B563),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AI Engine Proofreading...", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(scanProgressText, color = Color(0xFFB0A8CC), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Active Manuscript Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Manuscripts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "${manuscripts.size} Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA099B8)
                    )
                }
            }

            items(manuscripts) { ms ->
                ManuscriptCard(
                    manuscript = ms,
                    isSelected = activeManuscript?.id == ms.id,
                    onSelect = {
                        viewModel.selectManuscript(ms.id)
                    },
                    onScan = {
                        viewModel.selectManuscript(ms.id)
                        viewModel.scanActiveManuscript()
                    },
                    onDelete = {
                        viewModel.deleteManuscript(ms.id)
                    }
                )
            }

            // Quick launch to editor
            if (activeManuscript != null) {
                item {
                    Button(
                        onClick = onNavigateToEditor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_editor_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2440),
                            contentColor = Color(0xFFE2B563)
                        )
                    ) {
                        Icon(Icons.Outlined.Book, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Proofreader Workspace (${activeManuscript?.title})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showImportSheet) {
        ImportManuscriptBottomSheet(
            onDismiss = { showImportSheet = false },
            onImportText = { title, author, text ->
                viewModel.importManuscriptFromText(title, author, text)
            },
            onImportGoogleDocs = { url, title, author ->
                viewModel.importFromGoogleDocs(url, title, author)
            }
        )
    }
}
