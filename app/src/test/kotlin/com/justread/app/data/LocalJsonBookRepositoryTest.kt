package com.justread.app.data

import com.justread.app.data.model.Book
import java.io.ByteArrayInputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalJsonBookRepositoryTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun ensureLoaded_seedsSampleBook_whenEmpty() = runTest {
    val repo =
      LocalJsonBookRepository(
        filesDir = tempFolder.root,
        seedAssetOpener = { ByteArrayInputStream("%PDF-1.4".toByteArray()) },
        pdfPageCounter = { 8 },
      )

    val books = repo.ensureLoaded()
    assertEquals(1, books.size)
    assertEquals("Just Read Quick Guide", books.first().name)
    assertEquals(8, books.first().totalPages)
  }

  @Test
  fun concurrentSaveAndBookmarkUpdates_doNotLoseData() = runTest {
    val repo = LocalJsonBookRepository(filesDir = tempFolder.root)
    val seed = Book(id = "book-1", name = "Book 1", totalPages = 20, src = "b1.pdf")
    repo.saveBook(seed)

    val jobs =
      (1..10).map { idx ->
        async {
          repo.saveBook(
            Book(id = "extra-$idx", name = "Extra $idx", totalPages = 10, src = "e$idx.pdf")
          )
        }
      }
    jobs.awaitAll()

    repo.updateProgress("book-1", 7)
    repo.toggleBookmark("book-1", 5)
    repo.toggleBookmark("book-1", 2)

    val reloadedRepo = LocalJsonBookRepository(filesDir = tempFolder.root)
    val reloaded = reloadedRepo.ensureLoaded()
    assertEquals(11, reloaded.size)
    val book1 = reloadedRepo.getBookById("book-1")
    assertNotNull(book1)
    assertEquals(7, book1?.currentPage)
    assertEquals(listOf(2, 5), book1?.bookmarks)
  }
}
