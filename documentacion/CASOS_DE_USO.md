# Guander — Casos de uso

Documento de referencia para armar diagramas de casos de uso (UML).

---

## Actores

| Actor | Descripción |
|-------|-------------|
| **Usuario** | Persona que usa la app móvil (cliente del local) |
| **Sistema (API)** | Backend Cloudflare Worker que procesa las solicitudes |
| **Firebase Auth** | Servicio externo de autenticación (Google) |
| **Resend** | Servicio externo de envío de emails |
| **Cloudflare R2** | Servicio externo de almacenamiento de imágenes |

---

## Módulo 1: Autenticación y registro

### CU-01 — Registrarse con Google
- **Actor principal:** Usuario
- **Precondición:** Tener conexión a internet, tener cuenta de Google
- **Flujo principal:**
  1. El usuario abre la app y ve la pantalla de login
  2. Toca el botón "Continuar con Google"
  3. Se abre el selector de cuentas de Google
  4. Firebase Auth valida el token y crea la sesión
  5. La app envía `POST /register` con email y nombre al Worker
  6. El Worker crea las tablas `user_data`, `users` y `customer`
  7. Se envía email de bienvenida vía Resend
  8. La app redirige al Dashboard
- **Flujo alternativo:** Si el email ya existe, el Worker retorna `{existing: true}` y la app continúa al Dashboard sin error
- **Postcondición:** Usuario autenticado con sesión activa

### CU-02 — Registrarse con email y contraseña
- **Actor principal:** Usuario
- **Precondición:** Tener conexión a internet
- **Flujo principal:**
  1. El usuario toca "Registrarse con email"
  2. Completa email, nombre, apellido, teléfono y contraseña
  3. La app envía `POST /register` con los datos
  4. El Worker hashea la contraseña con PBKDF2-SHA256 y crea el usuario
  5. Se envía email de bienvenida vía Resend
  6. La app guarda el email en SharedPreferences y redirige al Dashboard
- **Flujo alternativo:** Si el email ya está registrado, se muestra error 409 "Email ya registrado"
- **Postcondición:** Usuario registrado y autenticado

### CU-03 — Iniciar sesión con email y contraseña
- **Actor principal:** Usuario
- **Precondición:** Tener conexión a internet, tener cuenta registrada con contraseña
- **Flujo principal:**
  1. El usuario ingresa email y contraseña
  2. La app envía `POST /login-email`
  3. El Worker verifica el hash PBKDF2 contra el almacenado
  4. Si coincide, retorna éxito con datos del usuario
  5. La app guarda la sesión en SharedPreferences y redirige al Dashboard
- **Flujo alternativo:** Credenciales incorrectas → error 401
- **Postcondición:** Sesión activa

### CU-04 — Cerrar sesión
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil y toca "Cerrar sesión"
  2. Se cierra la sesión de Firebase y Google Sign-In
  3. Se limpian las SharedPreferences
  4. La app redirige a la pantalla de Login
- **Postcondición:** Sesión eliminada

---

## Módulo 2: Dashboard

### CU-05 — Ver Dashboard
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. La app muestra saludo personalizado con el nombre del usuario
  2. Llama a `GET /dashboard?email=` para obtener datos
  3. Muestra los puntos acumulados
  4. Muestra las últimas notificaciones (canjes, compras, etc.)
- **Postcondición:** Dashboard visible con datos actualizados

### CU-06 — Navegar entre secciones
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario toca un ítem del BottomNavigationView
  2. La app navega a la sección correspondiente (Dashboard, Mapa, QR, Recompensas, Perfil)
- **Postcondición:** Pantalla seleccionada visible

---

## Módulo 3: Mapa y locales

### CU-07 — Explorar locales en el mapa
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, conceder permiso de ubicación
- **Flujo principal:**
  1. La app solicita permiso de ubicación GPS
  2. Obtiene coordenadas del usuario
  3. Llama a `GET /places?lat=&lng=&filter=all`
  4. Renderiza marcadores en el mapa (OpenStreetMap)
  5. Muestra la lista de locales debajo del mapa ordenados por distancia
- **Flujo alternativo:** Sin permiso de ubicación → usa coordenadas predeterminadas (Buenos Aires)
- **Postcondición:** Mapa y lista de locales visibles

### CU-08 — Filtrar locales por categoría
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, estar en la pantalla del mapa
- **Flujo principal:**
  1. El usuario expande el panel de filtros
  2. Selecciona una categoría: Todos / Locales / Restaurantes / Profesionales / Servicios
  3. La lista y los marcadores se actualizan al instante (filtro local)
- **Postcondición:** Solo se muestran locales de la categoría elegida

### CU-09 — Buscar local por nombre
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, estar en la pantalla del mapa
- **Flujo principal:**
  1. El usuario escribe en el campo de búsqueda
  2. La lista se filtra en tiempo real por nombre
- **Postcondición:** Lista filtrada por coincidencia de texto

### CU-10 — Ver detalle de un local desde el mapa
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener locales cargados
- **Flujo principal:**
  1. El usuario toca un marcador en el mapa
  2. Se abre un BottomSheet con resumen (nombre, distancia, estado)
  3. Puede tocar "Ver detalles" para ir a la pantalla completa
