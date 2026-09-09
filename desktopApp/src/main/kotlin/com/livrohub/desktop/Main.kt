package com.livrohub.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.livrohub.domain.revision.TextRevisionEngine
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
        ChapterPane(
            state = state,
            onNewChapter = { newChapterDialog = true }
        )
        EditorPane(state)
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

@Composable
private fun ChapterPane(state: DesktopAppState, onNewChapter: () -> Unit) {
    Column(
        modifier = Modifier.width(230.dp).fillMaxHeight().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(state.currentBook?.book?.title ?: "Capítulos", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onNewChapter,
            enabled = state.currentBook != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Novo capítulo")
        }
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(
                state.currentBook?.chapters?.sortedBy { it.chapter.orderIndex }.orEmpty(),
                key = { it.chapter.id }
            ) { document ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        state.selectChapter(document.chapter.id)
                    }
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(document.chapter.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${document.versions.size} versão(ões)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
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
    Row(modifier = Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(chapter.chapter.title, style = MaterialTheme.typography.headlineMedium)
            TextField(
                value = chapter.draft,
                onValueChange = state::updateDraft,
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Editor") }
            )
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
                                TextButton(onClick = { state.restoreVersion(version) }) {
                                    Text("Restaurar")
                                }
                            }
                        }
                    }
                }
            }
        }
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
