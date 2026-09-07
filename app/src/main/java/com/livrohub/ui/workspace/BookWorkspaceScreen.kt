package com.livrohub.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import com.livrohub.ui.characters.CharacterViewModel
import com.livrohub.ui.characters.CharactersScreen
import com.livrohub.ui.locations.LocationViewModel
import com.livrohub.ui.locations.LocationsScreen
import com.livrohub.ui.chapters.ChaptersScreen
import com.livrohub.ui.chapters.ChaptersViewModel
import com.livrohub.ui.theme.LivroHubTheme
import com.livrohub.domain.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookWorkspaceScreen(
    chaptersViewModel: ChaptersViewModel,
    characterViewModel: CharacterViewModel,
    locationViewModel: LocationViewModel,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onOpenChapter: (Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    var showTutorial by remember(settings.showWorkspaceTutorial) { mutableStateOf(settings.showWorkspaceTutorial) }
    var menuBounds by remember { mutableStateOf(Rect.Zero) }
    var bottomNavBounds by remember { mutableStateOf(Rect.Zero) }
    var fabBounds by remember { mutableStateOf(Rect.Zero) }

    val tutorialSteps = remember(menuBounds, bottomNavBounds, fabBounds) {
        listOf(
            com.livrohub.ui.components.TutorialStep(
                targetRect = menuBounds,
                title = "Configurações de Exibição",
                description = "Clique aqui para habilitar a visualização das abas de Personagens e Locais."
            ),
            com.livrohub.ui.components.TutorialStep(
                targetRect = bottomNavBounds,
                title = "Navegação",
                description = "Navegue entre os Capítulos, Personagens e Locais da sua história."
            ),
            com.livrohub.ui.components.TutorialStep(
                targetRect = fabBounds,
                title = "Novo Item",
                description = "Use este botão para criar novos itens dependendo da aba selecionada."
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Workspace", style = LivroHubTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Voltar para biblioteca"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LivroHubTheme.colors.background,
                        titleContentColor = LivroHubTheme.colors.onBackground,
                        navigationIconContentColor = LivroHubTheme.colors.onBackground
                    ),
                    actions = {
                        Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                            menuBounds = coordinates.boundsInRoot()
                        }) {
                            IconButton(
                                onClick = { menuExpanded = true }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opções")
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exibir Personagens") },
                                trailingIcon = {
                                    Checkbox(
                                        checked = settings.showCharactersTab,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = LivroHubTheme.colors.primary)
                                    )
                                },
                                onClick = {
                                    onUpdateSettings { it.copy(showCharactersTab = !it.showCharactersTab) }
                                    if (settings.showCharactersTab && selectedTab == 1) selectedTab = 0
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exibir Locais") },
                                trailingIcon = {
                                    Checkbox(
                                        checked = settings.showLocationsTab,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = LivroHubTheme.colors.primary)
                                    )
                                },
                                onClick = {
                                    onUpdateSettings { it.copy(showLocationsTab = !it.showLocationsTab) }
                                    if (settings.showLocationsTab && selectedTab == 2) selectedTab = 0
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = LivroHubTheme.colors.surface,
                    contentColor = LivroHubTheme.colors.onSurface,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        bottomNavBounds = coordinates.boundsInRoot()
                    }
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Capítulos") },
                        label = { Text("Capítulos", style = LivroHubTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LivroHubTheme.colors.onPrimary,
                            selectedTextColor = LivroHubTheme.colors.primary,
                            indicatorColor = LivroHubTheme.colors.primary,
                            unselectedIconColor = LivroHubTheme.colors.onSurfaceVariant,
                            unselectedTextColor = LivroHubTheme.colors.onSurfaceVariant
                        )
                    )
                    if (settings.showCharactersTab) {
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.People, contentDescription = "Personagens") },
                            label = { Text("Personagens", style = LivroHubTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LivroHubTheme.colors.onPrimary,
                                selectedTextColor = LivroHubTheme.colors.primary,
                                indicatorColor = LivroHubTheme.colors.primary,
                                unselectedIconColor = LivroHubTheme.colors.onSurfaceVariant,
                                unselectedTextColor = LivroHubTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                    if (settings.showLocationsTab) {
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Place, contentDescription = "Locais") },
                            label = { Text("Locais", style = LivroHubTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LivroHubTheme.colors.onPrimary,
                                selectedTextColor = LivroHubTheme.colors.primary,
                                indicatorColor = LivroHubTheme.colors.primary,
                                unselectedIconColor = LivroHubTheme.colors.onSurfaceVariant,
                                unselectedTextColor = LivroHubTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            containerColor = LivroHubTheme.colors.background
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> {
                        ChaptersScreen(
                            viewModel = chaptersViewModel,
                            onOpenChapter = onOpenChapter,
                            onFabBoundsPositioned = { fabBounds = it }
                        )
                    }
                    1 -> {
                        CharactersScreen(
                            viewModel = characterViewModel
                        )
                    }
                    2 -> {
                        LocationsScreen(
                            viewModel = locationViewModel
                        )
                    }
                }
            }
        }
        
        if (showTutorial && menuBounds != Rect.Zero && bottomNavBounds != Rect.Zero && (fabBounds != Rect.Zero || selectedTab != 0)) {
            com.livrohub.ui.components.TutorialOverlay(
                steps = tutorialSteps,
                isVisible = showTutorial,
                onFinish = { dontShowAgain ->
                    showTutorial = false
                    if (dontShowAgain) {
                        onUpdateSettings { it.copy(showWorkspaceTutorial = false) }
                    }
                }
            )
        }
    }
}
