package com.livrohub.domain.repository

import com.livrohub.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para leitura e atualização das preferências do app.
 */
interface SettingsRepository {
    /** Fluxo reativo que emite o estado atual das configurações do aplicativo. */
    val settings: Flow<AppSettings>

    /** Atualiza o tema do aplicativo. */
    suspend fun updateTheme(theme: com.livrohub.domain.model.AppTheme)
    /** Atualiza o tamanho base da fonte. */
    suspend fun updateFontSize(fontSizeSp: Int)
    /** Ativa ou desativa a exibição do número de linhas no editor. */
    suspend fun updateShowLineNumbers(show: Boolean)
    
    /** 
     * Método unificado para atualizar múltiplas configurações de forma atômica.
     * @param transform Função que recebe as configurações atuais e retorna as novas configurações.
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
