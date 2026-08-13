package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ManuscriptViewModel

@Composable
fun ExportShareScreen(
    viewModel: ManuscriptViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val chapters by viewModel.activeChapters.collectAsState()
    val inconsistencies by viewModel.activeInconsistencies.collectAsState()

    var includeReportFooter by remember { mutableStateOf(true) }

    val resolvedCount = inconsistencies.count { it.status != com.example.data.model.InconsistencyStatus.PENDING }

    val fullExportText = remember(chapters, includeReportFooter, resolvedCount) {
        val sb = java.lang.StringBuilder()
        sb.append("${activeManuscript?.title ?: "Manuscript"}\n")
        sb.append("Author: ${activeManuscript?.author ?: "Unknown"}\n")
        sb.append("=========================================\n\n")

        chapters.forEach { ch ->
            sb.append("${ch.title.uppercase()}\n\n")
            sb.append(ch.modifiedContent)
            sb.append("\n\n-----------------------------------------\n\n")
        }

        if (includeReportFooter) {
            sb.append("\n\n=========================================\n")
            sb.append("PROOFREADING AUDIT REPORT (Manuscript Sentinel)\n")
            sb.append("Total Chapters: ${chapters.size}\n")
            sb.append("Total Inconsistencies Audited: ${inconsistencies.size}\n")
            sb.append("Fixes Applied / Accepted: $resolvedCount\n")
            sb.append("Generated with Sentinel AI Engine.\n")
        }

        sb.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12101C))
            .padding(16.dp)
    ) {
        Text(
            text = "Export & Share Clean Manuscript",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Your applied fixes have been integrated cleanly into the manuscript text.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFA099B8)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Manuscript Text", fullExportText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Manuscript copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("copy_manuscript_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B563), contentColor = Color(0xFF12101C))
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, activeManuscript?.title ?: "Manuscript")
                        putExtra(Intent.EXTRA_TEXT, fullExportText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Manuscript via Google Docs or Apps"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_manuscript_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B579A), contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share / Google Docs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = includeReportFooter,
                onCheckedChange = { includeReportFooter = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFE2B563),
                    uncheckedColor = Color(0xFF4C4566)
                ),
                modifier = Modifier.testTag("toggle_report_footer_checkbox")
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Include AI Proofreader Summary Report Footer",
                color = Color(0xFFD0C8E8),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Manuscript Output Preview Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1728))
                .border(1.dp, Color(0xFF332D48), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = fullExportText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxSize().testTag("export_text_preview_area"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFFECE6FF),
                    unfocusedTextColor = Color(0xFFECE6FF)
                )
            )
        }
    }
}
