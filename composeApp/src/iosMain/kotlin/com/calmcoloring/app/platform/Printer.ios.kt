package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.model.Template
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Host
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.UIImage
import kotlin.math.roundToInt

// Target raster density for the print PDF. PRD §5.4/§6 ask for output that
// isn't capped by a raster template's resolution; a true CGPath vector draw
// (mirroring Android's direct android.graphics.Path -> PdfDocument.Canvas
// approach) is real, non-trivial cinterop work whose correctness can't be
// runtime-verified in this sandbox anyway (Xcode Command Line Tools only, no
// simulator run). Instead: render a DEDICATED offscreen raster sized for the
// page itself at a real print-quality density, rather than reusing whatever
// the on-screen RegionCanvas composable happens to be laid out at on a given
// phone (the bug this task was flagged for). This is a deliberately narrower
// fix than Android's — still a raster embed, just print-resolution rather
// than screen-resolution — documented here rather than silently doing less
// than asked.
private const val PRINT_DPI = 300.0

// NOTE (verification caveat): this file could not be linked/run in this
// environment (Xcode Command Line Tools only, no full Xcode) — it is
// verified to *compile* against the iosSimulatorArm64 target only. Runtime
// behavior (does the share sheet actually appear, does the PDF actually
// render correctly on-device) is UNVERIFIED here.
@OptIn(ExperimentalForeignApi::class)
actual suspend fun printArtwork(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    pageWidthPoints: Float,
    pageHeightPoints: Float,
) {
    try {
        val path = withContext(Dispatchers.Default) {
            val widthPx = (pageWidthPoints / 72f * PRINT_DPI).roundToInt()
            val heightPx = (pageHeightPoints / 72f * PRINT_DPI).roundToInt()
            val bitmap = rasterizeTemplate(template, fills, unfilledColor, outlineColor, widthPx, heightPx)
            val uiImage = bitmap.toUIImage()

            val pageRect = CGRectMake(0.0, 0.0, pageWidthPoints.toDouble(), pageHeightPoints.toDouble())
            val renderer = UIGraphicsPDFRenderer(bounds = pageRect, format = UIGraphicsPDFRendererFormat())
            val pdfData = renderer.PDFDataWithActions { context ->
                context?.beginPage()
                // The rasterized bitmap already represents the full page
                // (fit-and-centered at rasterizeTemplate time), so it's drawn
                // across the whole page rect with no further scale/center math.
                uiImage.drawInRect(pageRect)
            }

            val filePath = NSTemporaryDirectory() + "calm-coloring-export.pdf"
            // NSData itself doesn't expose writeToFile/writeToURL in this
            // klib's Foundation binding (those are an NSData category not
            // surfaced here — confirmed via `klib dump-metadata`);
            // NSFileManager.createFileAtPath(_:contents:attributes:) is the
            // real, available way to persist the rendered bytes to disk.
            NSFileManager.defaultManager.createFileAtPath(filePath, pdfData, null)
            filePath
        }

        val fileUrl = NSURL.fileURLWithPath(path)
        val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
        controller?.presentViewController(activityController, animated = true, completion = null)
    } catch (e: Exception) {
        // Don't let a PDF-rendering failure crash the app — log and give up
        // gracefully. No polished user-facing error UI in this task.
        NSLog("Printer: printArtwork failed: ${e.message}")
    }
}

/**
 * Converts a Compose [ImageBitmap] (Skia-backed on iOS) into a [UIImage] by
 * reading raw ARGB pixels and bridging them through a CoreGraphics
 * CGBitmapContext -> CGImage -> UIImage pipeline. There is no direct
 * ImageBitmap -> UIImage conversion in Compose Multiplatform; this bridge is
 * the standard approach.
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
