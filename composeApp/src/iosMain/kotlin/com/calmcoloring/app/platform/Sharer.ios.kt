package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.ui.share.ShareTarget
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.kCGBitmapByteOrder32Host
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage

// NOTE (verification caveat, same as Printer.ios.kt): this file could not be
// linked/run in this environment (Xcode Command Line Tools only, no full
// Xcode / simulator) — it is verified to *compile* against the
// iosSimulatorArm64 target only. Runtime behavior (does the share sheet
// actually appear with the image + caption) is UNVERIFIED here.
//
// iOS has no package-targeted share-intent equivalent to Android's
// `Intent.setPackage(...)` — the OS share sheet (`UIActivityViewController`)
// surfaces WhatsApp/Instagram automatically if installed, alongside every
// other app that registers a share extension. All three [ShareTarget]
// values therefore route through the same system sheet here; the distinct
// WhatsApp/Instagram/More buttons in `ShareSheet` are a UI affordance
// matching the mockup, not a routing difference on this platform.
@OptIn(ExperimentalForeignApi::class)
actual fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget) {
    val uiImage = artwork.toUIImage()
    val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    val activityController = UIActivityViewController(activityItems = listOf(uiImage, caption), applicationActivities = null)
    controller?.presentViewController(activityController, animated = true, completion = null)
}

/**
 * Converts a Compose [ImageBitmap] (Skia-backed on iOS) into a [UIImage] by
 * reading raw ARGB pixels and bridging them through a CoreGraphics
 * CGBitmapContext -> CGImage -> UIImage pipeline. Duplicated from
 * `Printer.ios.kt`'s private helper of the same name/shape (Task 6) rather
 * than shared, since that one is file-private there; there is no direct
 * ImageBitmap -> UIImage conversion in Compose Multiplatform.
 */
@OptIn(ExperimentalForeignApi::class)
private fun ImageBitmap.toUIImage(): UIImage {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    this.readPixels(pixels)

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    // Pixels are packed as 0xAARRGGBB Ints (Compose's common ARGB_8888
    // layout). kCGBitmapByteOrder32Host + AlphaPremultipliedFirst tells
    // CoreGraphics to interpret each 32-bit word using the host's native
    // byte order with alpha as the high-order byte, matching that layout.
    val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or kCGBitmapByteOrder32Host

    val cgImage = pixels.usePinned { pinned ->
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = (width * 4).toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo,
        )
        CGBitmapContextCreateImage(context)
    }
    return UIImage(cGImage = cgImage)
}
