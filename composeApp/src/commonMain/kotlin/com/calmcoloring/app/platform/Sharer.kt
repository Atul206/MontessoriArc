package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.ui.share.ShareTarget

/**
 * Shares [artwork] with [caption] as the accompanying text. On Android this
 * targets the named app's package for [ShareTarget.WhatsApp]/
 * [ShareTarget.Instagram] and falls back to the system chooser for
 * [ShareTarget.More]; on iOS all three targets route through the same
 * `UIActivityViewController` (see `Sharer.ios.kt` for why).
 */
expect fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget)