- **Flujo alternativo:** También puede tocar "Ver detalles y comentarios" desde la tarjeta en la lista
- **Postcondición:** Pantalla de detalle abierta

### CU-11 — Ver cómo llegar a un local
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, estar en el detalle de un local con coordenadas
- **Flujo principal:**
  1. El usuario toca "Cómo llegar"
  2. Se abre un Intent con la URI geo:// para elegir app de navegación
  3. La app externa (Google Maps, Waze, etc.) muestra la ruta
- **Postcondición:** App de navegación abierta con destino configurado

---

## Módulo 4: Detalle de local y reseñas

### CU-12 — Ver detalle de un local
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, tener un local seleccionado
- **Flujo principal:**
  1. La app muestra: foto, categoría, estado (abierto/cerrado), descripción, dirección, teléfono
  2. Muestra el listado de comentarios con nombre, estrellas, fecha y texto
  3. Muestra el conteo total de comentarios
- **Postcondición:** Información completa del local visible

### CU-13 — Dejar una reseña (estrellas + comentario)
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, haber realizado al menos una compra en ese local (validado por QR), no haber comentado previamente
- **Flujo principal:**
  1. La app muestra el formulario con RatingBar (1-5 estrellas) y campo de texto
  2. El usuario selecciona estrellas y escribe su comentario
  3. Toca "Publicar comentario"
  4. La app envía `POST /review` con email, placeId, rating y comment
  5. El Worker verifica que no exista reseña previa y la inserta
  6. Se recarga la lista de comentarios
- **Flujo alternativo 1:** Sin compra previa → muestra "No puedes comentar aún — Debes visitar y consumir en este lugar"
- **Flujo alternativo 2:** Ya comentó → muestra "Ya dejaste tu reseña — Solo se permite una reseña por lugar"
- **Flujo alternativo 3:** Intento duplicado vía API → Worker retorna 409 "Ya dejaste una reseña en este lugar"
- **Postcondición:** Reseña publicada, formulario oculto, mensaje de confirmación

---

## Módulo 5: Escáner QR y puntos

### CU-14 — Escanear código QR
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, conceder permiso de cámara
- **Flujo principal:**
  1. La app solicita permiso de cámara
  2. Activa la cámara con el escáner ZXing
  3. El usuario apunta al QR del local
  4. La app lee el JSON del QR y envía `POST /validate-qr` con email y qrData
  5. El Worker valida el secret, registra la compra y suma puntos
  6. Se muestra un diálogo de éxito con: nombre del local, monto, puntos ganados, nuevo balance
- **Flujo alternativo 1:** QR inválido (JSON mal formado o secret incorrecto) → error "QR inválido"
- **Flujo alternativo 2:** Sin permiso de cámara → muestra placeholder con botón para activar
- **Postcondición:** Compra registrada, puntos sumados al balance

### CU-15 — Visualizar puntos acumulados
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. Los puntos se muestran en el Dashboard y en la pantalla de Recompensas
  2. Se obtienen mediante `GET /dashboard` o `GET /rewards`
- **Postcondición:** Balance de puntos visible

---

## Módulo 6: Recompensas

### CU-16 — Ver catálogo de recompensas
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. La app llama a `GET /rewards?email=` 
  2. Muestra la lista de recompensas con nombre, descripción e ícono
  3. Cada recompensa muestra el costo en puntos
  4. Las recompensas con puntos insuficientes se deshabilitan visualmente
- **Postcondición:** Catálogo visible con estado de disponibilidad

### CU-17 — Buscar recompensas
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, estar en la pantalla de Recompensas
- **Flujo principal:**
  1. El usuario escribe en el buscador
  2. Las recompensas se filtran en tiempo real por nombre
- **Postcondición:** Lista filtrada

### CU-18 — Canjear una recompensa
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, tener puntos suficientes
- **Flujo principal:**
  1. El usuario toca "Canjear" en una recompensa
  2. Se muestra un diálogo de confirmación con el costo en puntos
  3. Confirma el canje
  4. La app envía `POST /redeem` con email y rewardId
  5. El Worker deduce los puntos y registra en `redeem_history`
  6. Se muestra diálogo de éxito y se actualiza el balance
- **Flujo alternativo:** Puntos insuficientes → botón deshabilitado, no se puede tocar
- **Postcondición:** Puntos deducidos, recompensa canjeada

### CU-19 — Ver historial de canjes
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. El usuario cambia a la pestaña "Historial" en Recompensas
  2. La app llama a `GET /redeem-history?email=`
  3. Muestra la lista de canjes previos con fecha y recompensa
- **Postcondición:** Historial visible

---

## Módulo 7: Perfil de usuario

### CU-20 — Ver perfil
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. La app llama a `GET /profile?email=`
  2. Muestra: foto de perfil, nombre completo, email, teléfono, fecha de registro, puntos
- **Postcondición:** Datos del perfil visibles

### CU-21 — Editar datos personales
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet
- **Flujo principal:**
  1. El usuario toca "Editar perfil"
  2. Se abre el formulario con datos precargados (nombre, apellido, teléfono, dirección)
  3. Modifica los campos deseados
  4. Toca "Guardar"
  5. La app envía `PUT /profile` con los datos actualizados
