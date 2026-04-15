# Guander — Documentación técnica

## ¿Qué es Guander?

Guander es una app Android de fidelización para locales pet-friendly. Los usuarios escanean códigos QR en locales afiliados, acumulan puntos y los canjean por recompensas. También pueden explorar locales en el mapa, ver sus detalles y dejar reseñas.

---

## Arquitectura general

```
┌─────────────────────────────────────────────────┐
│               App Android (Java)                │
│  Activities · Glide · OSMDroid · ZXing · Firebase│
└───────────────────┬─────────────────────────────┘
                    │  HTTP/JSON  (REST)
                    ▼
┌─────────────────────────────────────────────────┐
│        Cloudflare Worker  (JavaScript)          │
│   guander-api.tomas-gonzalezz.workers.dev       │
└───────────────────┬─────────────────────────────┘
                    │  Cloudflare D1  (SQLite)
                    ▼
┌─────────────────────────────────────────────────┐
│              Base de datos D1                   │
│  user_data · users · customer · stores          │
│  professionals · comments_store · comments_prof │
│  store_purchase · prof_purchase · rewards       │
│  redeem_history · points_history                │
└─────────────────────────────────────────────────┘

Servicios externos:
  Firebase Auth   — autenticación Google
  Cloudflare R2   — almacenamiento de fotos de perfil/locales
  Resend          — email de bienvenida al registrarse
```

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| App cliente | Android (Java), minSdk 24, targetSdk 36 |
| UI | Material Design 3 (Material3) |
| Mapas | OSMDroid 6.1.18 + OpenStreetMap tiles |
| Escáner QR | ZXing Android Embedded 4.3.0 |
| Carga de imágenes | Glide 4.16.0 |
| Autenticación | Firebase Auth (Google Sign-In + email/contraseña) |
| Backend / API | Cloudflare Workers (JavaScript ES modules) |
| Base de datos | Cloudflare D1 (SQLite serverless) |
| Storage de fotos | Cloudflare R2 |
| Email transaccional | Resend API |
| Build system | Gradle KTS |

---

## Estructura del proyecto

```
Guander/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/guander/
│       │   ├── GuanderApp.java              # Application class (init OSMDroid)
│       │   ├── LoginActivity.java           # Login / registro
│       │   ├── MainActivity.java            # Splash / router inicial
│       │   ├── DashboardActivity.java       # Pantalla principal con puntos y notificaciones
│       │   ├── MapActivity.java             # Mapa + lista de locales
│       │   ├── PlaceDetailActivity.java     # Detalle de local + reseñas
│       │   ├── QrScanActivity.java          # Escáner de QR
│       │   ├── RewardsActivity.java         # Recompensas + historial de canjes
│       │   ├── ProfileActivity.java         # Perfil del usuario + foto
│       │   ├── EditProfileActivity.java     # Edición de datos personales
│       │   ├── AppearanceActivity.java      # Tema claro/oscuro
│       │   ├── LanguageActivity.java        # Idioma de la app
│       │   ├── NotificationsSettingsActivity.java
│       │   ├── HelpCenterActivity.java
│       │   ├── PrivacyPolicyActivity.java
│       │   ├── PrivacySecurityActivity.java
│       │   └── TermsActivity.java
│       └── res/
│           ├── layout/                      # XMLs de vistas
│           └── values/                      # Colores, strings, temas (light + night)
├── cloudflare-worker/
│   ├── worker.js                            # API backend completa
│   ├── wrangler.toml                        # Config del worker
│   └── package.json
└── documentacion/
    └── README.md
```

---

## Pantallas y flujo de navegación

```
LoginActivity  ──►  MainActivity  ──►  DashboardActivity
                                             │
              ┌──────────────────────────────┤
              │            Bottom Navigation │
              ▼                              │
         MapActivity                   QrScanActivity
              │                              │
              ▼                              ▼
    PlaceDetailActivity             (dialogo de éxito)
    (reseñas + estrellas)
              
         RewardsActivity
         ProfileActivity
```

### Navegación principal
La app usa un `BottomNavigationView` con 4 ítems:
- **Dashboard** (`nav_home`) — puntos y notificaciones recientes
- **Mapa** (`nav_map`) — explorar locales
- **QR** (`nav_qr`) — escanear ticket
- **Recompensas** (`nav_rewards`) — canjear puntos

---

## Autenticación

Soporta dos métodos:

