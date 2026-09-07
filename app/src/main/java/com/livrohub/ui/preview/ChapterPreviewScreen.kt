package com.livrohub.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.livrohub.ui.theme.LivroHubTheme
import com.livrohub.utils.MarkdownParser

/**
 * Leitura do capítulo sem os marcadores Markdown visíveis.
 * Usa exatamente o conteúdo atual do editor, inclusive alterações ainda não salvas.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChapterPreviewScreen(
    chapterTitle: String,
    markdown: String,
    hasUnsavedChanges: Boolean,
    fontSizeSp: Int,
    onBack: () -> Unit
) {
    val renderedText = remember(markdown) {
        MarkdownParser.parseToAnnotatedString(markdown)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chapterTitle.ifBlank { "Capítulo" },
                            style = LivroHubTheme.typography.titleLarge
                        )
                        Text(
                            text = if (hasUnsavedChanges) {
                                "Visão • inclui alterações não salvas"
                            } else {
                                "Visão do capítulo"
                            },
                            style = LivroHubTheme.typography.bodySmall,
                            color = LivroHubTheme.colors.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao editor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground,
                    navigationIconContentColor = LivroHubTheme.colors.onBackground
                )
            )
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LivroHubTheme.spacing.l, vertical = LivroHubTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)
        ) {
            if (renderedText.isEmpty()) {
                Text(
                    text = "Este capítulo ainda está vazio.",
                    style = LivroHubTheme.typography.bodyLarge,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
            } else {
                Text(
                    text = renderedText,
                    modifier = Modifier.fillMaxWidth(),
                    style = LivroHubTheme.typography.bodyLarge.copy(
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.5f).sp
                    ),
                    color = LivroHubTheme.colors.onBackground
                )
            }
        }
    }
}
