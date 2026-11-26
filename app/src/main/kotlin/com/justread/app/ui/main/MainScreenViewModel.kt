package com.justread.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justread.app.data.BookRepository
import com.justread.app.data.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the Library screen ([MainScreen]). */
class MainScreenViewModel(
  private val bookRepository: BookRepository,
) : ViewModel() {

  private val loadingState = MutableStateFlow(true)
  private val errorState = MutableStateFlow<Throwable?>(null)

  val uiState: StateFlow<MainScreenUiState> =
    combine(bookRepository.books, loadingState, errorState) { books, isLoading, error ->
      when {
        error != null -> MainScreenUiState.Error(error)
        isLoading && books.isEmpty() -> MainScreenUiState.Loading
        else -> MainScreenUiState.Success(books)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = MainScreenUiState.Loading,
    )

  init {
    refresh()
  }

  /** Ensures books are loaded from [BookRepository]. */
  fun refresh() {
    viewModelScope.launch {
      loadingState.value = true
      errorState.value = null
      runCatching { bookRepository.ensureLoaded() }
        .onFailure { errorState.value = it }
      loadingState.value = false
    }
  }
}

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState

  data class Success(val books: List<Book>) : MainScreenUiState
}
