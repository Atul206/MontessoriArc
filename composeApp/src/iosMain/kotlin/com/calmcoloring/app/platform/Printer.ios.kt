package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Host
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.UIImage

// NOTE (PRD §5.4 fidelity, verification caveat): this file could not be
// linked/run in this environment (Xcode Command Line Tools only, no full
// Xcode) — it is verified to *compile* against the iosSimulatorArm64 target
// only. Runtime behavior (does the share sheet actually appear, does the PDF
// actually render correctly on-device) is UNVERIFIED here.
@OptIn(ExperimentalForeignApi::class)
actual suspend fun printArtwork(artwork: ImageBitmap, pageWidthPoints: Float, pageHeightPoints: Float) {
    val pageRect = CGRectMake(0.0, 0.0, pageWidthPoints.toDouble(), pageHeightPoints.toDouble())
    val format = UIGraphicsPDFRendererFormat()
    val renderer = UIGraphicsPDFRenderer(bounds = pageRect, format = format)
    val uiImage = artwork.toUIImage()

    // Fit-within-page, centered — same approach as the Android actual.
    val imageWidth = artwork.width.toDouble()
    val imageHeight = artwork.height.toDouble()
    val scale = minOf(pageWidthPoints / imageWidth, pageHeightPoints / imageHeight)
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale
    val dx = (pageWidthPoints - drawWidth) / 2.0
    val dy = (pageHeightPoints - drawHeight) / 2.0
    val drawRect = CGRectMake(dx, dy, drawWidth, drawHeight)

    val path = NSTemporaryDirectory() + "calm-coloring-export.pdf"
    val fileUrl = NSURL.fileURLWithPath(path)
    val pdfData = renderer.PDFDataWithActions { context ->
        context?.beginPage()
        uiImage.drawInRect(drawRect)
    }
    // NSData itself doesn't expose writeToFile/writeToURL in this klib's
    // Foundation binding (those are an NSData category not surfaced here);
    // NSFileManager.createFileAtPath(_:contents:attributes:) is the real,
    // available way to persist the rendered bytes to disk.
    NSFileManager.defaultManager.createFileAtPath(path, pdfData, null)

    val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
    controller?.presentViewController(activityController, animated = true, completion = null)
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
