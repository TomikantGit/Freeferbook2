package com.livrohub.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.domain.model.*
import com.livrohub.ui.settings.update.TestUpdateSection
import com.livrohub.ui.theme.LivroHubTheme

/**
 * Tela de configurações avançadas (Design System) do aplicativo LivroHub.
 *
 * Permite ao usuário alterar temas, tipografia, estilos de botões,
 * cartões e outras preferências visuais dinâmicas.
 *
 * @param viewModel ViewModel responsável pelo gerenciamento de estado das configurações.
 * @param onBack Callback acionado para voltar à tela anterior.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

    SettingsContent(
        settings = settings,
        onUpdate = { updater -> viewModel.updateSettings(updater) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", style = LivroHubTheme.typography.titleLarge) },
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
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory(
                title = "Aparência",
                description = "Tema, tipografia, espaçamento, componentes e animações."
            ) {
                SettingsSubsectionHeader(title = "Tema e cores")
                DropdownPreference(
                    title = "Tema visual",
                    description = "Altera a paleta de cores global do aplicativo.",
                    currentValue = settings.theme,
                    values = AppTheme.entries,
                    nameMapper = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { v -> onUpdate { it.copy(theme = v) } }
                )
                SwitchPreference(
                    title = "Ativar transparência",
                    subtitle = "Efeito de vidro (Glassmorphism) no fundo dos componentes.",
                    checked = settings.transparencyEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(transparencyEnabled = v) } }
                )
                SliderPreference(
                    title = "Intensidade do blur",
                    description = "Define o nível de desfoque quando a transparência está ativa.",
                    value = settings.blurIntensity,
                    range = 0f..3f,
                    onValueChange = { v -> onUpdate { it.copy(blurIntensity = v) } }
                )
                SliderPreference(
                    title = "Intensidade das sombras",
                    description = "Define a profundidade dos elementos flutuantes, como cartões e popups.",
                    value = settings.shadowIntensity,
                    range = 0f..3f,
                    onValueChange = { v -> onUpdate { it.copy(shadowIntensity = v) } }
                )

                SettingsSubsectionDivider()
                SettingsSubsectionHeader(title = "Tipografia")
                DropdownPreference(
                    title = "Família de fonte",
                    description = "Define o estilo visual das letras e afeta a legibilidade do texto.",
                    currentValue = settings.fontFamily,
                    values = FontFamilyPreference.entries,
                    nameMapper = { it.name.replace("_", " ") },
                    onSelect = { v -> onUpdate { it.copy(fontFamily = v) } }
                )
                DropdownPreference(
                    title = "Escala da fonte",
                    description = "Ajusta o tamanho geral dos textos da interface.",
                    currentValue = settings.fontScaling,
                    values = FontScaling.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(fontScaling = v) } }
                )
                DropdownPreference(
                    title = "Peso da fonte",
                    description = "Define a espessura padrão do texto.",
                    currentValue = settings.fontWeight,
                    values = FontWeightPreference.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(fontWeight = v) } }
                )
                SliderPreference(
                    title = "Espaçamento de letras",
                    description = "Aumenta ou diminui a distância horizontal entre letras.",
                    value = settings.letterSpacingSp,
                    range = -2f..5f,
                    onValueChange = { v -> onUpdate { it.copy(letterSpacingSp = v) } }
                )
                SliderPreference(
                    title = "Altura da linha",
                    description = "Controla o espaço vertical entre as linhas de texto.",
                    value = settings.lineHeightMultiplier,
                    range = 0.8f..2.5f,
                    onValueChange = { v -> onUpdate { it.copy(lineHeightMultiplier = v) } }
                )

                SettingsSubsectionDivider()
                SettingsSubsectionHeader(title = "Layout e componentes")
                DropdownPreference(
                    title = "Densidade",
                    description = "Ajusta o espaçamento geral entre os componentes.",
                    currentValue = settings.density,
                    values = AppDensity.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(density = v) } }
                )
                DropdownPreference(
                    title = "Estilo de navegação",
                    description = "Escolhe como menus e abas de navegação são apresentados.",
                    currentValue = settings.navigationStyle,
                    values = NavigationStyle.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(navigationStyle = v) } }
                )
                DropdownPreference(
                    title = "Estilo de cartões",
                    description = "Define a aparência global dos cartões.",
                    currentValue = settings.cardStyle,
                    values = CardStyle.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(cardStyle = v) } }
                )
                DropdownPreference(
                    title = "Estilo de botões",
                    description = "Define a aparência global dos botões.",
                    currentValue = settings.buttonStyle,
                    values = ButtonStyle.entries,
                    nameMapper = { it.name },
                    onSelect = { v -> onUpdate { it.copy(buttonStyle = v) } }
                )
                SliderPreference(
                    title = "Arredondamento das bordas",
                    description = "Controla o arredondamento de cartões e botões.",
                    value = settings.borderRadiusFactor,
                    range = 0f..3f,
                    onValueChange = { v -> onUpdate { it.copy(borderRadiusFactor = v) } }
                )

                SettingsSubsectionDivider()
                SettingsSubsectionHeader(title = "Animações e movimento")
                SwitchPreference(
                    title = "Ativar animações",
                    subtitle = "Transições e microinterações por todo o app.",
                    checked = settings.animationsEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(animationsEnabled = v) } }
                )
                SwitchPreference(
                    title = "Reduzir movimento",
                    subtitle = "Reduz animações não essenciais para acessibilidade.",
                    checked = settings.reducedMotion,
                    onCheckedChange = { v -> onUpdate { it.copy(reducedMotion = v) } }
                )
                SliderPreference(
                    title = "Velocidade das animações",
                    description = "Controla a velocidade das transições da interface.",
                    value = settings.animationSpeedFactor,
                    range = 0.1f..3f,
                    onValueChange = { v -> onUpdate { it.copy(animationSpeedFactor = v) } }
                )
            }

            SettingsCategory(
                title = "Funcionalidades",
                description = "Comportamento do editor e módulos disponíveis no workspace."
            ) {
                SettingsSubsectionHeader(title = "Editor")
                SwitchPreference(
                    title = "Mostrar números de linha",
                    subtitle = "Exibe a numeração das linhas na margem esquerda do editor.",
                    checked = settings.showLineNumbers,
                    onCheckedChange = { v -> onUpdate { it.copy(showLineNumbers = v) } }
                )

                SettingsSubsectionDivider()
                SettingsSubsectionHeader(title = "Workspace")
                SwitchPreference(
                    title = "Exibir personagens",
                    subtitle = "Mantém a área de personagens disponível no workspace dos livros.",
                    checked = settings.showCharactersTab,
                    onCheckedChange = { v -> onUpdate { it.copy(showCharactersTab = v) } }
                )
                SwitchPreference(
                    title = "Exibir locais",
                    subtitle = "Mantém a área de locais e cenários disponível no workspace dos livros.",
                    checked = settings.showLocationsTab,
                    onCheckedChange = { v -> onUpdate { it.copy(showLocationsTab = v) } }
                )
            }

            SettingsCategory(
                title = "Extras",
                description = "Tutoriais, ajuda e ferramentas auxiliares."
            ) {
                SettingsSubsectionHeader(title = "Tutoriais")
                SwitchPreference(
                    title = "Tutorial do workspace",
                    subtitle = "Exibe novamente as dicas guiadas da tela de workspace.",
                    checked = settings.showWorkspaceTutorial,
                    onCheckedChange = { v -> onUpdate { it.copy(showWorkspaceTutorial = v) } }
                )
                SwitchPreference(
                    title = "Tutorial do editor",
                    subtitle = "Exibe novamente as dicas guiadas do editor de capítulos.",
                    checked = settings.showEditorTutorial,
                    onCheckedChange = { v -> onUpdate { it.copy(showEditorTutorial = v) } }
                )
                SwitchPreference(
                    title = "Tutorial do histórico",
                    subtitle = "Exibe novamente as dicas guiadas do histórico de versões.",
                    checked = settings.showHistoryTutorial,
                    onCheckedChange = { v -> onUpdate { it.copy(showHistoryTutorial = v) } }
                )

                SettingsSubsectionDivider()
                SettingsSubsectionHeader(title = "Atualizações de teste")
                TestUpdateSection()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = LivroHubTheme.colors.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(LivroHubTheme.radius.medium),
        border = androidx.compose.foundation.BorderStroke(1.dp, LivroHubTheme.colors.border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        start = 16.dp,
                        end = 8.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = LivroHubTheme.colors.primary,
                        style = LivroHubTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = LivroHubTheme.colors.onSurfaceVariant,
                        style = LivroHubTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
                    tint = LivroHubTheme.colors.onSurfaceVariant
                )
            }

            if (expanded) {
                HorizontalDivider(color = LivroHubTheme.colors.border)
                content()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsSubsectionHeader(title: String) {
    Text(
        text = title,
        color = LivroHubTheme.colors.onBackground,
        style = LivroHubTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSubsectionDivider() {
    HorizontalDivider(
        color = LivroHubTheme.colors.border,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun <T> DropdownPreference(
    title: String,
    description: String? = null,
    currentValue: T,
    values: List<T>,
    nameMapper: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(text = title, style = LivroHubTheme.typography.bodyLarge, color = LivroHubTheme.colors.onBackground)
                if (description != null) {
                    Text(
                        text = description,
                        style = LivroHubTheme.typography.bodySmall,
                        color = LivroHubTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text(
                text = nameMapper(currentValue),
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LivroHubTheme.colors.surface)
        ) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(text = nameMapper(v), color = LivroHubTheme.colors.onSurface) },
                    trailingIcon = if (v == currentValue) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = LivroHubTheme.colors.primary) }
                    } else null,
                    onClick = {
                        onSelect(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = LivroHubTheme.typography.bodyLarge, color = LivroHubTheme.colors.onBackground)
            Text(text = subtitle, style = LivroHubTheme.typography.bodySmall, color = LivroHubTheme.colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LivroHubTheme.colors.primary,
                checkedTrackColor = LivroHubTheme.colors.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = LivroHubTheme.colors.onSurfaceVariant,
                uncheckedTrackColor = LivroHubTheme.colors.surfaceVariant
            )
        )
    }
}

@Composable
private fun SliderPreference(
    title: String,
    description: String? = null,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(text = title, style = LivroHubTheme.typography.bodyLarge, color = LivroHubTheme.colors.onBackground)
                if (description != null) {
                    Text(
                        text = description,
                        style = LivroHubTheme.typography.bodySmall,
                        color = LivroHubTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text(
                text = String.format(java.util.Locale.getDefault(), "%.2f", value),
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = LivroHubTheme.colors.primary,
                activeTrackColor = LivroHubTheme.colors.primary,
                inactiveTrackColor = LivroHubTheme.colors.primary.copy(alpha = 0.2f)
            )
        )
    }
}
