package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Pipeline
import com.example.data.model.PipelineStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ClusterBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.MetricBox
import com.example.ui.components.StatusPill
import com.example.ui.pipelines.CreatePipelineDialog
import com.example.ui.pipelines.PipelineViewModel
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
fun DashboardScreen(
    profile: UserProfile,
    pipelineViewModel: PipelineViewModel,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val filteredPipelines by pipelineViewModel.filteredPipelines.collectAsState()
    val searchQuery by pipelineViewModel.searchQuery.collectAsState()
    val selectedCategory by pipelineViewModel.selectedCategory.collectAsState()
    val selectedStatus by pipelineViewModel.selectedStatus.collectAsState()
    val statusMessage by pipelineViewModel.statusMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    val activePipelinesCount = filteredPipelines.count { it.status == "RUNNING" }
    val completedCount = filteredPipelines.count { it.status == "COMPLETED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.nexus_app_icon_1792054417992),
                            contentDescription = "NexusML Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NEXUS ML",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Text(
                                text = "${profile.displayName} • ${profile.role}",
                                fontSize = 11.sp,
                                color = if (profile.isAdmin) NeonViolet else TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    if (profile.isAdmin) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToAdmin() }
                                .testTag("admin_console_top_button"),
                            color = NeonViolet.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = NeonViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    color = NeonViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_button")) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkBg)
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("fab_create_pipeline")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Pipeline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        containerColor = CyberDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Status Toast Message
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = CyberSurfaceElevated,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { pipelineViewModel.clearStatusMessage() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Text("✕", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Hero Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nexus_hero_banner_1792054496747),
                    contentDescription = "Enterprise ML Pipeline Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CyberDarkBg.copy(alpha = 0.85f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "FLEET DATA ORCHESTRATION",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Enterprise ML Compute Cluster",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Telemetry Fleet Stats
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricBox(
                    title = "Active Jobs",
                    value = activePipelinesCount.toString(),
                    subValue = "$completedCount completed",
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    title = "GPU Utilization",
                    value = "87.4%",
                    subValue = "Fleet: 128x H100",
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    title = "Throughput",
                    value = "18.4 TB/s",
                    subValue = "Latency: 12ms",
                    accentColor = NeonViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { pipelineViewModel.setSearchQuery(it) },
                placeholder = { Text("Filter pipelines by model, cluster, or task...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pipeline_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            val categories = listOf("ALL", "LLM Fine-Tuning", "Computer Vision", "Tabular", "Audio")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat, ignoreCase = true),
                        onClick = { pipelineViewModel.setSelectedCategory(cat) },
                        label = { Text(cat, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header for Pipelines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED PIPELINES (${filteredPipelines.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presets", color = ElectricCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPipelines.isEmpty()) {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No pipelines configured yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Initialize an enterprise pipeline using presets or the custom builder.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Initialize Llama-3 Preset")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredPipelines.forEach { pipeline ->
                        PipelineCard(
                            pipeline = pipeline,
                            onClick = { pipelineViewModel.selectPipeline(pipeline) },
                            onQuickRun = { pipelineViewModel.runPipelineInteractive(pipeline) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showCreateDialog) {
        CreatePipelineDialog(
            onDismiss = { showCreateDialog = false },
            onPresetSelect = { preset ->
                pipelineViewModel.createPresetPipeline(preset)
            },
            onCreateCustom = { name, desc, cat, cluster, batch, lr, opt, epochs ->
                pipelineViewModel.createPipeline(name, desc, cat, cluster, batch, lr, opt, epochs)
            }
        )
    }
}

@Composable
private fun PipelineCard(
    pipeline: Pipeline,
    onClick: () -> Unit,
    onQuickRun: () -> Unit
) {
    val isRunning = pipeline.statusEnum == PipelineStatus.RUNNING

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isRunning) ElectricCyan.copy(alpha = 0.5f) else BorderSubtle,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status = pipeline.status)
                ClusterBadge(clusterName = pipeline.targetCluster)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pipeline.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = pipeline.description,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pipeline.category} • Epoch ${pipeline.currentEpoch}/${pipeline.totalEpochs}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${pipeline.progressPercentage}%",
                    color = if (isRunning) ElectricCyan else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { pipeline.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (pipeline.statusEnum == PipelineStatus.COMPLETED) EmeraldSuccess else ElectricCyan,
                trackColor = CyberSurfaceElevated
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pipeline.accuracy > 0.0) {
                        Text(
                            text = "Acc: ${String.format(Locale.US, "%.1f%%", pipeline.accuracy * 100)}",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Loss: ${String.format(Locale.US, "%.3f", pipeline.loss)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        if (!isRunning) onQuickRun() else onClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) CyberSurfaceElevated else ElectricCyan,
                        contentColor = if (isRunning) ElectricCyan else Color(0xFF00363D)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isRunning) "Inspect" else "Execute",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
