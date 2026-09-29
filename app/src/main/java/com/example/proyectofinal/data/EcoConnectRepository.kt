package com.example.proyectofinal.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.work.*
import com.example.proyectofinal.utils.EcoConnectImageManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private const val FIREBASE_DATABASE_URL = "https://proyecto-final-ecoconect-default-rtdb.firebaseio.com"

class EcoConnectCloudSyncEngine(
    private val database: EcoConnectRoomDatabase,
    private val context: Context
) {
    private val _estadoSincronizacion = MutableStateFlow("Sincronización Cloud Lista")
    val estadoSincronizacion: StateFlow<String> = _estadoSincronizacion

    private fun obtenerRealtimeDatabase(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    init {
        iniciarEscuchadorTiempoReal()
    }

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
                "fotoBase64" to (reporte.fotoBase64 ?: ""),
                "kgCO2" to reporte.kgCO2Evitados
            )
            
            db.collection("reportes").document(reporte.id).set(cloudReporte).await()
            
            // También subir a Realtime Database para sincronización inmediata multi-dispositivo
            subirReporteARealtimeDatabase(reporte.copy(fotoUrlCloud = cloudUrl))

            database.reporteDao().marcarComoSincronizado(reporte.id)
            _estadoSincronizacion.value = "Sincronizado con Firebase Cloud"
        } catch (e: Exception) {
            Log.e("FIREBASE_TEST", "Error en Firestore/Storage: ${e.localizedMessage}")
            subirReporteARealtimeDatabase(reporte)
            _estadoSincronizacion.value = "Sincronizado en Realtime DB"
        }
    }

    fun subirReporteARealtimeDatabase(reporte: ReporteEntity) {
        try {
            val realtimeDb = obtenerRealtimeDatabase()
            val ref = realtimeDb.getReference("reportes_comunitarios")
            
            val map = hashMapOf(
                "id" to reporte.id,
                "titulo" to reporte.titulo,
                "descripcion" to reporte.descripcion,
                "categoria" to reporte.categoria,
                "ubicacionTexto" to reporte.ubicacionTexto,
                "latitud" to reporte.latitud,
                "longitud" to reporte.longitud,
                "prioridad" to reporte.prioridad,
                "votosApoyo" to reporte.votosApoyo,
                "autorNombre" to reporte.autorNombre,
                "autorEmail" to reporte.autorEmail,
                "fechaCreacion" to reporte.fechaCreacion,
                "fotoBase64" to (reporte.fotoBase64 ?: ""),
                "fotoUrlCloud" to (reporte.fotoUrlCloud ?: ""),
                "resuelto" to reporte.resuelto,
                "kgCO2Evitados" to reporte.kgCO2Evitados
            )
            
            Log.d("FIREBASE_TEST", "🚀 Enviando reporte a Realtime DB -> ID: ${reporte.id}, Título: ${reporte.titulo}")

            ref.child(reporte.id).setValue(map)
                .addOnSuccessListener {
                    Log.d("FIREBASE_TEST", "✅ ¡ÉXITO TOTAL! Reporte '${reporte.titulo}' guardado en Firebase Realtime Database bajo 'reportes_comunitarios/${reporte.id}'.")
                    _estadoSincronizacion.value = "Reporte publicado en Realtime Database"
                }
                .addOnFailureListener { err ->
                    Log.e("FIREBASE_TEST", "❌ ERROR AL GUARDAR EN FIREBASE REALTIME DB: ${err.localizedMessage}", err)
                }
        } catch (e: Exception) {
            Log.e("FIREBASE_TEST", "❌ EXCEPCIÓN en subirReporteARealtimeDatabase: ${e.localizedMessage}", e)
        }
    }

    fun guardarUsuarioEnRealtimeDatabase(
        email: String,
        nombre: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val realtimeDb = obtenerRealtimeDatabase()
            val ref = realtimeDb.getReference("usuarios")
            val userId = email.replace(".", "_").replace("@", "_at_")

            val usuarioData = hashMapOf(
                "email" to email,
                "nombre" to nombre,
                "fechaRegistro" to SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                "sincronizadoEnLinea" to true,
                "plataforma" to "Android EcoConnect App"
            )

            Log.d("FIREBASE_TEST", "🚀 Registrando usuario en Realtime DB: $email")

            ref.child(userId).setValue(usuarioData)
                .addOnSuccessListener {
                    Log.d("FIREBASE_TEST", "✅ Usuario '$email' guardado en Realtime DB.")
                    _estadoSincronizacion.value = "Usuario guardado en Realtime Database"
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("FIREBASE_TEST", "❌ Error al guardar usuario: ${e.localizedMessage}", e)
                    _estadoSincronizacion.value = "Realtime DB Sync Error"
                    onError(e.localizedMessage ?: "Error de conexión")
                }
        } catch (e: Exception) {
            Log.e("FIREBASE_TEST", "❌ Excepción en guardarUsuario: ${e.localizedMessage}", e)
            _estadoSincronizacion.value = "Realtime DB Excepción"
            onError(e.localizedMessage ?: "Error de inicialización")
        }
    }

    fun eliminarReporteDeFirebase(id: String) {
        try {
            val realtimeDb = obtenerRealtimeDatabase()
            Log.d("FIREBASE_TEST", "🗑️ Eliminando reporte '$id' de Realtime DB...")
            
            realtimeDb.getReference("reportes_comunitarios").child(id).removeValue()
                .addOnSuccessListener {
                    Log.d("FIREBASE_TEST", "✅ Reporte '$id' borrado con éxito de Realtime DB.")
                }
                .addOnFailureListener { err ->
                    Log.e("FIREBASE_TEST", "❌ Error al borrar reporte '$id': ${err.localizedMessage}")
                }

            val db = FirebaseFirestore.getInstance()
            db.collection("reportes").document(id).delete()
            _estadoSincronizacion.value = "Reporte eliminado de Firebase Cloud"
        } catch (e: Exception) {
            Log.e("FIREBASE_TEST", "❌ Excepción en eliminarReporteDeFirebase: ${e.localizedMessage}", e)
        }
    }

    fun iniciarEscuchadorTiempoReal() {
        try {
            val realtimeDb = obtenerRealtimeDatabase()
            val ref = realtimeDb.getReference("reportes_comunitarios")
            Log.d("FIREBASE_TEST", "🎧 Escuchador en tiempo real activo en 'reportes_comunitarios' (${FIREBASE_DATABASE_URL})")
            
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("FIREBASE_TEST", "📩 DataChange recibido desde Firebase Realtime DB. Total de nodos: ${snapshot.childrenCount}")
                    GlobalScope.launch(Dispatchers.IO) {
                        val remoteIds = HashSet<String>()

                        for (child in snapshot.children) {
                            val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                            remoteIds.add(id)

                            val titulo = child.child("titulo").getValue(String::class.java) ?: ""
                            val descripcion = child.child("descripcion").getValue(String::class.java) ?: ""
                            val categoria = child.child("categoria").getValue(String::class.java) ?: "Basura"
                            val ubicacionTexto = child.child("ubicacionTexto").getValue(String::class.java) ?: ""
                            val latitud = child.child("latitud").getValue(Double::class.java) ?: 28.6353
                            val longitud = child.child("longitud").getValue(Double::class.java) ?: -106.0889
                            val prioridad = child.child("prioridad").getValue(String::class.java) ?: "Media"
                            val votosApoyo = child.child("votosApoyo").getValue(Int::class.java) ?: 1
                            val autorNombre = child.child("autorNombre").getValue(String::class.java) ?: "Usuario Eco"
                            val autorEmail = child.child("autorEmail").getValue(String::class.java) ?: "usuario@ecoconnect.app"
                            val fechaCreacion = child.child("fechaCreacion").getValue(String::class.java) ?: ""
                            val fotoBase64 = child.child("fotoBase64").getValue(String::class.java)
                            val fotoUrlCloud = child.child("fotoUrlCloud").getValue(String::class.java)
                            val resuelto = child.child("resuelto").getValue(Boolean::class.java) ?: false
                            val kgCO2Evitados = child.child("kgCO2Evitados").getValue(Double::class.java) ?: 0.0

                            val localReporte = database.reporteDao().obtenerReportePorId(id)

                            val entity = ReporteEntity(
                                id = id,
                                titulo = titulo,
                                descripcion = descripcion,
                                categoria = categoria,
                                ubicacionTexto = ubicacionTexto,
                                latitud = latitud,
                                longitud = longitud,
                                prioridad = prioridad,
                                votosApoyo = votosApoyo,
                                autorNombre = autorNombre,
                                autorEmail = autorEmail,
                                fechaCreacion = fechaCreacion,
                                fotoPathLocal = localReporte?.fotoPathLocal,
                                fotoBase64 = if (!fotoBase64.isNullOrBlank()) fotoBase64 else localReporte?.fotoBase64,
                                fotoUrlCloud = if (!fotoUrlCloud.isNullOrBlank()) fotoUrlCloud else localReporte?.fotoUrlCloud,
                                sincronizadoCloud = true,
                                resuelto = resuelto,
                                kgCO2Evitados = kgCO2Evitados
                            )
                            database.reporteDao().insertarReporte(entity)
                        }

                        // Sincronizar eliminaciones: borrar localmente reportes que fueron eliminados en Firebase
                        val localList = database.reporteDao().obtenerTodosLosReportes().first()
                        for (local in localList) {
                            if (local.sincronizadoCloud && !remoteIds.contains(local.id)) {
                                database.reporteDao().eliminarReporte(local.id)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
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
        var bitmapActual: Bitmap? = bitmapImagen

        // Decodificar Bitmap desde Uri de la galería si bitmapImagen es nulo
        if (bitmapActual == null && imageUri != null) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    bitmapActual = android.graphics.BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (bitmapActual != null) {
            val nombreArchivo = "foto_eco_${System.currentTimeMillis()}"
            pathLocal = EcoConnectImageManager.guardarBitmapEnAlmacenamientoInterno(context, bitmapActual!!, nombreArchivo)
            base64Imagen = EcoConnectImageManager.optimizarImagenParaSubida(bitmapActual!!)
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

    suspend fun apoyarReporte(idReporte: String, emailUsuario: String) = withContext(Dispatchers.IO) {
        database.reporteDao().incrementarVotos(idReporte)
        database.puntoHistorialDao().insertarEntrada(
            PuntoHistorialEntity(emailUsuario = emailUsuario, cantidad = 5, motivo = "Apoyo a reporte")
        )
    }

    suspend fun quitarApoyoReporte(idReporte: String) = withContext(Dispatchers.IO) {
        database.reporteDao().decrementarVotos(idReporte)
    }

    suspend fun agregarComentario(reporteId: String, autor: String, emailUsuario: String, mensaje: String) = withContext(Dispatchers.IO) {
        val nuevoComentario = ComentarioEntity(
            reporteId = reporteId,
            autor = autor,
            mensaje = mensaje
        )
        database.comentarioDao().insertarComentario(nuevoComentario)
        database.puntoHistorialDao().insertarEntrada(
            PuntoHistorialEntity(emailUsuario = emailUsuario, cantidad = 10, motivo = "Comentario comunitario")
        )
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
        cloudSyncEngine.eliminarReporteDeFirebase(id)
    }

    fun buscarReportes(query: String): Flow<List<ReporteEntity>> {
        return database.reporteDao().buscarReportes("%$query%")
    }

    suspend fun verificarYAsignarInsignias(
        email: String,
        numReportes: Int = 0,
        puntosTotales: Int = 0,
        numApoyos: Int = 0,
        numComentarios: Int = 0,
        esQr: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        if (numReportes >= 1) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_primer_impacto",
                usuarioEmail = email,
                titulo = "Primer Impacto 🥉",
                descripcion = "Has publicado tu primer reporte ambiental.",
                iconoNombre = "first_report",
                fechaObtencion = fecha
            ))
        }
        if (numReportes >= 5) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_guardian_comunitario",
                usuarioEmail = email,
                titulo = "Guardián Comunitario 🥈",
                descripcion = "Has contribuido con 5 reportes a tu comunidad.",
                iconoNombre = "activist",
                fechaObtencion = fecha
            ))
        }
        if (puntosTotales >= 500) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_lider_ambiental",
                usuarioEmail = email,
                titulo = "Líder Ambiental 🥇",
                descripcion = "Has acumulado más de 500 EcoPuntos.",
                iconoNombre = "leader",
                fechaObtencion = fecha
            ))
        }
        if (numApoyos >= 3) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_corazon_ecologico",
                usuarioEmail = email,
                titulo = "Corazón Ecológico 💖",
                descripcion = "Has apoyado 3 o más reportes comunitarios.",
                iconoNombre = "heart",
                fechaObtencion = fecha
            ))
        }
        if (numComentarios >= 3) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_voz_comunitaria",
                usuarioEmail = email,
                titulo = "Voz Comunitaria 💬",
                descripcion = "Has participado con 3 o más comentarios.",
                iconoNombre = "chat",
                fechaObtencion = fecha
            ))
        }
        if (esQr) {
            database.insigniaDao().insertarInsignia(InsigniaEntity(
                id = "${email}_badge_escaner_participativo",
                usuarioEmail = email,
                titulo = "Escáner Participativo 📲",
                descripcion = "Escaneaste un código QR ecológico comunitario.",
                iconoNombre = "qr",
                fechaObtencion = fecha
            ))
        }
    }
}
