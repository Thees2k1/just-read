package com.justread.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.border
import androidx.compose.foundation.style.contentPadding
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.scale
import androidx.compose.foundation.style.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable declarative styles using Jetpack Compose's experimental Styles API
 * (`androidx.compose.foundation.style`) for state-aware animations in layout/draw phases.
 */
object ReaderStyles {

  /** Interactive library book card style with press feedback and Dragonite surface framing. */
  fun bookCardStyle(
    containerColor: Color,
    pressedColor: Color,
    borderColor: Color,
  ): Style = Style {
    shape(RoundedCornerShape(18.dp))
    background(containerColor)
    border(1.dp, borderColor)
    contentPadding(14.dp)
    pressed {
      animate {
        background(pressedColor)
        scale(0.985f)
      }
    }
  }

  /** Floating capsule pill style used by the reader's bottom control bar. */
  fun floatingCapsuleStyle(
    containerColor: Color,
    borderColor: Color,
  ): Style = Style {
    shape(CircleShape)
    background(containerColor)
    border(1.2.dp, borderColor)
  }

  /** Interactive reader theme swatch button with animated selection ring & press shrink. */
  fun themeSwatchStyle(
    swatchColor: Color,
    unselectedBorder: Color,
    selectedBorder: Color,
  ): Style = Style {
    shape(RoundedCornerShape(12.dp))
    background(swatchColor)
    border(1.dp, unselectedBorder)
    selected {
      animate {
        border(2.5.dp, selectedBorder)
        scale(1.06f)
      }
    }
    pressed {
      animate {
        scale(0.94f)
      }
    }
  }
}
