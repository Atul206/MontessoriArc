package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.Color
import com.calmcoloring.app.model.Template
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.popoverPresentationController
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
                ?: error("Printer: CGBitmapContextCreate returned null for a ${widthPx}x$heightPx bitmap")

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
        val rootView = UIApplication.sharedApplication.keyWindow?.rootViewController
        val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
        // On iPad, UIActivityViewController presents as a popover and UIKit
        // throws unless the popover has an anchor (sourceView or
        // barButtonItem) — final review Finding 7b. Setting sourceView is
        // harmless on iPhone (no popover there), so it's set unconditionally
        // rather than gated on userInterfaceIdiom.
        rootView?.view?.let { anchor ->
            activityController.popoverPresentationController?.apply {
                sourceView = anchor
                sourceRect = anchor.bounds
            }
        }
        rootView?.presentViewController(activityController, animated = true, completion = null)
    } catch (e: Exception) {
        // Don't let a PDF-rendering failure crash the app — log and give up
        // gracefully. No polished user-facing error UI in this task.
        NSLog("Printer: printArtwork failed: ${e.message}")
    }
}
