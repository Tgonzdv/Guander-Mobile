# Arquitectura del Sistema — Guander

## 1. Visión General

Guander es una aplicación Android de fidelización pet-friendly que conecta usuarios con establecimientos amigables con mascotas. La arquitectura sigue un modelo **cliente-servidor** con tres capas principales:

```
┌───────────────────────────────────────────┐
│         Cliente Android (Java)            │
│  Material Design 3 · Min SDK 24 · API 36 │
└─────────────────┬─────────────────────────┘
                  │ HTTP/REST (JSON)
                  ▼
┌───────────────────────────────────────────┐
│       Cloudflare Worker (JavaScript)      │
│   Edge computing · Serverless · REST API  │
└─────────────────┬─────────────────────────┘
                  │ SQL (D1)
                  ▼
┌───────────────────────────────────────────┐
│         Cloudflare D1 (SQLite)            │
│       Base de datos relacional            │
└───────────────────────────────────────────┘
```

## 2. Diagrama de Arquitectura Completo

```
                    ┌──────────────┐
                    │  Firebase    │
                    │  Auth        │◄──── Google Sign-In
                    │  Analytics   │
                    └──────┬───────┘
                           │ Token / UID
    ┌──────────────────────┼──────────────────────┐
    │   App Android        │                      │
    │                      ▼                      │
    │  ┌─────────┐   ┌──────────┐   ┌─────────┐  │
    │  │ Login   │──▶│Dashboard │──▶│  Map    │  │
    │  │Activity │   │Activity  │   │Activity │  │
    │  └─────────┘   └────┬─────┘   └────┬────┘  │
    │                     │              │        │
    │  ┌─────────┐   ┌────┴─────┐   ┌───┴─────┐  │
    │  │ Profile │   │ Rewards  │   │  Place  │  │
    │  │Activity │   │ Activity │   │ Detail  │  │
    │  └─────────┘   └──────────┘   └─────────┘  │
    │       │                            │        │
    │  ┌────┴──────┐              ┌──────┴─────┐  │
    │  │  Edit     │              │  QR Scan   │  │
    │  │  Profile  │              │  Activity  │  │
    │  └───────────┘              └────────────┘  │
    └──────────────────┬──────────────────────────┘
                       │ HTTP (JSON)
                       ▼
    ┌──────────────────────────────────────────────┐
    │         Cloudflare Worker API                │
    │  guander-api.tomas-gonzalezz.workers.dev     │
    │                                              │
    │  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
    │  │  Auth    │  │  Places  │  │  Rewards  │  │
    │  │  Module  │  │  Module  │  │  Module   │  │
    │  └────┬─────┘  └────┬─────┘  └─────┬─────┘  │
    │       └──────────────┼──────────────┘        │
    │                      ▼                       │
    │              ┌──────────────┐                │
    │              │  Cloudflare  │                │
    │              │  D1 (SQLite) │                │
    │              └──────────────┘                │
    └──────────┬───────────────────┬───────────────┘
               │                   │
               ▼                   ▼
    ┌──────────────┐       ┌──────────────┐
    │  Cloudinary  │       │  Resend API  │
    │  (Imágenes)  │       │  (Emails)    │
    └──────────────┘       └──────────────┘
```

## 3. Componentes del Sistema

### 3.1 Cliente Android

| Aspecto | Detalle |
|---------|---------|
| **Lenguaje** | Java 11 |
| **SDK Mínimo** | API 24 (Android 7.0) |
| **SDK Objetivo** | API 36 (Android 15) |
| **UI Framework** | Material Design 3 |
| **Build System** | Gradle 8.9.1 (Kotlin DSL) |
| **Paquete** | `com.example.guander` |

**Bibliotecas principales:**

