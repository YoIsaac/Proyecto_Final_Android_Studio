package com.example.proyectofinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.proyectofinal.data.EcoConnectCloudSyncEngine
import com.example.proyectofinal.data.EcoConnectRoomDatabase
import com.example.proyectofinal.data.EcoConnectRepository
import com.example.proyectofinal.data.SettingsRepository
import com.example.proyectofinal.ui.PantallaDispatcherCentral
import com.example.proyectofinal.ui.theme.ProyectoFinalTheme
import com.example.proyectofinal.viewmodel.EcoConnectViewModelAvanzado

class EcoConnectViewModelFactory(
    private val repository: EcoConnectRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EcoConnectViewModelAvanzado::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EcoConnectViewModelAvanzado(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {

    private val database by lazy { EcoConnectRoomDatabase.obtenerBaseDatos(applicationContext) }
    private val cloudSyncEngine by lazy { EcoConnectCloudSyncEngine(database, applicationContext) }
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val repository by lazy { EcoConnectRepository(database, cloudSyncEngine, applicationContext) }

    private val viewModel: EcoConnectViewModelAvanzado by viewModels {
        EcoConnectViewModelFactory(repository, settingsRepository)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar Splash Screen antes de super.onCreate
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                // Se mantiene el splash mientras el ViewModel está en el destino inicial (opcional)
                false
            }
        }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            ProyectoFinalTheme(
                darkTheme = viewModel.esModoOscuro,
                highContrast = viewModel.esAltoContraste,
                tipoDaltonismo = viewModel.tipoDaltonismo
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaDispatcherCentral(viewModel = viewModel, windowSize = windowSizeClass)
                }
            }
        }
    }
}
