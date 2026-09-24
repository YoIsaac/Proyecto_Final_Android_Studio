# 🌱 EcoConnect - Plataforma Colaborativa de Monitoreo Ambiental

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-brightgreen.svg)
![WCAG](https://img.shields.io/badge/Accessibility-WCAG%202.1%20AA-orange.svg)
![Build](https://img.shields.io/badge/Build-Passing-success.svg)

**EcoConnect** es una aplicación móvil nativa para Android diseñada para empoderar a la ciudadanía en la localización, reporte y seguimiento de incidencias ecológicas y problemas urbanos (basura en canales, fugas de agua, focos de contaminación y deforestación).

---

## 🔗 **Enlaces Directos del Proyecto**

* 🌐 **Landing Page Pública:** [https://IsaacBetance.github.io/ProyectoFinal/landing-page/](https://IsaacBetance.github.io/ProyectoFinal/landing-page/)
* ⬇️ **Descargar APK Funcional:** [DESCARGAR ECOCONNECT.APK](./APK/EcoConnect.apk)
* 📄 **Documentación Técnica PDF:** [VER DOCUMENTO FINAL PDF](./documentacion/documento-final.pdf)
* 🎬 **Video Demostrativo:** [VER DETALLES Y ENLACE DE VIDEO](./video/enlace-video.txt)

---

## 👥 **Integrantes del Equipo**

* **Isaac Betance** - *Desarrollador Principal & UX/UI Designer*
* **Curso:** Desarrollo de Aplicaciones Móviles
* **Institución:** Universidad Tecmilenio - 2026

---

## 🛠️ **Tecnologías y Librerías Utilizadas**

* **Lenguaje:** Kotlin 2.0+
* **Interfaz de Usuario:** Jetpack Compose + Material Design 3
* **Persistencia Local:** Room Database 2.6.1 + SQLCipher + SQLite FTS4 (Full Text Search)
* **Procesamiento de Anotaciones:** Google KSP
* **Tareas en Segundo Plano:** WorkManager Runtime KTX
* **Preferencias Persistentes:** DataStore Preferences
* **Diseño Adaptativo:** Material3 `WindowSizeClass` (Smartphones, Tablets y Plegables)
* **Carga Asíncrona de Imágenes:** Coil Compose
* **Servicios en Nube:** Firebase Firestore & Cloud Storage BOM
* **Red:** Retrofit + OkHttp
* **Pruebas:** JUnit4 + Mockk + Kotlinx Coroutines Test

---

## ✨ **Principales Funcionalidades**

1. **Dashboard & Feed Comunitario Reactivo:**
   * Lista en tiempo real de todos los reportes comunitarios con filtros por categoría (*Basura*, *Fugas de Agua*, *Deforestación*, *Contaminación*).
   * Búsqueda en tiempo real potenciada por **SQLite FTS4**.

2. **Creación de Reportes con Fotografía y GPS:**
   * Captura desde la cámara o selección de galería.
   * Geolocalización y clasificación por niveles de prioridad.

3. **Asistente Inteligente de Residuos (IA Simulator):**
   * Clasificación automática para orientar al usuario sobre el tipo de residuo (PET, Orgánico, Vidrio, Papel/Cartón).

4. **Interacción Comunitaria y Votación:**
   * Sistema de apoyo para priorizar problemas urgentes.
   * Hilos de comentarios por reporte.

5. **Gamificación Ecológica:**
   * Muro de Insignias (*Badges*), Tabla de Clasificación comunitaria (*Leaderboard*) y acumulación de **EcoPuntos**.

6. **Accesibilidad e Inclusión WCAG 2.1 AA:**
   * Modo Oscuro, Modo de Alto Contraste y Filtros para Daltonismo (*Protanopia*, *Deuteranopia*, *Tritanopia*).
   * Etiquetas semánticas para TalkBack y soporte bilingüe (Español / Inglés).

7. **Persistencia y Sincronización Offline-First:**
   * Base de datos Room con sincronización en segundo plano vía WorkManager al conectarse a Internet.

---

## 📁 **Estructura del Repositorio**

```
/ProyectoFinal
├── /app                  -> Código fuente principal en Kotlin / Compose
├── /APK                  -> APK compilado funcional (EcoConnect.apk)
├── /documentacion        -> Documentación técnica (documento-final.pdf y .md)
├── /landing-page         -> Landing Page pública responsiva (index.html, styles.css, script.js)
├── /video                -> Enlace e instrucciones del video demostrativo (enlace-video.txt)
└── README.md             -> Portada principal del repositorio
```

---

## 🚀 **Instrucciones para Ejecutar el Proyecto**

### **Requisitos Previos:**
* Android Studio Ladybug (2024.2.1) o superior.
* JDK 11 o JDK 17 configurado.
* Dispositivo físico o Emulador Android con Android 7.0 (API 24) o superior.

### **Pasos para Construir:**
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/IsaacBetance/ProyectoFinal.git
   ```
2. Abrir el proyecto en Android Studio y esperar a que sincronice Gradle.
3. Compilar e instalar la aplicación en el dispositivo:
   ```bash
   ./gradlew app:assembleDebug
   ```
4. Para ejecutar las pruebas unitarias:
   ```bash
   ./gradlew app:testDebugUnitTest
   ```
