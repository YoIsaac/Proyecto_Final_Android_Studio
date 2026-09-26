package com.example.proyectofinal.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// ============================================================================
// ENTIDADES DE PERSISTENCIA LOCAL CON ROOM (SQLITE)
// ============================================================================

@Entity(tableName = "tabla_reportes_ambientales")
data class ReporteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "categoria") val categoria: String,
    @ColumnInfo(name = "ubicacion_texto") val ubicacionTexto: String,
    @ColumnInfo(name = "latitud") val latitud: Double,
    @ColumnInfo(name = "longitud") val longitud: Double,
    @ColumnInfo(name = "prioridad") val prioridad: String,
    @ColumnInfo(name = "votos_apoyo") val votosApoyo: Int,
    @ColumnInfo(name = "autor_nombre") val autorNombre: String,
    @ColumnInfo(name = "autor_email") val autorEmail: String,
    @ColumnInfo(name = "fecha_creacion") val fechaCreacion: String,
    @ColumnInfo(name = "foto_path_local") val fotoPathLocal: String?,
    @ColumnInfo(name = "foto_base64") val fotoBase64: String?,
    @ColumnInfo(name = "foto_url_cloud") val fotoUrlCloud: String? = null,
    @ColumnInfo(name = "sincronizado_cloud") val sincronizadoCloud: Boolean = false,
    @ColumnInfo(name = "resuelto") val resuelto: Boolean = false,
    @ColumnInfo(name = "kg_co2_evitados") val kgCO2Evitados: Double = 0.0
)

@Fts4(contentEntity = ReporteEntity::class)
@Entity(tableName = "tabla_reportes_fts")
data class ReporteFtsEntity(
    val titulo: String,
    val descripcion: String
)

@Entity(tableName = "tabla_historial_puntos")
data class PuntoHistorialEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "email_usuario") val emailUsuario: String,
    @ColumnInfo(name = "cantidad") val cantidad: Int,
    @ColumnInfo(name = "motivo") val motivo: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tabla_comentarios",
    foreignKeys = [
        ForeignKey(
            entity = ReporteEntity::class,
            parentColumns = ["id"],
            childColumns = ["reporte_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reporte_id"])]
)
data class ComentarioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "reporte_id") val reporteId: String,
    @ColumnInfo(name = "autor") val autor: String,
    @ColumnInfo(name = "mensaje") val mensaje: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabla_usuario_perfil")
data class UsuarioPerfilEntity(
    @PrimaryKey val email: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "puntos") val puntos: Int = 0,
    @ColumnInfo(name = "nivel") val nivel: String = "Guardián Ecológico",
    @ColumnInfo(name = "avatar_base64") val avatarBase64: String? = null,
    @ColumnInfo(name = "es_invitado") val esInvitado: Boolean = false
)

@Entity(tableName = "tabla_insignias")
data class InsigniaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "usuario_email") val usuarioEmail: String,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "icono_nombre") val iconoNombre: String,
    @ColumnInfo(name = "fecha_obtencion") val fechaObtencion: String
)

@Entity(tableName = "tabla_notificaciones")
data class NotificacionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "mensaje") val mensaje: String,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "leida") val leida: Boolean = false
)

// ============================================================================
// DATA ACCESS OBJECTS (DAO)
// ============================================================================

@Dao
interface ReporteDao {
    @Query("SELECT * FROM tabla_reportes_ambientales ORDER BY fecha_creacion DESC")
    fun obtenerTodosLosReportes(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM tabla_reportes_ambientales WHERE id = :id LIMIT 1")
    fun obtenerReportePorId(id: String): ReporteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarReporte(reporte: ReporteEntity)

    @Update
    fun actualizarReporte(reporte: ReporteEntity)

    @Query("UPDATE tabla_reportes_ambientales SET votos_apoyo = votos_apoyo + 1 WHERE id = :id")
    fun incrementarVotos(id: String)

    @Query("UPDATE tabla_reportes_ambientales SET votos_apoyo = CASE WHEN votos_apoyo > 0 THEN votos_apoyo - 1 ELSE 0 END WHERE id = :id")
    fun decrementarVotos(id: String)

    @Query("UPDATE tabla_reportes_ambientales SET sincronizado_cloud = 1 WHERE id = :id")
    fun marcarComoSincronizado(id: String)

    @Query("DELETE FROM tabla_reportes_ambientales WHERE id = :id")
    fun eliminarReporte(id: String)

        @Query("SELECT * FROM tabla_reportes_ambientales WHERE titulo LIKE :query OR descripcion LIKE :query")
    fun buscarReportes(query: String): Flow<List<ReporteEntity>>
}

@Dao
interface ComentarioDao {
    @Query("SELECT * FROM tabla_comentarios WHERE reporte_id = :reporteId ORDER BY timestamp ASC")
    fun obtenerComentariosPorReporte(reporteId: String): Flow<List<ComentarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarComentario(comentario: ComentarioEntity)
}

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM tabla_usuario_perfil WHERE email = :email LIMIT 1")
    fun obtenerUsuario(email: String): UsuarioPerfilEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun guardarUsuario(usuario: UsuarioPerfilEntity)

