package co.median.android.pdfviewer

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfPrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {

    override fun onLayout(oldAttributes: PrintAttributes, newAttributes: PrintAttributes,
                          cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: android.os.Bundle?) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        // Construct a PrintDocumentInfo object
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()

        // Inform the Android printing framework about the layout
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
        FileInputStream(file).use { inputStream ->
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                val buf = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buf).also { bytesRead = it } > 0) {
                    outputStream.write(buf, 0, bytesRead)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }
    }
}