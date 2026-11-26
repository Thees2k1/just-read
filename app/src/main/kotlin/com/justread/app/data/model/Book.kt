package com.justread.app.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** Domain model representing a PDF book in the user's library. */
@Serializable
data class Book(
  val id: String,
  val name: String,
  val description: String = "",
  val totalPages: Int = 0,
  val currentPage: Int = 0,
  val authorName: String = "Unknown Author",
  val genres: List<String> = emptyList(),
  val src: String,
  val bookmarks: List<Int> = emptyList(),
) {
  val progressText: String
    get() = if (currentPage <= 0) "New" else "$currentPage/$totalPages p."

  val progressFraction: Float
    get() = if (totalPages <= 0) 0f else (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)

  val genresText: String
    get() = genres.joinToString(", ")
}

/** Generated outline entry for quick chapter jumping inside the reader. */
data class ChapterItem(
  val title: String,
  val pageNumber: Int,
)

/** Editorial reading themes inspired by the Dragonite palette. */
enum class ReaderTheme(
  val displayName: String,
  val background: Color,
  val foreground: Color,
  val accent: Color,
  val surfaceVariant: Color,
) {
  CREAM(
    displayName = "Cream",
    background = Color(0xFFF8EED3),
    foreground = Color(0xFF241D15),
    accent = Color(0xFF0F6D61),
    surfaceVariant = Color(0xFFEDE0BE),
  ),
  SEPIA(
    displayName = "Sepia",
    background = Color(0xFFF1E3C6),
    foreground = Color(0xFF3D2D1B),
    accent = Color(0xFF8A4F0B),
    surfaceVariant = Color(0xFFE5D2AE),
  ),
  DARK(
    displayName = "Dark",
    background = Color(0xFF191612),
    foreground = Color(0xFFF8EED3),
    accent = Color(0xFF31AE9D),
    surfaceVariant = Color(0xFF26211B),
  ),
}
