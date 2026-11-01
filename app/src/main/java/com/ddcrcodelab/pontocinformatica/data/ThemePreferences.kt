package com.ddcrcodelab.pontocinformatica.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ddcrcodelab.pontocinformatica.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Delegate: cria UMA única instância do DataStore para o app inteiro
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persiste a escolha de tema do usuário.
 * Como o DataStore Preferences só aceita primitivos/String,
 * gravamos o .name do enum e convertemos de volta na leitura.
 */
class ThemePreferences(private val context: Context) {

    // Fluxo reativo: toda mudança re-compõe o tema na hora
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val nome = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        // valueOf lança exceção com texto inválido; caímos no SYSTEM
        runCatching { ThemeMode.valueOf(nome) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = mode.name }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
    }
}