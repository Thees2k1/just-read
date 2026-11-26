package com.justread.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlidingWindowCalculatorTest {

  @Test
  fun windowBounds_clampsStartAndEnd() {
    assertEquals(0..2, SlidingWindowCalculator.windowBounds(activeIndex = 0, totalPages = 10))
    assertEquals(8..9, SlidingWindowCalculator.windowBounds(activeIndex = 9, totalPages = 10))
    assertEquals(3..6, SlidingWindowCalculator.windowBounds(activeIndex = 4, totalPages = 10))
    assertTrue(SlidingWindowCalculator.windowBounds(activeIndex = 0, totalPages = 0).isEmpty())
  }

  @Test
  fun renderOrder_prioritizesActiveThenForwardAndBackwardNeighbors() {
    assertEquals(
      listOf(4, 5, 3, 6),
      SlidingWindowCalculator.renderOrder(activeIndex = 4, totalPages = 10),
    )
    assertEquals(
      listOf(0, 1, 2),
      SlidingWindowCalculator.renderOrder(activeIndex = 0, totalPages = 10),
    )
  }

  @Test
  fun evictionIndices_returnsOnlyPagesOutsideSlidingWindow() {
    val rendered = listOf(0, 1, 2, 3, 4, 5, 6, 7)
    val evicted = SlidingWindowCalculator.evictionIndices(
      activeIndex = 4,
      totalPages = 10,
      renderedIndices = rendered,
    )
    assertEquals(listOf(0, 1, 2, 7), evicted)
  }
}