| Biblioteca | Versión | Propósito |
|------------|---------|-----------|
| Firebase BoM | 34.11.0 | Autenticación + Analytics |
| Google Play Services Auth | 21.3.0 | Google Sign-In |
| Glide | 4.16.0 | Carga y caché de imágenes |
| osmdroid | 6.1.18 | Mapa OpenStreetMap |
| ZXing Android Embedded | 4.3.0 | Escaneo de códigos QR |
| Material Components | 1.13.0 | Componentes UI Material 3 |
| ConstraintLayout | 2.2.1 | Layouts responsivos |

### 3.2 Backend — Cloudflare Worker

| Aspecto | Detalle |
|---------|---------|
| **Runtime** | Cloudflare Workers (V8 Isolates) |
| **Lenguaje** | JavaScript (ES Modules) |
| **Base de datos** | Cloudflare D1 (SQLite distribuido) |
| **Dominio** | `guander-api.tomas-gonzalezz.workers.dev` |
| **Compatibilidad** | 2024-01-01 |

### 3.3 Servicios Externos

| Servicio | Uso |
|----------|-----|
| **Firebase Auth** | Autenticación con Google y gestión de sesiones |
| **Firebase Analytics** | Métricas y eventos de la app |
| **Cloudinary** | Almacenamiento y CDN de imágenes de perfil |
| **Resend** | Envío de emails transaccionales (bienvenida) |
| **OpenStreetMap (MAPNIK)** | Tiles del mapa |

## 4. Modelo de Datos

### 4.1 Diagrama Entidad-Relación

```
┌──────────────┐       ┌───────────────┐       ┌──────────────┐
│  user_data   │       │    users      │       │   customer   │
├──────────────┤       ├───────────────┤       ├──────────────┤
│ id_user_data │◄──────│ fk_user_data  │       │ id_customer  │
│ name         │       │ id_user       │◄──────│ fk_user      │
│ last_name    │       │ username      │       │ points       │
│ tel          │       │ fk_rol        │       └──────┬───────┘
│ email        │       │ date_reg      │              │
│ address      │       └───────────────┘              │
│ password_hash│                              ┌───────┴───────┐
│ photo_url    │                              │               │
└──────────────┘                              ▼               ▼
                                    ┌──────────────┐ ┌──────────────┐
                                    │store_purchase│ │prof_purchase │
                                    ├──────────────┤ ├──────────────┤
                                    │ id           │ │ id           │
                                    │ fk_customer  │ │ fk_customer  │
                                    │ fk_store     │ │ fk_professional│
                                    │ date         │ │ date         │
                                    │ amount       │ │ amount       │
                                    │ points_earn  │ │ points_earn  │
                                    └──────┬───────┘ └──────┬───────┘
                                           │                │
                                           ▼                ▼
                                    ┌──────────────┐ ┌──────────────┐
                                    │   stores     │ │professionals │
                                    ├──────────────┤ ├──────────────┤
                                    │ id_store     │ │id_professional│
                                    │ name         │ │ description  │
                                    │ description  │ │ address      │
                                    │ address      │ │ location     │
                                    │ location     │ │ fk_user_id   │
                                    │ fk_category  │ └──────────────┘
                                    │ image_url    │
                                    │ stars        │
                                    │ fk_schedule  │
                                    └──────────────┘

┌──────────────────┐    ┌────────────────────┐    ┌────────────────┐
│ comments_store   │    │   coupon_store      │    │ points_history │
├──────────────────┤    ├────────────────────┤    ├────────────────┤
│ id_comment       │    │ id_coupon           │    │ id             │
│ fk_customer_id   │    │ name               │    │ fk_customer    │
│ fk_store_id      │    │ description        │    │ description    │
│ body             │    │ point_req          │    │ points_change  │
│ stars            │    │ code_coupon        │    │ redemption_code│
│ date             │    │ state              │    │ created_at     │
└──────────────────┘    │ fk_coupon_state    │    └────────────────┘
                        │ expiration_date    │
┌──────────────────┐    │ fk_store           │    ┌────────────────┐
│ comments_prof    │    └────────────────────┘    │  schedule      │
├──────────────────┤                              ├────────────────┤
│ id_comment       │    ┌────────────────────┐    │ id_schedule    │
│ fk_customer_id   │    │ coupon_buy_store   │    │ week           │
│ fk_professional_id│   ├────────────────────┤    │ weekend        │
│ body             │    │ fk_customer_id     │    │ sunday         │
│ stars            │    │ fk_coupon_id       │    └────────────────┘
│ date             │    └────────────────────┘
└──────────────────┘                              ┌────────────────┐
                        ┌────────────────────┐    │   category     │
                        │  notifications     │    ├────────────────┤
                        ├────────────────────┤    │ id_category    │
                        │ id_notification    │    │ name           │
                        │ name               │    └────────────────┘
                        │ description        │
                        └────────┬───────────┘
                                 │
                        ┌────────┴───────────┐
                        │  notif_users       │
                        ├────────────────────┤
                        │ fk_notifications_id│
                        │ fk_users_id        │
                        └────────────────────┘
```

