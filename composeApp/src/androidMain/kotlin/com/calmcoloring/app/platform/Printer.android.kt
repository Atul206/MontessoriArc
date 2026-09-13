package com.calmcoloring.app.platform

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

actual suspend fun printArtwork(artwork: ImageBitmap, pageWidthPoints: Float, pageHeightPoints: Float) {
    val activity = currentActivity
        ?: error("printArtwork called with no foreground Activity (currentActivity is null)")

    val outputFile = File(appContext.cacheDir, "calm-coloring-export.pdf")
    renderPdf(artwork, pageWidthPoints, pageHeightPoints, outputFile)

    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = FilePrintDocumentAdapter(outputFile)
    printManager.print("Calm Coloring picture", adapter, PrintAttributes.Builder().build())
}

private fun renderPdf(artwork: ImageBitmap, pageWidthPoints: Float, pageHeightPoints: Float, outputFile: File) {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPoints.toInt(), pageHeightPoints.toInt(), 1).create()
    val page = document.startPage(pageInfo)
    val rawBitmap = artwork.asAndroidBitmap()
    // graphicsLayer.toImageBitmap() captures a GPU-backed (HARDWARE config)
    // bitmap; PdfDocument's page.canvas is a software canvas and
    // Canvas.drawBitmap refuses to draw a hardware bitmap onto it
    // ("Software rendering doesn't support hardware bitmaps"), so copy to a
    // software config first.
    val bitmap = if (rawBitmap.config == Bitmap.Config.HARDWARE) {
        rawBitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        rawBitmap
    }

    // Fit-within-page, centered — same "fit within bounds" approach used
    // elsewhere for scaling the template to available space.
    val scale = minOf(pageWidthPoints / bitmap.width, pageHeightPoints / bitmap.height)
    val dx = (pageWidthPoints - bitmap.width * scale) / 2f
    val dy = (pageHeightPoints - bitmap.height * scale) / 2f
    page.canvas.translate(dx, dy)
    page.canvas.scale(scale, scale)
    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
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
