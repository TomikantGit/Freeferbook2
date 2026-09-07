package com.livrohub.ui.images

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.livrohub.domain.model.ImageReference
import com.livrohub.ui.components.LivroHubButton
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme

@Composable
fun ImagesScreen(
    viewModel: ImagesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Imagens de Referência", style = LivroHubTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground,
                    navigationIconContentColor = LivroHubTheme.colors.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = LivroHubTheme.colors.accent,
                contentColor = LivroHubTheme.colors.onAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Imagem")
            }
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LivroHubTheme.colors.primary
                )
            } else if (uiState.images.isEmpty()) {
                Text(
                    text = "Nenhuma imagem de referência.",
                    modifier = Modifier.align(Alignment.Center),
                    style = LivroHubTheme.typography.bodyLarge,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(LivroHubTheme.spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
                ) {
                    items(uiState.images, key = { it.id }) { image ->
                        ImageCard(
                            image = image,
                            onDelete = { viewModel.deleteImage(image) }
                        )
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(LivroHubTheme.spacing.m),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = LivroHubTheme.colors.primary)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        if (showAddDialog) {
            AddImageDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { url, desc ->
                    viewModel.addImage(url, desc)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun ImageCard(
    image: ImageReference,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = LivroHubTheme.colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LivroHubTheme.shadows.elevationLow),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image.url,
                contentDescription = image.description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = LivroHubTheme.colors.surface.copy(alpha = 0.7f)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = LivroHubTheme.colors.error,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir imagem?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Excluir", color = LivroHubTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun AddImageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Imagem", style = LivroHubTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)) {
                LivroHubTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "URL da Imagem",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LivroHubTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = "Descrição (Opcional)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = LivroHubTheme.colors.surface,
        confirmButton = {
            LivroHubButton(
                onClick = { onConfirm(url, desc) },
                enabled = url.isNotBlank(),
                text = "Adicionar"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
            }
        }
    )
}
