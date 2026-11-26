package com.justread.app.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justread.app.data.BookRepository
import java.io.InputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for importing a new PDF book into the library. */
data class CreateBookUiState(
  val title: String = "",
  val authorName: String = "",
  val genresInput: String = "",
  val description: String = "",
  val selectedFileName: String? = null,
  val hasSelectedFile: Boolean = false,
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
)

/** ViewModel for [CreateBookScreen] mirroring UWP `CreateBookViewModel`. */
class CreateBookViewModel(
  private val bookRepository: BookRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateBookUiState())
  val uiState: StateFlow<CreateBookUiState> = _uiState.asStateFlow()

  private var selectedUri: Uri? = null

  fun onTitleChange(value: String) {
    _uiState.update { it.copy(title = value, errorMessage = null) }
  }

  fun onAuthorChange(value: String) {
    _uiState.update { it.copy(authorName = value) }
  }

  fun onGenresChange(value: String) {
    _uiState.update { it.copy(genresInput = value) }
  }

  fun onDescriptionChange(value: String) {
    _uiState.update { it.copy(description = value) }
  }

  fun onPdfSelected(uri: Uri?, displayName: String?) {
    selectedUri = uri
    _uiState.update { state ->
      val inferredTitle =
        if (state.title.isBlank() && !displayName.isNullOrBlank()) {
          displayName.substringBeforeLast('.')
        } else {
          state.title
        }
      state.copy(
        title = inferredTitle,
        selectedFileName = displayName,
        hasSelectedFile = uri != null,
        errorMessage = null,
      )
    }
  }

  /** Validates inputs, imports the selected PDF via [openStream], and invokes [onSuccess]. */
  fun saveBook(
    openStream: (Uri) -> InputStream?,
    onSuccess: () -> Unit,
  ) {
    val current = _uiState.value
    if (current.title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title is required.") }
      return
    }
    val uri = selectedUri
    if (uri == null) {
      _uiState.update { it.copy(errorMessage = "Please select a PDF file.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, errorMessage = null) }
      val genres =
        current.genresInput
          .split(',')
          .map { it.trim() }
          .filter { it.isNotEmpty() }

      val result =
        bookRepository.importPdf(
          sourceOpener = { openStream(uri) },
          title = current.title,
          authorName = current.authorName,
          genres = genres,
          description = current.description,
        )

      result
        .onSuccess {
          _uiState.update { it.copy(isSaving = false) }
          onSuccess()
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isSaving = false,
              errorMessage = "Failed to save book: ${err.message ?: "Unknown error"}",
            )
          }
        }
    }
  }
}