### 1. Google Sign-In (Firebase)
- Usa `GoogleSignInOptions` + `FirebaseAuth.signInWithCredential`
- El token de Google se intercambia por una sesión Firebase
- El email se guarda en `SharedPreferences` bajo la clave `email_auth`
- En el backend: si el email ya existe, retorna `{existing: true}` sin error

### 2. Email + Contraseña
- Registro y login manual mediante formulario
- Las contraseñas se hashean en el Worker con **PBKDF2-SHA256** + salt aleatorio de 16 bytes
- Se almacena en DB como `salt:hash` en `user_data.password_hash`
- El login llama a `POST /login-email` y la sesión se guarda en `SharedPreferences`

### Obtención del email activo (patrón común en todas las Activities)
```java
FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
if (user != null) {
    userEmail = user.getEmail();
} else {
    userEmail = getSharedPreferences("guander_prefs", MODE_PRIVATE)
                    .getString("email_auth", null);
}
```

---

## API REST — Cloudflare Worker

**Base URL:** `https://guander-api.tomas-gonzalezz.workers.dev`

Todos los endpoints devuelven `Content-Type: application/json` con CORS abierto (`Access-Control-Allow-Origin: *`).

### Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/register` | Registra usuario nuevo. Envía email de bienvenida (Resend). |
| `POST` | `/login-email` | Login con email y contraseña. |
| `GET` | `/dashboard?email=` | Puntos actuales + últimas notificaciones del usuario. |
| `GET` | `/profile?email=` | Datos de perfil: nombre, apellido, teléfono, foto. |
| `PUT` | `/profile` | Actualiza datos de perfil. |
| `POST` | `/sign-upload` | Genera URL firmada de Cloudflare R2 para subir foto. |
| `GET` | `/places?lat=&lng=&filter=` | Lista de locales con distancia calculada en el Worker. |
| `GET` | `/comments?placeId=&placeType=&email=` | Comentarios de un local + flag `canComment` + `alreadyCommented`. |
| `POST` | `/review` | Publica reseña (1 por usuario por local). |
| `POST` | `/validate-qr` | Valida QR escaneado, registra compra y suma puntos. |
| `GET` | `/rewards?email=` | Lista de recompensas disponibles con puntos del usuario. |
| `POST` | `/redeem` | Canjea una recompensa deduciendo puntos. |
| `GET` | `/redeem-history?email=` | Historial de canjes del usuario. |

### Detalle de endpoints clave

#### `POST /register`
```json
// Request
{ "email": "...", "name": "...", "lastName": "...", "tel": "...", "password": "..." }

// Response 201
{ "success": true, "userId": 42, "userDataId": 38 }

// Response 409
{ "error": "Email ya registrado. Intentá iniciar sesión." }
```
Después del INSERT exitoso llama internamente a `sendWelcomeEmail()` via Resend (no bloqueante).

#### `POST /validate-qr`
```json
// Request
{ "email": "user@mail.com", "qrData": "{\"type\":\"store\",\"id\":5,\"name\":\"Bar Roma\",\"amount\":2500,\"secret\":\"guander2026\"}" }

// Response 200
{
  "success": true,
  "storeName": "Bar Roma",
  "pointsEarned": 2,
  "newBalance": 150
}
```
Fórmula de puntos: `max(1, floor(amount / 1000))`.

El campo `secret` en el QR debe ser `"guander2026"` para ser válido.

#### `GET /comments`
```json
// Response
{
  "placeId": 5,
  "placeName": "Bar Roma",
  "totalComments": 3,
  "canComment": true,        // true si visitó pero aún no comentó
  "alreadyCommented": false, // true si ya dejó reseña
  "comments": [
    { "id": 1, "authorName": "Juan", "rating": 4, "comment": "Muy bueno", "date": "Hace 2 días" }
  ]
}
```

#### `POST /review`
- Bloquea duplicados: devuelve `409` si el usuario ya comentó ese local.
- Requiere que el usuario tenga al menos una compra registrada en el local.

---

## Base de datos (Cloudflare D1 — SQLite)

### Tablas principales

```sql
user_data        -- datos personales (nombre, email, tel, password_hash, photo_url)
users            -- cuenta de acceso (username, fk_user_data, fk_rol)
customer         -- puntos acumulados (points, fk_user)
stores           -- locales tipo tienda/restaurante/servicio
professionals    -- locales tipo profesional (veterinarios, etc.)
store_purchase   -- historial de compras en stores (fecha, monto, puntos)
prof_purchase    -- historial de compras en professionals
comments_store   -- reseñas de stores (body, stars, fecha, fk_customer_id, fk_store_id)
comments_prof    -- reseñas de professionals
rewards          -- catálogo de recompensas (nombre, descripción, costo en puntos)
redeem_history   -- historial de canjes (fk_customer, fk_reward, fecha)
points_history   -- log de movimientos de puntos (descripción, points_change)
```

