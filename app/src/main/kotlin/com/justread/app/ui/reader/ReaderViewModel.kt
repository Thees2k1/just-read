package com.justread.app.ui.reader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justread.app.data.BookRepository
import com.justread.app.data.model.Book
import com.justread.app.data.model.ChapterItem
import com.justread.app.data.model.ReaderTheme
import com.justread.app.data.pdf.PdfDocumentManager
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UI representation of a single PDF page slot inside the virtualized reader list. */
data class PageUiState(
  val pageIndex: Int,
  val imageBitmap: ImageBitmap? = null,
  val isLoading: Boolean = false,
) {
  val pageNumber: Int get() = pageIndex + 1
  val isRendered: Boolean get() = imageBitmap != null
}

/** Complete immutable state for [ReaderScreen]. */
data class ReaderUiState(
  val bookTitle: String = "Loading...",
  val totalPages: Int = 0,
  val currentPageNumber: Int = 1,
  val initialScrollPage: Int = 1,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val pages: List<PageUiState> = emptyList(),
  val chapters: List<ChapterItem> = emptyList(),
  val bookmarks: List<Int> = emptyList(),
  val selectedTheme: ReaderTheme = ReaderTheme.CREAM,
  val zoomLevel: Float = 1.0f,
  val isChromeVisible: Boolean = true,
  val isTocOpen: Boolean = false,
  val isSettingsOpen: Boolean = false,
) {
  val canGoToPrevious: Boolean get() = currentPageNumber > 1
  val canGoToNext: Boolean get() = currentPageNumber < totalPages
  val isCurrentPageBookmarked: Boolean get() = currentPageNumber in bookmarks
  val zoomPercentageText: String get() = "${(zoomLevel * 100f).roundToInt()}%"
}

/**
 * ViewModel for [ReaderScreen], managing PDF opening, sliding-window page rendering
 * guarded by [Mutex], dual zoom, reader themes, bookmarks, and reading progress persistence.
 */