### 4.2 Tablas Principales

| Tabla | Descripción | Relaciones |
|-------|-------------|------------|
| `user_data` | Datos personales del usuario | 1:1 con `users` |
| `users` | Cuenta del usuario con rol | FK → `user_data`, FK → rol |
| `customer` | Saldo de PetPoints | FK → `users` |
| `stores` | Establecimientos pet-friendly | FK → `category`, FK → `schedule` |
| `professionals` | Profesionales (veterinarios, peluqueros) | FK → `users` |
| `store_purchase` | Historial de compras en tiendas | FK → `customer`, FK → `stores` |
| `prof_purchase` | Historial de visitas a profesionales | FK → `customer`, FK → `professionals` |
| `comments_store` | Reseñas de tiendas | FK → `customer`, FK → `stores` |
| `comments_prof` | Reseñas de profesionales | FK → `customer`, FK → `professionals` |
| `coupon_store` | Catálogo de cupones/recompensas | FK → `stores` |
| `coupon_buy_store` | Cupones canjeados | FK → `customer`, FK → `coupon_store` |
| `points_history` | Registro de movimientos de puntos | FK → `customer` |
| `notifications` | Plantillas de notificaciones | M:N con `users` vía `notif_users` |
| `schedule` | Horarios de apertura | 1:N con `stores` |
| `category` | Categorías de establecimientos | 1:N con `stores` |

## 5. API REST — Endpoints

### 5.1 Autenticación