    @Query("UPDATE tabla_usuario_perfil SET puntos = puntos + :puntosSumar WHERE email = :email")
    fun sumarPuntos(email: String, puntosSumar: Int)

    @Query("SELECT SUM(puntos) FROM tabla_usuario_perfil")
    fun obtenerTotalPuntosComunitarios(): Int?
}

@Dao
interface PuntoHistorialDao {
    @Query("SELECT * FROM tabla_historial_puntos WHERE email_usuario = :email ORDER BY timestamp DESC")
    fun obtenerHistorial(email: String): Flow<List<PuntoHistorialEntity>>

    @Insert
    fun insertarEntrada(entrada: PuntoHistorialEntity)
}

@Dao
interface InsigniaDao {
    @Query("SELECT * FROM tabla_insignias WHERE usuario_email = :email")
    fun obtenerInsignias(email: String): Flow<List<InsigniaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarInsignia(insignia: InsigniaEntity)
}

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM tabla_notificaciones ORDER BY id DESC")
    fun obtenerNotificaciones(): Flow<List<NotificacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarNotificacion(notificacion: NotificacionEntity)
}

// ============================================================================
// BASE DE DATOS ROOM
// ============================================================================

@Database(
    entities = [
        ReporteEntity::class,
        ReporteFtsEntity::class,
        ComentarioEntity::class,
        UsuarioPerfilEntity::class,
        NotificacionEntity::class,
        PuntoHistorialEntity::class,
        InsigniaEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class EcoConnectRoomDatabase : RoomDatabase() {
    abstract fun reporteDao(): ReporteDao
    abstract fun comentarioDao(): ComentarioDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun puntoHistorialDao(): PuntoHistorialDao
    abstract fun insigniaDao(): InsigniaDao

    companion object {
        @Volatile
        private var INSTANCE: EcoConnectRoomDatabase? = null

        fun obtenerBaseDatos(context: Context): EcoConnectRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcoConnectRoomDatabase::class.java,
                    "ecoconnect_enterprise_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================================================================
// MODELOS DE DATOS COMPARTIDOS / RESIDUALES
// ============================================================================

data class ReporteAmbientalModelo(
    val id: String = UUID.randomUUID().toString().take(8),
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String = "Basura",
    val ubicacionTexto: String = "",
    val latitud: Double = 28.6353,
    val longitud: Double = -106.0889,
    val nivelPrioridad: String = "Media",
    var votosApoyo: Int = 0,
    val autorNombre: String = "Anonimo",
    val autorEmail: String = "demo@tecmilenio.mx",
    val fechaCreacion: String = "",
    val fotoBase64: String? = null,
    var estadoResolucion: Boolean = false,
    var comentariosList: List<ComentarioComunitario> = emptyList()
)

data class ComentarioComunitario(
    val id: String = UUID.randomUUID().toString().take(6),
    val autor: String,
    val mensaje: String,
    val hora: String
)

data class EcoRecompensaModelo(
    val id: String,
    val titulo: String,
    val costoPuntos: Int,
    val descripcion: String,
    val patrocinador: String,
    val categoriaItem: String
)

data class NotificacionAlerta(
    val id: String = UUID.randomUUID().toString().take(5),
    val titulo: String,
    val detalle: String,
    val fecha: String,
    val leido: Boolean = false
)

enum class EcoNavegacionDestino {
    SPLASH_ANIMADO,
    ONBOARDING_PASOS,
    LOGIN_ACCESO,
    REGISTRO_USUARIO,
    DASHBOARD_FEED,
    DETALLE_REPORTE,
    CREAR_REPORTE_CAMARA,
    PERFIL_ESTADISTICAS,
    TIENDA_ECOPUNTOS,
    SIMULADOR_FDROID,
    MAPA_INCIDENCIAS_SIM,
    HISTORIAL_NOTIFICACIONES,
    CONFIGURACION_SISTEMA,
    CALCULADORA_IMPUESTOS_FREELANCE,
    ECO_TIENDA_QR,
    LEADERBOARD_GLOBAL
}
