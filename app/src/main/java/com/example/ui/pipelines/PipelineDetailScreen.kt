package com.example.ui.pipelines

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pipeline
import com.example.data.model.PipelineStatus
import com.example.data.model.StageInfo
import com.example.ui.components.ClusterBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.MetricBox
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineDetailScreen(
    pipeline: Pipeline,
    onBack: () -> Unit,
    onRunPipeline: () -> Unit,
    onPausePipeline: () -> Unit,
    onClonePipeline: () -> Unit,
    onDeletePipeline: () -> Unit
) {
    BackHandler { onBack() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val stages = pipeline.getStages()
    val logs = pipeline.getRecentLogs()
    val logsListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logsListState.animateScrollToItem(logs.size - 1)
        }
    }

    val isRunning = pipeline.statusEnum == PipelineStatus.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pipeline.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${pipeline.category} • ID: ${pipeline.id}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClonePipeline, modifier = Modifier.testTag("clone_pipeline_button")) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Clone", tint = TextSecondary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("delete_pipeline_button")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonAlert)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkBg)
            )
        },
        containerColor = CyberDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Execution Controls & Status Banner
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isRunning) ElectricCyan.copy(alpha = 0.5f) else BorderSubtle
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(status = pipeline.status)
                        ClusterBadge(clusterName = pipeline.targetCluster)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = pipeline.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WORKFLOW PROGRESS",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${pipeline.progressPercentage}%",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { pipeline.progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricCyan,
                        trackColor = CyberSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (isRunning) {
                            Button(
                                onClick = onPausePipeline,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color(0xFF1E1A00)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("pause_pipeline_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pause Execution", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onRunPipeline,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("run_pipeline_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (pipeline.statusEnum == PipelineStatus.COMPLETED) "Rerun Pipeline" else "Run Pipeline",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid
            Text(
                text = "TRAINING & TELEMETRY METRICS",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricBox(
                    title = "Accuracy",
                    value = String.format(Locale.US, "%.1f%%", pipeline.accuracy * 100),
                    subValue = "F1: ${String.format(Locale.US, "%.3f", pipeline.f1Score)}",
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    title = "Loss",
                    value = String.format(Locale.US, "%.4f", pipeline.loss),
                    subValue = "Epoch ${pipeline.currentEpoch}/${pipeline.totalEpochs}",
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    title = "Est. Cost",
                    value = "$${pipeline.computeCostPerHour}/h",
                    subValue = pipeline.targetCluster.take(10),
                    accentColor = NeonViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visual DAG / Pipeline Stages Tracker
            Text(
                text = "PIPELINE DAG NODES",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            stages.forEachIndexed { index, stage ->
                StageNodeCard(
                    index = index + 1,
                    totalStages = stages.size,
                    stage = stage
                )
                if (index < stages.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 27.dp)
                            .width(2.dp)
                            .height(16.dp)
                            .background(
                                if (stage.status == "COMPLETED") EmeraldSuccess else BorderSubtle
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Cluster Logs Output
            Text(
                text = "LIVE CLUSTER EXECUTION STDOUT/STDERR",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(12.dp)),
                color = Color(0xFF060910)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WORKER PODS // STREAM",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isRunning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", color = EmeraldSuccess, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        state = logsListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { logLine ->
                            val color = when {
                                logLine.contains("[SUCCESS]") -> EmeraldSuccess
                                logLine.contains("[TRAIN]") -> ElectricCyan
                                logLine.contains("[EVAL]") -> NeonViolet
                                logLine.contains("[ERROR]") -> CrimsonAlert
                                else -> TextSecondary
                            }
                            Text(
                                text = logLine,
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hyperparameters Card
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HYPERPARAMETER SPECIFICATIONS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    HyperparamRow(label = "Batch Size", value = pipeline.batchSize.toString())
                    HyperparamRow(label = "Learning Rate", value = pipeline.learningRate.toString())
                    HyperparamRow(label = "Optimizer", value = pipeline.optimizer)
                    HyperparamRow(label = "Total Epochs", value = pipeline.totalEpochs.toString())
                    HyperparamRow(label = "Compute Target", value = pipeline.targetCluster)
                    HyperparamRow(label = "Author Identity", value = pipeline.userEmail)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Pipeline?", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${pipeline.name}'? This action will terminate cluster allocations and remove saved checkpoints.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePipeline()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }
}

@Composable
private fun StageNodeCard(
    index: Int,
    totalStages: Int,
    stage: StageInfo
) {
    val (statusColor, statusIcon) = when (stage.status) {
        "COMPLETED" -> Pair(EmeraldSuccess, Icons.Default.CheckCircle)
        "RUNNING" -> Pair(ElectricCyan, Icons.Default.PlayArrow)
        else -> Pair(TextMuted, Icons.Default.HourglassEmpty)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (stage.status == "RUNNING") ElectricCyan.copy(alpha = 0.6f) else BorderSubtle
                ),
                RoundedCornerShape(12.dp)
            ),
        color = CyberSurface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Node Number / Indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.5.dp, statusColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (stage.status == "RUNNING") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ElectricCyan
                    )
                } else {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NODE 0$index: ${stage.name}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (stage.durationSec > 0) {
                        Text(
                            text = "${stage.durationSec}s",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stage.description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (stage.detail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• ${stage.detail}",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun HyperparamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
