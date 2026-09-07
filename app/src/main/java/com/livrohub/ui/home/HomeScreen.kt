package com.livrohub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.livrohub.R
import com.livrohub.ui.components.LivroHubButton
import com.livrohub.ui.components.LivroHubHeader
import com.livrohub.ui.theme.LivroHubTheme

/**
 * Tela inicial do aplicativo LivroHub.
 *
 * Exibe as opções principais de navegação, como acesso à biblioteca de livros
 * e às configurações globais do aplicativo.
 *
 * @param onNavigateToLibrary Callback acionado para navegar até a tela da biblioteca.
 * @param onNavigateToSettings Callback acionado para navegar até a tela de configurações.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val appName = stringResource(R.string.app_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName, style = LivroHubTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground
                )
            )
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LivroHubTheme.spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LivroHubHeader(
                title = "Bem-vindo ao $appName",
                subtitle = "Seu ambiente completo para escrita e desenvolvimento de histórias.",
                modifier = Modifier.padding(bottom = LivroHubTheme.spacing.xxl)
            )

            LivroHubButton(
                text = "Acessar Biblioteca",
                onClick = onNavigateToLibrary,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Default.Book, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(LivroHubTheme.spacing.m))

            LivroHubButton(
                text = "Configurações",
                onClick = onNavigateToSettings,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
        }
    }
}
