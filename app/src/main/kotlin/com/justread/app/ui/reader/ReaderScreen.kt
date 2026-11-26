package com.justread.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justread.app.data.model.ChapterItem
import com.justread.app.data.model.ReaderTheme
import com.justread.app.ui.theme.ReaderStyles
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  var didInitialScroll by remember { mutableStateOf(false) }

  // Restore saved reading page once PDF page count is ready.
  LaunchedEffect(uiState.totalPages, uiState.initialScrollPage) {
    if (uiState.totalPages > 0 && !didInitialScroll) {
      val targetIdx = (uiState.initialScrollPage - 1).coerceIn(0, uiState.totalPages - 1)
      if (targetIdx > 0) {
        listState.scrollToItem(targetIdx)
      }
      didInitialScroll = true
    }
  }

  // Track viewport-centered page during infinite scroll.
  LaunchedEffect(listState, uiState.totalPages) {
    if (uiState.totalPages <= 0) return@LaunchedEffect
    snapshotFlow { findCenterPageIndex(listState) }
      .filterNotNull()
      .distinctUntilChanged()
      .collect { centerIdx ->
        viewModel.onPageSelectedFromScroll(centerIdx)
      }
  }

  val theme = uiState.selectedTheme
  val navigateToPage: (Int) -> Unit = { pageNumber ->
    viewModel.jumpToPage(pageNumber)
    val idx = (pageNumber - 1).coerceAtLeast(0)
    scope.launch { listState.animateScrollToItem(idx) }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(theme.background)
        .safeDrawingPadding(),
  ) {
    // Layer 0: Infinite virtualized PDF viewport with pinch-to-zoom + tap-to-toggle chrome.
    ReaderPagesViewport(
      uiState = uiState,
      listState = listState,
      onViewportMeasuredPx = viewModel::updateViewportWidthPx,
      onPinchZoom = viewModel::adjustZoomBy,
      onTapViewport = viewModel::toggleChrome,
    )

    // Layer 1: Floating Top Header.
    AnimatedVisibility(
      visible = uiState.isChromeVisible && !uiState.isLoading,
      enter = fadeIn() + slideInVertically { -it },
      exit = fadeOut() + slideOutVertically { -it },
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      ReaderFloatingHeader(
        title = uiState.bookTitle,
        theme = theme,
        isBookmarked = uiState.isCurrentPageBookmarked,
        onBack = onBack,
        onToggleBookmark = viewModel::toggleBookmark,
      )
    }

    // Layer 2: Dimmer when TOC or Settings overlay is open.
    if (uiState.isTocOpen || uiState.isSettingsOpen) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = viewModel::closeOverlays,
            ),
      )
    }

    // Layer 3: TOC / Bookmarks overlay.
    AnimatedVisibility(
      visible = uiState.isTocOpen,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp, start = 16.dp, end = 16.dp),
    ) {
      TocAndBookmarksOverlay(
        theme = theme,
        chapters = uiState.chapters,
        bookmarks = uiState.bookmarks,
        onSelectPage = navigateToPage,
      )
    }

    // Layer 4: Settings overlay (Cream / Sepia / Dark swatches + Zoom slider).
    AnimatedVisibility(
      visible = uiState.isSettingsOpen,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp, start = 16.dp, end = 16.dp),
    ) {
      ReaderSettingsOverlay(
        theme = theme,
        zoomLevel = uiState.zoomLevel,
        zoomLabel = uiState.zoomPercentageText,
        onSelectTheme = viewModel::selectTheme,
        onZoomChange = viewModel::setZoomLevel,
      )
    }

    // Layer 5: Floating Bottom Pill (collapses during active scroll).
    AnimatedVisibility(
      visible = uiState.isChromeVisible && !uiState.isLoading && uiState.totalPages > 0,
      enter = fadeIn() + slideInVertically { it },
      exit = fadeOut() + slideOutVertically { it },
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
    ) {
      FloatingBottomPill(
        theme = theme,
        currentPage = uiState.currentPageNumber,
        totalPages = uiState.totalPages,
        canGoPrev = uiState.canGoToPrevious,
        canGoNext = uiState.canGoToNext,
        isCollapsed = listState.isScrollInProgress && !uiState.isTocOpen && !uiState.isSettingsOpen,
        onToggleToc = viewModel::toggleToc,
        onPrevPage = { navigateToPage(uiState.currentPageNumber - 1) },
        onJumpToPage = navigateToPage,
        onNextPage = { navigateToPage(uiState.currentPageNumber + 1) },
        onToggleSettings = viewModel::toggleSettings,
      )
    }

    // Layer 6: Blocking loading overlay on initial PDF open.
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = theme.accent)
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Loading E-Book...",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Serif,
          )
        }
      }
    }
  }
}

