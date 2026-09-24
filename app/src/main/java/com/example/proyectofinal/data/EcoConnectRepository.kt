package com.example.proyectofinal.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.example.proyectofinal.utils.EcoConnectImageManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class EcoConnectCloudSyncEngine(
    private val database: EcoConnectRoomDatabase,
    private val context: Context
) {
    private val _estadoSincronizacion = MutableStateFlow("Sincronización Cloud Lista")
    val estadoSincronizacion: StateFlow<String> = _estadoSincronizacion

    suspend fun subirReporteAFirebase(reporte: ReporteEntity, imageUri: Uri? = null) = withContext(Dispatchers.IO) {
        try {
            _estadoSincronizacion.value = "Conectando con Firebase Cloud..."
            
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            
            var cloudUrl: String? = null
            
            if (imageUri != null) {
                val ref = storage.reference.child("reportes/${reporte.id}.jpg")
                ref.putFile(imageUri).await()
                cloudUrl = ref.downloadUrl.await().toString()
            }
            
            val cloudReporte = hashMapOf(
                "titulo" to reporte.titulo,
                "descripcion" to reporte.descripcion,
                "categoria" to reporte.categoria,
                "ubicacion" to reporte.ubicacionTexto,
                "autor" to reporte.autorNombre,
                "email" to reporte.autorEmail,
                "fecha" to reporte.fechaCreacion,
                "fotoUrl" to cloudUrl,
                "kgCO2" to reporte.kgCO2Evitados
            )
            
            db.collection("reportes").document(reporte.id).set(cloudReporte).await()
            
            database.reporteDao().marcarComoSincronizado(reporte.id)
            _estadoSincronizacion.value = "Sincronizado con Firebase Cloud"
        } catch (e: Exception) {
            _estadoSincronizacion.value = "Modo Demo Online Activo. (Error: ${e.message})"
        }
    }

    fun guardarUsuarioEnRealtimeDatabase(
        email: String,
        nombre: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val realtimeDb = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = realtimeDb.getReference("usuarios")
            val userId = email.replace(".", "_").replace("@", "_at_")

            val usuarioData = hashMapOf(
                "email" to email,
                "nombre" to nombre,
                "fechaRegistro" to SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                "sincronizadoEnLinea" to true,
                "plataforma" to "Android EcoConnect App"
            )

            ref.child(userId).setValue(usuarioData)
                .addOnSuccessListener {
                    _estadoSincronizacion.value = "Usuario guardado en Realtime Database"
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    _estadoSincronizacion.value = "Realtime DB Sync Modo Demo"
                    onError(e.localizedMessage ?: "Error de conexión")
                }
        } catch (e: Exception) {
            _estadoSincronizacion.value = "Realtime DB Modo Demo"
            onError(e.localizedMessage ?: "Error de inicialización")
        }
    }
}



