package com.calmcoloring.app.platform

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.calmcoloring.app.ui.share.ShareTarget
import java.io.File
import java.io.FileOutputStream

private const val TAG = "Sharer"

/**
 * Writes [artwork] to a cache-dir PNG and hands a `content://` [Uri] for it
 * (via [FileProvider] — a raw `file://` Uri would trip
 * `FileUriExposedException` on API 24+, and the receiving app couldn't read
 * it anyway) to `Intent.ACTION_SEND`. [ShareTarget.WhatsApp]/
 * [ShareTarget.Instagram] target that app's package directly; if the app
 * isn't installed, `startActivity` throws `ActivityNotFoundException`, which
 * is swallowed here in favor of falling back to the system chooser rather
 * than crashing.
 *
 * Reads the application [Context][android.content.Context] set on
 * [appContext] (declared in `Printer.android.kt`, assigned from
 * `MainActivity.onCreate`) — no `Activity` is required for
 * `ACTION_SEND` when launched with `FLAG_ACTIVITY_NEW_TASK`.
 */
actual fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget) {
    val file = File(appContext.cacheDir, "calm-coloring-share.png")
    FileOutputStream(file).use { out ->
        artwork.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri: Uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        when (target) {
            ShareTarget.WhatsApp -> setPackage("com.whatsapp")
            ShareTarget.Instagram -> setPackage("com.instagram.android")
            ShareTarget.More -> {} // no package -> system chooser
        }
    }

    val launchIntent = if (target == ShareTarget.More) Intent.createChooser(intent, "Share this picture") else intent
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        appContext.startActivity(launchIntent)
    } catch (e: android.content.ActivityNotFoundException) {
        // Targeted app isn't installed (or, in principle, no chooser handler
        // exists at all) — fall back to the system chooser rather than
        // crashing the app on a share tap.
        Log.w(TAG, "shareArtwork: $target not installed, falling back to chooser", e)
        val chooser = Intent.createChooser(intent.apply { setPackage(null) }, "Share this picture")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(chooser)
    }
}
