package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.viewmodel.SplashState
import com.aktarjabed.inbusiness.presentation.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val state by viewModel.splashState.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            is SplashState.GoToDashboard -> onNavigateToDashboard()
            is SplashState.GoToSetup -> onNavigateToSetup()
            is SplashState.Loading -> { } // Do nothing, just wait
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