class EcoConnectRepository(
    private val database: EcoConnectRoomDatabase,
    private val cloudSyncEngine: EcoConnectCloudSyncEngine,
    private val context: Context
) {
    fun guardarUsuarioEnRealtimeDatabase(email: String, nombre: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        cloudSyncEngine.guardarUsuarioEnRealtimeDatabase(email, nombre, onSuccess, onError)
    }
    val todosLosReportes: Flow<List<ReporteEntity>> = database.reporteDao().obtenerTodosLosReportes()
    val notificaciones: Flow<List<NotificacionEntity>> = database.notificacionDao().obtenerNotificaciones()

    fun obtenerHistorialPuntos(email: String): Flow<List<PuntoHistorialEntity>> =
        database.puntoHistorialDao().obtenerHistorial(email)

    fun obtenerInsignias(email: String): Flow<List<InsigniaEntity>> =
        database.insigniaDao().obtenerInsignias(email)

    suspend fun loginInvitado(nombre: String): UsuarioPerfilEntity = withContext(Dispatchers.IO) {
        val email = "invitado_${System.currentTimeMillis()}@ecoconnect.local"
        val user = UsuarioPerfilEntity(email = email, nombre = nombre, puntos = 0, esInvitado = true)
        database.usuarioDao().guardarUsuario(user)
        user
    }

    suspend fun crearReporteConFoto(
        titulo: String,
        descripcion: String,
        categoria: String,
        ubicacion: String,
        latitud: Double,
        longitud: Double,
        prioridad: String,
        autorNombre: String,
        autorEmail: String,
        bitmapImagen: Bitmap?,
        imageUri: Uri? = null
    ) = withContext(Dispatchers.IO) {

        var pathLocal: String? = null
        var base64Imagen: String? = null

        if (bitmapImagen != null) {
            val nombreArchivo = "foto_eco_${System.currentTimeMillis()}"
            pathLocal = EcoConnectImageManager.guardarBitmapEnAlmacenamientoInterno(context, bitmapImagen, nombreArchivo)
            base64Imagen = EcoConnectImageManager.optimizarImagenParaSubida(bitmapImagen)
        }

        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val nuevoReporte = ReporteEntity(
            titulo = titulo,
            descripcion = descripcion,
            categoria = categoria,
            ubicacionTexto = ubicacion,
            latitud = latitud,
            longitud = longitud,
            prioridad = prioridad,
            votosApoyo = 1,
            autorNombre = autorNombre,
            autorEmail = autorEmail,
            fechaCreacion = fechaActual,
            fotoPathLocal = pathLocal,
            fotoBase64 = base64Imagen,
            sincronizadoCloud = false,
            resuelto = false,
            kgCO2Evitados = if (categoria == "Reciclaje") 5.2 else 0.0
        )

        database.reporteDao().insertarReporte(nuevoReporte)
        database.usuarioDao().sumarPuntos(autorEmail, 50)
        database.puntoHistorialDao().insertarEntrada(
            PuntoHistorialEntity(emailUsuario = autorEmail, cantidad = 50, motivo = "Publicación: $titulo")
        )

        // Verificar insignias por participación
        verificarYAsignarInsignias(autorEmail)

        database.notificacionDao().insertarNotificacion(
            NotificacionEntity(
                titulo = "Reporte Registrado",
                mensaje = "Has ganado +50 puntos por publicar '$titulo'.",
                fecha = fechaActual
            )
        )

        cloudSyncEngine.subirReporteAFirebase(nuevoReporte, imageUri)


        // Programar sincronización de fondo (Offline-First)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<EcoConnectSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    suspend fun apoyarReporte(idReporte: String) = withContext(Dispatchers.IO) {
        database.reporteDao().incrementarVotos(idReporte)
    }

    suspend fun agregarComentario(reporteId: String, autor: String, mensaje: String) = withContext(Dispatchers.IO) {
        val nuevoComentario = ComentarioEntity(
            reporteId = reporteId,
            autor = autor,
            mensaje = mensaje
        )
        database.comentarioDao().insertarComentario(nuevoComentario)
    }

    fun obtenerComentarios(reporteId: String): Flow<List<ComentarioEntity>> {
        return database.comentarioDao().obtenerComentariosPorReporte(reporteId)
    }

    suspend fun resolverReporte(reporte: ReporteEntity) = withContext(Dispatchers.IO) {
        val kg = when(reporte.categoria) {
            "Basura" -> 15.0
            "Fuga Agua" -> 25.0
            else -> 10.0
        }
        val reporteActualizado = reporte.copy(resuelto = true, kgCO2Evitados = kg)
        database.reporteDao().actualizarReporte(reporteActualizado)
        database.usuarioDao().sumarPuntos(reporte.autorEmail, 100)
        database.puntoHistorialDao().insertarEntrada(
            PuntoHistorialEntity(emailUsuario = reporte.autorEmail, cantidad = 100, motivo = "Reporte Resuelto: ${reporte.titulo}")
        )
    }

    suspend fun eliminarReporte(id: String) = withContext(Dispatchers.IO) {
        database.reporteDao().eliminarReporte(id)
    }

    fun buscarReportes(query: String): Flow<List<ReporteEntity>> {
        return database.reporteDao().buscarReportes("%$query%")
    }

    private suspend fun verificarYAsignarInsignias(email: String) {
        // Lógica real de insignias
        val historial = database.puntoHistorialDao().obtenerHistorial(email).first()
        val numPublicaciones = historial.count { it.motivo.startsWith("Publicación") }
        
        if (numPublicaciones == 1) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                usuarioEmail = email,
                titulo = "Primer Impacto",
                descripcion = "Has realizado tu primer reporte ambiental.",
                iconoNombre = "first_report",
                fechaObtencion = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            ))
        } else if (numPublicaciones == 5) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                usuarioEmail = email,
                titulo = "Eco-Activista",
                descripcion = "Has contribuido con 5 reportes a la comunidad.",
                iconoNombre = "activist",
                fechaObtencion = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            ))
        }
    }
}
