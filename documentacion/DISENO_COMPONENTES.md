# Diseño de Componentes — Guander

## 1. Vista General de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                      App Android (Java)                        │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ Presentación│  │   Lógica    │  │     Datos / Red         │ │
│  │   (UI)      │  │  de Negocio │  │                         │ │
│  │             │  │             │  │  ┌─────────────────────┐ │ │
│  │ Activities  │  │ Validación  │  │  │ HTTP (URLConnection)│ │ │
│  │ Layouts XML │  │ Navegación  │  │  │ Firebase Auth       │ │ │
│  │ Dialogs     │  │ Sesión      │  │  │ SharedPreferences   │ │ │
│  │ BottomNav   │  │ Permisos    │  │  │ Cloudinary Upload   │ │ │
│  │             │  │             │  │  └─────────────────────┘ │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Cloudflare Worker API                         │
│                                                                 │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────┐ │
│  │  Router  │  │   Handlers   │  │  Queries │  │ Servicios  │ │
│  │          │  │              │  │   (D1)   │  │ Externos   │ │
│  │ GET/POST │  │ Auth         │  │          │  │            │ │
│  │ PUT      │  │ Dashboard    │  │ Prepared │  │ Cloudinary │ │
│  │ OPTIONS  │  │ Places       │  │ Stmts    │  │ Resend     │ │
│  │          │  │ Reviews      │  │          │  │            │ │
│  │          │  │ Rewards      │  │          │  │            │ │
│  │          │  │ QR           │  │          │  │            │ │
│  │          │  │ Profile      │  │          │  │            │ │
│  └──────────┘  └──────────────┘  └──────────┘  └────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Componentes del Cliente Android

### 2.1 GuanderApp (Application)

**Archivo:** `com.example.guander.GuanderApp`

**Responsabilidad:** Inicialización global de la aplicación.

```
┌────────────────────────────────────────┐
│            GuanderApp                  │
│         (Application)                  │
├────────────────────────────────────────┤
│ - prefs: SharedPreferences            │
├────────────────────────────────────────┤
│ + onCreate(): void                    │
│   → Carga SharedPreferences           │
│   → Inicializa osmdroid config        │
│   → Aplica tema e idioma              │
│ - applyTheme(): void                  │
│   → 0=sistema, 1=claro, 2=oscuro     │
│ - applyLocale(): void                 │
│   → Cambia Locale global (def: "es")  │
└────────────────────────────────────────┘
```

**Diagrama de inicialización:**
```
App Launch
    │
    ▼
GuanderApp.onCreate()
    │
    ├── SharedPreferences("guander_prefs")
    │
    ├── Configuration.getInstance()
    │   └── osmdroid user agent
    │
    ├── applyTheme()
    │   ├── mode 0 → MODE_NIGHT_FOLLOW_SYSTEM
    │   ├── mode 1 → MODE_NIGHT_NO
    │   └── mode 2 → MODE_NIGHT_YES
    │
    └── applyLocale()
        └── Locale(language) → createConfigurationContext()
```

---

### 2.2 LoginActivity

**Archivo:** `com.example.guander.LoginActivity`

**Responsabilidad:** Autenticación del usuario (Google OAuth / Email).

```
┌────────────────────────────────────────────┐
│             LoginActivity                  │
├────────────────────────────────────────────┤
│ - mAuth: FirebaseAuth                     │
│ - googleSignInClient: GoogleSignInClient  │
│ - workerUrl: String                       │
│ - isEmailFormVisible: boolean             │
├────────────────────────────────────────────┤
│ + onCreate(): void                        │
│ + onStart(): void                         │
│   → Auto-login si sesión activa           │
│ - signInWithGoogle(): void                │
│ - firebaseAuthWithGoogle(token): void     │
│ - handleEmailLogin(): void                │
│ - handleEmailRegister(): void             │
│ - registerWithCloudflare(...): void       │
│ - saveUserToWorker(..., callback): void   │
│ - mapAuthError(message): String           │
│ - setLoading(loading): void               │
│ - goToWelcome(): void                     │
└────────────────────────────────────────────┘
```

**Flujo interno:**
```
                    LoginActivity
                         │
           ┌─────────────┼──────────────┐
           ▼             ▼              ▼
     Google Sign-In  Email Login   Email Register
           │             │              │
           ▼             │              │
     Firebase Auth       │              │
     (ID Token)          │              │
           │             │              │
           ▼             ▼              ▼
     POST /register  POST /login   POST /register
     (silent)        -email        (con password)
           │             │              │
           └─────────────┼──────────────┘
                         ▼
                  DashboardActivity
```

