# Requerimientos — Guander

## 1. Requerimientos Funcionales

### RF-01: Autenticación y Registro

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-01.1 | El sistema debe permitir el registro e inicio de sesión mediante cuenta de Google (OAuth2 vía Firebase Auth). | Alta |
| RF-01.2 | El sistema debe permitir el registro con email y contraseña, solicitando nombre, apellido y teléfono. | Alta |
| RF-01.3 | El sistema debe permitir el inicio de sesión con email y contraseña previamente registrados. | Alta |
| RF-01.4 | Las contraseñas deben almacenarse con hash PBKDF2-SHA256 (100k iteraciones, salt aleatorio de 16 bytes). | Alta |
| RF-01.5 | El sistema debe detectar si un usuario ya existe al registrarse con Google y permitir el acceso sin crear duplicados. | Media |
| RF-01.6 | Al registrarse exitosamente, el sistema debe enviar un email de bienvenida. | Baja |
| RF-01.7 | El sistema debe mantener la sesión activa entre reinicios de la app (Firebase Auth o SharedPreferences). | Alta |
| RF-01.8 | El sistema debe permitir cerrar sesión, limpiando tanto la sesión de Firebase como los datos locales. | Alta |

### RF-02: Dashboard (Pantalla Principal)

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-02.1 | El dashboard debe mostrar un saludo personalizado con el nombre del usuario. | Media |
| RF-02.2 | El dashboard debe mostrar el saldo actual de PetPoints del usuario con formato numérico con separadores. | Alta |
| RF-02.3 | El dashboard debe mostrar las últimas 5 notificaciones con ícono emoji, título y descripción. | Media |
| RF-02.4 | El dashboard debe proveer acceso rápido a las secciones: Mapa, Recompensas, QR Scanner y Perfil. | Alta |
| RF-02.5 | El dashboard debe incluir una barra de navegación inferior con 5 ítems: Inicio, Mapa, QR, Puntos y Perfil. | Alta |

### RF-03: Mapa y Descubrimiento de Lugares

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-03.1 | El sistema debe mostrar un mapa interactivo con los establecimientos pet-friendly como marcadores. | Alta |
| RF-03.2 | El mapa debe centrarse en la ubicación actual del usuario al abrirse (con fallback a Buenos Aires). | Alta |
| RF-03.3 | El sistema debe calcular la distancia entre el usuario y cada establecimiento usando la fórmula de Haversine (cálculo server-side). | Alta |
| RF-03.4 | Los establecimientos deben listarse debajo del mapa, ordenados por distancia ascendente. | Alta |
| RF-03.5 | Cada tarjeta de establecimiento debe mostrar: nombre, distancia, estrellas, horarios (semana/fin de semana/domingo) y estado abierto/cerrado. | Alta |
| RF-03.6 | El sistema debe permitir filtrar establecimientos por categoría: Todos, Locales, Restaurantes, Profesionales, Servicios. | Media |
| RF-03.7 | El sistema debe permitir buscar establecimientos por nombre o descripción en tiempo real. | Media |
| RF-03.8 | Los marcadores del mapa deben actualizarse al aplicar filtros o búsqueda. | Media |
| RF-03.9 | Al tocar un establecimiento, el usuario debe poder ver su detalle completo. | Alta |

