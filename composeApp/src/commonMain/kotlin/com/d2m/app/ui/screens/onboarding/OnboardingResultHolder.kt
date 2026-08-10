package com.d2m.app.ui.screens.onboarding

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory hand-off between OnboardingWizardScreen and HandoffScreen --
 * mirrors the web app's use of React Router's `location.state` for exactly
 * this purpose (HandoffScreen.jsx reads required data only from router
 * state; a direct refresh/bookmark shows a "no invite to show" fallback,
 * same intentional non-persistence here). Registered as a Koin single so
 * both screens see the same instance without threading it through nav args.
 */
data class OnboardingHandoff(val sponsorId: String, val inviteToken: String, val childName: String, val inviteExpiresAt: String)

class OnboardingResultHolder {
    val current = MutableStateFlow<OnboardingHandoff?>(null)
}
