package com.calmcoloring.app.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform's system back gesture/button (Android's predictive
 * back gesture or hardware/nav-bar back button) and invokes [onBack] instead
 * of the default behavior (which would otherwise exit the app from
 * `ColoringScreen` — see Finding 5 of the final review).
 *
 * `androidx.activity.compose.BackHandler` — the natural implementation — is
 * Android-only (lives in the Android-only `activity-compose` artifact, not a
 * Compose Multiplatform commonMain API), so this is a small `expect`/`actual`
 * shim: the Android `actual` wires the real `BackHandler`, the iOS `actual`
 * is a no-op (iOS has no equivalent system back gesture to intercept here;
 * `ColoringScreen`'s in-app back arrow remains the way back on iOS).
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
