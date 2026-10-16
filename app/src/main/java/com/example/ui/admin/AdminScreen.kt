package com.example.ui.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.UserProfile
import com.example.ui.components.CyberCard
import com.example.ui.components.StatusPill
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val filteredUsers by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.userSearchQuery.collectAsState()
    val filterTab by viewModel.filterTab.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val adminMessage by viewModel.adminMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var activeScreenTab by remember { mutableIntStateOf(0) } // 0: User Directory, 1: Audit Logs

    var banningUser by remember { mutableStateOf<UserProfile?>(null) }
    var banReasonInput by remember { mutableStateOf("") }

    var roleEditUser by remember { mutableStateOf<UserProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = CrimsonAlert,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADMIN SECURITY CONSOLE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Root Privilege Mode // User Governance & Moderation",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Main Top Navigation Tabs (User Directory vs Audit Logs)
            TabRow(
                selectedTabIndex = activeScreenTab,
                containerColor = CyberSurfaceVariant,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeScreenTab]),
                        color = ElectricCyan
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = activeScreenTab == 0,
                    onClick = { activeScreenTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "User Directory (${filteredUsers.size})",
                                fontSize = 12.sp,
                                fontWeight = if (activeScreenTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeScreenTab == 0) ElectricCyan else TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_user_directory")
                )
                Tab(
                    selected = activeScreenTab == 1,
                    onClick = { activeScreenTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Audit Trail (${auditLogs.size})",
                                fontSize = 12.sp,
                                fontWeight = if (activeScreenTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeScreenTab == 1) ElectricCyan else TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_audit_logs")
                )
            }

            // Notification / Status Banner
            AnimatedVisibility(visible = adminMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = CyberSurfaceElevated,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = adminMessage ?: "",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearAdminMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeScreenTab == 0) {
                // User Directory Content
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by name, email, or role...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTab == "ALL",
                        onClick = { viewModel.setFilterTab("ALL") },
                        label = { Text("All Users", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        )
                    )
                    FilterChip(
                        selected = filterTab == "ACTIVE",
                        onClick = { viewModel.setFilterTab("ACTIVE") },
                        label = { Text("Active", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldSuccess
                        )
                    )
                    FilterChip(
                        selected = filterTab == "BANNED",
                        onClick = { viewModel.setFilterTab("BANNED") },
                        label = { Text("Banned / Suspended", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonAlert.copy(alpha = 0.2f),
                            selectedLabelColor = CrimsonAlert
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found matching query",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers) { user ->
                            AdminUserCard(
                                user = user,
                                onBanClick = {
                                    banningUser = user
                                    banReasonInput = ""
                                },
                                onUnbanClick = {
                                    viewModel.unbanUser(user)
                                },
                                onRoleClick = {
                                    roleEditUser = user
                                }
                            )
                        }
                    }
                }
            } else {
                // Audit Trail Content
                if (auditLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No audit log entries recorded yet",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs) { log ->
                            CyberCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            status = if (log.action.contains("BAN")) "BANNED" else "ACTIVE"
                                        )
                                        Text(
                                            text = log.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "Just now",
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${log.action}: ${log.details}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Target: ${log.targetUserEmail} • Enforcer: ${log.actorEmail}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Ban User Dialog
    if (banningUser != null) {
        val target = banningUser!!
        AlertDialog(
            onDismissRequest = { banningUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suspend User Access", color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to ban ${target.displayName} (${target.email})? This user will be immediately locked out of all enterprise pipeline compute clusters.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = banReasonInput,
                        onValueChange = { banReasonInput = it },
                        label = { Text("Toxicity / Violation Reason") },
                        placeholder = { Text("e.g., Toxic abusive comments, compute queue spam") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ban_reason_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonAlert,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Presets:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                            color = CyberSurfaceVariant
                        ) {
                            Text(
                                text = "Toxic Conduct",
                                color = ElectricCyan,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("preset_toxic_conduct")
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                            color = CyberSurfaceVariant
                        ) {
                            Text(
                                text = "Resource Abuse",
                                color = ElectricCyan,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("preset_resource_abuse")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (banReasonInput.isNotBlank()) banReasonInput else "Flagged for toxic or abusive conduct violating community guidelines"
                        viewModel.banUser(target, reason)
                        banningUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_ban_button")
                ) {
                    Text("Confirm Ban & Suspend")
                }
            },
            dismissButton = {
                TextButton(onClick = { banningUser = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }

    // Role Manager Dialog
    if (roleEditUser != null) {
        val target = roleEditUser!!
        val roles = listOf("ML_ENGINEER", "DATA_SCIENTIST", "VIEWER", "ADMIN")
        AlertDialog(
            onDismissRequest = { roleEditUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign Role", color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Update enterprise permissions for ${target.displayName} (${target.email}):",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    roles.forEach { role ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (target.role == role) ElectricCyan else BorderSubtle
                                    ),
                                    RoundedCornerShape(8.dp)
                                ),
                            color = if (target.role == role) ElectricCyan.copy(alpha = 0.15f) else CyberSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = role,
                                    color = if (target.role == role) ElectricCyan else TextPrimary,
                                    fontWeight = if (target.role == role) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateRole(target, role)
                                        roleEditUser = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (target.role == role) CyberSurfaceElevated else ElectricCyan,
                                        contentColor = if (target.role == role) TextSecondary else Color(0xFF00363D)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (target.role == role) "Current" else "Select", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { roleEditUser = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }
}

@Composable
private fun AdminUserCard(
    user: UserProfile,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit,
    onRoleClick: () -> Unit
) {
    val isBanned = user.isBanned
    val borderColor = if (isBanned) CrimsonAlert.copy(alpha = 0.6f) else BorderSubtle

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBanned) CrimsonAlert.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            color = if (isBanned) CrimsonAlert else ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                                    color = NeonViolet.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = NeonViolet,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = user.email,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                StatusPill(status = if (isBanned) "BANNED" else "ACTIVE")
            }

            if (isBanned && !user.banReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp)),
                    color = CrimsonAlert.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Ban Reason: ${user.banReason}",
                        color = CrimsonAlert,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role: ${user.role} • Pipelines: ${user.pipelineCount}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRoleClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Role", fontSize = 11.sp, color = TextSecondary)
                    }

                    if (isBanned) {
                        Button(
                            onClick = onUnbanClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF00391A)),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("unban_user_button_${user.userId.take(6)}")
                        ) {
                            Text("Reinstate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!user.isAdmin) {
                        Button(
                            onClick = onBanClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = Color.White),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("ban_user_button_${user.userId.take(6)}")
                        ) {
                            Text("Ban User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
