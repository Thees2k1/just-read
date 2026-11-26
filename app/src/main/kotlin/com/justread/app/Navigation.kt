package com.justread.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.justread.app.data.LocalJsonBookRepository
import com.justread.app.data.pdf.PdfDocumentManager
import com.justread.app.ui.create.CreateBookScreen
import com.justread.app.ui.create.CreateBookViewModel
import com.justread.app.ui.main.MainScreen
import com.justread.app.ui.main.MainScreenViewModel
import com.justread.app.ui.reader.ReaderScreen
import com.justread.app.ui.reader.ReaderViewModel

@Composable
fun MainNavigation() {
  val context = LocalContext.current.applicationContext
  val repository = remember {
    val repoPdfCounter = PdfDocumentManager(context)
    LocalJsonBookRepository.create(context, repoPdfCounter)
  }
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          val vm: MainScreenViewModel = viewModel(key = "library") {
            MainScreenViewModel(repository)
          }
          MainScreen(
            viewModel = vm,
            onBookClick = { bookId -> backStack.add(Reader(bookId)) },
            onAddBookClick = { backStack.add(AddBook) },
          )
        }
        entry<AddBook> {
          val vm: CreateBookViewModel = viewModel(key = "add-book") {
            CreateBookViewModel(repository)
          }
          CreateBookScreen(
            viewModel = vm,
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<Reader> { key ->
          val vm: ReaderViewModel = viewModel(key = "reader-${key.bookId}") {
            ReaderViewModel(
              bookId = key.bookId,
              bookRepository = repository,
              pdfManager = PdfDocumentManager(context),
            )
          }
          ReaderScreen(
            viewModel = vm,
            onBack = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}
