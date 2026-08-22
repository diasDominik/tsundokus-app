package uk.tsundokus.composeapp.navigation

import androidx.compose.runtime.Composable

// iOS exposes no host-level back affordance to bridge here; in-app back drives NavDisplay.
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
