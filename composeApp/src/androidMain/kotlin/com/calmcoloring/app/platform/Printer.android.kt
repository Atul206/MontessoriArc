package com.calmcoloring.app.platform

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.calmcoloring.app.model.Template
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Printer"

// Set once from MainActivity.onCreate before any print/share call. Task 8's
// Sharer reads this too.
lateinit var appContext: Context

// android.print.PrintManager.print() requires its owning Context to
// literally be an Activity instance (it checks `context is Activity` and
// throws "Can print only from an activity" otherwise) — verified on-device;
// applicationContext alone is not enough. Kept separate from [appContext]
// (which stays an application Context per the brief, for Task 8's Sharer and
// for file I/O here) and set/cleared alongside the activity lifecycle by
// MainActivity.
var currentActivity: Activity? = null

// Same stroke width RegionCanvas uses for outlines, so print output matches
// what's shown on screen.
private const val OUTLINE_STROKE_WIDTH = 3.5f

actual suspend fun printArtwork(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    pageWidthPoints: Float,
    pageHeightPoints: Float,
) {
    val activity = currentActivity
    if (activity == null) {
        Log.e(TAG, "printArtwork called with no foreground Activity (currentActivity is null); aborting.")
        return
    }

    try {
        val outputFile = withContext(Dispatchers.IO) {
            val file = File(appContext.cacheDir, "calm-coloring-export.pdf")
            renderPdf(template, fills, unfilledColor, outlineColor, pageWidthPoints, pageHeightPoints, file)
            file
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = FilePrintDocumentAdapter(outputFile)
        printManager.print("Calm Coloring picture", adapter, PrintAttributes.Builder().build())
    } catch (e: Exception) {
        // Don't let a PDF/print-framework failure crash the app — log and
        // give up gracefully. No polished user-facing error UI in this task.
        Log.e(TAG, "printArtwork failed", e)
    }
}

/**
 * Renders the template's real vector region paths (the same [Template]
 * /[com.calmcoloring.app.model.RegionSpec] data [com.calmcoloring.app.ui.canvas.RegionCanvas]
 * draws from) directly onto the PDF page's [android.graphics.Canvas] — a
 * true vector draw, not a rasterized bitmap embed, so print output isn't
 * capped by the screen's on-screen resolution (PRD §5.4/§6).
 */
private fun renderPdf(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    pageWidthPoints: Float,
    pageHeightPoints: Float,
    outputFile: File,
) {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPoints.toInt(), pageHeightPoints.toInt(), 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    // Fit-within-page, centered — same "fit viewBox into available space"
    // math RegionCanvas uses for the on-screen render.
    val scale = minOf(pageWidthPoints / template.viewBoxWidth, pageHeightPoints / template.viewBoxHeight)
    val dx = (pageWidthPoints - template.viewBoxWidth * scale) / 2f
    val dy = (pageHeightPoints - template.viewBoxHeight * scale) / 2f

    canvas.save()
    canvas.translate(dx, dy)
    canvas.scale(scale, scale)

    val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = OUTLINE_STROKE_WIDTH
        color = outlineColor.toArgb()
    }

    template.regions.forEach { region ->
        val androidPath = region.path.asAndroidPath()
        fillPaint.color = (fills[region.id] ?: unfilledColor).toArgb()
        canvas.drawPath(androidPath, fillPaint)
        canvas.drawPath(androidPath, strokePaint)
    }

    canvas.restore()
    document.finishPage(page)

    FileOutputStream(outputFile).use { document.writeTo(it) }
    document.close()
}

/**
 * Streams an already-rendered PDF file to the destination the print
 * framework hands us. `android.print.PdfDocumentAdapter` is not a real
 * Android class — [PrintDocumentAdapter] is the actual abstract base, and it
 * expects [onLayout] to describe the document and [onWrite] to copy bytes
 * into the supplied [ParcelFileDescriptor].
 */
private class FilePrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("calm-coloring-export.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