**Componentes UI:**
- `btn_google` — MaterialButton para Google Sign-In
- `btn_toggle_email` — MaterialButton para expandir formulario email
- `et_email`, `et_password` — TextInputEditText
- `btn_login`, `btn_register` — Botones dentro del formulario
- `progress_bar` — Indicador de carga
- `AlertDialog` — Diálogo para datos de registro (nombre, apellido, teléfono)

---

### 2.3 DashboardActivity

**Archivo:** `com.example.guander.DashboardActivity`

**Responsabilidad:** Pantalla principal con resumen del usuario.

```
┌────────────────────────────────────────────┐
│           DashboardActivity                │
├────────────────────────────────────────────┤
│ - mAuth: FirebaseAuth                     │
│ - workerUrl: String                       │
│ - userEmail: String                       │
├────────────────────────────────────────────┤
│ + onCreate(): void                        │
│ - fetchDashboardData(email): void         │
│ - updatePoints(points): void              │
│ - updateNotifications(JSONArray): void    │
│ - logout(): void                          │
│ - setupBottomNav(): void                  │
└────────────────────────────────────────────┘
```

**Layout:**
```
┌─────────────────────────────────────┐
│  MaterialToolbar                    │
├─────────────────────────────────────┤
│  ┌─────┐  ¡Hola, [nombre]!        │
│  │ 🐾  │  avatar circular         │
│  └─────┘                           │
├─────────────────────────────────────┤
│        ★ 1,250 PetPoints ★         │
│        [    Canjear    ]            │
├─────────────────────────────────────┤
│  Notificaciones                     │
│  ┌──────────────────────────┐      │
│  │ 🎉 Título  │ Descripción │      │
│  │ 🐶 Título  │ Descripción │      │
│  │ 🎁 Título  │ Descripción │      │
│  └──────────────────────────┘      │
├─────────────────────────────────────┤
│  Acceso rápido                      │
│  ┌──────────┐  ┌──────────────┐    │
│  │ 📍Lugares│  │ 🎁Recompensas│    │
│  └──────────┘  └──────────────┘    │
├─────────────────────────────────────┤
│  Inicio │ Mapa │ QR │ Puntos│Perfil│
└─────────────────────────────────────┘
```

---

### 2.4 MapActivity

**Archivo:** `com.example.guander.MapActivity`

**Responsabilidad:** Mapa interactivo con listado y filtrado de establecimientos.

```
┌────────────────────────────────────────────────┐
│              MapActivity                       │
├────────────────────────────────────────────────┤
│ - mapView: MapView                            │
│ - allPlaces: List<JSONObject>                 │
│ - filteredPlaces: List<JSONObject>            │
│ - userLat, userLng: double                    │
│ - activeFilter: String                        │
│ - searchQuery: String                         │
├────────────────────────────────────────────────┤
│ + onCreate(): void                            │
│ - setupMap(): void                            │
│ - requestLocationAndLoad(): void              │
│ - getLastLocation(): void                     │
│ - loadPlaces(): void                          │
│ - renderPlaces(List<JSONObject>): void         │
│ - applyFilterAndSearch(): void                │
│ - setActiveFilter(category): void             │
│ - updateMapMarkers(List<JSONObject>): void     │
│ - applyPlaceIcon(category, view): void        │
│ + onRequestPermissionsResult(): void          │
└────────────────────────────────────────────────┘
```

**Diagrama de flujo de datos:**
```
                  MapActivity
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
    requestLocation  setupMap   setupBottomNav
          │           │
          ▼           │
    getLastLocation   │
    (GPS/Network)     │
          │           │
          ▼           │
    loadPlaces()──────┘
    GET /places?      │
    lat=X&lng=Y       │
          │           │
          ▼           ▼
    allPlaces[] ──► renderPlaces()
          │              │
          │         ┌────┴────┐
          │         ▼         ▼
          │    inflateCards  updateMapMarkers
          │         │              │
          │    ┌────┴────┐    Marker overlay
          │    ▼         ▼         │
          │  title    distance     │
          │  stars    schedule     │
          │  status   category    │
          │         │              │
          └─────────┼──────────────┘
                    │
              ┌─────┴──────┐
              ▼            ▼
        filterPanel    searchInput
              │            │
              └─────┬──────┘
                    ▼
          applyFilterAndSearch()
                    │
                    ▼
            filteredPlaces[]
                    │
                    ▼
           renderPlaces(filtered)
```

**Categorías y sus íconos:**

| Categoría | Emoji | Color |
|-----------|-------|-------|
| restaurant | 🍽️ | Deep Orange `#FF5722` |
| store | 🛒 | Teal `#009688` |
| service | ✂️ | Purple `#9C27B0` |
| professional | 🏥 | Blue `#2196F3` |
| default | 📍 | Grey `#607D8B` |

