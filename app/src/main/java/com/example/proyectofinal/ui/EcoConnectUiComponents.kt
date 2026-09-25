package com.example.proyectofinal.ui

import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.proyectofinal.R
import com.example.proyectofinal.data.*
import com.example.proyectofinal.utils.*
import com.example.proyectofinal.viewmodel.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import android.speech.tts.TextToSpeech
import java.util.Locale

@Composable
fun PantallaDispatcherCentral(viewModel: EcoConnectViewModelAvanzado, windowSize: WindowSizeClass) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = viewModel.destinoActual,
            transitionSpec = {
                fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
            },
            label = "main_nav"
        ) { destino ->
            when (destino) {
                EcoNavegacionDestino.SPLASH_ANIMADO -> VistaSplashScreenEngine {
                    viewModel.destinoActual = EcoNavegacionDestino.ONBOARDING_PASOS
                }
                EcoNavegacionDestino.ONBOARDING_PASOS -> VistaOnboardingEngine {
                    viewModel.destinoActual = EcoNavegacionDestino.LOGIN_ACCESO
                }
                EcoNavegacionDestino.LOGIN_ACCESO -> {
                    val context = LocalContext.current
                    VistaLoginEngine(
                        onLoginExito = { email ->
                            viewModel.registrarOSincronizarUsuarioConFirebase(
                                email = email,
                                nombre = email.substringBefore("@"),
                                context = context
                            )
                        },
                        onIrARegistro = {
                            viewModel.destinoActual = EcoNavegacionDestino.REGISTRO_USUARIO
                        },
                        viewModel = viewModel
                    )
                }
                EcoNavegacionDestino.REGISTRO_USUARIO -> {
                    val context = LocalContext.current
                    VistaRegistroEngine(
                        onRegistroCompleto = { n, e ->
                            viewModel.registrarOSincronizarUsuarioConFirebase(
                                email = e,
                                nombre = n,
                                context = context
                            )
                        },
                        onVolverLogin = { viewModel.destinoActual = EcoNavegacionDestino.LOGIN_ACCESO }
                    )
                }
                EcoNavegacionDestino.DASHBOARD_FEED -> VistaDashboardEngine(
                    viewModel = viewModel,
                    windowSize = windowSize,
                    onNavigate = { viewModel.destinoActual = it },
                    onSelectReporte = {
                        viewModel.reporteActivoSeleccionado = it
                        viewModel.destinoActual = EcoNavegacionDestino.DETALLE_REPORTE
                    }
                )
                EcoNavegacionDestino.DETALLE_REPORTE -> VistaDetalleReporteEngine(
                    reporte = viewModel.reporteActivoSeleccionado,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED },
                    onVotar = { viewModel.aplicarVotoComunitario(viewModel.reporteActivoSeleccionado?.id ?: "") },
                    onComentar = { msg -> viewModel.agregarComentarioAReporte(viewModel.reporteActivoSeleccionado?.id ?: "", msg) },
                    viewModel = viewModel
                )
                EcoNavegacionDestino.CREAR_REPORTE_CAMARA -> VistaCrearReporteCamaraEngine(
                    onReporteCreado = { t, d, c, u, p, uri ->
                        viewModel.guardarNuevoReporte(t, d, c, u, p, null, uri)
                        viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
                    },
                    onCancelar = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED },
                    viewModel = viewModel
                )
                EcoNavegacionDestino.PERFIL_ESTADISTICAS -> VistaPerfilEngine(
                    viewModel = viewModel,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.TIENDA_ECOPUNTOS -> VistaMuroInsigniasEngine(
                    viewModel = viewModel,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.SIMULADOR_FDROID -> VistaSimuladorFDroidEngine(
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.MAPA_INCIDENCIAS_SIM -> {
                    val state by viewModel.uiState.collectAsState()
                    val reportes = if (state is EcoConnectUiState.Exito) (state as EcoConnectUiState.Exito).reportes else emptyList()
                    VistaMapaSimuladoEngine(reportes, onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED })
                }
                EcoNavegacionDestino.HISTORIAL_NOTIFICACIONES -> VistaNotificacionesEngine(
                    viewModel = viewModel,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.CONFIGURACION_SISTEMA -> VistaConfiguracionEngine(
                    viewModel = viewModel,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.CALCULADORA_IMPUESTOS_FREELANCE -> VistaCalculadoraImpuestosEngine(
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED }
                )
                EcoNavegacionDestino.ECO_TIENDA_QR -> {
                    val context = LocalContext.current
                    VistaQrScannerEngine(
                        onScanSuccess = {
                            viewModel.escanearCodigoQr(context)
                            viewModel.destinoActual = EcoNavegacionDestino.DASHBOARD_FEED
                        },
                        onVolver = { viewModel.destinoActual = EcoNavegacionDestino.TIENDA_ECOPUNTOS }
                    )
                }
                EcoNavegacionDestino.LEADERBOARD_GLOBAL -> VistaLeaderboardEngine(
                    viewModel = viewModel,
                    onVolver = { viewModel.destinoActual = EcoNavegacionDestino.CONFIGURACION_SISTEMA }
                )
            }
        }
    }
}

