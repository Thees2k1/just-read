package com.justread.app.ui.reader

import com.justread.app.data.model.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderOutlineAndZoomTest {

  @Test
  fun generateOutline_createsSensibleChapterStepForShortAndLongBooks() {
    val shortOutline = ReaderViewModel.generateOutline(totalPages = 8)
    assertEquals(listOf(1, 3, 5, 7), shortOutline.map { it.pageNumber })

    val mediumOutline = ReaderViewModel.generateOutline(totalPages = 45)
    assertEquals(1, mediumOutline.first().pageNumber)
    assertEquals(5, mediumOutline[1].pageNumber - mediumOutline[0].pageNumber)

    val emptyOutline = ReaderViewModel.generateOutline(totalPages = 0)
    assertEquals(0, emptyOutline.size)
  }

  @Test
  fun readerThemes_includeCreamSepiaAndDarkWithDistinctColors() {
    assertEquals(3, ReaderTheme.entries.size)
    assertEquals("Cream", ReaderTheme.CREAM.displayName)
    assertEquals("Sepia", ReaderTheme.SEPIA.displayName)
    assertEquals("Dark", ReaderTheme.DARK.displayName)
  }
}