class ReaderViewModel(
  private val bookId: String,
  private val bookRepository: BookRepository,
  private val pdfManager: PdfDocumentManager,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ReaderUiState())
  val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

  private val windowMutex = Mutex()
  private val preloadTriggers = MutableSharedFlow<Int>(extraBufferCapacity = 16)

  private var loadedBook: Book? = null
  private var targetRenderWidthPx: Int = DEFAULT_RENDER_WIDTH_PX

  init {
    observeWindowPreloads()
    loadBook()
  }

  private fun observeWindowPreloads() {
    viewModelScope.launch {
      preloadTriggers.collectLatest { activeIndex ->
        preloadWindow(activeIndex)
      }
    }
  }

  private fun loadBook() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val book = bookRepository.getBookById(bookId)
      if (book == null) {
        _uiState.update {
          it.copy(isLoading = false, bookTitle = "Book Not Found", errorMessage = "Unable to find book.")
        }
        return@launch
      }
      loadedBook = book

      runCatching {
        val pageCount = pdfManager.open(book.src)
        val startPage = if (book.currentPage in 1..pageCount) book.currentPage else 1
        val skeletonPages = List(pageCount) { index -> PageUiState(pageIndex = index) }
        val chapters = generateOutline(pageCount)

        _uiState.update {
          it.copy(
            bookTitle = book.name,
            totalPages = pageCount,
            currentPageNumber = startPage,
            initialScrollPage = startPage,
            pages = skeletonPages,
            chapters = chapters,
            bookmarks = book.bookmarks.sorted(),
            isLoading = false,
          )
        }
        preloadTriggers.tryEmit(startPage - 1)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoading = false,
            bookTitle = "Error Loading PDF",
            errorMessage = err.message ?: "Unable to open PDF",
          )
        }
      }
    }
  }

  /** Updates render resolution width when the reader viewport size is measured. */
  fun updateViewportWidthPx(widthPx: Int) {
    if (widthPx > 0 && widthPx != targetRenderWidthPx) {
      targetRenderWidthPx = widthPx
    }
  }

  /** Called as the user scrolls; updates active page, persists progress, and shifts the window. */
  fun onPageSelectedFromScroll(pageIndex: Int) {
    val total = _uiState.value.totalPages
    if (total <= 0 || pageIndex !in 0 until total) return
    val newPageNumber = pageIndex + 1
    if (_uiState.value.currentPageNumber == newPageNumber) return

    _uiState.update { it.copy(currentPageNumber = newPageNumber) }
    persistProgress(newPageNumber)
    preloadTriggers.tryEmit(pageIndex)
  }

  /** Jumps to a 1-indexed page number (from input, TOC, bookmark, or prev/next arrows). */
  fun jumpToPage(pageNumber: Int) {
    val total = _uiState.value.totalPages
    if (total <= 0) return
    val clamped = pageNumber.coerceIn(1, total)
    _uiState.update {
      it.copy(
        currentPageNumber = clamped,
        isTocOpen = false,
        isSettingsOpen = false,
      )
    }
    persistProgress(clamped)
    preloadTriggers.tryEmit(clamped - 1)
  }

  fun previousPage() = jumpToPage(_uiState.value.currentPageNumber - 1)

  fun nextPage() = jumpToPage(_uiState.value.currentPageNumber + 1)

  fun toggleBookmark() {
    val page = _uiState.value.currentPageNumber
    if (_uiState.value.totalPages <= 0) return
    viewModelScope.launch {
      val updated = bookRepository.toggleBookmark(bookId, page) ?: return@launch
      loadedBook = updated
      _uiState.update { it.copy(bookmarks = updated.bookmarks.sorted()) }
    }
  }

  fun selectTheme(theme: ReaderTheme) {
    _uiState.update { it.copy(selectedTheme = theme) }
  }

  fun setZoomLevel(zoom: Float) {
    val clamped = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    _uiState.update { it.copy(zoomLevel = clamped) }
  }

  fun adjustZoomBy(factor: Float) {
    setZoomLevel(_uiState.value.zoomLevel * factor)
  }

  fun toggleChrome() {
    _uiState.update { state ->
      if (state.isTocOpen || state.isSettingsOpen) {
        state.copy(isTocOpen = false, isSettingsOpen = false)
      } else {
        state.copy(isChromeVisible = !state.isChromeVisible)
      }
    }
  }

  fun toggleToc() {
    _uiState.update {
      it.copy(
        isTocOpen = !it.isTocOpen,
        isSettingsOpen = false,
        isChromeVisible = true,
      )
    }
  }

  fun toggleSettings() {
    _uiState.update {
      it.copy(
        isSettingsOpen = !it.isSettingsOpen,
        isTocOpen = false,
        isChromeVisible = true,
      )
    }
  }

  fun closeOverlays() {
    _uiState.update { it.copy(isTocOpen = false, isSettingsOpen = false) }
  }

  private fun persistProgress(pageNumber: Int) {
    loadedBook = loadedBook?.copy(currentPage = pageNumber)
    viewModelScope.launch {
      bookRepository.updateProgress(bookId, pageNumber)
    }
  }

  private suspend fun preloadWindow(activeIndex: Int) {
    windowMutex.withLock {
      val currentPages = _uiState.value.pages
      val total = currentPages.size
      if (total == 0) return

      val renderedIndices = currentPages.filter { it.isRendered }.map { it.pageIndex }
      val toEvict = SlidingWindowCalculator.evictionIndices(activeIndex, total, renderedIndices).toSet()

      if (toEvict.isNotEmpty()) {
        _uiState.update { state ->
          state.copy(
            pages = state.pages.map { page ->
              if (page.pageIndex in toEvict) page.copy(imageBitmap = null, isLoading = false) else page
            },
          )
        }
      }

      val renderOrder = SlidingWindowCalculator.renderOrder(activeIndex, total)
      for (idx in renderOrder) {
        val pageSnapshot = _uiState.value.pages.getOrNull(idx) ?: continue
        if (pageSnapshot.isRendered) continue

        setPageLoading(idx, loading = true)
        val rendered = pdfManager.renderPage(idx, targetRenderWidthPx)
        _uiState.update { state ->
          state.copy(
            pages = state.pages.map { page ->
              if (page.pageIndex == idx) {
                page.copy(imageBitmap = rendered, isLoading = false)
              } else {
                page
              }
            },
          )
        }
      }
    }
  }

  private fun setPageLoading(index: Int, loading: Boolean) {
    _uiState.update { state ->
      state.copy(
        pages = state.pages.map { page ->
          if (page.pageIndex == index) page.copy(isLoading = loading) else page
        },
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      pdfManager.close()
    }
  }

  companion object {
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 2.0f
    private const val DEFAULT_RENDER_WIDTH_PX = 1080

    fun generateOutline(totalPages: Int): List<ChapterItem> {
      if (totalPages <= 0) return emptyList()
      val step =
        when {
          totalPages <= 10 -> 2
          totalPages <= 50 -> 5
          totalPages <= 150 -> 10
          else -> 15
        }
      return (0 until totalPages step step).mapIndexed { idx, pageIdx ->
        ChapterItem(title = "Chapter ${idx + 1}: Progress Point", pageNumber = pageIdx + 1)
      }
    }
  }
}
