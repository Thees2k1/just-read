package com.justread.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justread.app.data.model.Book
import com.justread.app.ui.theme.JustReadTheme
import com.justread.app.ui.theme.ReaderStyles

@Composable
fun MainScreen(
  viewModel: MainScreenViewModel,
  onBookClick: (String) -> Unit,
  onAddBookClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  MainScreenContent(
    uiState = uiState,
    onBookClick = onBookClick,
    onAddBookClick = onAddBookClick,
    modifier = modifier,
  )
}

@Composable
internal fun MainScreenContent(
  uiState: MainScreenUiState,
  onBookClick: (String) -> Unit,
  onAddBookClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "My Library",
            fontWeight = FontWeight.SemiBold,
          )
        },
        actions = {
          IconButton(onClick = onAddBookClick) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = "Add New Book",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddBookClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text("Add PDF", fontWeight = FontWeight.SemiBold) },
      )
    },
  ) { innerPadding ->
    when (uiState) {
      MainScreenUiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }

      is MainScreenUiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Error loading library: ${uiState.throwable.message}",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
          )
        }
      }

      is MainScreenUiState.Success -> {
        if (uiState.books.isEmpty()) {
          EmptyLibraryState(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(items = uiState.books, key = { it.id }) { book ->
              BookCard(
                book = book,
                onClick = { onBookClick(book.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyLibraryState(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.AutoStories,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.size(72.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Your Library is Empty",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Tap + Add PDF below to import your first PDF book.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookCard(
  book: Book,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val styleState = rememberUpdatedStyleState(interactionSource)
  val cardStyle =
    ReaderStyles.bookCardStyle(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      pressedColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      borderColor = MaterialTheme.colorScheme.outlineVariant,
    )

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .styleable(styleState, cardStyle)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        ),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier =
        Modifier
          .width(58.dp)
          .height(78.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(30.dp),
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = book.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = book.authorName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (book.genres.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          book.genres.take(3).forEach { genre ->
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = genre,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      if (book.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = book.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        LinearProgressIndicator(
          progress = { book.progressFraction },
          modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
          text = book.progressText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
  JustReadTheme {
    MainScreenContent(
      uiState =
        MainScreenUiState.Success(
          listOf(
            Book(
              id = "1",
              name = "Just Read Quick Guide",
              authorName = "Just Read",
              description = "A clean sample PDF for testing infinite scroll and Dragonite themes.",
              totalPages = 8,
              currentPage = 3,
              genres = listOf("Guide", "Sample"),
              src = "asset:///sample.pdf",
            )
          )
        ),
      onBookClick = {},
      onAddBookClick = {},
    )
  }
}