---

### 2.5 PlaceDetailActivity

**Archivo:** `com.example.guander.PlaceDetailActivity`

**Responsabilidad:** Vista detallada de un establecimiento con sistema de reseñas.

```
┌────────────────────────────────────────────────┐
│          PlaceDetailActivity                   │
├────────────────────────────────────────────────┤
│ - placeId: int                                │
│ - placeType: String                           │
│ - userEmail: String                           │
│ - canComment: boolean                         │
│ - alreadyCommented: boolean                   │
├────────────────────────────────────────────────┤
│ + onCreate(): void                            │
│ - loadPlaceDetails(): void                    │
│ - loadComments(): void                        │
│ - renderComments(JSONArray): void             │
│ - submitComment(): void                       │
│ - applyPlaceIcon(category): void              │
│ - openDirections(): void                      │
└────────────────────────────────────────────────┘
```

**Estados del formulario de reseña:**
```
┌──────────────────────────────────────────┐
│ Estado 1: Sin compra previa              │
│ ┌──────────────────────────────────────┐ │
│ │  🔒 No puedes comentar aún          │ │
│ │  Necesitas una compra previa         │ │
│ └──────────────────────────────────────┘ │
├──────────────────────────────────────────┤
│ Estado 2: Con compra, sin reseña         │
│ ┌──────────────────────────────────────┐ │
│ │  ★★★★☆  RatingBar (1-5)             │ │
│ │  [Escribe tu comentario...]          │ │
│ │  [    Enviar reseña    ]             │ │
│ └──────────────────────────────────────┘ │
├──────────────────────────────────────────┤
│ Estado 3: Ya comentó                     │
│ ┌──────────────────────────────────────┐ │
│ │  ✅ Ya dejaste tu reseña             │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

**Diseño del item de reseña:**
```
┌────────────────────────────────────────────┐
│ ┌──┐  Juan Pérez              Hace 2 días │
│ │JP│  ★★★★☆                              │
│ └──┘  "Excelente lugar, mi perro          │
│        fue muy bien recibido"              │
├────────────────────────────────────────────┤
│ ┌──┐  María García                   Hoy  │
│ │MG│  ★★★★★                              │
│ └──┘  "Increíble atención pet-friendly"   │
└────────────────────────────────────────────┘
```

---

### 2.6 QrScanActivity

**Archivo:** `com.example.guander.QrScanActivity`

**Responsabilidad:** Escaneo de códigos QR y acreditación de PetPoints.

```
┌────────────────────────────────────────────────┐
│            QrScanActivity                      │
├────────────────────────────────────────────────┤
│ - barcodeView: BarcodeView                    │
│ - isProcessing: boolean                       │
│ - userEmail: String                           │
├────────────────────────────────────────────────┤
│ + onCreate(): void                            │
│ - requestCameraAndStart(): void               │
│ - startScanning(): void                       │
│ - processQrCode(qrText): void                │
│ - showSuccessDialog(response): void           │
│ - resetScanner(): void                        │
│ + onRequestPermissionsResult(): void          │
│ + onResume() / onPause(): void                │
└────────────────────────────────────────────────┘
```

**Flujo del escaneo:**
```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  Placeholder │────▶│  Camera View │────▶│  Processing  │
│  "Activar   │     │  (BarcodeView)│     │  QR JSON     │
│   cámara"   │     │  decodeSingle │     │  parse +     │
│             │     │              │     │  validate    │
└─────────────┘     └──────────────┘     └──────┬───────┘
                                                │
                           ┌────────────────────┤
                           ▼                    ▼
                    ┌──────────────┐     ┌──────────────┐
                    │ Error Toast  │     │ POST         │
                    │ "QR inválido"│     │ /validate-qr │
                    │              │     │              │
                    └──────┬───────┘     └──────┬───────┘
                           │                    │
                           ▼                    ▼
                    ┌──────────────┐     ┌──────────────┐
                    │ resetScanner │     │ Success      │
                    │ (volver a   │     │ Dialog       │
                    │  escanear)  │     │ +X puntos    │
                    └──────────────┘     │ nuevo saldo  │
                                        └──────┬───────┘
                                               │
                                               ▼
                                        ┌──────────────┐
                                        │ resetScanner │
                                        └──────────────┘
