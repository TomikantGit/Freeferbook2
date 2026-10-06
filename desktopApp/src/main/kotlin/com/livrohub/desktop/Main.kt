package com.livrohub.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.livrohub.domain.revision.TextRevisionEngine
import com.livrohub.domain.markdown.MarkdownTextFormat
import com.livrohub.domain.markdown.MarkdownTextFormatter
import com.livrohub.domain.diff.DiffLine
import com.livrohub.domain.diff.DiffLineType
import com.livrohub.domain.diff.DiffSpanType
import kotlinx.coroutines.delay
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

fun main() = application {
    val state = remember { DesktopAppState() }
    Window(
        onCloseRequest = {
            state.persist()
            exitApplication()
        },
        title = "Freeferbook"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                DesktopApp(state)
            }
        }
    }
}

@Composable
private fun DesktopApp(state: DesktopAppState) {
    var newBookDialog by remember { mutableStateOf(false) }
    var newChapterDialog by remember { mutableStateOf(false) }
    var newImageUrlDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.books) {
        delay(700)
        state.persist()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        LibraryPane(
            state = state,
            onNewBook = { newBookDialog = true },
            onImportBook = {
                chooseImportArchive()?.let(state::importBook)
            },
            onExportBook = {
                val book = state.currentBook ?: return@LibraryPane
                chooseExportArchive(DesktopArchiveManager.suggestedFileName(book.book.title))
                    ?.let(state::exportCurrentBook)
            }
        )
        WorkspacePane(
            state = state,
            onNewChapter = { newChapterDialog = true },
            onNewImageUrl = { newImageUrlDialog = true },
            onNewLocalImage = {
                chooseLocalImage()?.let(state::createLocalImage)
            }
        )
        when (state.workspaceMode) {
            DesktopWorkspaceMode.CHAPTERS -> EditorPane(state)
            DesktopWorkspaceMode.CHARACTERS -> CharacterEditorPane(state)
            DesktopWorkspaceMode.LOCATIONS -> LocationEditorPane(state)
            DesktopWorkspaceMode.IMAGES -> ImageEditorPane(state)
        }
    }

    if (newBookDialog) {
        NameDialog(
            title = "Novo livro",
            label = "Título",
            onDismiss = { newBookDialog = false },
            onConfirm = {
                state.createBook(it)
                newBookDialog = false
            }
        )
    }

    if (newChapterDialog) {
        NameDialog(
            title = "Novo capítulo",
            label = "Título",
            onDismiss = { newChapterDialog = false },
            onConfirm = {
                state.createChapter(it)
                newChapterDialog = false
            }
        )
    }

    if (newImageUrlDialog) {
        NameDialog(
            title = "Nova imagem por URL",
            label = "URL http/https",
            onDismiss = { newImageUrlDialog = false },
            onConfirm = {
                state.createImageUrl(it)
                newImageUrlDialog = false
            }
        )
    }
}

