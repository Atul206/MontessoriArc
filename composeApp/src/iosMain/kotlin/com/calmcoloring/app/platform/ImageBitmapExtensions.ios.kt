package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.kCGBitmapByteOrder32Host
import platform.UIKit.UIImage

/**
 * Converts a Compose [ImageBitmap] (Skia-backed on iOS) into a [UIImage] by
 * reading raw ARGB pixels and bridging them through a CoreGraphics
 * CGBitmapContext -> CGImage -> UIImage pipeline. There is no direct
 * ImageBitmap -> UIImage conversion in Compose Multiplatform; this bridge is
 * the standard approach.
 *
 * Shared by `Printer.ios.kt` and `Sharer.ios.kt` (previously duplicated
 * file-private copies in each — final review Finding 7a) so the
 * Core-Foundation-lifetime fix below only has to exist once.
 *
 * `CGColorSpaceCreateDeviceRGB`, `CGBitmapContextCreate`, and
 * `CGBitmapContextCreateImage` all follow the Core Foundation "Create Rule"
 * — the caller owns the returned reference and must release it — and none
 * of that is ARC-managed from Kotlin/Native. Left unreleased, each call here
 * leaked a colorspace + bitmap context + image (at print resolution, 300
 * DPI/~2550x3300px, tens of MB per call). The context and colorspace are
 * released as soon as the CGImage has been created from them (the CGImage
 * retains its own reference to the pixel data internally), and the CGImage
 * itself is released once the UIImage has been constructed from it (UIImage
 * retains its own reference too).
 */
@OptIn(ExperimentalForeignApi::class)
internal fun ImageBitmap.toUIImage(): UIImage? {
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

    val uiImage = try {
        pixels.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow = (width * 4).toULong(),
                space = colorSpace,
                bitmapInfo = bitmapInfo,
            )
            if (context == null) {
                null
            } else {
                try {
                    val cgImage = CGBitmapContextCreateImage(context)
                    if (cgImage == null) {
                        null
                    } else {
                        try {
                            UIImage(cGImage = cgImage)
                        } finally {
                            CGImageRelease(cgImage)
                        }
                    }
                } finally {
                    CGContextRelease(context)
                }
            }
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
    return uiImage
}