```

**Dialog de éxito:**
```
┌──────────────────────────────────────┐
│         ✅ ¡Puntos sumados!          │
│                                      │
│  🏪 Bar Roma                         │
│  📦 Cerveza                          │
│  💰 $2,500                           │
│                                      │
│      +2 PetPoints                    │
│      Nuevo saldo: 152               │
│                                      │
│         [  Aceptar  ]               │
└──────────────────────────────────────┘
```

---

### 2.7 RewardsActivity

**Archivo:** `com.example.guander.RewardsActivity`

**Responsabilidad:** Catálogo de recompensas y historial de canjes.

```
┌────────────────────────────────────────────────┐
│           RewardsActivity                      │
├────────────────────────────────────────────────┤
│ - allRewards: List<JSONObject>                │
│ - userPoints: int                             │
│ - userEmail: String                           │
│ - isCanjearTab: boolean                       │
├────────────────────────────────────────────────┤
│ + onCreate(): void                            │
│ - loadRewards(): void                         │
│ - loadHistory(): void                         │
│ - switchTab(canjear): void                    │
│ - renderRewards(List<JSONObject>): void        │
│ - filterRewards(query): void                  │
│ - renderHistory(JSONArray): void              │
│ - showConfirmDialog(...): void                │
│ - applyRewardIcon(storeId, view): void        │
└────────────────────────────────────────────────┘
```

**Sistema de pestañas:**
```
┌────────────────────────────────────────────────┐
│  [  Canjear  ]     [  Historial  ]             │
│  ────────────       ─ ─ ─ ─ ─ ─               │
├────────────────────────────────────────────────┤
│                                                │
│  Tab "Canjear":                                │
│  ┌──────────────────────────────────────────┐  │
│  │ 🐾 Descuento Mundo Animal               │  │
│  │    10% en alimento premium               │  │
│  │                        150 pts [Canjear] │  │
│  ├──────────────────────────────────────────┤  │
│  │ ☕ Café gratis Ladrido                   │  │
│  │    Un café de cortesía                   │  │
│  │                        200 pts [Canjear] │  │
│  └──────────────────────────────────────────┘  │
│                                                │
│  Tab "Historial":                              │
│  ┌──────────────────────────────────────────┐  │
│  │ − 150 pts  │ Canje cupón     │ Hace 3 días│ │
│  │ + 5 pts    │ Compra en local │ Hoy        │ │
│  └──────────────────────────────────────────┘  │
└────────────────────────────────────────────────┘
```

**Mapeo de íconos por tienda:**

| ID | Tienda | Emoji | Color |
|----|--------|-------|-------|
| 1 | Mundo Animal | 🐾 | `#795548` (Brown) |
| 2 | Clínica Patas | 🏥 | `#2196F3` (Blue) |
| 3 | Café Ladrido | ☕ | `#FF9800` (Orange) |
| 4 | PetSpa | ✂️ | `#9C27B0` (Purple) |
| 5 | PetResort | 🏨 | `#009688` (Teal) |
| 6 | PetParadise | 🛒 | `#E91E63` (Pink) |
| 7 | Restaurante Woof | 🍽️ | `#FF5722` (Deep Orange) |
| 8 | MascotaExpress | 📦 | `#607D8B` (Grey) |

---

### 2.8 ProfileActivity y EditProfileActivity

**Archivo:** `com.example.guander.ProfileActivity`, `EditProfileActivity`

**Responsabilidad:** Visualización y edición del perfil de usuario.

```
┌──────────────────────────────────────────┐
│          ProfileActivity                 │
├──────────────────────────────────────────┤
│ - mAuth: FirebaseAuth                   │
│ - userEmail: String                     │
├──────────────────────────────────────────┤
│ + onCreate(): void                      │
│ - loadProfileData(): void               │
│ - updateUI(JSONObject): void            │
│ - handleImageSelected(Uri): void        │
│ - uploadToCloudinary(...): void         │
│ - savePhotoUrl(url): void               │
│ - setRow(rowId, icon, label, value):void│
│ - navigate(viewId, target): void        │
│ - logout(): void                        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│        EditProfileActivity               │
├──────────────────────────────────────────┤
│ - userEmail: String                     │
├──────────────────────────────────────────┤
│ + onCreate(): void                      │
│ - loadCurrentProfile(): void            │
│ - saveProfile(): void                   │
│ - handleImageSelected(Uri): void        │
│ - uploadToCloudinary(...): void         │
│ - validateFields(): boolean             │
└──────────────────────────────────────────┘
```

