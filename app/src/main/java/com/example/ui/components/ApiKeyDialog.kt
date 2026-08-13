package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ManuscriptViewModel

@Composable
fun ApiKeyDialog(
    viewModel: ManuscriptViewModel,
    onDismiss: () -> Unit
) {
    var customKey by remember { mutableStateOf(viewModel.getCurrentApiKey()) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val hasKey = viewModel.isApiKeyConfigured()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C182A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFE2B563), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Gemini AI Proofreader Setup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (hasKey) Color(0xFF1B3828) else Color(0xFF38291B),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasKey) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasKey) Color(0xFF81C784) else Color(0xFFFFB020),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasKey) "Gemini 2.5 Flash API is Active" else "No API Key (Using Offline Heuristics Engine)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "You can configure your Gemini API Key via AI Studio Secrets (injected via BuildConfig) or enter a custom key below to run live deep-narrative scans on any custom manuscript.",
                    color = Color(0xFFC3BBDC),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customKey,
                    onValueChange = {
                        customKey = it
                        testStatusMessage = null
                        isSuccess = null
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2B563),
                        unfocusedBorderColor = Color(0xFF3B3454),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE2B563),
                        unfocusedLabelColor = Color(0xFFA099B8)
                    )
                )

                if (testStatusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testStatusMessage ?: "",
                        color = if (isSuccess == true) Color(0xFF81C784) else Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testStatusMessage = "Testing Gemini 2.5 Flash connection..."
                        if (customKey.isNotBlank()) {
                            viewModel.setCustomApiKey(customKey)
                        }
                        viewModel.testApiKeyConnection { success, message ->
                            isTesting = false
                            isSuccess = success
                            testStatusMessage = message
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2B563))
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = Color(0xFFE2B563), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...")
                    } else {
                        Text("Verify & Test Connection", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customKey.isNotBlank()) {
                        viewModel.setCustomApiKey(customKey)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2B563),
                    contentColor = Color(0xFF12101C)
                )
            ) {
                Text("Save & Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA099B8))
            }
        }
    )
}
