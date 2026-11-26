package com.justread.app.ui.reader

/** Pure window-math calculator mirroring UWP `PreloadWindowAsync` sliding window behavior. */
object SlidingWindowCalculator {

  /** Inclusive page-index bounds `[active - behind, active + ahead]` clamped to `[0, totalPages - 1]`. */
  fun windowBounds(
    activeIndex: Int,
    totalPages: Int,
    behind: Int = 1,
    ahead: Int = 2,
  ): IntRange {
    if (totalPages <= 0) return IntRange.EMPTY
    val safeActive = activeIndex.coerceIn(0, totalPages - 1)
    val minIdx = (safeActive - behind).coerceAtLeast(0)
    val maxIdx = (safeActive + ahead).coerceAtMost(totalPages - 1)
    return minIdx..maxIdx
  }

  /** Returns indices inside the window ordered closest-first: active, +1, -1, +2, ... */
  fun renderOrder(
    activeIndex: Int,
    totalPages: Int,
    behind: Int = 1,
    ahead: Int = 2,
  ): List<Int> {
    if (totalPages <= 0) return emptyList()
    val bounds = windowBounds(activeIndex, totalPages, behind, ahead)
    val safeActive = activeIndex.coerceIn(0, totalPages - 1)
    val maxOffset = maxOf(behind, ahead)

    return buildList {
      add(safeActive)
      for (offset in 1..maxOffset) {
        val next = safeActive + offset
        if (next in bounds) add(next)
        val prev = safeActive - offset
        if (prev in bounds) add(prev)
      }
    }
  }

  /** Returns indices from [renderedIndices] that fall outside the active sliding window. */
  fun evictionIndices(
    activeIndex: Int,
    totalPages: Int,
    renderedIndices: Iterable<Int>,
    behind: Int = 1,
    ahead: Int = 2,
  ): List<Int> {
    val bounds = windowBounds(activeIndex, totalPages, behind, ahead)
    return renderedIndices.filter { it !in bounds }
  }
}
