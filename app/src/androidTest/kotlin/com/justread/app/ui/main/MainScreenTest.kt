package com.justread.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.justread.app.data.model.Book
import com.justread.app.ui.theme.JustReadTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreenContent]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent {
      JustReadTheme {
        MainScreenContent(
          uiState = MainScreenUiState.Success(FAKE_BOOKS),
          onBookClick = {},
          onAddBookClick = {},
        )
      }
    }
  }

  @Test
  fun books_areDisplayedInLibrary() {
    FAKE_BOOKS.forEach { book ->
      composeTestRule.onNodeWithText(book.name).assertExists()
    }
  }
}

private val FAKE_BOOKS =
  listOf(
    Book(id = "1", name = "Just Read Quick Guide", totalPages = 8, currentPage = 2, src = "sample.pdf"),
    Book(id = "2", name = "Kotlin Compose Notes", totalPages = 24, currentPage = 0, src = "notes.pdf"),
  )
