package com.justread.app.data

import android.content.Context
import android.net.Uri
import com.justread.app.data.model.Book
import com.justread.app.data.pdf.PdfDocumentManager
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Repository contract for managing the local PDF library and reading state. */
interface BookRepository {
  val books: StateFlow<List<Book>>

  suspend fun ensureLoaded(): List<Book>

  suspend fun getBookById(bookId: String): Book?

  suspend fun saveBook(book: Book)

  suspend fun updateProgress(bookId: String, currentPage: Int)

  suspend fun toggleBookmark(bookId: String, pageNumber: Int): Book?

  suspend fun importPdf(
    sourceOpener: () -> InputStream?,
    title: String,
    authorName: String,
    genres: List<String>,
    description: String,
  ): Result<Book>
}

/**
 * JSON-backed [BookRepository] serialized with a [Mutex] (Android equivalent of UWP's
 * `SemaphoreSlim(1, 1)`), keeping `books.json` and the in-memory list race-free.
 */
class LocalJsonBookRepository(
  private val filesDir: File,
  private val seedAssetOpener: (() -> InputStream?)? = null,
  private val pdfPageCounter: suspend (File) -> Int = { 0 },
) : BookRepository {

  private val mutex = Mutex()
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val _books = MutableStateFlow<List<Book>>(emptyList())
  override val books: StateFlow<List<Book>> = _books.asStateFlow()

  private var initialized = false

  override suspend fun ensureLoaded(): List<Book> =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        if (initialized) return@withLock _books.value
        val loaded = readBooksFromDisk().toMutableList()
        if (loaded.isEmpty() && seedAssetOpener != null) {
          seedSampleBook()?.let {
            loaded.add(it)
            writeBooksToDisk(loaded)
          }
        }
        _books.value = loaded.toList()
        initialized = true
        _books.value
      }
    }

  override suspend fun getBookById(bookId: String): Book? {
    val current = ensureLoaded()
    return current.find { it.id == bookId }
  }

  override suspend fun saveBook(book: Book) {
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = ensureLoadedLocked().toMutableList()
        val idx = list.indexOfFirst { it.id == book.id }
        if (idx >= 0) {
          list[idx] = book
        } else {
          list.add(book)
        }
        writeBooksToDisk(list)
        _books.value = list.toList()
      }
    }
  }

  override suspend fun updateProgress(bookId: String, currentPage: Int) {
    if (currentPage <= 0) return
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = ensureLoadedLocked().toMutableList()
        val idx = list.indexOfFirst { it.id == bookId }
        if (idx >= 0 && list[idx].currentPage != currentPage) {
          list[idx] = list[idx].copy(currentPage = currentPage)
          writeBooksToDisk(list)
          _books.value = list.toList()
        }
      }
    }
  }

  override suspend fun toggleBookmark(bookId: String, pageNumber: Int): Book? =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = ensureLoadedLocked().toMutableList()
        val idx = list.indexOfFirst { it.id == bookId }
        if (idx < 0) return@withLock null
        val existing = list[idx]
        val updatedBookmarks =
          if (pageNumber in existing.bookmarks) {
            existing.bookmarks - pageNumber
          } else {
            (existing.bookmarks + pageNumber).sorted()
          }
        val updated = existing.copy(bookmarks = updatedBookmarks)
        list[idx] = updated
        writeBooksToDisk(list)
        _books.value = list.toList()
        updated
      }
    }

  override suspend fun importPdf(
    sourceOpener: () -> InputStream?,
    title: String,
    authorName: String,
    genres: List<String>,
    description: String,
  ): Result<Book> =
    withContext(Dispatchers.IO) {
      runCatching {
        val bookId = UUID.randomUUID().toString()
        val destFileName = "book_$bookId.pdf"
        val destFile = File(filesDir, destFileName)

        val stream = sourceOpener() ?: error("Unable to open selected PDF.")
        stream.use { input ->
          destFile.outputStream().use { output -> input.copyTo(output) }
        }

        val pageCount = pdfPageCounter(destFile)
        val book =
          Book(
            id = bookId,
            name = title.trim(),
            authorName = authorName.trim().ifEmpty { "Unknown Author" },
            description = description.trim(),
            totalPages = pageCount,
            currentPage = 0,
            genres = genres,
            src = destFileName,
          )
        saveBook(book)
        book
      }
    }

  private fun ensureLoadedLocked(): List<Book> {
    if (!initialized) {
      val loaded = readBooksFromDisk()
      _books.value = loaded
      initialized = true
    }
    return _books.value
  }

  private suspend fun seedSampleBook(): Book? {
    return runCatching {
      val cachedSample = File(filesDir, "asset_$SAMPLE_ASSET_NAME")
      if (!cachedSample.exists()) {
        seedAssetOpener?.invoke()?.use { input ->
          cachedSample.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
      }
      val totalPages = pdfPageCounter(cachedSample).coerceAtLeast(1)
      Book(
        id = "sample-guide",
        name = "Just Read Quick Guide",
        authorName = "Just Read",
        description = "An 8-page sample PDF showcasing infinite scroll, Dragonite themes, and dual zoom.",
        totalPages = totalPages,
        currentPage = 0,
        genres = listOf("Guide", "Sample"),
        src = "${PdfDocumentManager.ASSET_PREFIX}$SAMPLE_ASSET_NAME",
      )
    }.getOrNull()
  }

  private fun readBooksFromDisk(): List<Book> {
    val file = File(filesDir, JSON_FILE_NAME)
    if (!file.exists()) return emptyList()
    return runCatching {
      val text = file.readText()
      if (text.isBlank()) emptyList() else json.decodeFromString<List<Book>>(text)
    }.getOrDefault(emptyList())
  }

  private fun writeBooksToDisk(books: List<Book>) {
    filesDir.mkdirs()
    val file = File(filesDir, JSON_FILE_NAME)
    file.writeText(json.encodeToString(books))
  }

  companion object {
    private const val JSON_FILE_NAME = "books.json"
    private const val SAMPLE_ASSET_NAME = "sample.pdf"

    fun create(context: Context, pdfManager: PdfDocumentManager): LocalJsonBookRepository {
      val appContext = context.applicationContext
      return LocalJsonBookRepository(
        filesDir = appContext.filesDir,
        seedAssetOpener = { appContext.assets.open(SAMPLE_ASSET_NAME) },
        pdfPageCounter = { file -> pdfManager.countPages(file) },
      )
    }

    fun openUriStream(context: Context, uri: Uri): InputStream? =
      context.contentResolver.openInputStream(uri)
  }
}
