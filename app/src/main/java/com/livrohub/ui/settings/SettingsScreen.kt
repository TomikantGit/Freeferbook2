package com.livrohub.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.data.update.TestUpdateCheckResult
import com.livrohub.data.update.TestUpdateInfo
import com.livrohub.data.update.TestUpdateInstallResult
import com.livrohub.data.update.TestUpdateManager
import com.livrohub.domain.model.*
import com.livrohub.ui.theme.LivroHubTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

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
                .verticalScroll(rememberScrollState())
        ) {
            SettingsPreview(settings = settings)

            SettingsSectionHeader(title = "Tema e Cores")
            DropdownPreference(
                title = "Tema Visual",
                description = "Altera a paleta de cores global do aplicativo.",
                currentValue = settings.theme,
                values = AppTheme.entries,
                nameMapper = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = { v -> onUpdate { it.copy(theme = v) } }
            )
            SwitchPreference(
                title = "Ativar Transparência",
                subtitle = "Efeito de vidro (Glassmorphism) no background de componentes.",
                checked = settings.transparencyEnabled,
                onCheckedChange = { v -> onUpdate { it.copy(transparencyEnabled = v) } }
            )
            SliderPreference(
                title = "Intensidade do Blur",
                description = "Define o nível de desfoque quando a transparência está ativa.",
                value = settings.blurIntensity,
                range = 0f..3f,
                onValueChange = { v -> onUpdate { it.copy(blurIntensity = v) } }
            )
            SliderPreference(
                title = "Intensidade das Sombras",
                description = "Define a profundidade dos elementos flutuantes (como cartões e popups).",
                value = settings.shadowIntensity,
                range = 0f..3f,
                onValueChange = { v -> onUpdate { it.copy(shadowIntensity = v) } }
            )
            InlinePreviewBox {
                Card(
                    modifier = Modifier.size(100.dp),
                    colors = CardDefaults.cardColors(containerColor = LivroHubTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = LivroHubTheme.shadows.elevationHigh)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sombra", style = LivroHubTheme.typography.labelLarge, color = LivroHubTheme.colors.primary)
                    }
                }
            }

            HorizontalDivider(color = LivroHubTheme.colors.border)

            SettingsSectionHeader(title = "Tipografia")
            DropdownPreference(
                title = "Família de Fonte",
                description = "Define o estilo visual das letras (ex: Serif, Sans-Serif). Afeta a legibilidade e a personalidade.",
                currentValue = settings.fontFamily,
                values = FontFamilyPreference.entries,
                nameMapper = { it.name.replace("_", " ") },
                onSelect = { v -> onUpdate { it.copy(fontFamily = v) } }
            )
            DropdownPreference(
                title = "Escala da Fonte",
                description = "Ajusta o tamanho geral dos textos. Útil para quem prefere letras maiores ou mais conteúdo na tela.",
                currentValue = settings.fontScaling,
                values = FontScaling.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(fontScaling = v) } }
            )
            DropdownPreference(
                title = "Peso da Fonte",
                description = "Define a espessura padrão do texto. Textos mais grossos dão mais impacto.",
                currentValue = settings.fontWeight,
                values = FontWeightPreference.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(fontWeight = v) } }
            )
            SliderPreference(
                title = "Espaçamento de Letras",
                description = "Aumenta ou diminui a distância horizontal entre cada letra.",
                value = settings.letterSpacingSp,
                range = -2f..5f,
                onValueChange = { v -> onUpdate { it.copy(letterSpacingSp = v) } }
            )
            SliderPreference(
                title = "Altura da Linha",
                description = "Modifica o espaço vertical entre as linhas de um parágrafo.",
                value = settings.lineHeightMultiplier,
                range = 0.8f..2.5f,
                onValueChange = { v -> onUpdate { it.copy(lineHeightMultiplier = v) } }
            )
            InlinePreviewBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tipografia", style = LivroHubTheme.typography.displayMedium, color = LivroHubTheme.colors.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "O rápido raposa marrom pula sobre o cão preguiçoso.",
                        style = LivroHubTheme.typography.bodyLarge,
                        color = LivroHubTheme.colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = LivroHubTheme.colors.border)

            SettingsSectionHeader(title = "Layout e Densidade")
            DropdownPreference(
                title = "Densidade",
                description = "Ajusta o espaçamento geral entre os componentes (Compacto vs Confortável).",
                currentValue = settings.density,
                values = AppDensity.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(density = v) } }
            )
            DropdownPreference(
                title = "Estilo de Navegação",
                description = "Escolhe como os menus e abas de navegação são apresentados.",
                currentValue = settings.navigationStyle,
                values = NavigationStyle.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(navigationStyle = v) } }
            )
            DropdownPreference(
                title = "Estilo de Cartões",
                description = "Alterna entre cartões com sombras (Elevated) ou com bordas (Outlined).",
                currentValue = settings.cardStyle,
                values = CardStyle.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(cardStyle = v) } }
            )
            DropdownPreference(
                title = "Estilo de Botões",
                description = "Define a aparência dos botões: preenchidos ou delineados.",
                currentValue = settings.buttonStyle,
                values = ButtonStyle.entries,
                nameMapper = { it.name },
                onSelect = { v -> onUpdate { it.copy(buttonStyle = v) } }
            )
            SliderPreference(
                title = "Arredondamento das Bordas",
                description = "Controla quão arredondados são os cantos dos cartões e botões.",
                value = settings.borderRadiusFactor,
                range = 0f..3f,
                onValueChange = { v -> onUpdate { it.copy(borderRadiusFactor = v) } }
            )
            InlinePreviewBox {
                Button(
                    onClick = {},
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        if (settings.buttonStyle == ButtonStyle.FILLED) LivroHubTheme.radius.full else LivroHubTheme.radius.medium
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = LivroHubTheme.colors.primary)
                ) {
                    Text("Exemplo de Borda", style = LivroHubTheme.typography.labelLarge)
                }
            }

            HorizontalDivider(color = LivroHubTheme.colors.border)

            SettingsSectionHeader(title = "Animações")
            SwitchPreference(
                title = "Ativar Animações",
                subtitle = "Transições e microinterações por todo o app.",
                checked = settings.animationsEnabled,
                onCheckedChange = { v -> onUpdate { it.copy(animationsEnabled = v) } }
            )
            SwitchPreference(
                title = "Reduzir Movimento (Acessibilidade)",
                subtitle = "Desativa animações não essenciais para evitar enjoo ou distração.",
                checked = settings.reducedMotion,
                onCheckedChange = { v -> onUpdate { it.copy(reducedMotion = v) } }
            )
            SliderPreference(
                title = "Velocidade das Animações",
                description = "Muda o quão rápido ou lento as animações acontecem.",
                value = settings.animationSpeedFactor,
                range = 0.1f..3f,
                onValueChange = { v -> onUpdate { it.copy(animationSpeedFactor = v) } }
            )

            HorizontalDivider(color = LivroHubTheme.colors.border)

            SettingsSectionHeader(title = "Editor")
            SwitchPreference(
                title = "Mostrar Números de Linha",
                subtitle = "Exibe a contagem de linhas na margem esquerda do editor de texto.",
                checked = settings.showLineNumbers,
                onCheckedChange = { v -> onUpdate { it.copy(showLineNumbers = v) } }
            )

            HorizontalDivider(color = LivroHubTheme.colors.border)

            SettingsSectionHeader(title = "Atualizações de teste")
            TestUpdatePreference()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private sealed interface TestUpdateUiState {
    data object Idle : TestUpdateUiState
    data object Checking : TestUpdateUiState
    data object UpToDate : TestUpdateUiState
    data class Available(val update: TestUpdateInfo) : TestUpdateUiState
    data class Downloading(val update: TestUpdateInfo) : TestUpdateUiState
    data class Ready(val update: TestUpdateInfo, val apkFile: File) : TestUpdateUiState
    data class Error(val message: String) : TestUpdateUiState
}

