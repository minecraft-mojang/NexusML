package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.banned.BannedScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.pipelines.PipelineDetailScreen
import com.example.ui.pipelines.PipelineViewModel
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NexusMLTheme

enum class ScreenState {
    DASHBOARD,
    ADMIN_CONSOLE
}

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NexusMLTheme {
                MainAppEntry(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun MainAppEntry(authViewModel: AuthViewModel) {
    val authState by authViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.Initial, AuthUiState.Loading -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = CyberDarkBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }
        }
        is AuthUiState.Unauthenticated -> {
            AuthScreen(
                onGoogleSignInClick = { authViewModel.signInWithGoogle() },
                isLoading = false
            )
        }
        is AuthUiState.Error -> {
            AuthScreen(
                onGoogleSignInClick = { authViewModel.signInWithGoogle() },
                isLoading = false,
                errorMessage = state.message,
                onDismissError = { authViewModel.checkCurrentSession() }
            )
        }
        is AuthUiState.Banned -> {
            BannedScreen(
                profile = state.profile,
                onSignOutClick = { authViewModel.signOut() }
            )
        }
        is AuthUiState.Authenticated -> {
            AuthenticatedWorkspace(
                profile = state.profile,
                onSignOut = { authViewModel.signOut() }
            )
        }
    }
}

@Composable
fun AuthenticatedWorkspace(
    profile: UserProfile,
    onSignOut: () -> Unit
) {
    val pipelineViewModel: PipelineViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()

    var currentScreen by remember { mutableStateOf(ScreenState.DASHBOARD) }
    val selectedPipeline by pipelineViewModel.selectedPipeline.collectAsState()
    val rawPipelines by pipelineViewModel.filteredPipelines.collectAsState()

    LaunchedEffect(profile.userId) {
        pipelineViewModel.initialize(profile)
    }

    // Seed realistic enterprise pipelines on first launch if user has none
    LaunchedEffect(rawPipelines.isEmpty()) {
        if (rawPipelines.isEmpty()) {
            pipelineViewModel.createPresetPipeline("LLAMA3_QLORA")
            pipelineViewModel.createPresetPipeline("VIT_DETECTION")
            pipelineViewModel.createPresetPipeline("TABULAR_FRAUD")
        }
    }

    if (selectedPipeline != null) {
        PipelineDetailScreen(
            pipeline = selectedPipeline!!,
            onBack = { pipelineViewModel.selectPipeline(null) },
            onRunPipeline = { pipelineViewModel.runPipelineInteractive(selectedPipeline!!) },
            onPausePipeline = { pipelineViewModel.pausePipeline(selectedPipeline!!) },
            onClonePipeline = { pipelineViewModel.clonePipeline(selectedPipeline!!) },
            onDeletePipeline = { pipelineViewModel.deletePipeline(selectedPipeline!!) }
        )
    } else {
        when (currentScreen) {
            ScreenState.DASHBOARD -> {
                DashboardScreen(
                    profile = profile,
                    pipelineViewModel = pipelineViewModel,
                    onNavigateToAdmin = { currentScreen = ScreenState.ADMIN_CONSOLE },
                    onSignOut = onSignOut
                )
            }
            ScreenState.ADMIN_CONSOLE -> {
                AdminScreen(
                    viewModel = adminViewModel,
                    onBack = { currentScreen = ScreenState.DASHBOARD }
                )
            }
        }
    }
}
