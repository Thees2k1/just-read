package com.justread.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookTest {

  @Test
  fun progressText_returnsNew_whenCurrentPageIsZero() {
    val book = Book(id = "1", name = "Test", totalPages = 10, currentPage = 0, src = "a.pdf")
    assertEquals("New", book.progressText)
    assertEquals(0f, book.progressFraction, 0.0001f)
  }

  @Test
  fun progressText_andFraction_computeCorrectly_forActiveReading() {
    val book = Book(id = "1", name = "Test", totalPages = 8, currentPage = 4, src = "a.pdf")
    assertEquals("4/8 p.", book.progressText)
    assertEquals(0.5f, book.progressFraction, 0.0001f)
  }

  @Test
  fun progressFraction_handlesZeroTotalPagesSafely() {
    val book = Book(id = "1", name = "Test", totalPages = 0, currentPage = 3, src = "a.pdf")
    assertEquals(0f, book.progressFraction, 0.0001f)
  }

  @Test
  fun genresText_joinsWithCommaAndSpace() {
    val book = Book(
      id = "1",
      name = "Test",
      genres = listOf("Guide", "Compose", "Android"),
      src = "a.pdf",
    )
    assertEquals("Guide, Compose, Android", book.genresText)
  }
}
