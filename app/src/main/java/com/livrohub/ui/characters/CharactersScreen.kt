package com.livrohub.ui.characters

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
import androidx.compose.material.icons.filled.Person
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
import com.livrohub.domain.model.Character
import com.livrohub.ui.components.LivroHubCard
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme

@Composable
fun CharactersScreen(
    viewModel: CharacterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = LivroHubTheme.colors.primary
            )
        } else if (uiState.characters.isEmpty()) {
            Text(
                text = "Nenhum personagem catalogado ainda.",
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
                items(uiState.characters) { character ->
                    CharacterCard(
                        character = character,
                        onClick = {
                            selectedCharacter = character
                            isEditing = true
                        },
                        onDelete = { viewModel.deleteCharacter(character) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                selectedCharacter = null
                isEditing = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(LivroHubTheme.spacing.m),
            containerColor = LivroHubTheme.colors.accent,
            contentColor = LivroHubTheme.colors.onAccent
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
        }
    }

    if (isEditing) {
        CharacterEditDialog(
            character = selectedCharacter,
            onDismiss = { isEditing = false },
            onSave = {
                viewModel.saveCharacter(it)
                isEditing = false
            }
        )
    }
}

@Composable
fun CharacterCard(
    character: Character,
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
            if (character.imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(character.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de ${character.name}",
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(LivroHubTheme.spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${character.name} ${character.surnames}".trim(),
                    style = LivroHubTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LivroHubTheme.colors.onSurface
                )
                if (character.chapters.isNotBlank()) {
                    Text(
                        text = "Capítulos: ${character.chapters}",
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
            title = { Text("Excluir Personagem", style = LivroHubTheme.typography.titleLarge) },
            text = { Text("Tem certeza que deseja excluir ${character.name}?", style = LivroHubTheme.typography.bodyMedium) },
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
fun CharacterEditDialog(
    character: Character?,
    onDismiss: () -> Unit,
    onSave: (Character) -> Unit
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var surnames by remember { mutableStateOf(character?.surnames ?: "") }
    var chapters by remember { mutableStateOf(character?.chapters ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(character?.imageUri?.let { Uri.parse(it) }) }

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
        title = { Text(if (character == null) "Novo Personagem" else "Editar Personagem", style = LivroHubTheme.typography.titleLarge) },
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
                            imageVector = Icons.Default.Person,
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
                    label = "Nome",
                    singleLine = true
                )
                LivroHubTextField(
                    value = surnames,
                    onValueChange = { surnames = it },
                    label = "Sobrenomes",
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
                    val updated = Character(
                        id = character?.id ?: 0,
                        bookId = character?.bookId ?: 0,
                        name = name.trim(),
                        surnames = surnames.trim(),
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
