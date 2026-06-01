# 🐾 Guander

> Aplicación Android de fidelización para locales pet-friendly

Guander conecta a dueños de mascotas con establecimientos amigables con animales. Los usuarios descubren lugares en un mapa interactivo, acumulan **PetPoints** escaneando códigos QR y los canjean por recompensas exclusivas.

---

## ✨ Funcionalidades principales

| Función | Descripción |
|---|---|
| 🔐 **Autenticación** | Inicio de sesión con Google o email/contraseña |
| 🗺️ **Mapa interactivo** | Explora locales pet-friendly cercanos con distancia en tiempo real |
| 📷 **Escáner QR** | Escanea el código QR del local para sumar PetPoints |
| 🏆 **Recompensas** | Catálogo de premios canjeables con tus puntos acumulados |
| ⭐ **Reseñas** | Califica y comenta los establecimientos que visitaste |
| 👤 **Perfil** | Gestiona tus datos, foto de perfil y estadísticas personales |
| 🌙 **Apariencia** | Tema claro, oscuro o automático (sistema) |

---

## 📱 Capturas de pantalla

> *Próximamente*

---

## 🏗️ Arquitectura

Guander sigue un modelo **cliente-servidor** con tres capas principales:

```
┌─────────────────────────────────────┐
│       App Android (Java)            │
│  Material 3 · OSMDroid · ZXing      │
│  Glide · Firebase Auth              │
└──────────────┬──────────────────────┘
               │ HTTP/REST (JSON)
               ▼
┌─────────────────────────────────────┐
│     Cloudflare Worker (JS)          │
│     API serverless en el edge       │
└──────────────┬──────────────────────┘
               │ SQL
               ▼
┌─────────────────────────────────────┐
│       Cloudflare D1 (SQLite)        │
│       Base de datos relacional      │
└─────────────────────────────────────┘

Servicios externos:
  Firebase Auth   — autenticación Google
  Cloudflare R2   — almacenamiento de imágenes
  Resend          — emails transaccionales
```

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Cliente | Android (Java), minSdk 24, targetSdk 36 |
| UI | Material Design 3 |
| Mapas | OSMDroid 6.1.18 + OpenStreetMap |
| Escáner QR | ZXing Android Embedded 4.3.0 |
| Imágenes | Glide 4.16.0 |
| Autenticación | Firebase Auth (Google Sign-In + email/contraseña) |
| Backend | Cloudflare Workers (JavaScript ES Modules) |
| Base de datos | Cloudflare D1 (SQLite serverless) |
| Storage | Cloudflare R2 |
| Email | Resend API |
| Build | Gradle KTS |

---

## 📋 Requisitos previos

- Android Studio Hedgehog o superior
- JDK 11+
- Android SDK 36
- Cuenta en Firebase (para `google-services.json`)
- Cuenta en Cloudflare (Workers + D1 + R2)

---

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/Guander.git
cd Guander
```

### 2. Configurar Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Habilita **Authentication** (Google + Email/Contraseña).
3. Descarga el archivo `google-services.json` y colócalo en `app/`.

### 3. Configurar el backend (Cloudflare Worker)

1. Instala [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/install-and-update/).
2. Entra a la carpeta del worker:
   ```bash
   cd cloudflare-worker
   npm install
   ```
3. Edita `wrangler.toml` con tu `account_id` y el ID de tu base de datos D1.
4. Crea y migra la base de datos:
   ```bash
   wrangler d1 create guander-db
   wrangler d1 execute guander-db --file=./schema.sql
   ```
5. Despliega el worker:
   ```bash
   wrangler deploy
   ```

### 4. Configurar la URL de la API en la app

En la clase `ApiClient.java` (o donde corresponda), reemplaza la URL base:

```java
private static final String BASE_URL = "https://guander-api.TU-USUARIO.workers.dev";
```

### 5. Compilar y ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta en un dispositivo o emulador con API 24+.

---

## 📂 Estructura del proyecto

```
Guander/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/guander/
│           ├── LoginActivity.java          # Login y registro
│           ├── MainActivity.java           # Splash / router inicial
│           ├── DashboardActivity.java      # Pantalla principal
│           ├── MapActivity.java            # Mapa + lista de locales
│           ├── PlaceDetailActivity.java    # Detalle del local + reseñas
│           ├── QrScanActivity.java         # Escáner QR
│           ├── RewardsActivity.java        # Recompensas + historial
│           ├── ProfileActivity.java        # Perfil del usuario
│           └── ...
├── cloudflare-worker/
│   ├── worker.js                           # API REST serverless
│   └── wrangler.toml                       # Configuración del worker
└── documentacion/                          # Documentación técnica
```

---

## 🔑 Permisos requeridos

| Permiso | Uso |
|---|---|
| `INTERNET` | Comunicación con la API |
| `ACCESS_FINE_LOCATION` | Centrar el mapa en la ubicación del usuario |
| `CAMERA` | Escanear códigos QR |
| `READ_MEDIA_IMAGES` | Cambiar foto de perfil desde la galería |
| `POST_NOTIFICATIONS` | Notificaciones de puntos y recompensas |

---

## 🧩 Cómo funciona el sistema de puntos

1. El usuario escanea el código QR de un local afiliado.
2. El QR contiene: tipo, ID del local, nombre, monto y secreto de validación.
3. El backend valida el secreto y registra la compra.
4. Los puntos se calculan como `max(1, floor(monto / 1000))`.
5. Los PetPoints se acreditan al instante y se pueden usar para canjear recompensas.

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<p align="center">Hecho con ❤️ para los amantes de las mascotas 🐶🐱</p>
