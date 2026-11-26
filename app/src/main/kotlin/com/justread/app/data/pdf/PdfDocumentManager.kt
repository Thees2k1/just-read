package com.justread.app.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thread-safe wrapper around Android's [PdfRenderer].
 *
 * Android's [PdfRenderer] allows only a single [PdfRenderer.Page] open at a time;
 * [renderMutex] serializes all open/render/close operations (matching UWP's SemaphoreSlim).
 */
class PdfDocumentManager(private val context: Context) {

  private val renderMutex = Mutex()
  private var fileDescriptor: ParcelFileDescriptor? = null
  private var renderer: PdfRenderer? = null

  /** Opens a PDF from `asset:///...` or a file inside [Context.getFilesDir] and returns page count. */
  suspend fun open(sourcePath: String): Int =
    withContext(Dispatchers.IO) {
      renderMutex.withLock {
        closeInternal()
        val file = resolvePdfFile(sourcePath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(pfd)
        fileDescriptor = pfd
        renderer = pdfRenderer
        pdfRenderer.pageCount
      }
    }

  /** Counts pages in a standalone local PDF file without replacing the active reader document. */
  suspend fun countPages(file: File): Int =
    withContext(Dispatchers.IO) {
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { it.pageCount }
      }
    }

  /** Renders [pageIndex] into an [ImageBitmap] scaled to [targetWidthPx] on a white page sheet. */
  suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
      renderMutex.withLock {
        val pdfRenderer = renderer ?: return@withLock null
        if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) return@withLock null

        pdfRenderer.openPage(pageIndex).use { page ->
          val clampedWidth = targetWidthPx.coerceIn(MIN_RENDER_WIDTH_PX, MAX_RENDER_WIDTH_PX)
          val aspectRatio = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
          val targetHeight = (clampedWidth * aspectRatio).roundToInt().coerceAtLeast(1)

          val bitmap = Bitmap.createBitmap(clampedWidth, targetHeight, Bitmap.Config.ARGB_8888)
          Canvas(bitmap).drawColor(Color.WHITE)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          bitmap.asImageBitmap()
        }
      }
    }

  /** Releases the active [PdfRenderer] and underlying file descriptor. */
  suspend fun close() {
    withContext(Dispatchers.IO) {
      renderMutex.withLock { closeInternal() }
    }
  }

  private fun closeInternal() {
    renderer?.close()
    renderer = null
    fileDescriptor?.close()
    fileDescriptor = null
  }

  private fun resolvePdfFile(sourcePath: String): File {
    if (sourcePath.startsWith(ASSET_PREFIX)) {
      val assetName = sourcePath.removePrefix(ASSET_PREFIX)
      val cachedAsset = File(context.filesDir, "asset_$assetName")
      if (!cachedAsset.exists()) {
        context.assets.open(assetName).use { input ->
          cachedAsset.outputStream().use { output -> input.copyTo(output) }
        }
      }
      return cachedAsset
    }
    return File(context.filesDir, sourcePath)
  }

  companion object {
    const val ASSET_PREFIX = "asset:///"
    private const val MIN_RENDER_WIDTH_PX = 360
    private const val MAX_RENDER_WIDTH_PX = 2048
  }
}
