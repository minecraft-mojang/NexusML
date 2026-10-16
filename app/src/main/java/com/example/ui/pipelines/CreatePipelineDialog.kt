package com.example.ui.pipelines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePipelineDialog(
    onDismiss: () -> Unit,
    onPresetSelect: (String) -> Unit,
    onCreateCustom: (
        name: String,
        description: String,
        category: String,
        targetCluster: String,
        batchSize: Long,
        learningRate: Double,
        optimizer: String,
        epochs: Long
    ) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Presets, 1: Custom Builder

    // Custom form fields
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("LLM Fine-Tuning") }
    var targetCluster by remember { mutableStateOf("NVIDIA H100 SXM5 x8") }
    var batchSizeText by remember { mutableStateOf("64") }
    var learningRateText by remember { mutableStateOf("0.0003") }
    var epochsText by remember { mutableStateOf("10") }
    var optimizer by remember { mutableStateOf("AdamW (Cosine Decay)") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var clusterExpanded by remember { mutableStateOf(false) }

    val categories = listOf("LLM Fine-Tuning", "Computer Vision", "Tabular", "Audio", "Multimodal")
    val clusters = listOf(
        "NVIDIA H100 SXM5 x8 ($28.50/hr)",
        "Google TPU v5e-8 Node ($19.80/hr)",
        "AMD MI300X 4-way ($22.00/hr)",
        "Distributed CPU High-Mem ($8.50/hr)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "New Pipeline", tint = ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Initialize ML Pipeline", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberSurfaceVariant,
                    contentColor = ElectricCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricCyan
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Enterprise Presets", fontSize = 12.sp, color = if (selectedTab == 0) ElectricCyan else TextSecondary) },
                        modifier = Modifier.testTag("tab_presets")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custom Builder", fontSize = 12.sp, color = if (selectedTab == 1) ElectricCyan else TextSecondary) },
                        modifier = Modifier.testTag("tab_custom_builder")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Enterprise Presets
                    PresetOptionCard(
                        title = "Llama-3-70B QLoRA Instruct",
                        category = "LLM Fine-Tuning",
                        cluster = "NVIDIA H100 SXM5 x8",
                        icon = Icons.Default.DataObject,
                        description = "4-bit quantized adapter training on multi-turn dialogue telemetry datasets.",
                        onClick = {
                            onPresetSelect("LLAMA3_QLORA")
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PresetOptionCard(
                        title = "Vision Transformer (ViT-H)",
                        category = "Computer Vision",
                        cluster = "Google TPU v5e-8 Node",
                        icon = Icons.Default.Image,
                        description = "Multi-scale industrial visual defect segmentation and anomaly detection.",
                        onClick = {
                            onPresetSelect("VIT_DETECTION")
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PresetOptionCard(
                        title = "Transaction Fraud Risk XGBoost",
                        category = "Tabular",
                        cluster = "Distributed CPU High-Mem",
                        icon = Icons.Default.TableChart,
                        description = "Gradient boosting decision trees optimized for sub-10ms inference and drift check.",
                        onClick = {
                            onPresetSelect("TABULAR_FRAUD")
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PresetOptionCard(
                        title = "Whisper Large-v3 Speech",
                        category = "Audio",
                        cluster = "AMD MI300X 4-way",
                        icon = Icons.Default.GraphicEq,
                        description = "Acoustic sequence-to-sequence fine-tuning for domain jargon transcription.",
                        onClick = {
                            onPresetSelect("WHISPER_SPEECH")
                            onDismiss()
                        }
                    )
                } else {
                    // Custom Pipeline Builder
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Pipeline Name") },
                        placeholder = { Text("e.g., Customer Churn Embeddings v2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_pipeline_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Objective") },
                        placeholder = { Text("Train on enterprise holdout data") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_pipeline_desc_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Domain Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            containerColor = CyberSurfaceElevated
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cluster Dropdown
                    ExposedDropdownMenuBox(
                        expanded = clusterExpanded,
                        onExpandedChange = { clusterExpanded = !clusterExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetCluster,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Compute Cluster") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clusterExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = clusterExpanded,
                            onDismissRequest = { clusterExpanded = false },
                            containerColor = CyberSurfaceElevated
                        ) {
                            clusters.forEach { cl ->
                                DropdownMenuItem(
                                    text = { Text(cl, color = TextPrimary) },
                                    onClick = {
                                        targetCluster = cl.substringBefore(" (")
                                        clusterExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = epochsText,
                            onValueChange = { epochsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Epochs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = batchSizeText,
                            onValueChange = { batchSizeText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Batch Size") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = learningRateText,
                        onValueChange = { learningRateText = it },
                        label = { Text("Learning Rate (e.g. 0.0003)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsedEpochs = epochsText.toLongOrNull() ?: 10L
                            val parsedBatch = batchSizeText.toLongOrNull() ?: 64L
                            val parsedLr = learningRateText.toDoubleOrNull() ?: 0.0003
                            onCreateCustom(
                                name.ifBlank { "Custom Pipeline" },
                                description.ifBlank { "Custom enterprise data processing pipeline" },
                                category,
                                targetCluster,
                                parsedBatch,
                                parsedLr,
                                optimizer,
                                parsedEpochs
                            )
                            onDismiss()
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_create_pipeline_button")
                    ) {
                        Text("Create Pipeline Instance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberSurfaceElevated
    )
}

@Composable
private fun PresetOptionCard(
    title: String,
    category: String,
    cluster: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = CyberSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    color = NeonViolet,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = cluster,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