- **Postcondición:** Datos actualizados en la BD

### CU-22 — Cambiar foto de perfil
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado, tener conexión a internet, conceder permiso de galería
- **Flujo principal:**
  1. El usuario toca el ícono de cámara en su foto de perfil
  2. Se abre el selector de imágenes del sistema
  3. Selecciona una imagen
  4. La app llama a `POST /sign-upload` para obtener URL pre-firmada de R2
  5. Sube la imagen directamente al bucket R2 vía HTTP PUT
  6. Se actualiza `user_data.photo_url` en la BD
  7. Se recarga la foto con Glide
- **Postcondición:** Foto actualizada en perfil y servidor

---

## Módulo 8: Configuración

### CU-23 — Configurar notificaciones
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil → Notificaciones
  2. Activa/desactiva switches de notificaciones
- **Postcondición:** Preferencia guardada localmente

### CU-24 — Ver privacidad y seguridad
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil → Privacidad y Seguridad
  2. Visualiza opciones de privacidad con toggles
- **Postcondición:** Pantalla informativa visible

### CU-25 — Ver centro de ayuda
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil → Centro de ayuda
  2. Visualiza preguntas frecuentes y contacto de soporte
- **Postcondición:** Información de ayuda visible

### CU-26 — Ver términos y condiciones
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil → Términos y Condiciones
  2. Visualiza el texto legal completo
- **Postcondición:** Texto visible

### CU-27 — Ver política de privacidad
- **Actor principal:** Usuario
- **Precondición:** Estar autenticado
- **Flujo principal:**
  1. El usuario va a Perfil → Política de Privacidad
  2. Visualiza la política completa
- **Postcondición:** Texto visible

---

## Módulo 9: Sistema (backend)

### CU-28 — Enviar email de bienvenida
- **Actor principal:** Sistema (Worker)
- **Disparador:** Registro exitoso de usuario nuevo (CU-01 o CU-02)
- **Flujo principal:**
  1. El Worker llama a la API de Resend con HTML de bienvenida
  2. El email se envía al correo registrado
- **Flujo alternativo:** Si Resend falla, el registro no se ve afectado (no bloqueante)
- **Postcondición:** Email en bandeja de entrada del usuario

### CU-29 — Calcular distancia a locales
- **Actor principal:** Sistema (Worker)
- **Disparador:** `GET /places` con coordenadas del usuario
- **Flujo principal:**
  1. El Worker recibe lat/lng del usuario
  2. Calcula la distancia a cada local usando fórmula de Haversine
  3. Ordena por proximidad y retorna la lista
- **Postcondición:** Locales ordenados por cercanía

### CU-30 — Calcular puntos por compra
- **Actor principal:** Sistema (Worker)
- **Disparador:** `POST /validate-qr` exitoso
- **Flujo principal:**
  1. El Worker extrae el monto del QR
  2. Calcula puntos: `max(1, floor(monto / 1000))`
  3. Suma los puntos al balance del customer
  4. Registra en `points_history`
- **Postcondición:** Puntos calculados y acreditados

---

## Resumen para diagramas

| ID | Caso de uso | Actor |
|----|-------------|-------|
| CU-01 | Registrarse con Google | Usuario, Firebase, Resend |
| CU-02 | Registrarse con email/contraseña | Usuario, Resend |
| CU-03 | Iniciar sesión con email/contraseña | Usuario |
| CU-04 | Cerrar sesión | Usuario |
| CU-05 | Ver Dashboard | Usuario |
| CU-06 | Navegar entre secciones | Usuario |
| CU-07 | Explorar locales en el mapa | Usuario |
| CU-08 | Filtrar locales por categoría | Usuario |
| CU-09 | Buscar local por nombre | Usuario |
| CU-10 | Ver detalle de local desde el mapa | Usuario |
| CU-11 | Ver cómo llegar a un local | Usuario |
| CU-12 | Ver detalle de un local | Usuario |
| CU-13 | Dejar una reseña | Usuario |
| CU-14 | Escanear código QR | Usuario |
| CU-15 | Visualizar puntos acumulados | Usuario |
| CU-16 | Ver catálogo de recompensas | Usuario |
| CU-17 | Buscar recompensas | Usuario |
| CU-18 | Canjear una recompensa | Usuario |
| CU-19 | Ver historial de canjes | Usuario |
| CU-20 | Ver perfil | Usuario |
| CU-21 | Editar datos personales | Usuario |
| CU-22 | Cambiar foto de perfil | Usuario, R2 |
| CU-23 | Configurar notificaciones | Usuario |
| CU-24 | Ver privacidad y seguridad | Usuario |
| CU-25 | Ver centro de ayuda | Usuario |
| CU-26 | Ver términos y condiciones | Usuario |
| CU-27 | Ver política de privacidad | Usuario |
| CU-28 | Enviar email de bienvenida | Sistema, Resend |
| CU-29 | Calcular distancia a locales | Sistema |
| CU-30 | Calcular puntos por compra | Sistema |
