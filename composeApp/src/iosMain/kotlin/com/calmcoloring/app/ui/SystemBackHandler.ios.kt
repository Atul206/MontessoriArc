package com.calmcoloring.app.ui

import androidx.compose.runtime.Composable

// iOS has no system back gesture/button to intercept in this app (no
// predictive-back-style swipe registered here) — a no-op actual. The in-app
// back arrow in `ColoringScreen` remains the way back on iOS.
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