| Método | Endpoint | Descripción | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/register` | Registro de usuario | `{ email, name?, lastName?, tel?, password? }` | `201 { success, userId }` / `409 duplicado` |
| POST | `/login-email` | Login con email | `{ email, password }` | `200 { success, email, name }` / `401 error` |

### 5.2 Dashboard y Perfil

| Método | Endpoint | Descripción | Parámetros | Response |
|--------|----------|-------------|------------|----------|
| GET | `/dashboard` | Datos del home | `?email=` | `{ points, name, notifications[] }` |
| GET | `/profile` | Datos del perfil | `?email=` | `{ name, lastName, tel, email, address, photoUrl, dateReg, points, placesVisited, coupons }` |
| PUT | `/profile` | Actualizar perfil | `{ email, name?, lastName?, tel?, address?, photoUrl? }` | `200 { success }` |
| POST | `/sign-upload` | Firma para Cloudinary | `{}` | `{ timestamp, signature, apiKey, cloudName }` |

### 5.3 Lugares y Reseñas

| Método | Endpoint | Descripción | Parámetros | Response |
|--------|----------|-------------|------------|----------|
| GET | `/places` | Listar establecimientos | `?email=&lat=&lng=` | `{ places[] }` con distancia |
| GET | `/comments` | Reseñas de un lugar | `?placeId=&placeType=&email=` | `{ comments[], canComment, alreadyCommented }` |
| POST | `/review` | Publicar reseña | `{ email, placeId, placeType, rating, comment }` | `200 { success }` / `409 duplicado` |

### 5.4 Recompensas y QR

| Método | Endpoint | Descripción | Parámetros | Response |
|--------|----------|-------------|------------|----------|
| GET | `/rewards` | Catálogo de recompensas | `?email=` | `{ points, rewards[] }` |
| POST | `/redeem` | Canjear recompensa | `{ email, couponId, couponType }` | `{ success, code, remainingPoints }` |
| GET | `/redeem-history` | Historial de canjes | `?email=` | `{ history[] }` |
| POST | `/validate-qr` | Validar QR y sumar puntos | `{ email, qrData }` | `{ success, pointsEarned, newBalance }` |

## 6. Flujo de Autenticación

```
┌────────┐                  ┌──────────┐              ┌──────────┐
│Usuario │                  │  App     │              │ Backend  │
└───┬────┘                  └────┬─────┘              └────┬─────┘
    │                            │                         │
    │ ── Opción A: Google ──────▶│                         │
    │                            │──► Firebase Auth        │
    │                            │◄── ID Token             │
    │                            │                         │
    │                            │── POST /register ──────▶│
    │                            │   (email, name)         │
    │                            │◄── 201 / 409 (existing)─│
    │                            │                         │
    │ ── Opción B: Email ───────▶│                         │
    │    (email + password)      │                         │
    │                            │── POST /login-email ───▶│
    │                            │   (email, pwd)          │
    │                            │◄── 200 / 401 ──────────│
    │                            │                         │
    │ ── Opción C: Registro ────▶│                         │
    │    (email + pwd + datos)   │                         │
    │                            │── POST /register ──────▶│
    │                            │   (email, name,         │
    │                            │    lastName, tel, pwd)   │
    │                            │◄── 201 Created ────────│
    │                            │                         │
    │◄── Dashboard ─────────────│                         │
```

### Almacenamiento de Contraseñas

- **Algoritmo:** PBKDF2-SHA256
- **Iteraciones:** 100,000
- **Salt:** 16 bytes aleatorios
- **Formato:** `{salt_hex}:{hash_hex}`

## 7. Flujo de Escaneo QR y PetPoints

```
┌────────┐    ┌──────────┐    ┌──────────┐    ┌────────┐
│  QR    │    │   App    │    │  Worker  │    │   D1   │
│(Tienda)│    │(Scanner) │    │  (API)   │    │  (DB)  │
└───┬────┘    └────┬─────┘    └────┬─────┘    └───┬────┘
    │              │               │              │
    │── código ──▶│               │              │
    │  JSON        │               │              │
    │              │── parse ──▶   │              │
    │              │  valida       │              │
    │              │  secret=      │              │
    │              │  "guander2026"│              │
    │              │               │              │
    │              │── POST ──────▶│              │
    │              │  /validate-qr │              │
    │              │               │── INSERT ──▶│
    │              │               │  purchase    │
    │              │               │              │
    │              │               │── UPDATE ──▶│
    │              │               │  points +=   │
    │              │               │  floor($/1000)
    │              │               │              │
    │              │               │── INSERT ──▶│
    │              │               │  history     │
    │              │               │              │
    │              │◄── response──│              │
    │              │  pointsEarned │              │
    │              │  newBalance   │              │
    │              │               │              │
    │              │── Dialog ──▶  │              │
    │              │  (éxito)      │              │