**Layout del perfil:**
```
┌─────────────────────────────────────────┐
│  ← Perfil                      [Edit]  │
├─────────────────────────────────────────┤
│           ┌──────┐                      │
│           │  📷  │  ← foto / inicial   │
│           └──────┘                      │
│        Juan Pérez                       │
│        juan@email.com                   │
├─────────────────────────────────────────┤
│  📍 Lugares visitados        12         │
│  ⭐ PetPoints               1,250       │
│  🎁 Cupones canjeados       5          │
├─────────────────────────────────────────┤
│  📧 Email          juan@email.com       │
│  📱 Teléfono       +54 11 1234-5678    │
│  📍 Dirección      Av. Corrientes 1234 │
│  📅 Miembro desde  Ene 2025            │
├─────────────────────────────────────────┤
│  🔔 Notificaciones              >      │
│  🔒 Privacidad y seguridad      >      │
│  ❓ Centro de ayuda              >      │
│  📄 Términos y condiciones       >      │
│  🛡️ Política de privacidad      >      │
├─────────────────────────────────────────┤
│  [      Cerrar sesión      ]            │
└─────────────────────────────────────────┘
```

**Flujo de subida de foto:**
```
Usuario selecciona imagen
         │
         ▼
  POST /sign-upload ──► Worker genera firma SHA-1
         │                    │
         │ ◄── { timestamp,   │
         │      signature,    │
         │      apiKey,       │
         │      cloudName }   │
         │                    │
         ▼
  POST upload ──────────────► Cloudinary
  multipart/form-data              │
  (file + signature + ts)          │
         │                         │
         │ ◄── { secure_url }      │
         │                         │
         ▼
  PUT /profile ──────────────► Worker
  { email, photoUrl }              │
         │                         │
         │ ◄── 200 OK              │
         ▼
  Actualizar UI con Glide
```

---

### 2.9 Actividades de Configuración

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│   NotificationsSettingsActivity                               │
│   ├── SwitchMaterial: notif_general (master)                 │
│   ├── SwitchMaterial: notif_points                           │
│   ├── SwitchMaterial: notif_coupons                          │
│   ├── SwitchMaterial: notif_places                           │
│   └── SwitchMaterial: notif_promo                            │
│       → Hijos habilitados solo si master está ON              │
│       → Persistencia: SharedPreferences("guander_prefs")      │
│                                                               │
│   PrivacySecurityActivity                                     │
│   ├── SwitchMaterial: privacy_location                       │
│   ├── SwitchMaterial: privacy_analytics                      │
│   ├── SwitchMaterial: privacy_personalized                   │
│   ├── InfoCard: "🔐 Cuenta vinculada con Google"             │
│   └── InfoCard: "🛡️ Protección de datos"                     │
│                                                               │
│   HelpCenterActivity                                          │
│   └── 7 FAQs expandibles (programáticas)                     │
│                                                               │
│   TermsActivity                                               │
│   └── 8 secciones de texto (programáticas)                   │
│                                                               │
│   PrivacyPolicyActivity                                       │
│   └── 8 secciones de texto (programáticas)                   │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## 3. Componentes del Backend (Cloudflare Worker)

### 3.1 Estructura del Worker

```
worker.js
│
├── export default { fetch(request, env) }
│   │
│   ├── CORS Preflight (OPTIONS) ──► 200 con headers
│   │
│   ├── Router (pathname + method)
│   │   │
│   │   ├── POST /register ──────► handleRegister(body, env)
│   │   ├── POST /login-email ───► handleLoginEmail(body, env)
│   │   ├── GET  /dashboard ─────► handleDashboard(params, env)
│   │   ├── GET  /profile ───────► handleProfile(params, env)
│   │   ├── PUT  /profile ───────► handleUpdateProfile(body, env)
│   │   ├── POST /sign-upload ───► handleSignUpload(env)
│   │   ├── GET  /places ────────► handlePlaces(params, env)
│   │   ├── GET  /comments ──────► handleComments(params, env)
│   │   ├── POST /review ────────► handleReview(body, env)
│   │   ├── GET  /rewards ───────► handleRewards(params, env)
│   │   ├── POST /redeem ────────► handleRedeem(body, env)
│   │   ├── GET  /redeem-history ► handleRedeemHistory(params, env)
│   │   └── POST /validate-qr ──► handleValidateQr(body, env)
│   │
│   └── 404 Not Found (ruta no encontrada)
│
├── Funciones auxiliares
│   ├── hashPassword(password) ──► PBKDF2-SHA256
│   ├── verifyPassword(pwd, hash) ► boolean
│   ├── corsHeaders() ──────────► headers CORS
│   ├── haversine(lat1,lng1,lat2,lng2) ► distancia km
│   └── sendWelcomeEmail(email, name, env) ► Resend API
│
└── Bindings (env)
    ├── env.DB ──────────────► Cloudflare D1
    ├── env.RESEND_API_KEY ──► Resend secret
    ├── env.CLOUDINARY_API_KEY ► Cloudinary key
    ├── env.CLOUDINARY_API_SECRET ► Cloudinary secret
    └── env.CLOUDINARY_CLOUD_NAME ► Cloudinary cloud
```

### 3.2 Módulo de Autenticación