### RF-04: Detalle de Establecimiento

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-04.1 | La pantalla de detalle debe mostrar: foto, nombre, categoría (con ícono), estado abierto/cerrado, descripción, dirección y teléfono. | Alta |
| RF-04.2 | El usuario debe poder abrir la dirección del establecimiento en una app de navegación (intent geo://). | Media |
| RF-04.3 | El sistema debe mostrar la lista de reseñas del establecimiento con: avatar, nombre del autor, fecha relativa, estrellas y texto. | Alta |
| RF-04.4 | El sistema debe permitir al usuario dejar una reseña (1-5 estrellas + texto) solo si tiene una compra previa en ese establecimiento. | Alta |
| RF-04.5 | El sistema debe impedir que un usuario deje más de una reseña por establecimiento (prevención de duplicados con respuesta 409). | Alta |
| RF-04.6 | Si el usuario no tiene compras previas, debe mostrarse el mensaje "No puedes comentar aún". | Media |
| RF-04.7 | Si el usuario ya dejó reseña, debe mostrarse "Ya dejaste tu reseña". | Media |

### RF-05: Escaneo QR y Acumulación de PetPoints

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-05.1 | El sistema debe permitir escanear códigos QR mediante la cámara del dispositivo. | Alta |
| RF-05.2 | El QR debe contener un JSON con: type, id, name, amount, item y secret. | Alta |
| RF-05.3 | El sistema debe validar que el campo secret del QR sea "guander2026". | Alta |
| RF-05.4 | Al validar un QR exitosamente, el sistema debe registrar la compra (store_purchase o prof_purchase) y sumar puntos al usuario. | Alta |
| RF-05.5 | Los puntos se calculan como `max(1, floor(monto / 1000))`. | Alta |
| RF-05.6 | Tras un escaneo exitoso, se debe mostrar un diálogo con: puntos ganados, nombre del local, descripción del item, monto y nuevo saldo. | Alta |
| RF-05.7 | El scanner debe funcionar en modo one-shot (un escaneo a la vez, con botón para resetear). | Media |

### RF-06: Recompensas y Canje

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-06.1 | El sistema debe mostrar un catálogo de recompensas disponibles con: nombre, descripción, costo en puntos e ícono del local. | Alta |
| RF-06.2 | Cada recompensa debe indicar visualmente si el usuario tiene puntos suficientes para canjearla (botón deshabilitado con opacidad reducida si no). | Alta |
| RF-06.3 | El sistema debe filtrar recompensas expiradas y ya canjeadas por el usuario. | Alta |
| RF-06.4 | El usuario debe poder buscar recompensas por nombre o descripción en tiempo real. | Media |
| RF-06.5 | Al seleccionar una recompensa, se debe mostrar un diálogo de confirmación con el costo en puntos. | Alta |
| RF-06.6 | Al confirmar el canje, el sistema debe deducir los puntos, registrar la transacción y mostrar el código de canje. | Alta |
| RF-06.7 | El sistema debe prevenir canje duplicado de la misma recompensa. | Alta |
| RF-06.8 | El sistema debe proveer una pestaña "Historial" con los canjes pasados: ícono, descripción, fecha relativa, puntos y código de canje. | Media |

### RF-07: Perfil de Usuario

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-07.1 | El perfil debe mostrar: avatar, nombre, email, teléfono, dirección, fecha de alta y estadísticas (lugares visitados, puntos, cupones canjeados). | Alta |
| RF-07.2 | El usuario debe poder actualizar su nombre, apellido, teléfono y dirección. | Alta |
| RF-07.3 | El usuario debe poder cambiar su foto de perfil seleccionando una imagen de la galería. | Media |
| RF-07.4 | La foto de perfil debe subirse a Cloudinary usando firma criptográfica generada por el backend. | Alta |
| RF-07.5 | Si no tiene foto, el avatar debe mostrar la inicial del nombre sobre un fondo circular de color. | Baja |

### RF-08: Configuración

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-08.1 | El sistema debe permitir configurar notificaciones: general (master), puntos, cupones, lugares nuevos y promociones. | Media |
| RF-08.2 | El sistema debe permitir configurar privacidad: compartir ubicación, datos de uso y contenido personalizado. | Media |
| RF-08.3 | El sistema debe incluir un centro de ayuda con preguntas frecuentes (7 FAQs). | Baja |
| RF-08.4 | El sistema debe mostrar los Términos y Condiciones (8 secciones). | Baja |
| RF-08.5 | El sistema debe mostrar la Política de Privacidad (8 secciones). | Baja |
| RF-08.6 | Las preferencias de notificaciones y privacidad deben persistir localmente con SharedPreferences. | Media |

### RF-09: Temas e Internacionalización

| ID | Requerimiento | Prioridad |
|----|---------------|-----------|
| RF-09.1 | El sistema debe soportar tres modos de apariencia: sistema, claro y oscuro. | Media |
| RF-09.2 | El sistema debe soportar cambio de idioma (por defecto español). | Baja |
| RF-09.3 | Las preferencias de tema e idioma deben persistir entre sesiones. | Media |

---

## 2. Requerimientos No Funcionales

### RNF-01: Rendimiento

| ID | Requerimiento | Métrica |
|----|---------------|---------|
| RNF-01.1 | Las llamadas a la API deben responder en menos de 2 segundos en condiciones normales. | Latencia < 2s (p95) |
| RNF-01.2 | La app debe mostrar indicadores de carga (ProgressBar) durante operaciones de red. | 100% de llamadas async |
| RNF-01.3 | Las imágenes deben cargarse de forma asíncrona con caché local (Glide). | Cache hit > 80% |
| RNF-01.4 | El mapa debe cargar tiles en background sin bloquear la UI principal. | UI thread < 16ms/frame |
| RNF-01.5 | El heap de la JVM del build debe ser de al menos 2048 MB. | -Xmx2048m |

### RNF-02: Seguridad

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-02.1 | Las contraseñas deben almacenarse hasheadas con PBKDF2-SHA256, 100k iteraciones y salt aleatorio. | Nunca en texto plano |
| RNF-02.2 | La comunicación entre app y API debe realizarse exclusivamente sobre HTTPS. | TLS 1.2+ |
| RNF-02.3 | Las subidas de imágenes a Cloudinary deben autorizarse mediante firma criptográfica generada server-side. | SHA-1 HMAC |
| RNF-02.4 | Los códigos QR deben incluir un secreto de validación para prevenir QRs falsificados. | secret="guander2026" |
| RNF-02.5 | El backend debe prevenir condiciones de carrera en canjes de puntos y reseñas duplicadas. | Control server-side |
| RNF-02.6 | Las API keys y secrets del backend no deben estar expuestas en el código fuente de la app. | Wrangler secrets |
| RNF-02.7 | Firebase Auth debe gestionar los tokens de autenticación de Google. | OAuth2 / JWT |

### RNF-03: Usabilidad

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-03.1 | La interfaz debe seguir las guías de Material Design 3. | Material Components 1.13+ |
| RNF-03.2 | La app debe soportar modo oscuro nativo (values-night/). | Tema dinámico |
| RNF-03.3 | Los mensajes de error deben mostrarse en español y ser comprensibles para el usuario. | Traducción de errores Firebase |
| RNF-03.4 | La navegación principal debe estar disponible en todas las pantallas mediante BottomNavigationView. | 5 destinos fijos |
| RNF-03.5 | Las fechas deben mostrarse en formato relativo y legible ("Hoy", "Hace 1 día", "Hace N días"). | Localización española |
| RNF-03.6 | Los montos deben mostrarse con formato de moneda local ($) y separadores de miles. | NumberFormat locale |

### RNF-04: Compatibilidad

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-04.1 | La app debe ser compatible con Android 7.0 (API 24) como versión mínima. | Min SDK 24 |
| RNF-04.2 | La app debe compilar contra Android 15 (API 36). | Target SDK 36 |
| RNF-04.3 | La app debe manejar permisos en runtime para: cámara, ubicación (fina y gruesa), almacenamiento e imágenes. | Runtime permissions |
| RNF-04.4 | La app debe diferenciar permisos de almacenamiento según la versión de Android (READ_MEDIA_IMAGES en API 33+ vs READ_EXTERNAL_STORAGE en versiones anteriores). | Manejo condicional |
| RNF-04.5 | Java 11 debe ser la versión de compilación del código fuente. | sourceCompatibility 11 |

### RNF-05: Disponibilidad y Escalabilidad

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-05.1 | El backend debe funcionar como servicio serverless con alta disponibilidad inherente. | Cloudflare Workers (edge) |
| RNF-05.2 | La base de datos debe ser distribuida y tolerante a fallos. | Cloudflare D1 (SQLite distribuido) |
| RNF-05.3 | Las imágenes deben servirse a través de un CDN global. | Cloudinary CDN |
| RNF-05.4 | El envío de emails no debe bloquear el flujo de registro (fire-and-forget). | Llamada async sin await |

### RNF-06: Mantenibilidad

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-06.1 | El proyecto debe usar Gradle con Kotlin DSL y version catalogs para gestión de dependencias. | libs.versions.toml |
| RNF-06.2 | Las dependencias deben estar centralizadas en un archivo de versiones (libs.versions.toml). | Single source of truth |
| RNF-06.3 | El backend debe estar en un solo archivo desplegable (worker.js) sin dependencias de build externas. | Zero build step |
| RNF-06.4 | La app debe usar R class no transitivo para evitar conflictos de recursos. | android.nonTransitiveRClass=true |

### RNF-07: Privacidad y Datos

| ID | Requerimiento | Detalle |
|----|---------------|---------|
| RNF-07.1 | Los datos de perfil del usuario deben ser modificables y consultables por el usuario. | CRUD de perfil |
| RNF-07.2 | Las preferencias de privacidad y notificaciones deben ser configurables por el usuario. | SharedPreferences locales |
| RNF-07.3 | La app debe incluir información de contacto para solicitudes de privacidad (privacidad@guander.app). | Política de privacidad |
| RNF-07.4 | Las reglas de backup y extracción de datos deben estar configuradas (backup_rules.xml, data_extraction_rules.xml). | Android backup framework |

---

## 3. Matriz de Trazabilidad

| Caso de Uso | Requerimientos Funcionales | Requerimientos No Funcionales |
|-------------|---------------------------|-------------------------------|
| CU-01: Registro/Login | RF-01.1 a RF-01.8 | RNF-02.1, RNF-02.2, RNF-02.7 |
| CU-02: Dashboard | RF-02.1 a RF-02.5 | RNF-01.2, RNF-03.4 |
| CU-03: Explorar Mapa | RF-03.1 a RF-03.9 | RNF-01.4, RNF-04.3 |
| CU-04: Ver Detalle | RF-04.1 a RF-04.7 | RNF-03.5, RNF-03.6 |
| CU-05: Escanear QR | RF-05.1 a RF-05.7 | RNF-02.4, RNF-04.3 |
| CU-06: Canjear Recompensas | RF-06.1 a RF-06.8 | RNF-02.5, RNF-03.6 |
| CU-07: Gestionar Perfil | RF-07.1 a RF-07.5 | RNF-02.3, RNF-07.1 |
| CU-08: Configuración | RF-08.1 a RF-08.6, RF-09.1 a RF-09.3 | RNF-03.2, RNF-07.2 |