```

**Formato QR esperado:**
```json
{
  "type": "store",
  "id": 5,
  "name": "Bar Roma",
  "amount": 2500,
  "item": "Cerveza",
  "secret": "guander2026"
}
```

**Cálculo de puntos:** `puntos = max(1, floor(monto / 1000))`

## 8. Gestión de Imágenes (Cloudinary)

```
┌────────┐    ┌──────────┐    ┌──────────┐    ┌────────────┐
│Usuario │    │   App    │    │  Worker  │    │ Cloudinary │
└───┬────┘    └────┬─────┘    └────┬─────┘    └─────┬──────┘
    │              │               │                │
    │── selecciona │               │                │
    │   imagen    ─▶               │                │
    │              │── POST ──────▶│                │
    │              │  /sign-upload  │                │
    │              │◄── firma ─────│                │
    │              │  (timestamp,  │                │
    │              │   signature,  │                │
    │              │   apiKey)     │                │
    │              │               │                │
    │              │── POST upload ─────────────────▶
    │              │  multipart/form                │
    │              │  (file, signature, timestamp)  │
    │              │◄── secure_url ─────────────────│
    │              │               │                │
    │              │── PUT ───────▶│                │
    │              │  /profile     │                │
    │              │  {photoUrl}   │                │
    │              │◄── 200 ──────│                │
```

## 9. Patrones Arquitectónicos

### 9.1 Arquitectura General
- **Cliente-Servidor:** App Android se comunica con API REST serverless
- **Serverless:** Backend sin servidor mediante Cloudflare Workers (edge computing)
- **Edge Computing:** Lógica de negocio ejecutada en el edge node más cercano al usuario

### 9.2 Patrones en el Cliente
- **Activity-Based Navigation:** Cada pantalla es una Activity independiente con su propio ciclo de vida
- **SharedPreferences para Estado Local:** Persistencia de sesión, preferencias de tema, idioma y notificaciones
- **Adapter Pattern:** Inflado dinámico de listas (lugares, reseñas, recompensas) mediante `LayoutInflater`
- **Application Class (GuanderApp):** Inicialización global de configuración (tema, locale, osmdroid)

### 9.3 Patrones en el Backend
- **Router Pattern:** Despacho de rutas por método HTTP y pathname
- **Repository Pattern implícito:** Acceso a datos via D1 SQL preparado con bindings
- **Signed Upload:** Delegación segura de uploads a Cloudinary con firma HMAC server-side

### 9.4 Seguridad
- **PBKDF2-SHA256:** Hashing de contraseñas con 100k iteraciones y salt aleatorio
- **Firebase Auth:** Autenticación OAuth2 con Google
- **Signed Uploads:** Firmas criptográficas para subida de imágenes
- **Prevención de duplicados:** Control server-side de reseñas y canjes únicos (respuestas 409)
- **CORS configurado:** Headers permisivos para la comunicación app-API

## 10. Infraestructura y Despliegue

```
┌─────────────────────────────────────────────────────┐
│                  Cloudflare Edge                    │
│                                                     │
│  ┌─────────────────┐    ┌─────────────────────────┐ │
│  │ Worker Runtime  │    │   D1 Database           │ │
│  │ (V8 Isolates)   │───▶│   (SQLite distribuido)  │ │
│  │ worker.js       │    │   guander               │ │
│  └─────────────────┘    └─────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────┐    ┌──────────────────────────┐
│  Firebase (Google)  │    │  Cloudinary CDN          │
│  - Auth             │    │  - Imágenes de perfil    │
│  - Analytics        │    │  - Folder: profile_pics  │
│  Project: guander-  │    └──────────────────────────┘
│  7cf49              │
└─────────────────────┘    ┌──────────────────────────┐
                           │  Resend                  │
                           │  - Emails transaccionales│
                           │  - Bienvenida            │
                           └──────────────────────────┘
```

### Herramientas de Despliegue

| Componente | Herramienta | Comando |
|------------|-------------|---------|
| App Android | Android Studio / Gradle | `./gradlew assembleDebug` |
| Worker API | Wrangler CLI | `wrangler deploy` |
| Secrets | Wrangler CLI | `wrangler secret put <NAME>` |
| Base de datos | Wrangler CLI | `wrangler d1 execute guander --file=schema.sql` |