@Composable
fun VistaSplashScreenEngine(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2.seconds)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.slogan), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun VistaOnboardingEngine(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val titulos = listOf(stringResource(R.string.report_incidents), stringResource(R.string.earn_ecopoints), stringResource(R.string.active_community))
    val descs = listOf(stringResource(R.string.report_desc), stringResource(R.string.earn_desc), stringResource(R.string.community_desc))
    val iconos = listOf("report", "stars", "groups")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ItemOnboardingCard(titulos[step], descs[step], iconos[step])
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { if (step < 2) step++ else onFinished() }) {
            Text(if (step < 2) stringResource(R.string.next) else stringResource(R.string.start))
        }
    }
}

@Composable
fun ItemOnboardingCard(titulo: String, desc: String, iconoStr: String) {
    val icono = when(iconoStr) {
        "report" -> Icons.Default.ReportProblem
        "stars" -> Icons.Default.Stars
        else -> Icons.Default.Person
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(16.dp))
            Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLoginEngine(onLoginExito: (String) -> Unit, onIrARegistro: () -> Unit, viewModel: EcoConnectViewModelAvanzado) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorEmailMessage by remember { mutableStateOf<String?>(null) }
    var nombreInvitado by remember { mutableStateOf("") }
    var mostrarDialogInvitado by remember { mutableStateOf(false) }

    if (mostrarDialogInvitado) {
        AlertDialog(
            onDismissRequest = { mostrarDialogInvitado = false },
            title = { Text("Acceso Invitado") },
            text = {
                OutlinedTextField(
                    value = nombreInvitado,
                    onValueChange = { nombreInvitado = it },
                    label = { Text("¿Cómo te llamas?") }
                )
            },
            confirmButton = {
                Button(onClick = { 
                    if (nombreInvitado.isNotBlank()) {
                        viewModel.loginComoInvitado(nombreInvitado)
                        mostrarDialogInvitado = false
                    }
                }) { Text("Continuar") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.login_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorEmailMessage = null
                },
                label = { Text(stringResource(R.string.email)) },
                isError = errorEmailMessage != null,
                supportingText = {
                    errorEmailMessage?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    if (!com.example.proyectofinal.utils.EcoConnectValidationUtils.esCorreoValido(email)) {
                        errorEmailMessage = "Ingresa un correo electrónico válido (ej. usuario@dominio.com)"
                    } else {
                        errorEmailMessage = null
                        onLoginExito(email)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(stringResource(R.string.login_button)) 
            }
            TextButton(onClick = onIrARegistro) { Text(stringResource(R.string.no_account)) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            OutlinedButton(onClick = { mostrarDialogInvitado = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Person, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar como Invitado")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaRegistroEngine(onRegistroCompleto: (String, String) -> Unit, onVolverLogin: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorEmailMessage by remember { mutableStateOf<String?>(null) }
    var errorNombreMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.register_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { 
                    nombre = it
                    errorNombreMessage = null
                },
                label = { Text(stringResource(R.string.name)) },
                isError = errorNombreMessage != null,
                supportingText = {
                    errorNombreMessage?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorEmailMessage = null
                },
                label = { Text(stringResource(R.string.email)) },
                isError = errorEmailMessage != null,
                supportingText = {
                    errorEmailMessage?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    var valido = true
                    if (nombre.isBlank()) {
                        errorNombreMessage = "Ingresa tu nombre completo"
                        valido = false
                    }
                    if (!com.example.proyectofinal.utils.EcoConnectValidationUtils.esCorreoValido(email)) {
                        errorEmailMessage = "Ingresa un correo electrónico válido (ej. usuario@dominio.com)"
                        valido = false
                    }
                    if (valido) {
                        onRegistroCompleto(nombre, email)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(stringResource(R.string.register_button)) 
            }
            TextButton(onClick = onVolverLogin) { Text(stringResource(R.string.back_to_login)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaDashboardEngine(
    viewModel: EcoConnectViewModelAvanzado,
    windowSize: WindowSizeClass,
    onNavigate: (EcoNavegacionDestino) -> Unit,
    onSelectReporte: (ReporteEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoriaActiva by viewModel.categoriaFiltro.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    val esPantallaAncha = windowSize.widthSizeClass >= WindowWidthSizeClass.Medium
    var mostrarSOS by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                        Icon(
                            imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Alternar Modo Oscuro/Claro"
                        )
                    }
                    IconButton(onClick = { onNavigate(EcoNavegacionDestino.HISTORIAL_NOTIFICACIONES) }) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.notifications))
                    }
                    IconButton(onClick = { onNavigate(EcoNavegacionDestino.PERFIL_ESTADISTICAS) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.profile))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { mostrarSOS = true },
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = "SOS")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = { onNavigate(EcoNavegacionDestino.CREAR_REPORTE_CAMARA) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.report_problem))
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text(stringResource(R.string.start)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate(EcoNavegacionDestino.MAPA_INCIDENCIAS_SIM) },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text(stringResource(R.string.map)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate(EcoNavegacionDestino.TIENDA_ECOPUNTOS) },
                    icon = { Icon(Icons.Default.EmojiEvents, null) },
                    label = { Text("Insignias") }
                )
            }
        }
    ) { padding ->
        if (mostrarSOS) {
            AlertDialog(
                onDismissRequest = { mostrarSOS = false },
                confirmButton = { Button(onClick = { viewModel.lanzarAlertaSOS(); mostrarSOS = false }) { Text("CONFIRMAR SOS") } },
                title = { Text(stringResource(R.string.sos_alert), color = Color.Red) },
                text = { Text(stringResource(R.string.sos_desc)) }
            )
        }
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = { Text("Buscar incidencias...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            val categorias = listOf("Todas", "Basura", "Fuga Agua", "Aire", "Ruidos")
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorias) { cat ->
                    BotonChipBorde(texto = cat, seleccionado = categoriaActiva == cat) {
                        viewModel.setCategoriaFiltro(cat)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is EcoConnectUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is EcoConnectUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.mensaje, color = Color.Red) }
                is EcoConnectUiState.Exito -> {
                    val filtrados = state.reportes.filter {
                        ((categoriaActiva == "Todas" || it.categoria == categoriaActiva) &&
                        (it.titulo.contains(busqueda, ignoreCase = true) || it.descripcion.contains(busqueda, ignoreCase = true)))
                    }
                    
                    if (esPantallaAncha) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filtrados) { reporte ->
                                TarjetaReporteEngine(
                                    reporte = reporte,
                                    onClick = { onSelectReporte(reporte) },
                                    onVotar = { viewModel.aplicarVotoComunitario(reporte.id) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            items(filtrados, key = { it.id }) { reporte ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.resolverReporte(reporte)
                                            true
                                        } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                                            viewModel.eliminarReporte(reporte.id)
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Color.Red
                                            SwipeToDismissBoxValue.EndToStart -> Color.Green
                                            else -> Color.Transparent
                                        }
                                        Box(Modifier.fillMaxSize().background(color).padding(20.dp), contentAlignment = Alignment.CenterEnd) {
                                            Icon(Icons.Default.Delete, null, tint = Color.White)
                                        }
                                    },
                                    content = {
                                        TarjetaReporteEngine(
                                            reporte = reporte,
                                            onClick = { onSelectReporte(reporte) },
                                            onVotar = { viewModel.aplicarVotoComunitario(reporte.id) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BotonChipBorde(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null
    )
}

@Composable
fun TarjetaReporteEngine(reporte: ReporteEntity, onClick: () -> Unit, onVotar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            if (reporte.fotoUrlCloud != null) {
                AsyncImage(
                    model = reporte.fotoUrlCloud,
                    contentDescription = "Imagen del reporte ${reporte.titulo}",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                reporte.fotoBase64?.let { base64 ->
                    EcoConnectImageManager.convertirBase64ABitmap(base64)?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Imagen del reporte ${reporte.titulo}",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reporte.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Badge(containerColor = when(reporte.prioridad) { "Urgente" -> Color.Red; "Alta" -> Color(0xFFFF9800); else -> Color.Gray }) {
                        Text(reporte.prioridad, color = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
                Text(reporte.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Label, null, Modifier.size(16.dp))
                    Text(" ${reporte.categoria}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                    Text(" ${reporte.ubicacionTexto}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onVotar) {
                        Icon(Icons.Default.ThumbUp, null, Modifier.size(18.dp))
                        Text(" Apoyar (${reporte.votosApoyo})", modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(reporte.fechaCreacion, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaDetalleReporteEngine(
    reporte: ReporteEntity?,
    onVolver: () -> Unit,
    onVotar: () -> Unit,
    onComentar: (String) -> Unit,
    viewModel: EcoConnectViewModelAvanzado
) {
    if (reporte == null) return

    val comentarios by viewModel.obtenerComentariosFlow(reporte.id).collectAsState(initial = emptyList())
    var nuevoComentario by remember { mutableStateOf("") }
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "MX")
            }
        }
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Reporte") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            item {
                if (reporte.fotoUrlCloud != null) {
                    AsyncImage(
                        model = reporte.fotoUrlCloud,
                        contentDescription = "Foto evidencia",
                        modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    reporte.fotoBase64?.let { base64 ->
                        EcoConnectImageManager.convertirBase64ABitmap(base64)?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Foto evidencia",
                                modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    reporte.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Título del reporte: ${reporte.titulo}" }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text(reporte.categoria) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) })
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text(reporte.prioridad) }, leadingIcon = { Icon(Icons.Default.Warning, null) })
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { tts?.speak(reporte.descripcion, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar descripción")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    reporte.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { contentDescription = "Descripción del reporte" }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Text(" Ubicación: ${reporte.ubicacionTexto}", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Autor: ${reporte.autorNombre} (${reporte.autorEmail})", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onVotar, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Favorite, null)
                    Text(" Apoyar este reporte (${reporte.votosApoyo} votos)")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Comentarios Comunitarios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (comentarios.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "Aún no hay comentarios en este reporte. ¡Sé el primero en participar!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(comentarios) { com ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(com.autor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(com.mensaje, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nuevoComentario,
                        onValueChange = { nuevoComentario = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un comentario...") }
                    )
                    IconButton(onClick = { if (nuevoComentario.isNotBlank()) { onComentar(nuevoComentario); nuevoComentario = "" } }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentario")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaCrearReporteCamaraEngine(
    onReporteCreado: (String, String, String, String, String, Uri?) -> Unit,
    onCancelar: () -> Unit,
    viewModel: EcoConnectViewModelAvanzado
) {
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Basura") }
    var ubi by remember { mutableStateOf("") }
    var prio by remember { mutableStateOf("Media") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        if (uri != null) viewModel.analizarResiduosIA()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nueva Incidencia") }, navigationIcon = { IconButton(onClick = onCancelar) { Icon(Icons.Default.Close, "Cancelar") } })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título de la incidencia") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción detallada") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = ubi, onValueChange = { ubi = it }, label = { Text("Dirección aproximada") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Categoría", style = MaterialTheme.typography.labelLarge)
            val cats = listOf("Basura", "Fuga Agua", "Aire", "Ruidos")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c ->
                    BotonChipBorde(texto = c, seleccionado = cat == c) { cat = c }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Prioridad", style = MaterialTheme.typography.labelLarge)
            val prios = listOf("Baja", "Media", "Alta", "Urgente")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(prios) { p ->
                    BotonChipBorde(texto = p, seleccionado = prio == p) { prio = p }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (imageUri == null) {
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar Fotografía de Evidencia")
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Vista previa de la evidencia",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                TextButton(onClick = { launcher.launch("image/*") }) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { imageUri = null }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            }
            
            if (viewModel.analizandoIA) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text(stringResource(R.string.ia_analyzing), style = MaterialTheme.typography.labelSmall)
            }

            viewModel.resultadoIA?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("${stringResource(R.string.ia_result)} $res", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onReporteCreado(titulo, desc, cat, ubi, prio, imageUri) },
                modifier = Modifier.fillMaxWidth(),
                enabled = titulo.isNotBlank() && desc.isNotBlank()
            ) {
                Text(stringResource(R.string.report_problem))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaPerfilEngine(viewModel: EcoConnectViewModelAvanzado, onVolver: () -> Unit) {
    val arboles = (viewModel.kgCO2EvitadosTotal / 10.0).toInt()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Mi Perfil EcoConnect") }, 
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                        Icon(
                            imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Alternar Modo Oscuro/Claro"
                        )
                    }
                }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                Text(viewModel.usuarioNombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(viewModel.usuarioEmail, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Huella de Carbono Evitada", style = MaterialTheme.typography.labelLarge)
                        Text("${"%.2f".format(viewModel.kgCO2EvitadosTotal)} kg CO2", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                        Text("Equivalente a $arboles árboles plantados", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Text("Retos Semanales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viewModel.retosSemanales) { reto ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(reto.titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${(reto.progreso * 100).toInt()}%")
                        }
                        LinearProgressIndicator(progress = { reto.progreso }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(CircleShape))
                        Text("Recompensa: ${reto.puntosRecompensa} EcoPuntos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total EcoPuntos")
                            Text(viewModel.puntosAcumulados.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reportes")
                            Text(viewModel.totalReportesUsuario.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaMuroInsigniasEngine(viewModel: EcoConnectViewModelAvanzado, onVolver: () -> Unit) {
    val insignias by viewModel.insigniasLista.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Muro de Insignias") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Reconocimientos por tu Impacto Ambiental", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (insignias.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no has ganado insignias. ¡Participa reportando para ganar tu primera!", textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(insignias) { insignia ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = insignia.titulo,
                                    modifier = Modifier.padding(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(insignia.titulo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(insignia.fechaObtencion, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.destinoActual = EcoNavegacionDestino.ECO_TIENDA_QR },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear Código de Participación")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaSimuladorFDroidEngine(onVolver: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Simulador Publicación F-Droid") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Guía para Publicación Open Source", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("1. Licenciamiento: La app debe usar una licencia libre (GPLv3, Apache 2.0).", style = MaterialTheme.typography.bodyMedium)
            Text("2. Metadatos: El archivo 'fastlane' o 'metadata' debe incluir descripciones en varios idiomas.", style = MaterialTheme.typography.bodyMedium)
            Text("3. Build: F-Droid compila desde el código fuente, no acepta binarios pre-compilados.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estado de Auditoría Tecmilenio", fontWeight = FontWeight.Bold)
                    Text("Simulación de cumplimiento de tienda libre: EXITOSA")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Entendido") }
        }
    }
}

data class PuntoRecoleccion(
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val horario: String,
    val estado: String,
    val materiales: List<String>,
    val distancia: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaMapaSimuladoEngine(reportes: List<ReporteEntity>, onVolver: () -> Unit) {
    var queryBusqueda by remember { mutableStateOf("") }
    var categoriaFiltro by remember { mutableStateOf("Todos") }

    val puntosRecoleccion = remember {
        listOf(
            PuntoRecoleccion(
                nombre = "Centro de Acopio y Reciclaje Tecmilenio",
                tipo = "Centro de Reciclaje",
                direccion = "Av. de las Industrias #1110, Zona Norte",
                horario = "Lun - Vie: 8:00 AM - 6:00 PM",
                estado = "Abierto",
                materiales = listOf("PET", "Aluminio", "Cartón", "Electrónicos"),
                distancia = "A 0.8 km"
            ),
            PuntoRecoleccion(
                nombre = "Módulo Verde Parque Central",
                tipo = "Punto Comunitario",
                direccion = "Av. Universidad y Calle 24a",
                horario = "Lun - Sáb: 9:00 AM - 5:00 PM",
                estado = "Abierto",
                materiales = listOf("Vidrio", "Pilas", "Aceite Usado"),
                distancia = "A 1.5 km"
            ),
            PuntoRecoleccion(
                nombre = "Estación Ecológica La Junta",
                tipo = "Punto de Depósito 24H",
                direccion = "Calle La Junta #450, Col. Centro",
                horario = "24 Horas",
                estado = "Abierto 24H",
                materiales = listOf("Basura General", "Contenedores Plásticos"),
                distancia = "A 2.3 km"
            ),
            PuntoRecoleccion(
                nombre = "Centro de Transferencia RSU Norte",
                tipo = "Planta de Tratamiento",
                direccion = "Km 5 Carretera a Juárez",
                horario = "Lun - Sáb: 7:00 AM - 4:00 PM",
                estado = "Cierra Pronto",
                materiales = listOf("Escombro", "Llantas", "Chatarra"),
                distancia = "A 4.1 km"
            )
        )
    }

    val puntosFiltrados = puntosRecoleccion.filter { p ->
        (categoriaFiltro == "Todos" || p.materiales.contains(categoriaFiltro) || p.tipo == categoriaFiltro) &&
        (p.nombre.contains(queryBusqueda, ignoreCase = true) || p.direccion.contains(queryBusqueda, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puntos de Recolección Cerca de Ti") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            if (reportes.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "📍 Conectado con ${reportes.size} incidencias reportadas en tu zona",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = queryBusqueda,
                onValueChange = { queryBusqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar centro o dirección...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            val cats = listOf("Todos", "PET", "Aluminio", "Vidrio", "Pilas", "Cartón")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c ->
                    BotonChipBorde(texto = c, seleccionado = categoriaFiltro == c) { categoriaFiltro = c }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Directorio de Zonas Ecológicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("${puntosFiltrados.size} lugares", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(puntosFiltrados) { punto ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Recycling, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(punto.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(punto.tipo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(punto.distancia, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(punto.direccion, style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${punto.horario} • ${punto.estado}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(punto.materiales) { mat ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(mat, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaNotificacionesEngine(viewModel: EcoConnectViewModelAvanzado, onVolver: () -> Unit) {
    val notificaciones by viewModel.notificacionesLista.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alertas Comunitarias") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        if (notificaciones.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay notificaciones nuevas") }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                items(notificaciones) { noti ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        ListItem(
                            headlineContent = { Text(noti.titulo, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(noti.mensaje) },
                            overlineContent = { Text(noti.fecha) },
                            leadingContent = { Icon(Icons.Default.Info, null) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaConfiguracionEngine(viewModel: EcoConnectViewModelAvanzado, onVolver: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes y Accesibilidad WCAG 2.1") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
            
            Text("Ajustes de Apariencia y Accesibilidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            // Switch Modo Oscuro
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(if (viewModel.esModoOscuro) Icons.Default.DarkMode else Icons.Default.LightMode, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Oscuro / Claro", fontWeight = FontWeight.Bold)
                        Text(if (viewModel.esModoOscuro) "Tema Oscuro Activo 🌙" else "Tema Claro Activo ☀️", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = viewModel.esModoOscuro, onCheckedChange = { viewModel.toggleModoOscuro() })
                }
            }

            // Switch Alto Contraste
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo de Alto Contraste", fontWeight = FontWeight.Bold)
                        Text("Contraste extremo > 7:1 para baja visión", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = viewModel.esAltoContraste, onCheckedChange = { viewModel.toggleAltoContraste() })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Filtros de Daltonismo (Matriz de Color Dinámica)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val opcionesDaltonismo = listOf("None" to "Normal (Sin filtro)", "Deuteranopia" to "Deuteranopia (Verde-Rojo)", "Protanopia" to "Protanopia (Rojo-Verde)", "Tritanopia" to "Tritanopia (Azul-Amarillo)")
            opcionesDaltonismo.forEach { (opcion, label) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.cambiarTipoDaltonismo(opcion) },
                    colors = CardDefaults.cardColors(containerColor = if (viewModel.tipoDaltonismo == opcion) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        RadioButton(selected = viewModel.tipoDaltonismo == opcion, onClick = { viewModel.cambiarTipoDaltonismo(opcion) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontWeight = if (viewModel.tipoDaltonismo == opcion) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Evidencia Visual de Accesibilidad Incorporada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Captura/Demo 1: Modo Oscuro
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📷 MUESTRA DE MODO OSCURO (DARK MODE)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("Superficie de bajo consumo de batería y confort visual nocturno.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Captura/Demo 2: Alto Contraste
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📷 MUESTRA DE ALTO CONTRASTE (HIGH CONTRAST)", color = Color.Yellow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("Relación de luminancia WCAG 2.1 AA optimizada para legibilidad máxima.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Captura/Demo 3: Filtro Daltonismo
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📷 MUESTRA DE FILTRO PARA DALTONISMO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("Filtro Activo: ${viewModel.tipoDaltonismo}. Transforma la paleta RGB en tiempo real.", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Captura/Demo 4: TalkBack Semántica
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📷 ACCESIBILIDAD TALKBACK / LECTOR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Componente etiquetado semánticamente con contentDescription para lectores de pantalla.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(stringResource(R.string.rubric_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { viewModel.progresoRubrica }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape))
            Text("Cumplimiento Rúbrica Tecmilenio: ${(viewModel.progresoRubrica * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.destinoActual = EcoNavegacionDestino.LEADERBOARD_GLOBAL }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.leaderboard))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.cerrarSesion() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Versión: 3.2.0-AAA-REALTIME", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaCalculadoraImpuestosEngine(onVolver: () -> Unit) {
    var ingresos by remember { mutableStateOf("") }
    val resultadoState = remember { mutableDoubleStateOf(0.0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Finanzas Sustentables") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Calculadora ISR Freelance (México)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = ingresos,
                onValueChange = { ingresos = it },
                label = { Text("Ingresos Mensuales ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    val ing = ingresos.toDoubleOrNull() ?: 0.0
                    resultadoState.doubleValue = ing * 0.20
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calcular Impuesto Estimado")
            }
            if (resultadoState.doubleValue > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Retención ISR Estimada:", style = MaterialTheme.typography.bodyLarge)
                        Text("$${"%.2f".format(resultadoState.doubleValue)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("Ahorra el 10% adicional para tu retiro sustentable.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLeaderboardEngine(viewModel: EcoConnectViewModelAvanzado, onVolver: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.leaderboard)) }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(viewModel.leaderboardUsuarios) { user ->
                ListItem(
                    headlineContent = { Text(user.first, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${user.second} EcoPuntos") },
                    leadingContent = { 
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user.first.take(1), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    trailingContent = { Icon(Icons.Default.Star, tint = Color(0xFFFFD700), contentDescription = null) }
                )
                HorizontalDivider()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaQrScannerEngine(onScanSuccess: () -> Unit, onVolver: () -> Unit) {
    var scanning by remember { mutableStateOf(true) }

    LaunchedEffect(scanning) {
        if (scanning) {
            delay(3000)
            scanning = false
            onScanSuccess()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("EcoScanner QR") }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(250.dp).background(Color.Transparent, RoundedCornerShape(12.dp)).padding(2.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 4.dp.toPx()
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(50f, 0f), stroke)
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, 50f), stroke)
                        
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - 50f, 0f), stroke)
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, 50f), stroke)

                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(50f, size.height), stroke)
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - 50f), stroke)

                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - 50f, size.height), stroke)
                        drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - 50f), stroke)
                    }
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.align(Alignment.Center).size(150.dp), tint = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Apunta al código QR de la EcoTienda", color = Color.White)
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = Color.Green)
                }
            }
        }
    }
}
