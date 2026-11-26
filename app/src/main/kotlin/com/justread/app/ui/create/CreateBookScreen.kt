package com.justread.app.ui.create

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justread.app.data.LocalJsonBookRepository

@Composable
fun CreateBookScreen(
  viewModel: CreateBookViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val pdfPicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) {
        val displayName = resolveDisplayName(context, uri)
        viewModel.onPdfSelected(uri, displayName)
      }
    }

  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Add New Book", fontWeight = FontWeight.SemiBold) },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to Library",
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      },
    ) { innerPadding ->
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Title *") },
            placeholder = { Text("Enter book title (Required)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = uiState.authorName,
            onValueChange = viewModel::onAuthorChange,
            label = { Text("Author") },
            placeholder = { Text("Enter author name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = uiState.genresInput,
            onValueChange = viewModel::onGenresChange,
            label = { Text("Genres") },
            placeholder = { Text("Self-Help, Technology (comma separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("Enter a brief summary") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
          )

          Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "PDF Source File",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = uiState.selectedFileName ?: "No PDF selected",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
              ) {
                Text("Select PDF")
              }
            }
          }

          uiState.errorMessage?.let { err ->
            Text(
              text = err,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = {
              viewModel.saveBook(
                openStream = { uri -> LocalJsonBookRepository.openUriStream(context, uri) },
                onSuccess = onBack,
              )
            },
            enabled = !uiState.isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
          ) {
            Text("Save Book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    if (uiState.isSaving) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primaryContainer)
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Saving Book...",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
  context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
      val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (idx >= 0) {
        return cursor.getString(idx) ?: "document.pdf"
      }
    }
  }
  return uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
}
