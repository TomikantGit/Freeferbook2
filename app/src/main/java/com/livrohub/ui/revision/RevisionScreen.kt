package com.livrohub.ui.revision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.livrohub.domain.revision.RevisionIssue
import com.livrohub.domain.revision.TextRevisionEngine
import com.livrohub.ui.theme.LivroHubTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    chapterTitle: String,
    text: String,
    onBack: () -> Unit,
    onApplyText: (String) -> Unit
) {
    val result = remember(text) { TextRevisionEngine.review(text) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Revisão", style = LivroHubTheme.typography.titleLarge)
                        Text(
                            chapterTitle.ifBlank { "Capítulo" },
                            style = LivroHubTheme.typography.bodySmall,
                            color = LivroHubTheme.colors.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (result.autoFixableCount > 0) {
                        TextButton(
                            onClick = {
                                onApplyText(TextRevisionEngine.applyAllSafe(text, result))
                            }
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                            Text("Corrigir ${result.autoFixableCount}")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground,
                    navigationIconContentColor = LivroHubTheme.colors.onBackground,
                    actionIconContentColor = LivroHubTheme.colors.primary
                )
            )
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LivroHubTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)
        ) {
            item {
                RevisionSummary(
                    wordCount = result.wordCount,
                    sentenceCount = result.sentenceCount,
                    paragraphCount = result.paragraphCount,
                    issueCount = result.issues.size
                )
            }

            if (text.isBlank()) {
                item {
                    EmptyRevisionMessage(
                        title = "Nada para revisar",
                        description = "Escreva algum conteúdo no capítulo e abra a revisão novamente."
                    )
                }
            } else if (result.issues.isEmpty()) {
                item {
                    EmptyRevisionMessage(
                        title = "Nenhum problema mecânico encontrado",
                        description = "A revisão local não encontrou espaçamento, repetição ou frases longas pelos critérios atuais."
                    )
                }
            } else {
                item {
                    Text(
                        "Ocorrências",
                        style = LivroHubTheme.typography.titleMedium,
                        color = LivroHubTheme.colors.onBackground
                    )
                }

                items(
                    items = result.issues,
                    key = { issue -> "${issue.type}-${issue.start}-${issue.endExclusive}" }
                ) { issue ->
                    RevisionIssueCard(
                        issue = issue,
                        onApply = {
                            onApplyText(TextRevisionEngine.applyIssue(text, issue))
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(LivroHubTheme.spacing.m)) }
        }
    }
}

@Composable
private fun RevisionSummary(
    wordCount: Int,
    sentenceCount: Int,
    paragraphCount: Int,
    issueCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LivroHubTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(LivroHubTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = null,
                    tint = LivroHubTheme.colors.primary
                )
                Text(
                    "  Resumo do capítulo",
                    style = LivroHubTheme.typography.titleMedium,
                    color = LivroHubTheme.colors.onSurface
                )
            }
            HorizontalDivider(color = LivroHubTheme.colors.border)
            Text(
                "$wordCount palavras · $sentenceCount frases · $paragraphCount parágrafos",
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.onSurface
            )
            Text(
                if (issueCount == 1) "1 ponto para revisar" else "$issueCount pontos para revisar",
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RevisionIssueCard(
    issue: RevisionIssue,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LivroHubTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(LivroHubTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    issue.title,
                    style = LivroHubTheme.typography.titleMedium,
                    color = LivroHubTheme.colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (issue.isAutoFixable) {
                    TextButton(onClick = onApply) {
                        Text("Corrigir")
                    }
                } else {
                    Text(
                        "Sugestão",
                        style = LivroHubTheme.typography.labelMedium,
                        color = LivroHubTheme.colors.primary
                    )
                }
            }

            Text(
                issue.description,
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.onSurfaceVariant
            )
            Text(
                issue.excerpt,
                style = LivroHubTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = LivroHubTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun EmptyRevisionMessage(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = LivroHubTheme.colors.primary
        )
        Text(title, style = LivroHubTheme.typography.titleMedium)
        Text(
            description,
            style = LivroHubTheme.typography.bodyMedium,
            color = LivroHubTheme.colors.onSurfaceVariant
        )
    }
}