```
┌────────────────────────────────────────────────────────┐
│  handleRegister(body, env)                             │
│                                                        │
│  Entrada: { email, name?, lastName?, tel?, password? } │
│                                                        │
│  1. Verificar si email existe en user_data             │
│     ├── Existe + tiene password → 409 "Ya registrado"  │
│     └── Existe + sin password → { existing: true }     │
│                                                        │
│  2. Si password → hashPassword(password)               │
│     └── PBKDF2-SHA256, 100k iters, 16B salt            │
│                                                        │
│  3. INSERT user_data (name, last_name, tel, email,     │
│     password_hash)                                     │
│                                                        │
│  4. INSERT users (username=email, fk_user_data,        │
│     fk_rol=2, date_reg=TODAY)                          │
│                                                        │
│  5. INSERT customer (points=0, fk_user)                │
│                                                        │
│  6. sendWelcomeEmail(email, name, env) — fire & forget │
│                                                        │
│  Salida: 201 { success, userId, userDataId }           │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│  handleLoginEmail(body, env)                           │
│                                                        │
│  Entrada: { email, password }                          │
│                                                        │
│  1. SELECT password_hash, name, last_name              │
│     FROM user_data WHERE email = ?                     │
│     └── No existe → 401 "Credenciales incorrectas"     │
│                                                        │
│  2. verifyPassword(password, stored_hash)              │
│     └── No coincide → 401 "Credenciales incorrectas"   │
│                                                        │
│  Salida: 200 { success, email, name, lastName }        │
└────────────────────────────────────────────────────────┘
```

### 3.3 Módulo de Lugares

```
┌────────────────────────────────────────────────────────┐
│  handlePlaces(params, env)                             │
│                                                        │
│  Entrada: ?lat={lat}&lng={lng}                         │
│                                                        │
│  1. SELECT stores con JOIN category, schedule          │
│     → Mapeo: category.name → "restaurant"|"store"|    │
│       "service"                                        │
│     → place_type = "store"                             │
│                                                        │
│  2. SELECT professionals con JOIN user_data            │
│     → category = "service"                             │
│     → place_type = "professional"                      │
│                                                        │
│  3. Para cada lugar:                                   │
│     distance_km = haversine(userLat, userLng,          │
│                             placeLat, placeLng)        │
│                                                        │
│  4. Concatenar arrays y ordenar por distance_km ASC    │
│                                                        │
│  Salida: { places: [...] }                             │
└────────────────────────────────────────────────────────┘
```

### 3.4 Módulo de Reseñas

```
┌────────────────────────────────────────────────────────┐
│  handleComments(params, env)                           │
│                                                        │
│  Verificaciones:                                       │
│  ┌─────────────────────────────────────────┐           │
│  │ canComment = tiene compra previa        │           │
│  │             AND no tiene reseña existente│           │
│  │                                         │           │
│  │ alreadyCommented = tiene reseña         │           │
│  └─────────────────────────────────────────┘           │
│                                                        │
│  Tablas según placeType:                               │
│  ├── "store" → comments_store + store_purchase         │
│  └── "professional" → comments_prof + prof_purchase    │
│                                                        │
│  Fechas relativas:                                     │
│  ├── diff == 0 → "Hoy"                                │
│  ├── diff == 1 → "Hace 1 día"                         │
│  └── diff > 1  → "Hace N días"                        │
└────────────────────────────────────────────────────────┘
```

### 3.5 Módulo de QR y Puntos

```
┌────────────────────────────────────────────────────────┐
│  handleValidateQr(body, env)                           │
│                                                        │
│  Entrada: { email, qrData (JSON string) }              │
│                                                        │
│  1. JSON.parse(qrData)                                 │
│     → { type, id, name, amount, item, secret }         │
│                                                        │
│  2. Validar: secret === "guander2026"                  │
│     └── Fallo → 400 "QR inválido"                      │
│                                                        │
│  3. Calcular: pointsEarned = max(1, floor(amount/1000))│
│                                                        │
│  4. SELECT customer WHERE fk_user → user → user_data   │
│     WHERE email = ?                                    │
│                                                        │
│  5. INSERT store_purchase / prof_purchase               │
│     (date, amount, points_earn, fk_store, fk_customer) │
│                                                        │
│  6. UPDATE customer SET points = points + pointsEarned │
│                                                        │
│  7. INSERT points_history                              │
│     (description="Consumo en {name}",                  │
│      points_change=+pointsEarned)                      │
│                                                        │
│  Salida: 200 { success, storeName, item, amount,       │
│               pointsEarned, newBalance }               │
└────────────────────────────────────────────────────────┘
```

### 3.6 Módulo de Recompensas

