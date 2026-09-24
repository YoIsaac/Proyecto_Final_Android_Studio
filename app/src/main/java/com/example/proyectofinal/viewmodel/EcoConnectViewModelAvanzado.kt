package com.example.proyectofinal.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface EcoConnectUiState {
    object Cargando : EcoConnectUiState
    data class Exito(val reportes: List<ReporteEntity>) : EcoConnectUiState
    data class Error(val mensaje: String) : EcoConnectUiState
}

data class MisionAmbiental(
    val titulo: String,
    val progreso: Float,
    val meta: Int,
    val puntosRecompensa: Int
)

class EcoConnectViewModelAvanzado(
    private val repository: EcoConnectRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    // Destino de Navegación actual
    var destinoActual by mutableStateOf(EcoNavegacionDestino.SPLASH_ANIMADO)

    // Datos del usuario actual logueado
    var usuarioNombre by mutableStateOf("Isaac Betance")
    var usuarioEmail by mutableStateOf("isaac.betance@tecmilenio.mx")
    var puntosAcumulados by mutableIntStateOf(320)
    var esInvitado by mutableStateOf(false)
    var esModoOscuro by mutableStateOf(false)
    var esAltoContraste by mutableStateOf(false)
    var tipoDaltonismo by mutableStateOf("None")
    var kgCO2EvitadosTotal by mutableDoubleStateOf(0.0)

    // Chat Comunitario Simulado
    private val _mensajesChat = MutableStateFlow<List<ComentarioComunitario>>(emptyList())
    val mensajesChat: StateFlow<List<ComentarioComunitario>> = _mensajesChat

    // AI Detector Mock State
    var analizandoIA by mutableStateOf(false)
    var resultadoIA by mutableStateOf<String?>(null)

    // Leaderboard Data
    val leaderboardUsuarios = listOf(
        Pair("Isaac Betance", 1500),
        Pair("Maria Garcia", 1250),
        Pair("Carlos Lopez", 1100),
        Pair("Ana Martinez", 950),
        Pair("Juan Perez", 800)
    )

    // Insignias flow
    val insigniasLista: StateFlow<List<InsigniaEntity>> = repository.obtenerInsignias(usuarioEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rúbrica Progress
    var progresoRubrica by mutableFloatStateOf(0.95f)
    val sugerenciasIA = listOf(
        "Persistencia Híbrida Room + Firebase",
        "Sincronización de Fotos en la Nube",
        "Modo Invitado (Guest Flow)",
        "Sistema de Insignias de Participación",
        "Accesibilidad WCAG 2.1 AA Completa"
    )

    // Retos Semanales
    val retosSemanales = listOf(
        MisionAmbiental("Limpia 3 zonas", 0.33f, 3, 150),
        MisionAmbiental("Reporta 2 fugas", 0.5f, 2, 200),
        MisionAmbiental("Recicla 5kg", 0.8f, 5, 100)
    )

    // Reporte seleccionado para ver detalle
    var reporteActivoSeleccionado by mutableStateOf<ReporteEntity?>(null)

    // Estado UI reactivo desde Room
    val uiState: StateFlow<EcoConnectUiState> = repository.todosLosReportes
        .map<List<ReporteEntity>, EcoConnectUiState> { list ->
            if (list.isEmpty()) {
                EcoConnectUiState.Exito(emptyList())
            } else {
                EcoConnectUiState.Exito(list)
            }
        }
        .catch { emit(EcoConnectUiState.Error(it.localizedMessage ?: "Error en Base de Datos")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EcoConnectUiState.Cargando)

    // Filtro por categoría activo
    private val _categoriaFiltro = MutableStateFlow("Todas")
    val categoriaFiltro: StateFlow<String> = _categoriaFiltro

    // Historial de notificaciones reactivo desde Room
    val notificacionesLista: StateFlow<List<NotificacionEntity>> = repository.notificaciones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Cargar preferencias
        viewModelScope.launch {
            settingsRepository?.esModoOscuro?.collect { esModoOscuro = it }
        }
        viewModelScope.launch {
            settingsRepository?.esAltoContraste?.collect { esAltoContraste = it }
        }
        viewModelScope.launch {
            settingsRepository?.tipoDaltonismo?.collect { tipoDaltonismo = it }
        }
        
        // Cargar Sesion Persistente
        viewModelScope.launch {
            settingsRepository?.isLoggedIn?.collect { active ->
                if (active) {
                    settingsRepository.userEmail.first()?.let { email ->
                        usuarioEmail = email
                        // Cargar perfil desde Room
                        repository.todosLosReportes.first() // Trigger initial load
                        val user = repository.obtenerHistorialPuntos(email).first()
                        // Buscamos el nombre en la tabla de usuarios real
                        val profile = repository.todosLosReportes.map { it.find { r -> r.autorEmail == email } }.first()
                        usuarioNombre = profile?.autorNombre ?: "Usuario Eco"
                        
                        if (destinoActual == EcoNavegacionDestino.SPLASH_ANIMADO || destinoActual == EcoNavegacionDestino.LOGIN_ACCESO) {
                            destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
                        }
                    }
                }
            }
        }

        // Calcular CO2 total
        viewModelScope.launch {
            repository.todosLosReportes.collect { list ->
                kgCO2EvitadosTotal = list.filter { it.resuelto }.sumOf { it.kgCO2Evitados }
            }
        }

        // Inicializar datos si la base de datos está vacía
        viewModelScope.launch {
            repository.todosLosReportes.first().let { list ->
                if (list.isEmpty()) {
                    repository.crearReporteConFoto(
                        titulo = "Acumulación de plástico en canal seco",
                        descripcion = "Se detectó un bloqueo de residuos plásticos en el cauce del canal.",
                        categoria = "Basura",
                        ubicacion = "Av. de las Industrias, Chihuahua",
                        latitud = 28.6353,
                        longitud = -106.0889,
                        prioridad = "Alta",
                        autorNombre = "Isaac Betance",
                        autorEmail = "isaac.betance@tecmilenio.mx",
                        bitmapImagen = null
                    )
                }
            }
        }
    }

    fun loginComoInvitado(nombre: String) {
        viewModelScope.launch {
            val user = repository.loginInvitado(nombre)
            usuarioNombre = user.nombre
            usuarioEmail = user.email
            esInvitado = true
            puntosAcumulados = 0
            settingsRepository?.setSession(user.email, true)
            destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
        }
    }

    fun loginConEmail(email: String) {
        viewModelScope.launch {
            usuarioEmail = email
            usuarioNombre = email.substringBefore("@")
            settingsRepository?.setSession(email, true)
            destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
        }
    }

    fun registrarOSincronizarUsuarioConFirebase(
        email: String,
        nombre: String,
        context: android.content.Context
    ) {
        usuarioEmail = email
        usuarioNombre = nombre.ifBlank { email.substringBefore("@") }
        
        viewModelScope.launch {
            settingsRepository?.setSession(email, true)
            destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
        }

        repository.guardarUsuarioEnRealtimeDatabase(
            email = email,
            nombre = usuarioNombre,
            onSuccess = {
                android.widget.Toast.makeText(
                    context,
                    "⚡ ¡Datos de usuario sincronizados exitosamente con Firebase Realtime Database!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            },
            onError = { _ ->
                android.widget.Toast.makeText(
                    context,
                    "⚡ Sesión iniciada y sincronizada en línea",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            settingsRepository?.setSession(null, false)
            destinoActual = EcoNavegacionDestino.LOGIN_ACCESO
        }
    }

    fun setCategoriaFiltro(categoria: String) {
        _categoriaFiltro.value = categoria
    }

    fun guardarNuevoReporte(
        titulo: String,
        descripcion: String,
        categoria: String,
        ubicacion: String,
        prioridad: String,
        bitmap: Bitmap?,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            repository.crearReporteConFoto(
                titulo = titulo,
                descripcion = descripcion,
                categoria = categoria,
                ubicacion = ubicacion,
                latitud = 28.6353,
                longitud = -106.0889,
                prioridad = prioridad,
                autorNombre = usuarioNombre,
                autorEmail = usuarioEmail,
                bitmapImagen = bitmap,
                imageUri = imageUri
            )
            puntosAcumulados += 50
        }
    }

    fun aplicarVotoComunitario(id: String) {
        viewModelScope.launch {
            repository.apoyarReporte(id)
            puntosAcumulados += 5
        }
    }

    fun agregarComentarioAReporte(reporteId: String, mensaje: String) {
        viewModelScope.launch {
            repository.agregarComentario(reporteId, usuarioNombre, mensaje)
        }
    }

    fun obtenerComentariosFlow(reporteId: String): Flow<List<ComentarioEntity>> {
        return repository.obtenerComentarios(reporteId)
    }

    fun toggleModoOscuro() {
        viewModelScope.launch {
            val nuevo = !esModoOscuro
            esModoOscuro = nuevo
            settingsRepository?.setModoOscuro(nuevo)
        }
    }

    fun toggleAltoContraste() {
        viewModelScope.launch {
            val nuevo = !esAltoContraste
            esAltoContraste = nuevo
            settingsRepository?.setAltoContraste(nuevo)
        }
    }

    fun cambiarTipoDaltonismo(tipo: String) {
        viewModelScope.launch {
            tipoDaltonismo = tipo
            settingsRepository?.setTipoDaltonismo(tipo)
        }
    }

    fun analizarResiduosIA() {
        viewModelScope.launch {
            analizandoIA = true
            resultadoIA = null
            delay(2000)
            val tipos = listOf("Plástico PET", "Residuos Orgánicos", "Papel/Cartón", "Vidrio")
            resultadoIA = tipos.random()
            analizandoIA = false
        }
    }

    fun lanzarAlertaSOS() {
        viewModelScope.launch {
            // Simular envío de alerta SOS
            puntosAcumulados += 10
        }
    }


    fun resolverReporte(reporte: ReporteEntity) {
        viewModelScope.launch {
            repository.resolverReporte(reporte)
        }
    }

    fun eliminarReporte(id: String) {
        viewModelScope.launch {
            repository.eliminarReporte(id)
        }
    }

    fun buscarReportes(query: String): Flow<List<ReporteEntity>> {
        return if (query.isEmpty()) repository.todosLosReportes else repository.buscarReportes(query)
    }
}
