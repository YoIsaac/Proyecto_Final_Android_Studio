# DOCUMENTACIÓN TÉCNICA Y REPORTE FINAL DE DESARROLLO (FASE I + FASE II)

## **EcoConnect: Plataforma Colaborativa de Monitoreo y Reporte Ambiental**

---

### **1. Nombre de la Aplicación e Integrantes del Equipo**
* **Nombre del Proyecto:** EcoConnect (Plataforma Colaborativa de Monitoreo Ambiental)
* **Versión de la App:** 1.0.0 (Build Final Integrado)
* **Integrantes del Equipo:**
  * **Isaac Betance** - Desarrollador Principal, Arquitecto Android & UX/UI Designer
* **Curso:** Desarrollo de Aplicaciones Móviles
* **Institución:** Universidad Tecmilenio
* **Fecha de Entrega:** Septiembre 2026

---

### **2. Descripción General del Proyecto**
**EcoConnect** es una aplicación móvil nativa para Android desarrollada en **Kotlin** y **Jetpack Compose**. Su objetivo principal es empoderar a los ciudadanos para que identifiquen, reporten y den seguimiento a problemas ecológicos e infraestructura urbana dañada (acumulación de plásticos, fugas de agua, focos de contaminación, áreas desforestadas). 

La plataforma combina **geolocalización, captura multimedia, inteligencia artificial simbólica para clasificación de residuos, sincronización híbrida en la nube y mecanismos de gamificación (EcoPuntos e Insignias)** para fomentar la participación comunitaria activa.

---

### **3. Problema o Necesidad que Busca Resolver (Avance Fase I)**
En muchas comunidades y zonas urbanas:
1. **Atención tardía:** Los reportes sobre tiraderos clandestinos de basura o fugas de agua potable tardan semanas en ser atendidos por falta de canales directos y transparentes.
2. **Poca visibilidad:** La ciudadanía desconoce si un problema ya fue reportado por un vecino o si está en proceso de atención.
3. **Falta de incentivos:** No existen mecanismos que reconozcan el impacto positivo de los vecinos activos.
4. **Barreras de accesibilidad:** Muchas aplicaciones gubernamentales o comunitarias carecen de soporte para personas con discapacidad visual o daltonismo.

---

### **4. Público Objetivo y Justificación Social**
* **Ciudadanos activos y ambientalistas:** Personas interesadas en preservar su entorno local.
* **Comités vecinales y grupos ecológicos:** Agrupaciones organizadas que realizan jornadas de limpieza y reforestación.
* **Usuarios en general:** Cualquier ciudadano con un smartphone Android que desee reportar un incidente de manera rápida y sin fricción (incluyendo **modo invitado** sin registro previo obligatorio).

---

### **5. Diseños Iniciales, Wireframes y Flujos de Usuario (Fase I)**
Durante la Fase I del proyecto se diseñaron la estructura de navegación y las pantallas principales:
* **Wireframe 1: Login / Acceso Rápido:** Pantalla limpia con campos de correo, contraseña y botón destacado "Continuar como Invitado".
* **Wireframe 2: Dashboard & Feed Comunitario:** Muestra tarjetas informativas con prioridad (Alta/Media/Baja), botón de apoyo y filtro por categorías en la parte superior.
* **Wireframe 3: Crear Reporte con Cámara:** Formulario simplificado para captura de foto, asignación de título, descripción y botón de análisis asistido por IA.
* **Wireframe 4: Muro de Insignias y Perfil:** Espacio para visualizar el nivel del usuario, total de EcoPuntos y medallas obtenidas.

---

### **6. Funcionalidades Principales e Interacciones**

1. **Dashboard & Feed Comunitario Reactivo:**
   * Lista en tiempo real de todos los reportes comunitarios.
   * Filtrado dinámico por categoría (*Basura*, *Fugas de Agua*, *Deforestación*, *Contaminación*, *Todas*).
   * Búsqueda en tiempo real potenciada por **SQLite FTS4 (Full-Text Search)**.

2. **Validación Estricta de Eventos y Formularios (Email Validation):**
   * Verificación obligatoria de formato de correo electrónico mediante expresiones regulares (`android.util.Patterns.EMAIL_ADDRESS`) en los módulos de Login y Registro (`VistaLoginEngine` y `VistaRegistroEngine`).
   * Indicadores visuales inmediatos de error (`isError = true` y `supportingText`) que previenen datos inválidos o vacíos en la base de datos.

3. **Captura Fotográfica y Geolocalización:**
   * Selección desde galería o captura desde la cámara del dispositivo.
   * Asignación de ubicación y nivel de prioridad (*Alta*, *Media*, *Baja*).

4. **Asistente Inteligente de Clasificación (IA Mock Simulator):**
   * Módulo que analiza la imagen capturada para recomendar la categoría adecuada del residuo (PET, Orgánico, Vidrio, Papel/Cartón).