### Tipos de locales (`place_type`)
- `store` → tablas `stores`, `store_purchase`, `comments_store`
- `professional` → tablas `professionals`, `prof_purchase`, `comments_prof`

---

## Sistema de puntos

1. El usuario escanea el QR del local con la app.
2. El QR contiene un JSON con `type`, `id`, `name`, `amount`, `item` y `secret`.
3. El Worker valida el `secret`, calcula los puntos y los registra en `store_purchase` / `prof_purchase`.
4. Se actualiza `customer.points` y se inserta en `points_history`.
5. El dialog de éxito muestra el nombre del local, monto, puntos ganados y balance nuevo.

---

## Fotos de perfil y locales

- El usuario selecciona una imagen desde la galería en `ProfileActivity`.
- La app llama a `POST /sign-upload` para obtener una URL pre-firmada de **Cloudflare R2**.
- La imagen se sube directamente al bucket R2 vía `PUT` HTTP desde la app.
- La URL pública resultante se guarda en `user_data.photo_url` (o en el local correspondiente).
- En las cards de locales (`MapActivity`) se carga la foto con **Glide** usando `CircleCrop`. Si no hay foto, se muestra la inicial de categoría sobre un círculo de color.

---

## Email de bienvenida (Resend)

- Se envía al completar el registro exitoso (`POST /register`).
- La API key de Resend se almacena como **Wrangler secret** (`RESEND_API_KEY`), nunca en el código.
- El envío es **no bloqueante**: si falla, el registro igual se completa.
- Remitente: `Guander <onboarding@resend.dev>` (dominio verificado pendiente).

---

## Mapa de locales

- Usa **OpenStreetMap** via la librería `osmdroid`.
- Al abrir `MapActivity` solicita permiso de ubicación (`ACCESS_FINE_LOCATION`).
- Llama a `GET /places?lat=&lng=&filter=` — el Worker calcula la distancia en el servidor usando la fórmula de Haversine.
- Los locales se muestran como marcadores en el mapa y en lista debajo.
- Filtros: Todos / Locales / Restaurantes / Profesionales / Servicios. El filtro empieza **colapsado**.
- Al tocar un marcador se abre un `BottomSheetDialog` con resumen del local.

---

## Reseñas y estrellas

- Solo puede comentar un usuario que tenga al menos una compra registrada en ese local.
- Cada usuario puede dejar **una sola reseña** por local.
- El `RatingBar` permite seleccionar de 1 a 5 estrellas junto con un comentario de texto libre.
- La API bloquea duplicados con un check previo al INSERT y devuelve `409` si ya existe.
- La app muestra estados diferenciados:
  - **Sin visita:** "No puedes comentar aún — Debés visitar y consumir en este lugar"
  - **Ya comentó:** "Ya dejaste tu reseña — Solo se permite una reseña por lugar"
  - **Puede comentar:** muestra el formulario de estrellas + texto

---

## Permisos Android

| Permiso | Uso |
|---------|-----|
| `INTERNET` | Todas las llamadas HTTP a la API |
| `CAMERA` | Escáner QR |
| `ACCESS_FINE_LOCATION` | Ubicación precisa para el mapa |
| `ACCESS_COARSE_LOCATION` | Ubicación aproximada (fallback) |
| `READ_MEDIA_IMAGES` | Selector de foto de perfil (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Selector de foto (Android ≤ 12) |
| `WRITE_EXTERNAL_STORAGE` | Cache de tiles del mapa (Android ≤ 9) |
| `ACCESS_NETWORK_STATE` | Verificación de conectividad (osmdroid) |

---

## Configuración y secretos

| Variable | Dónde vive | Descripción |
|----------|-----------|-------------|
| `RESEND_API_KEY` | Wrangler secret (cifrado) | API key de Resend para emails |
| `google-services.json` | `app/` | Config de Firebase (no commitear) |
| `local.properties` | raíz del proyecto | SDK path (no commitear) |
| `secret` en QR | hardcoded en los QRs de locales | `"guander2026"` — validación básica de QR |

---

## Cómo desplegar el Worker

```bash
cd cloudflare-worker

# Primera vez: configurar el secret de Resend
echo "tu_api_key" | npx wrangler secret put RESEND_API_KEY

# Desplegar
npx wrangler deploy
```

## Cómo compilar la app

```bash
# Debug APK
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```
