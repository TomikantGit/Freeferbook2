package com.livrohub.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.domain.diff.DiffLine
import com.livrohub.domain.diff.DiffLineType
import com.livrohub.domain.diff.DiffSpanType
import com.livrohub.domain.diff.TextDiffEngine
import com.livrohub.ui.theme.LivroHubTheme

data class StaticDiffRequest(
    val title: String,
    val oldLabel: String,
    val newLabel: String,
    val oldContent: String,
    val newContent: String
)

object DiffColors {
    // Light mode
    private val addedBgLight = Color(0xFFE6FFEC)
    private val removedBgLight = Color(0xFFFFEBE9)
    private val addedInlineLight = Color(0xFFABF2BC)
    private val removedInlineLight = Color(0xFFFFC1C0)
    private val addedTextLight = Color(0xFF1A7F37)
    private val removedTextLight = Color(0xFFCF222E)

    // Dark mode
    private val addedBgDark = Color(0xFF0D2818)
    private val removedBgDark = Color(0xFF3D1214)
    private val addedInlineDark = Color(0xFF1B4332)
    private val removedInlineDark = Color(0xFF5C1D1F)
    private val addedTextDark = Color(0xFF56D364)
    private val removedTextDark = Color(0xFFFF7B72)

    @Composable
    fun addedBackground(): Color = if (!LivroHubTheme.colors.isLight) addedBgDark else addedBgLight

    @Composable
    fun removedBackground(): Color = if (!LivroHubTheme.colors.isLight) removedBgDark else removedBgLight

    @Composable
    fun addedInline(): Color = if (!LivroHubTheme.colors.isLight) addedInlineDark else addedInlineLight

    @Composable
    fun removedInline(): Color = if (!LivroHubTheme.colors.isLight) removedInlineDark else removedInlineLight

    @Composable
    fun addedText(): Color = if (!LivroHubTheme.colors.isLight) addedTextDark else addedTextLight

    @Composable
    fun removedText(): Color = if (!LivroHubTheme.colors.isLight) removedTextDark else removedTextLight
}

@Composable
fun DiffScreen(
    viewModel: DiffViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiffContent(
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
fun StaticDiffScreen(
    request: StaticDiffRequest,
    onBack: () -> Unit
) {
    val uiState = remember(request) {
        val lines = TextDiffEngine().compare(
            oldText = request.oldContent,
            newText = request.newContent
        )
        DiffUiState(
            title = request.title,
            comparedVersionLabel = request.oldLabel,
            targetLabel = request.newLabel,
            lines = lines,
            addedCount = lines.count { it.type == DiffLineType.Added },
            removedCount = lines.count { it.type == DiffLineType.Removed },
            isLoading = false
        )
    }

    DiffContent(
        uiState = uiState,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiffContent(
    uiState: DiffUiState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = uiState.title.ifBlank { "Diff" }, style = LivroHubTheme.typography.titleLarge)
                        Text(
                            text = "${uiState.comparedVersionLabel} x ${uiState.targetLabel}",
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
        ) {
            DiffSummary(
                addedCount = uiState.addedCount,
                removedCount = uiState.removedCount
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(LivroHubTheme.spacing.l),
                    color = LivroHubTheme.colors.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = LivroHubTheme.spacing.s)
                ) {
                    if (uiState.lines.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhuma diferença encontrada.",
                                modifier = Modifier.padding(LivroHubTheme.spacing.m),
                                style = LivroHubTheme.typography.bodyMedium,
                                color = LivroHubTheme.colors.onBackground
                            )
                        }
                    }

                    items(uiState.lines) { line ->
                        DiffLineRow(line = line)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffSummary(
    addedCount: Int,
    removedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LivroHubTheme.spacing.m, vertical = LivroHubTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)
    ) {
        Text(
            text = "+$addedCount adicionadas",
            color = DiffColors.addedText(),
            fontWeight = FontWeight.SemiBold,
            style = LivroHubTheme.typography.bodyMedium
        )
        Text(
            text = "-$removedCount removidas",
            color = DiffColors.removedText(),
            fontWeight = FontWeight.SemiBold,
            style = LivroHubTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DiffLineRow(
    line: DiffLine
) {
    val background = when (line.type) {
        DiffLineType.Unchanged -> LivroHubTheme.colors.background
        DiffLineType.Added -> DiffColors.addedBackground()
        DiffLineType.Removed -> DiffColors.removedBackground()
    }
    val prefixColor = when (line.type) {
        DiffLineType.Unchanged -> LivroHubTheme.colors.onBackground
        DiffLineType.Added -> DiffColors.addedText()
        DiffLineType.Removed -> DiffColors.removedText()
    }

    val addedInline = DiffColors.addedInline()
    val removedInline = DiffColors.removedInline()

    val annotatedText = buildAnnotatedString {
        for (span in line.spans) {
            when (span.type) {
                DiffSpanType.Unchanged -> append(span.text)
                DiffSpanType.Added -> {
                    withStyle(style = SpanStyle(background = addedInline)) {
                        append(span.text)
                    }
                }
                DiffSpanType.Removed -> {
                    withStyle(style = SpanStyle(background = removedInline)) {
                        append(span.text)
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = LivroHubTheme.spacing.m, vertical = LivroHubTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
    ) {
        Text(
            text = line.prefix,
            color = prefixColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = annotatedText,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Monospace,
            style = LivroHubTheme.typography.bodyMedium,
            color = LivroHubTheme.colors.onBackground
        )
    }
}