5. **Sistema de Votación y Comentarios en Tiempo Real:**
   * La comunidad puede respaldar reportes para escalar su prioridad.
   * Hilo de comentarios por reporte con almacenamiento relacional en base de datos.

6. **Gamificación Ecológica (EcoPuntos, Leaderboard e Insignias):**
   * Asignación automática de **EcoPuntos** por cada reporte creado o votado.
   * Tabla de clasificación comunitaria (Leaderboard).
   * Muro de Insignias ganadas (ej. *Primer Reporte*, *Guardián del Agua*, *Héroe Comunitario*).

7. **Calculadora de Huella de Carbono / Impacto:**
   * Cálculo en tiempo real de los kilogramos de CO₂ evitados al resolver o gestionar adecuadamente los residuos.

8. **Sincronización Híbrida Offline-First:**
   * Permite crear y consultar reportes sin conexión a Internet.
   * Worker programado (**WorkManager**) que sincroniza los cambios pendientes con la nube cuando la conexión se restablece.

---

### **7. Métodos de Verificación de Funcionamiento en Línea y Sincronización**
Para garantizar la integración efectiva con servicios en la nube (Firebase Cloud Firestore / Remote Sync), se ejecutaron 3 pruebas de conectividad:

1. **Prueba 1: Consola en Tiempo Real (Firebase Console):**
   * Al crear o respaldar un reporte en la app móvil, los cambios se reflejan inmediatamente en la pestaña *Database/Firestore* del navegador web, resaltando las actualizaciones en tiempo real.
2. **Prueba 2: Sincronización Offline a Online (Modo Avión):**
   * Se creó un reporte con Wi-Fi y datos desactivados. El reporte se guardó de inmediato en la base de datos local **Room**.
   * Al reactivar la conectividad a Internet, el componente **WorkManager (`EcoConnectSyncWorker`)** detectó la red y transmitió automáticamente el registro pendiente a la nube sin requerir reinicio de la app.
3. **Prueba 3: Inspección de Red (Android Studio Network Inspector):**
   * Mediante la herramienta *App Inspection > Network Inspector*, se verificaron las peticiones de envío/recepción de datos observando códigos de respuesta HTTP **200 OK** durante la navegación.

---