@Composable
private fun ReaderPagesViewport(
  uiState: ReaderUiState,
  listState: LazyListState,
  onViewportMeasuredPx: (Int) -> Unit,
  onPinchZoom: (Float) -> Unit,
  onTapViewport: () -> Unit,
) {
  val density = LocalDensity.current
  val theme = uiState.selectedTheme

  BoxWithConstraints(
    modifier =
      Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures(onTap = { onTapViewport() })
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            while (true) {
              val event = awaitPointerEvent()
              val pressedCount = event.changes.count { it.pressed }
              if (pressedCount >= 2) {
                val zoomChange = event.calculateZoom()
                if (abs(zoomChange - 1f) > 0.002f) {
                  onPinchZoom(zoomChange)
                  event.changes.forEach { it.consume() }
                }
              }
              if (pressedCount == 0) break
            }
          }
        },
  ) {
    val viewportWidthPx = with(density) { maxWidth.roundToPx() }
    LaunchedEffect(viewportWidthPx) {
      onViewportMeasuredPx(viewportWidthPx)
    }

    val targetPageWidth: Dp = (maxWidth - 24.dp) * uiState.zoomLevel
    val horizontalScrollState = rememberScrollState()

    val listModifier =
      if (uiState.zoomLevel > 1.02f) {
        Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)
      } else {
        Modifier.fillMaxSize()
      }

    Box(modifier = listModifier) {
      LazyColumn(
        state = listState,
        modifier =
          if (uiState.zoomLevel > 1.02f) {
            Modifier.width(targetPageWidth + 24.dp)
          } else {
            Modifier.fillMaxSize()
          },
        contentPadding = PaddingValues(top = 76.dp, bottom = 92.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        items(items = uiState.pages, key = { it.pageIndex }) { page ->
          PdfPageSlot(
            page = page,
            pageWidth = targetPageWidth,
            theme = theme,
          )
        }
      }
    }
  }
}

@Composable
private fun PdfPageSlot(
  page: PageUiState,
  pageWidth: Dp,
  theme: ReaderTheme,
) {
  val bitmap = page.imageBitmap
  if (bitmap != null) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
    val heightDp = pageWidth / aspectRatio
    Image(
      bitmap = bitmap,
      contentDescription = "Page ${page.pageNumber}",
      contentScale = ContentScale.FillBounds,
      modifier =
        Modifier
          .width(pageWidth)
          .height(heightDp)
          .clip(RoundedCornerShape(6.dp)),
    )
  } else {
    Box(
      modifier =
        Modifier
          .width(pageWidth)
          .height(pageWidth * 1.294f)
          .clip(RoundedCornerShape(8.dp))
          .background(theme.foreground.copy(alpha = 0.06f)),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (page.isLoading) {
          CircularProgressIndicator(
            color = theme.accent,
            modifier = Modifier.size(34.dp),
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
          text = "Page ${page.pageNumber}",
          color = theme.foreground.copy(alpha = 0.5f),
          fontFamily = FontFamily.Serif,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun ReaderFloatingHeader(
  title: String,
  theme: ReaderTheme,
  isBookmarked: Boolean,
  onBack: () -> Unit,
  onToggleBookmark: () -> Unit,
) {
  Surface(
    color = theme.background.copy(alpha = 0.94f),
    contentColor = theme.foreground,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth().height(60.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back to Library",
          tint = theme.accent,
        )
      }
      Text(
        text = title,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium,
        color = theme.foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
      )
      IconButton(onClick = onToggleBookmark) {
        Icon(
          imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
          contentDescription = "Toggle Bookmark",
          tint = theme.accent,
        )
      }
    }
  }
}

@Composable
private fun FloatingBottomPill(
  theme: ReaderTheme,
  currentPage: Int,
  totalPages: Int,
  canGoPrev: Boolean,
  canGoNext: Boolean,
  isCollapsed: Boolean,
  onToggleToc: () -> Unit,
  onPrevPage: () -> Unit,
  onJumpToPage: (Int) -> Unit,
  onNextPage: () -> Unit,
  onToggleSettings: () -> Unit,
) {
  val pillStyle =
    ReaderStyles.floatingCapsuleStyle(
      containerColor = theme.background.copy(alpha = 0.96f),
      borderColor = theme.accent.copy(alpha = 0.35f),
    )

  var editingPage by remember { mutableStateOf(false) }
  var pageInputText by remember(currentPage) { mutableStateOf(currentPage.toString()) }

  Row(
    modifier =
      Modifier
        .styleable(null, pillStyle)
        .padding(horizontal = if (isCollapsed) 18.dp else 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (isCollapsed) {
      Text(
        text = "$currentPage / $totalPages",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        color = theme.foreground,
        modifier = Modifier.padding(vertical = 10.dp),
      )
    } else {
      IconButton(onClick = onToggleToc) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
          contentDescription = "Table of Contents",
          tint = theme.foreground,
        )
      }
      IconButton(onClick = onPrevPage, enabled = canGoPrev) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = "Previous Page",
          tint = if (canGoPrev) theme.foreground else theme.foreground.copy(alpha = 0.35f),
        )
      }

      if (editingPage) {
        OutlinedTextField(
          value = pageInputText,
          onValueChange = { value ->
            if (value.all { it.isDigit() }) pageInputText = value
          },
          singleLine = true,
          textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif,
            color = theme.foreground,
          ),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Go,
          ),
          keyboardActions = KeyboardActions(
            onGo = {
              pageInputText.toIntOrNull()?.let(onJumpToPage)
              editingPage = false
            },
          ),
          modifier = Modifier.width(68.dp),
        )
      } else {
        Text(
          text = "$currentPage / $totalPages",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.SemiBold,
          color = theme.foreground,
          modifier =
            Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                pageInputText = currentPage.toString()
                editingPage = true
              }
              .padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }

      IconButton(onClick = onNextPage, enabled = canGoNext) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = "Next Page",
          tint = if (canGoNext) theme.foreground else theme.foreground.copy(alpha = 0.35f),
        )
      }
      IconButton(onClick = onToggleSettings) {
        Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = "Reading Settings",
          tint = theme.foreground,
        )
      }
    }
  }
}