@Composable
private fun LibraryPane(
    state: DesktopAppState,
    onNewBook: () -> Unit,
    onImportBook: () -> Unit,
    onExportBook: () -> Unit
) {
    Column(
        modifier = Modifier.width(230.dp).fillMaxHeight().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Freeferbook", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onNewBook, modifier = Modifier.fillMaxWidth()) {
            Text("+ Novo livro")
        }
        OutlinedButton(onClick = onImportBook, modifier = Modifier.fillMaxWidth()) {
            Text("Importar backup")
        }
        OutlinedButton(
            onClick = onExportBook,
            enabled = state.currentBook != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exportar livro (.zip)")
        }
        state.notice?.let { message ->
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = state::clearNotice)) {
                Text(message, modifier = Modifier.padding(9.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider()
        if (state.books.isEmpty()) {
            Text("Nenhum livro local.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.books, key = { it.book.id }) { document ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            state.selectBook(document.book.id)
                        }
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(document.book.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${document.chapters.size} capítulo(s)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun chooseImportArchive(): Path? {
    val dialog = FileDialog(null as Frame?, "Importar backup Freeferbook", FileDialog.LOAD).apply {
        isVisible = true
    }
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}

private fun chooseExportArchive(suggestedName: String): Path? {
    val dialog = FileDialog(null as Frame?, "Exportar backup Freeferbook", FileDialog.SAVE).apply {
        file = suggestedName
        isVisible = true
    }
    val file = dialog.file ?: return null
    val selected = Path.of(dialog.directory, file)
    val name = selected.fileName.toString()
    return if (name.endsWith(".zip", ignoreCase = true) || name.endsWith(".freeferbook", ignoreCase = true)) {
        selected
    } else {
        selected.resolveSibling("$name.zip")
    }
}

private fun chooseLocalImage(): Path? {
    val dialog = FileDialog(null as Frame?, "Adicionar imagem local", FileDialog.LOAD).apply {
        isVisible = true
    }
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}

@Composable
private fun WorkspacePane(
    state: DesktopAppState,
    onNewChapter: () -> Unit,
    onNewImageUrl: () -> Unit,
    onNewLocalImage: () -> Unit
) {
    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(state.currentBook?.book?.title ?: "Workspace", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            WorkspaceModeButton(
                label = "Capítulos",
                selected = state.workspaceMode == DesktopWorkspaceMode.CHAPTERS,
                onClick = { state.selectWorkspaceMode(DesktopWorkspaceMode.CHAPTERS) },
                modifier = Modifier.weight(1f)
            )
            WorkspaceModeButton(
                label = "Personagens",
                selected = state.workspaceMode == DesktopWorkspaceMode.CHARACTERS,
                onClick = { state.selectWorkspaceMode(DesktopWorkspaceMode.CHARACTERS) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            WorkspaceModeButton(
                label = "Locais",
                selected = state.workspaceMode == DesktopWorkspaceMode.LOCATIONS,
                onClick = { state.selectWorkspaceMode(DesktopWorkspaceMode.LOCATIONS) },
                modifier = Modifier.weight(1f)
            )
            WorkspaceModeButton(
                label = "Imagens",
                selected = state.workspaceMode == DesktopWorkspaceMode.IMAGES,
                onClick = { state.selectWorkspaceMode(DesktopWorkspaceMode.IMAGES) },
                modifier = Modifier.weight(1f)
            )
        }

        when (state.workspaceMode) {
            DesktopWorkspaceMode.CHAPTERS -> Button(
                onClick = onNewChapter,
                enabled = state.currentBook != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Novo capítulo") }

            DesktopWorkspaceMode.CHARACTERS -> Button(
                onClick = state::createCharacter,
                enabled = state.currentBook != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Novo personagem") }

            DesktopWorkspaceMode.LOCATIONS -> Button(
                onClick = state::createLocation,
                enabled = state.currentBook != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Novo local") }

            DesktopWorkspaceMode.IMAGES -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onNewImageUrl,
                    enabled = state.currentBook != null,
                    modifier = Modifier.weight(1f)
                ) { Text("+ URL") }
                OutlinedButton(
                    onClick = onNewLocalImage,
                    enabled = state.currentBook != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Arquivo") }
            }
        }

        HorizontalDivider()

        when (state.workspaceMode) {
            DesktopWorkspaceMode.CHAPTERS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(
                    state.currentBook?.chapters?.sortedBy { it.chapter.orderIndex }.orEmpty(),
                    key = { it.chapter.id }
                ) { document ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { state.selectChapter(document.chapter.id) }) {
                        Column(Modifier.padding(10.dp)) {
                            Text(document.chapter.title, style = MaterialTheme.typography.titleSmall)
                            Text("${document.versions.size} versão(ões)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            DesktopWorkspaceMode.CHARACTERS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.currentBook?.characters.orEmpty()) { index, character ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { state.selectCharacter(index) }) {
                        Column(Modifier.padding(10.dp)) {
                            Text(character.name.ifBlank { "Personagem" }, style = MaterialTheme.typography.titleSmall)
                            if (character.surnames.isNotBlank()) Text(character.surnames, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            DesktopWorkspaceMode.LOCATIONS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.currentBook?.locations.orEmpty()) { index, location ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { state.selectLocation(index) }) {
                        Column(Modifier.padding(10.dp)) {
                            Text(location.name.ifBlank { "Local" }, style = MaterialTheme.typography.titleSmall)
                            if (location.description.isNotBlank()) {
                                Text(location.description.take(80), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            DesktopWorkspaceMode.IMAGES -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.currentBook?.images.orEmpty()) { index, image ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { state.selectImage(index) }) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                image.description.ifBlank { image.url.ifBlank { "Imagem" } },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (image.mediaId != null) "Arquivo incorporado" else "Referência remota",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun EditorPane(state: DesktopAppState) {
    val chapter = state.currentChapter
    if (chapter == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Selecione ou crie um capítulo", style = MaterialTheme.typography.headlineMedium)
            Text("Os dados ficam em ${DesktopStore.defaultPath()}.")
        }
        return
    }

    val revision = remember(chapter.draft) { TextRevisionEngine.review(chapter.draft) }
    var previewMode by remember(chapter.chapter.id) { mutableStateOf(false) }
    var editorValue by remember(chapter.chapter.id) {
        mutableStateOf(TextFieldValue(chapter.draft, TextRange(chapter.draft.length)))
    }
    LaunchedEffect(chapter.draft) {
        if (editorValue.text != chapter.draft) {
            editorValue = TextFieldValue(chapter.draft, TextRange(chapter.draft.length))
        }
    }

    fun applyFormat(format: MarkdownTextFormat) {
        val result = MarkdownTextFormatter.apply(
            editorValue.text,
            editorValue.selection.start,
            editorValue.selection.end,
            format
        )
        editorValue = TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd))
        state.updateDraft(result.text)
    }

    fun applyCustomMarker(marker: String) {
        val result = MarkdownTextFormatter.applyCustomMarker(
            editorValue.text,
            editorValue.selection.start,
            editorValue.selection.end,
            marker
        )
        editorValue = TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd))
        state.updateDraft(result.text)
    }

    Row(modifier = Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(chapter.chapter.title, style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WorkspaceModeButton("Editar", !previewMode, { previewMode = false })
                WorkspaceModeButton("Visão", previewMode, { previewMode = true })
            }
            if (!previewMode) {
                val formatEnabled = !editorValue.selection.collapsed
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FormatButton("B", formatEnabled) { applyFormat(MarkdownTextFormat.Bold) }
                        FormatButton("I", formatEnabled) { applyFormat(MarkdownTextFormat.Italic) }
                        FormatButton("S", formatEnabled) { applyFormat(MarkdownTextFormat.Strikethrough) }
                        FormatButton("U", formatEnabled) { applyFormat(MarkdownTextFormat.Underline) }
                        FormatButton("==", formatEnabled) { applyFormat(MarkdownTextFormat.Highlight) }
                        FormatButton("H3", formatEnabled) { applyFormat(MarkdownTextFormat.Heading) }
                        FormatButton(">", formatEnabled) { applyFormat(MarkdownTextFormat.Quote) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FormatButton("•", formatEnabled) { applyFormat(MarkdownTextFormat.BulletedList) }
                        FormatButton("1.", formatEnabled) { applyFormat(MarkdownTextFormat.NumberedList) }
                        FormatButton("☐", formatEnabled) { applyFormat(MarkdownTextFormat.Checklist) }
                        MarkdownTextFormatter.customMarkers.forEach { marker ->
                            FormatButton(marker, formatEnabled) { applyCustomMarker(marker) }
                        }
                    }
                }
                TextField(
                    value = editorValue,
                    onValueChange = {
                        editorValue = it
                        state.updateDraft(it.text)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    label = { Text("Editor Markdown") }
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SelectionContainer {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                            item {
                                Text(
                                    DesktopMarkdownRenderer.toAnnotatedString(chapter.draft),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { state.saveVersion() }) { Text("Salvar versão") }
                OutlinedButton(
                    onClick = state::applySafeRevisionFixes,
                    enabled = revision.autoFixableCount > 0
                ) {
                    Text("Corrigir seguros (${revision.autoFixableCount})")
                }
            }
            Text(
                "${revision.wordCount} palavras • ${revision.sentenceCount} frases • ${revision.issues.size} ponto(s) para revisar",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column(
            modifier = Modifier.width(300.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Histórico", style = MaterialTheme.typography.titleLarge)
            if (chapter.versions.isEmpty()) {
                Text("Nenhuma versão salva.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(chapter.versions.sortedByDescending { it.sequenceNumber }, key = { it.id }) { version ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Versão #${version.sequenceNumber}", style = MaterialTheme.typography.titleSmall)
                                Text("${version.wordCount} palavras", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { state.restoreVersion(version) }) {
                                        Text("Restaurar")
                                    }
                                    TextButton(onClick = { state.selectVersionForComparison(version) }) {
                                        val isBase = state.compareBaseVersionId == version.id
                                        Text(
                                            when {
                                                isBase -> "Cancelar"
                                                state.compareBaseVersionId != null -> "Comparar com esta"
                                                else -> "Comparar"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.versionDiff?.let { diff ->
        VersionDiffDialog(diff = diff, onDismiss = state::clearVersionDiff)
    }
}

@Composable
private fun VersionDiffDialog(diff: DesktopVersionDiff, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Versão #${diff.baseSequence} → Versão #${diff.targetSequence}") },
        text = {
            LazyColumn(
                modifier = Modifier.width(760.dp).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(diff.lines) { line ->
                    Text(
                        text = diffLineAnnotated(line),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

private fun diffLineAnnotated(line: DiffLine): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val prefixStart = builder.length
    builder.append("${line.prefix} ")
    val lineBackground = when (line.type) {
        DiffLineType.Added -> Color(0x2234A853)
        DiffLineType.Removed -> Color(0x22EA4335)
        DiffLineType.Unchanged -> Color.Transparent
    }
    if (lineBackground != Color.Transparent) {
        builder.addStyle(SpanStyle(background = lineBackground), prefixStart, builder.length)
    }
    line.spans.forEach { span ->
        val start = builder.length
        builder.append(span.text)
        val background = when (span.type) {
            DiffSpanType.Added -> Color(0x5534A853)
            DiffSpanType.Removed -> Color(0x55EA4335)
            DiffSpanType.Unchanged -> lineBackground
        }
        if (background != Color.Transparent) {
            builder.addStyle(SpanStyle(background = background), start, builder.length)
        }
    }
    return builder.toAnnotatedString()
}

@Composable
private fun FormatButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled) {
        Text(label)
    }
}

@Composable
private fun CharacterEditorPane(state: DesktopAppState) {
    val character = state.currentCharacter
    if (character == null) {
        EmptyWorkspacePane("Selecione ou crie um personagem")
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Personagem", style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = character.name,
            onValueChange = { state.updateCurrentCharacter(character.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome") }
        )
        TextField(
            value = character.surnames,
            onValueChange = { state.updateCurrentCharacter(character.copy(surnames = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sobrenomes") }
        )
        TextField(
            value = character.chapters,
            onValueChange = { state.updateCurrentCharacter(character.copy(chapters = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Capítulos") }
        )
        TextField(
            value = character.imageUri.orEmpty(),
            onValueChange = {
                state.updateCurrentCharacter(character.copy(imageUri = it.trim().takeIf(String::isNotEmpty)))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Imagem / URL") }
        )
        if (character.mediaId != null) {
            Text("Este personagem possui mídia incorporada preservada no backup.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = state::deleteCurrentCharacter) { Text("Excluir personagem") }
    }
}

@Composable
private fun LocationEditorPane(state: DesktopAppState) {
    val location = state.currentLocation
    if (location == null) {
        EmptyWorkspacePane("Selecione ou crie um local")
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local", style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = location.name,
            onValueChange = { state.updateCurrentLocation(location.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome") }
        )
        TextField(
            value = location.description,
            onValueChange = { state.updateCurrentLocation(location.copy(description = it)) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Descrição") }
        )
        TextField(
            value = location.chapters,
            onValueChange = { state.updateCurrentLocation(location.copy(chapters = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Capítulos") }
        )
        TextField(
            value = location.imageUri.orEmpty(),
            onValueChange = {
                state.updateCurrentLocation(location.copy(imageUri = it.trim().takeIf(String::isNotEmpty)))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Imagem / URL") }
        )
        if (location.mediaId != null) {
            Text("Este local possui mídia incorporada preservada no backup.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = state::deleteCurrentLocation) { Text("Excluir local") }
    }
}

@Composable
private fun ImageEditorPane(state: DesktopAppState) {
    val image = state.currentImage
    if (image == null) {
        EmptyWorkspacePane("Adicione ou selecione uma imagem")
        return
    }
    val media = state.currentBook?.media?.firstOrNull { it.id == image.mediaId }
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Imagem de referência", style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = image.url,
            onValueChange = { state.updateCurrentImage(image.copy(url = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL / nome") }
        )
        TextField(
            value = image.description,
            onValueChange = { state.updateCurrentImage(image.copy(description = it)) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Descrição") }
        )
        if (media != null) {
            Text("Arquivo local incorporado: ${media.localPath}", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Referência externa; o backup preserva a URL.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = state::deleteCurrentImage) { Text("Excluir imagem") }
    }
}

@Composable
private fun EmptyWorkspacePane(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, style = MaterialTheme.typography.headlineMedium)
        Text("Escolha um item no painel do workspace.")
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
