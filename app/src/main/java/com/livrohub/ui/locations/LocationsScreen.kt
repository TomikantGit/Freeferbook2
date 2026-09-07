package com.livrohub.ui.locations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.livrohub.domain.model.Location
import com.livrohub.ui.components.LivroHubCard
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme

@Composable
fun LocationsScreen(
    viewModel: LocationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = LivroHubTheme.colors.primary
            )
        } else if (uiState.locations.isEmpty()) {
            Text(
                text = "Nenhum local catalogado ainda.",
                modifier = Modifier.align(Alignment.Center),
                style = LivroHubTheme.typography.bodyLarge,
                color = LivroHubTheme.colors.onSurfaceVariant
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(LivroHubTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.locations) { location ->
                    LocationCard(
                        location = location,
                        onClick = {
                            selectedLocation = location
                            isEditing = true
                        },
                        onDelete = { viewModel.deleteLocation(location) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                selectedLocation = null
                isEditing = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(LivroHubTheme.spacing.m),
            containerColor = LivroHubTheme.colors.accent,
            contentColor = LivroHubTheme.colors.onAccent
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar Local")
        }
    }

    if (isEditing) {
        LocationEditDialog(
            location = selectedLocation,
            onDismiss = { isEditing = false },
            onSave = {
                viewModel.saveLocation(it)
                isEditing = false
            }
        )
    }
}

@Composable
fun LocationCard(
    location: Location,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LivroHubCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (location.imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(location.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de ${location.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(LivroHubTheme.spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = LivroHubTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LivroHubTheme.colors.onSurface
                )
                if (location.description.isNotBlank()) {
                    Text(
                        text = location.description,
                        style = LivroHubTheme.typography.bodySmall,
                        color = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
                if (location.chapters.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capítulos: ${location.chapters}",
                        style = LivroHubTheme.typography.bodyMedium,
                        color = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = LivroHubTheme.colors.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Local", style = LivroHubTheme.typography.titleLarge) },
            text = { Text("Tem certeza que deseja excluir ${location.name}?", style = LivroHubTheme.typography.bodyMedium) },
            containerColor = LivroHubTheme.colors.surface,
            titleContentColor = LivroHubTheme.colors.onSurface,
            textContentColor = LivroHubTheme.colors.onSurfaceVariant,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Excluir", color = LivroHubTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun LocationEditDialog(
    location: Location?,
    onDismiss: () -> Unit,
    onSave: (Location) -> Unit
) {
    var name by remember { mutableStateOf(location?.name ?: "") }
    var description by remember { mutableStateOf(location?.description ?: "") }
    var chapters by remember { mutableStateOf(location?.chapters ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(location?.imageUri?.let { Uri.parse(it) }) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (location == null) "Novo Local" else "Editar Local", style = LivroHubTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Foto selecionada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Adicionar foto",
                            modifier = Modifier.size(48.dp),
                            tint = LivroHubTheme.colors.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = "Toque para adicionar foto",
                    style = LivroHubTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(LivroHubTheme.spacing.xs))

                LivroHubTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome do local",
                    singleLine = true
                )
                LivroHubTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descrição / Apelidos",
                    singleLine = true
                )
                LivroHubTextField(
                    value = chapters,
                    onValueChange = { chapters = it },
                    label = "Capítulos (ex: 1, 3-5)",
                    singleLine = true
                )
            }
        },
        containerColor = LivroHubTheme.colors.surface,
        titleContentColor = LivroHubTheme.colors.onSurface,
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = Location(
                        id = location?.id ?: 0,
                        bookId = location?.bookId ?: 0,
                        name = name.trim(),
                        description = description.trim(),
                        chapters = chapters.trim(),
                        imageUri = imageUri?.toString()
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar", color = if (name.isNotBlank()) LivroHubTheme.colors.primary else LivroHubTheme.colors.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
            }
        }
    )
}
