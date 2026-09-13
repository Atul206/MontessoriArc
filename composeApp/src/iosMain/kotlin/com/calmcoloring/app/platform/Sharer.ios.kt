package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.ui.share.ShareTarget
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

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
//
// `toUIImage()` is the shared helper in `ImageBitmapExtensions.ios.kt` (final
// review Finding 7a: it used to be duplicated here and in `Printer.ios.kt`,
// each leaking the CGColorSpace/CGBitmapContext/CGImage it created).
@OptIn(ExperimentalForeignApi::class)
actual fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget) {
    val uiImage = artwork.toUIImage()
    if (uiImage == null) {
        NSLog("Sharer: CGBitmapContextCreate returned null; nothing to share")
        return
    }
    val rootView = UIApplication.sharedApplication.keyWindow?.rootViewController
    val activityController = UIActivityViewController(activityItems = listOf(uiImage, caption), applicationActivities = null)
    // iPad presents UIActivityViewController as a popover and UIKit throws
    // unless it has an anchor (sourceView or barButtonItem) — final review
    // Finding 7b. Setting sourceView is harmless on iPhone (no popover
    // there), so it's set unconditionally rather than gated on
    // userInterfaceIdiom.
    rootView?.view?.let { anchor ->
        activityController.popoverPresentationController?.apply {
            sourceView = anchor
            sourceRect = anchor.bounds
        }
    }
    rootView?.presentViewController(activityController, animated = true, completion = null)
}