### **8. Diseño UX/UI y Decisiones de Diseño**
* **Material Design 3 (M3):** Uso de paleta de colores ecológica (*Primary #2E7D32*), bordes fluidos (`enableEdgeToEdge`) y tipografía accesible (`Plus Jakarta Sans`).
* **Flujo Unificado y Navegación Declarativa:** Control por enum de estado (`EcoNavegacionDestino`) con pantallas independientes para Login, Dashboard, Detalles, Perfil, Configuración, Muro de Insignias y Calculadora.

---

### **9. Accesibilidad e Inclusión (WCAG 2.1 AA)**
Se implementaron de origen las siguientes medidas de accesibilidad:
* **Modo Oscuro Adaptativo:** Configurable globalmente en el tema `ProyectoFinalTheme`.
* **Modo de Alto Contraste:** Ajusta automáticamente las tonalidades a contrastes extremos (> 7:1) para personas con baja visión.
* **Filtros para Daltonismo:** Algoritmo dinámico que transforma las matrices de color para apoyar a usuarios con *Protanopia*, *Deuteranopia* y *Tritanopia*.
* **Semántica para TalkBack:** Uso explícito de `contentDescription` en todos los iconos interactivos.
* **Soporte Multilingüe:** Cadenas extraídas en `res/values/strings.xml` y `res/values-en/strings.xml`.

---

### **10. Tecnologías y Herramientas Utilizadas**
| Categoria | Tecnología / Librería | Propósito |
|---|---|---|
| **Lenguaje** | Kotlin 2.0+ | Lenguaje nativo de desarrollo Android |
| **UI Framework** | Jetpack Compose + Material 3 | Interfaz de usuario declarativa |
| **Base de Datos Local** | Room Database 2.6.1 + SQLCipher | Persistencia relacional local con FTS4 |
| **Procesamiento KSP** | Google KSP | Generación eficiente de código Room |
| **Sincronización** | WorkManager runtime KTX | Tareas en segundo plano offline-first |
| **Almacenamiento Prefs** | DataStore Preferences | Guardado persistente de configuración de usuario |
| **Adaptación Pantalla** | Material3 WindowSizeClass | Soporte para teléfonos, tablets y plegables |
| **Carga de Imágenes** | Coil Compose | Carga asíncrona y eficiente de bitmaps |
| **Servicios Nube** | Firebase Firestore & Storage BOM | Respaldo remoto y almacenamiento |
| **Pruebas** | JUnit4 + Mockk + Coroutines Test | Pruebas unitarias de ViewModel y repositorio |

---

### **11. Arquitectura y Persistencia de Datos**
El proyecto implementa la arquitectura recomendada por Google (Clean Architecture + MVVM + Repository Pattern):

```
+-----------------------------------------------------------+
|                      UI Layer                             |
| (Jetpack Compose + EcoConnectViewModelAvanzado + Theme)  |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
|                    Repository Layer                       |
|                 (EcoConnectRepository)                    |
+-----------------------------------------------------------+
              /                               \
             v                                 v
+--------------------------+     +--------------------------+
|       Local Data         |     |       Cloud Data         |
|   (Room DB + DataStore)  |     |  (Firebase Sync Engine)  |
+--------------------------+     +--------------------------+
```

---

### **12. Adaptación a Diferentes Dispositivos y Resoluciones**
Se utiliza la API `calculateWindowSizeClass(activity)` para categorizar las pantallas en:
* **Compact Width (< 600dp):** Disposición en 1 columna vertical con navegación mediante `NavigationBar` inferior.
* **Medium & Expanded Width (>= 600dp):** Disposición multi-columna (Grid/Master-Detail) con optimización de espacio para tablets y dispositivos plegables.

---

### **13. Pruebas Realizadas**
Se ejecutaron exitosamente las pruebas unitarias automáticas con el comando:
```bash
./gradlew app:testDebugUnitTest
```
* **Resultados:** 3/3 Pruebas Pasadas (0 fallos, 0 omitidas).
* **Casos probados:**
  1. Inicialización correcta de EcoPuntos en ViewModel.
  2. Incremento correcto de puntos al emitir un voto comunitario.
  3. Carga e inserción inicial en Room cuando la base de datos se encuentra vacía.

---

### **14. Problemas Encontrados y Soluciones Implementadas**
1. **Desafío:** Entrada de correos mal formados que distorsionaban el perfil del usuario.
   * **Solución:** Implementación de expresiones regulares de validación (`android.util.Patterns.EMAIL_ADDRESS`) y avisos de error dinámicos.
2. **Desafío:** Lentitud al realizar búsquedas de texto plano en listas extensas de reportes.
   * **Solución:** Se implementó una entidad indexada **SQLite FTS4 (`ReporteFtsEntity`)** con consultas optimizadas en el DAO.
3. **Desafío:** Incompatibilidad de contraste para usuarios con daltonismo en mapas y badges.
   * **Solución:** Adición de filtros de matriz de color dinámicos en la raíz del Compose Theme.

---

### **15. Integración del Feedback Recibido en la Fase 1**
Durante la Fase 1 se sugirió:
* *Incluir un modo directo de prueba sin registro.* -> **Implementado:** Se creó el flujo `loginComoInvitado` con acceso directo a todas las vistas.
* *Agregar un sistema visible de logros.* -> **Implementado:** Se desarrolló la pantalla `VistaMuroInsigniasEngine` e integración de badges dinámicos.
* *Aumentar el soporte de accesibilidad y validaciones.* -> **Implementado:** Se agregaron las validaciones de formulario por Regex, los modificadores de contraste y los 3 filtros de daltonismo.

---

### **16. Uso de Inteligencia Artificial (Declaración Transparente)**
El uso de herramientas de Inteligencia Artificial durante el proyecto incluyó:
* **Optimización de Código:** Apoyo en la formulación de expresiones regulares para validaciones de formularios y sintaxis de Room DAOs con FTS4.
* **Diseño de Accesibilidad:** Revisión de contraste de color y generación de modificadores de daltonismo.
* **Documentación y README:** Estructuración del archivo `README.md` y guía de pruebas.
* **Revisión Humana:** Todo el código y arquitectura fueron revisados, probados y validados directamente por el estudiante.

---

### **17. Enlaces del Proyecto**
* **Repositorio de GitHub:** [https://github.com/IsaacBetance/ProyectoFinal](https://github.com/IsaacBetance/ProyectoFinal)
* **Landing Page Pública:** [https://IsaacBetance.github.io/ProyectoFinal/landing-page/](https://IsaacBetance.github.io/ProyectoFinal/landing-page/)
* **Descarga de APK:** [https://github.com/IsaacBetance/ProyectoFinal/raw/main/APK/EcoConnect.apk](https://github.com/IsaacBetance/ProyectoFinal/raw/main/APK/EcoConnect.apk)
* **Video Demostrativo:** Enlace contenido en `/video/enlace-video.txt`

---

### **18. Conclusión**
El desarrollo de **EcoConnect** integra la totalidad de las competencias solicitadas en las Fases I y II: diseño centrado en el usuario, validaciones robustas de entrada, persistencia local con Room, sincronización híbrida en la nube con WorkManager, accesibilidad inclusiva WCAG 2.1 AA y publicación mediante landing page profesional.
