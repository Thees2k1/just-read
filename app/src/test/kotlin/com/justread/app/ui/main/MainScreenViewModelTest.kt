package com.justread.app.ui.main

import com.justread.app.data.BookRepository
import com.justread.app.data.model.Book
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initiallyLoading_thenEmitsBooks() = runTest {
    val repo = FakeBookRepository(
      listOf(Book(id = "1", name = "Just Read Quick Guide", totalPages = 8, src = "sample.pdf"))
    )
    val viewModel = MainScreenViewModel(repo)

    assertEquals(MainScreenUiState.Loading, viewModel.uiState.first())
    val success = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
    advanceUntilIdle()
    assertEquals(1, success.books.size)
    assertEquals("Just Read Quick Guide", success.books.first().name)
  }

  @Test
  fun uiState_emitsError_whenRepositoryThrows() = runTest {
    val repo = FakeBookRepository(emptyList(), shouldFail = true)
    val viewModel = MainScreenViewModel(repo)

    val errorState = viewModel.uiState.first { it is MainScreenUiState.Error }
    assertTrue(errorState is MainScreenUiState.Error)
  }
}

private class FakeBookRepository(
  private val seed: List<Book>,
  private val shouldFail: Boolean = false,
) : BookRepository {

  private val _books = MutableStateFlow<List<Book>>(emptyList())
  override val books: StateFlow<List<Book>> = _books.asStateFlow()

  override suspend fun ensureLoaded(): List<Book> {
    if (shouldFail) error("Disk error")
    _books.value = seed
    return seed
  }

  override suspend fun getBookById(bookId: String): Book? = _books.value.find { it.id == bookId }

  override suspend fun saveBook(book: Book) {
    _books.value = _books.value + book
  }

  override suspend fun updateProgress(bookId: String, currentPage: Int) = Unit

  override suspend fun toggleBookmark(bookId: String, pageNumber: Int): Book? = null

  override suspend fun importPdf(
    sourceOpener: () -> InputStream?,
    title: String,
    authorName: String,
    genres: List<String>,
    description: String,
  ): Result<Book> = Result.failure(UnsupportedOperationException())
}
