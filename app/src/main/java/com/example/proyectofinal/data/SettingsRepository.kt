package com.example.proyectofinal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ecoconnect_settings")

class SettingsRepository(private val context: Context) {
    private val MODO_OSCURO = booleanPreferencesKey("modo_oscuro")
    private val ALTO_CONTRASTE = booleanPreferencesKey("alto_contraste")
    private val TAMANO_TEXTO = floatPreferencesKey("tamano_texto")
    private val TIPO_DALTONISMO = stringPreferencesKey("tipo_daltonismo")
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")

    val esModoOscuro: Flow<Boolean> = context.dataStore.data.map { it[MODO_OSCURO] ?: false }
    val esAltoContraste: Flow<Boolean> = context.dataStore.data.map { it[ALTO_CONTRASTE] ?: false }
    val tipoDaltonismo: Flow<String> = context.dataStore.data.map { it[TIPO_DALTONISMO] ?: "None" }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    suspend fun setModoOscuro(activo: Boolean) {
        context.dataStore.edit { it[MODO_OSCURO] = activo }
    }

    suspend fun setAltoContraste(activo: Boolean) {
        context.dataStore.edit { it[ALTO_CONTRASTE] = activo }
    }

    suspend fun setTipoDaltonismo(tipo: String) {
        context.dataStore.edit { it[TIPO_DALTONISMO] = tipo }
    }

    suspend fun setSession(email: String?, active: Boolean) {
        context.dataStore.edit {
            if (email != null) it[USER_EMAIL] = email
            it[IS_LOGGED_IN] = active
        }
    }
}