/**
 * Controle manual do canal de APKs de teste.
 *
 * Nenhuma consulta de rede é feita automaticamente: o usuário decide quando verificar
 * e quando baixar uma atualização.
 */
@Composable
private fun TestUpdatePreference() {
    val context = LocalContext.current
    val manager = remember(context.applicationContext) {
        TestUpdateManager(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<TestUpdateUiState>(TestUpdateUiState.Idle) }
    var installNotice by remember { mutableStateOf<String?>(null) }

    fun requestInstall(apkFile: File) {
        when (val result = manager.requestInstall(apkFile)) {
            TestUpdateInstallResult.InstallerOpened -> {
                installNotice = "Instalador aberto. Confirme a atualização no Android."
            }

            TestUpdateInstallResult.PermissionRequired -> {
                installNotice =
                    "Autorize o Freeferbook a instalar apps desta fonte. Ao voltar, toque em Instalar atualização."
            }

            is TestUpdateInstallResult.Error -> {
                installNotice = result.message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Versão instalada",
            style = LivroHubTheme.typography.bodyLarge,
            color = LivroHubTheme.colors.onBackground
        )
        Text(
            text = "${manager.currentVersionName} (código ${manager.currentVersionCode})",
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (val currentState = state) {
            TestUpdateUiState.Idle -> {
                Text(
                    text = "Verifique manualmente o APK de teste mais recente publicado pelo GitHub Actions.",
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
            }

            TestUpdateUiState.Checking -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verificando atualização...", style = LivroHubTheme.typography.bodySmall)
                }
            }

            TestUpdateUiState.UpToDate -> {
                Text(
                    text = "Você já está usando a versão de teste mais recente.",
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
            }

            is TestUpdateUiState.Available -> {
                Text(
                    text = "Nova versão: ${currentState.update.versionName} (código ${currentState.update.versionCode})",
                    style = LivroHubTheme.typography.bodyMedium,
                    color = LivroHubTheme.colors.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                if (currentState.update.notes.isNotBlank()) {
                    Text(
                        text = currentState.update.notes,
                        style = LivroHubTheme.typography.bodySmall,
                        color = LivroHubTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            is TestUpdateUiState.Downloading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Baixando ${currentState.update.versionName}...",
                        style = LivroHubTheme.typography.bodySmall
                    )
                }
            }

            is TestUpdateUiState.Ready -> {
                Text(
                    text = "${currentState.update.versionName} foi baixada e validada por SHA-256.",
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
            }

            is TestUpdateUiState.Error -> {
                Text(
                    text = currentState.message,
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.error
                )
            }
        }

        installNotice?.let { notice ->
            Text(
                text = notice,
                style = LivroHubTheme.typography.bodySmall,
                color = LivroHubTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val currentState = state) {
            TestUpdateUiState.Checking,
            is TestUpdateUiState.Downloading -> {
                Button(onClick = {}, enabled = false) {
                    Text(if (currentState is TestUpdateUiState.Downloading) "Baixando..." else "Verificando...")
                }
            }

            is TestUpdateUiState.Available -> {
                Button(
                    onClick = {
                        installNotice = null
                        state = TestUpdateUiState.Downloading(currentState.update)
                        scope.launch {
                            manager.downloadUpdate(currentState.update)
                                .onSuccess { apkFile ->
                                    state = TestUpdateUiState.Ready(currentState.update, apkFile)
                                    requestInstall(apkFile)
                                }
                                .onFailure { error ->
                                    state = TestUpdateUiState.Error(
                                        error.message ?: "Não foi possível baixar a atualização."
                                    )
                                }
                        }
                    }
                ) {
                    Text("Baixar e instalar")
                }
            }

            is TestUpdateUiState.Ready -> {
                Button(onClick = { requestInstall(currentState.apkFile) }) {
                    Text("Instalar atualização")
                }
            }

            else -> {
                Button(
                    onClick = {
                        installNotice = null
                        state = TestUpdateUiState.Checking
                        scope.launch {
                            state = when (val result = manager.checkForUpdate()) {
                                is TestUpdateCheckResult.Available ->
                                    TestUpdateUiState.Available(result.update)
                                TestUpdateCheckResult.UpToDate -> TestUpdateUiState.UpToDate
                                is TestUpdateCheckResult.Error ->
                                    TestUpdateUiState.Error(result.message)
                            }
                        }
                    }
                ) {
                    Text("Verificar atualização")
                }
            }
        }

        if (state is TestUpdateUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = manager::openGitHubRelease) {
                Text("Abrir release no GitHub")
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = LivroHubTheme.colors.primary,
        style = LivroHubTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun InlinePreviewBox(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = LivroHubTheme.colors.surfaceVariant.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(LivroHubTheme.radius.medium)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
        content = content
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

@Composable
private fun SettingsPreview(settings: AppSettings) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(LivroHubTheme.radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = LivroHubTheme.colors.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (settings.cardStyle == CardStyle.ELEVATED) LivroHubTheme.shadows.elevationLow else 0.dp
        ),
        border = if (settings.cardStyle == CardStyle.OUTLINED) androidx.compose.foundation.BorderStroke(1.dp, LivroHubTheme.colors.border) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Visualização em Tempo Real",
                style = LivroHubTheme.typography.titleLarge,
                color = LivroHubTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Observe como as configurações afetam o design instantaneamente. A tipografia, as cores, o espaçamento e os estilos dos componentes mudam conforme suas preferências.",
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        if (settings.buttonStyle == ButtonStyle.FILLED) LivroHubTheme.radius.full else LivroHubTheme.radius.medium
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivroHubTheme.colors.primary,
                        contentColor = LivroHubTheme.colors.onPrimary
                    )
                ) {
                    Text("Primário", style = LivroHubTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        if (settings.buttonStyle == ButtonStyle.FILLED) LivroHubTheme.radius.full else LivroHubTheme.radius.medium
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LivroHubTheme.colors.secondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivroHubTheme.colors.secondary)
                ) {
                    Text("Secundário", style = LivroHubTheme.typography.labelLarge)
                }
            }
        }
    }
}
