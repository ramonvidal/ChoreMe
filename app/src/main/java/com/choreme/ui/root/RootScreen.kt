package com.choreme.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.domain.session.SessionState
import com.choreme.feature.onboarding.AwaitingApprovalScreen
import com.choreme.feature.onboarding.FirebaseSetupScreen
import com.choreme.feature.onboarding.OnboardingScreen
import com.choreme.feature.onboarding.SignInScreen
import com.choreme.ui.navigation.ChoreMeNavHost

@Composable
fun RootScreen(viewModel: RootViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val pendingJoinRequests by viewModel.pendingJoinRequests.collectAsStateWithLifecycle()

    when (val state = session) {
        SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SessionState.FirebaseNotConfigured -> FirebaseSetupScreen()
        SessionState.SignedOut -> SignInScreen()
        is SessionState.NoFamily -> OnboardingScreen(state)
        is SessionState.AwaitingApproval -> AwaitingApprovalScreen(state)
        is SessionState.InFamily -> ChoreMeNavHost(pendingJoinRequests = pendingJoinRequests)
    }
}