@Composable
private fun TocAndBookmarksOverlay(
  theme: ReaderTheme,
  chapters: List<ChapterItem>,
  bookmarks: List<Int>,
  onSelectPage: (Int) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Surface(
    color = theme.background,
    contentColor = theme.foreground,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.accent),
    shadowElevation = 8.dp,
    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(320.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Table of Contents",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.accent.copy(alpha = 0.12f))
            .padding(3.dp),
      ) {
        listOf("Chapters", "Bookmarks").forEachIndexed { idx, label ->
          val active = selectedTab == idx
          Box(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) theme.accent.copy(alpha = 0.22f) else Color.Transparent)
                .clickable { selectedTab = idx }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
              color = theme.foreground.copy(alpha = if (active) 1f else 0.65f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (selectedTab == 0) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(chapters) { chapter ->
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { onSelectPage(chapter.pageNumber) }
                  .padding(vertical = 10.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = chapter.title,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "p. ${chapter.pageNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = theme.foreground.copy(alpha = 0.6f),
              )
            }
            HorizontalDivider(color = theme.foreground.copy(alpha = 0.1f))
          }
        }
      } else if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "No Bookmarks Saved",
            fontFamily = FontFamily.Serif,
            color = theme.foreground.copy(alpha = 0.5f),
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(bookmarks) { pageNum ->
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { onSelectPage(pageNum) }
                  .padding(vertical = 10.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(text = "Page $pageNum", fontFamily = FontFamily.Serif)
            }
            HorizontalDivider(color = theme.foreground.copy(alpha = 0.1f))
          }
        }
      }
    }
  }
}

@Composable
private fun ReaderSettingsOverlay(
  theme: ReaderTheme,
  zoomLevel: Float,
  zoomLabel: String,
  onSelectTheme: (ReaderTheme) -> Unit,
  onZoomChange: (Float) -> Unit,
) {
  Surface(
    color = theme.background,
    contentColor = theme.foreground,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.accent),
    shadowElevation = 8.dp,
    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Reading Theme",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        ReaderTheme.entries.forEach { option ->
          val interactionSource = remember { MutableInteractionSource() }
          val isSelected = option == theme
          val styleState =
            rememberUpdatedStyleState(interactionSource) { state ->
              state.isSelected = isSelected
            }
          val swatchStyle =
            ReaderStyles.themeSwatchStyle(
              swatchColor = option.background,
              unselectedBorder = theme.foreground.copy(alpha = 0.25f),
              selectedBorder = theme.accent,
            )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier
                  .size(width = 64.dp, height = 42.dp)
                  .styleable(styleState, swatchStyle)
                  .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onSelectTheme(option) },
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Aa",
                color = option.foreground,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = option.displayName,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) theme.accent else theme.foreground.copy(alpha = 0.7f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Zoom Level",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = zoomLabel,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.SemiBold,
          color = theme.accent,
        )
      }

      Slider(
        value = zoomLevel,
        onValueChange = onZoomChange,
        valueRange = ReaderViewModel.MIN_ZOOM..ReaderViewModel.MAX_ZOOM,
        steps = 29,
        colors = SliderDefaults.colors(
          thumbColor = theme.accent,
          activeTrackColor = theme.accent,
          inactiveTrackColor = theme.foreground.copy(alpha = 0.2f),
        ),
      )
    }
  }
}

private fun findCenterPageIndex(listState: LazyListState): Int? {
  val layoutInfo = listState.layoutInfo
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return null
  val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
  return visibleItems.minByOrNull { item ->
    val itemCenter = item.offset + item.size / 2
    abs(itemCenter - viewportCenter)
  }?.index
}
