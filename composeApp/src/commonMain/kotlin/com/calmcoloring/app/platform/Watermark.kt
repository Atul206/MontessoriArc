package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Draws [caption] and [madeWith] beneath [artwork] on a plain, opaque
 * light-cream card background, returning a NEW bitmap with the watermark
 * baked directly into its pixels (see `ui/share/ShareSheet.kt` and the
 * final-review Finding 3 this addresses — a share/print target only ever
 * receives the pixels of a single shared image, never separate caption
 * text, so the caption has to live inside the bitmap itself to reliably
 * reach the recipient).
 *
 * Deliberately implemented with a low-level platform Canvas/text-drawing API
 * (`android.graphics.Canvas`/`Paint` on Android, `UIGraphicsImageRenderer`/
 * `NSString` drawing on iOS) rather than by capturing a live Composable
 * preview card via `GraphicsLayer.record()`: an earlier version of this
 * feature did exactly that (recording a small preview `Card` composable
 * into a `GraphicsLayer` and reading it back with `toImageBitmap()`), but
 * that was verified on a real Android emulator to intermittently drop the
 * caption text from the captured bitmap entirely — the on-screen preview
 * kept rendering the text correctly, but the exported bitmap sometimes
 * didn't, in a way that persisted across several different mitigations
 * (an explicit capture size, a fresh `GraphicsLayer` per distinct caption,
 * proactively capturing ahead of the parental-gate dialog rather than
 * inside its `onPassed`). Drawing straight onto a bitmap with the
 * platform's own text APIs has no such failure mode.
 */
expect fun composeWatermarkedBitmap(
    artwork: ImageBitmap,
    caption: String,
    madeWith: String = "Made with Calm Coloring",
): ImageBitmap