```
┌────────────────────────────────────────────────────────┐
│  handleRedeem(body, env)                               │
│                                                        │
│  Entrada: { email, couponId, couponType }              │
│                                                        │
│  Validaciones:                                         │
│  ├── 1. Cupón existe y está activo (state=1)           │
│  ├── 2. No expirado (expiration_date >= today)         │
│  ├── 3. No duplicado (no en coupon_buy_store)          │
│  └── 4. Puntos suficientes (customer.points >= cost)   │
│                                                        │
│  Transacción:                                          │
│  ├── UPDATE customer SET points -= point_req           │
│  ├── INSERT coupon_buy_store                           │
│  └── INSERT points_history                             │
│       (points_change = -point_req,                     │
│        redemption_code = code_coupon)                  │
│                                                        │
│  Salida: 200 { success, code, remainingPoints }        │
│         400 { error: "Puntos insuficientes" }          │
└────────────────────────────────────────────────────────┘
```

---

## 4. Componentes de UI Reutilizables

### 4.1 Layouts de Items (Inflados Dinámicamente)

| Layout | Usado en | Contenido |
|--------|----------|-----------|
| `item_place.xml` | MapActivity | Tarjeta de establecimiento con foto, nombre, distancia, estrellas, horarios |
| `item_comment.xml` | PlaceDetailActivity | Reseña con avatar circular, autor, fecha, estrellas, texto |
| `item_notification.xml` | DashboardActivity | Notificación con emoji, título y descripción |
| `item_reward.xml` | RewardsActivity | Recompensa con ícono, nombre, descripción, costo y botón de canje |
| `item_reward_history.xml` | RewardsActivity | Entrada de historial con ícono +/−, puntos, fecha y código |
| `item_info_row.xml` | ProfileActivity | Fila de información con ícono, etiqueta y valor |
| `item_switch_setting.xml` | Notifications/Privacy | Toggle switch con etiqueta |

### 4.2 Diálogos Personalizados

| Layout | Usado en | Propósito |
|--------|----------|-----------|
| `dialog_qr_success.xml` | QrScanActivity | Muestra puntos ganados y detalles de la compra |
| `dialog_confirm_redeem.xml` | RewardsActivity | Confirmación antes de canjear puntos |
| `dialog_redeem_success.xml` | RewardsActivity | Código de canje tras canjeo exitoso |
| `dialog_register_name.xml` | LoginActivity | Solicita nombre/apellido/teléfono al registrarse con email |

### 4.3 Navegación (BottomNavigationView)

```
┌─────────────────────────────────────────────────────┐
│  🏠 Inicio  │  🗺️ Mapa  │  📷 QR  │  🎁 Puntos  │  👤 Perfil  │
│  nav_inicio  │  nav_mapa  │  nav_qr │  nav_puntos │  nav_perfil │
└──────┬───────┴──────┬─────┴────┬────┴──────┬──────┴──────┬──────┘
       │              │          │           │             │
       ▼              ▼          ▼           ▼             ▼
  Dashboard      MapActivity  QrScan   RewardsActivity  Profile
  Activity                   Activity                  Activity
```

**Comportamiento:**
- Presente en: Dashboard, Map, QR, Rewards, Profile
- Al cambiar de pestaña: `startActivity()` + `finish()` (sin back stack)
- Ítem seleccionado: resaltado con color primario
- Implementación: `setOnItemSelectedListener` + switch por `itemId`

---

## 5. Comunicación entre Componentes

### 5.1 Activity → Activity (Intent Extras)

```
MapActivity ──────────────────────► PlaceDetailActivity
   Intent extras:
   ├── PLACE_ID (int)
   ├── PLACE_NAME (String)
   ├── PLACE_CATEGORY (String)
   ├── PLACE_TYPE (String)
   ├── PLACE_LAT, PLACE_LNG (double)
   ├── PLACE_DESC (String)
   ├── PLACE_ADDRESS (String)
   ├── PLACE_PHONE (String)
   ├── PLACE_IS_OPEN (int)
   ├── PLACE_DISTANCE (String)
   └── PLACE_PHOTO (String)

LoginActivity ────────────────────► DashboardActivity
   (sin extras, sesión via Firebase/SharedPreferences)

ProfileActivity ──────────────────► EditProfileActivity
   (sin extras, datos cargados del API)

ProfileActivity ──────────────────► Settings Activities
   (sin extras, datos de SharedPreferences)
```

### 5.2 Persistencia Local (SharedPreferences)

```
SharedPreferences: "guander_prefs"
│
├── Sesión
│   ├── email_auth: String     ← email del usuario (login email)
│   ├── user_name: String      ← nombre para mostrar
│   └── user_email: String     ← email almacenado por ProfileActivity
│
├── Apariencia
│   ├── appearance_mode: int   ← 0=sistema, 1=claro, 2=oscuro
│   └── language: String       ← código de idioma ("es")
│
├── Notificaciones
│   ├── notif_general: boolean
│   ├── notif_points: boolean
│   ├── notif_coupons: boolean
│   ├── notif_places: boolean
│   └── notif_promo: boolean
│
└── Privacidad
    ├── privacy_location: boolean
    ├── privacy_analytics: boolean
    └── privacy_personalized: boolean
```

### 5.3 App → Worker (HTTP)

```
Patrón de comunicación (todas las Activities):
│
├── Thread en background (new Thread / ExecutorService)
│   ├── URL url = new URL(workerUrl + endpoint)
│   ├── HttpURLConnection con = url.openConnection()
│   ├── Enviar JSON payload (si POST/PUT)
│   ├── Leer response como String
│   └── Parsear con JSONObject/JSONArray
│
└── Actualizar UI en main thread
    └── runOnUiThread(() -> { ... })
```

---

## 6. Diagrama de Navegación Completo

```
                         ┌─────────────┐
                         │   Login     │
                         │  Activity   │
                         └──────┬──────┘
                                │ (autenticación exitosa)
                                ▼
┌───────────┐    ┌──────────────────────────┐    ┌───────────┐
│           │◄───│      Dashboard           │───▶│           │
│   Map     │    │      Activity            │    │  Rewards  │
│ Activity  │    │                          │    │ Activity  │
│           │    └─────┬──────────────┬─────┘    │           │
└─────┬─────┘          │              │          └───────────┘
      │                │              │
      ▼                ▼              ▼
┌───────────┐    ┌───────────┐  ┌───────────┐
│   Place   │    │  QR Scan  │  │  Profile  │
│  Detail   │    │ Activity  │  │ Activity  │
│ Activity  │    └───────────┘  └─────┬─────┘
└───────────┘                         │
                          ┌───────────┼───────────────┐
                          │           │               │
                          ▼           ▼               ▼
                   ┌───────────┐ ┌──────────┐  ┌───────────┐
                   │   Edit    │ │Notificat.│  │ Privacy   │
                   │  Profile  │ │ Settings │  │ Security  │
                   └───────────┘ └──────────┘  └───────────┘
                                                      │
                                          ┌───────────┼──────┐
                                          ▼           ▼      ▼
                                   ┌──────────┐ ┌────────┐ ┌──────┐
                                   │  Help    │ │ Terms  │ │Policy│
                                   │  Center  │ │        │ │      │
                                   └──────────┘ └────────┘ └──────┘
```

---

## 7. Resumen de Archivos de Recursos

### 7.1 Layouts (23 archivos)

| Archivo | Tipo | Descripción |
|---------|------|-------------|
| `activity_login.xml` | Activity | Formulario de login/registro |
| `activity_main.xml` | Activity | Router (legacy) |
| `activity_dashboard.xml` | Activity | Pantalla principal |
| `activity_map.xml` | Activity | Mapa + filtros + lista |
| `activity_place_detail.xml` | Activity | Detalle de lugar + reseñas |
| `activity_qr_scan.xml` | Activity | Scanner QR |
| `activity_rewards.xml` | Activity | Recompensas + historial |
| `activity_profile.xml` | Activity | Perfil de usuario |
| `activity_edit_profile.xml` | Activity | Edición de perfil |
| `activity_settings_base.xml` | Activity | Base para pantallas de settings |
| `item_place.xml` | Item | Tarjeta de lugar |
| `item_comment.xml` | Item | Reseña individual |
| `item_notification.xml` | Item | Notificación individual |
| `item_reward.xml` | Item | Tarjeta de recompensa |
| `item_reward_history.xml` | Item | Registro de historial |
| `item_info_row.xml` | Item | Fila de información |
| `item_switch_setting.xml` | Item | Toggle de configuración |
| `dialog_qr_success.xml` | Dialog | Éxito de escaneo QR |
| `dialog_confirm_redeem.xml` | Dialog | Confirmar canje |
| `dialog_redeem_success.xml` | Dialog | Éxito de canje |
| `dialog_register_name.xml` | Dialog | Datos de registro |
| `bottom_sheet_place.xml` | BottomSheet | Preview de lugar en mapa |

### 7.2 Tema de Colores (Material 3)

| Token | Claro | Oscuro |
|-------|-------|--------|
| Primary | `#1A6B2E` | (inversión automática) |
| Secondary | `#4CAF50` | (inversión automática) |
| Error | `#BA1A1A` | (inversión automática) |
| Background | Blanco | `#1A3A20` (login) |
| Surface | Blanco | Dark grey |
