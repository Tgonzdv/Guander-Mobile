# Diagramas PlantUML — Casos de Uso Guander v2.0

Todos los diagramas usan notación UML de Caso de Uso:
actores como muñeco, elipses para los casos de uso, rectángulo de límite del sistema,
y relaciones `<<include>>` / `<<extend>>` con flecha punteada.

---

## Diagrama General del Sistema

```plantuml
@startuml SISTEMA
left to right direction
title Guander — Diagrama de Casos de Uso General

actor "Usuario" as U

rectangle "Módulo 1: Autenticación" {
  usecase "CU-01\nRegistrarse con Google" as CU01
  usecase "CU-02\nRegistrarse con email" as CU02
  usecase "CU-03\nIniciar sesión" as CU03
  usecase "CU-04\nCerrar sesión" as CU04
  usecase "CU-05\nMantener sesión" as CU05
}
rectangle "Módulo 2: Dashboard" {
  usecase "CU-06\nVer Dashboard" as CU06
  usecase "CU-07\nNavegar secciones" as CU07
}
rectangle "Módulo 3: Mapa" {
  usecase "CU-08\nExplorar locales" as CU08
  usecase "CU-09\nFiltrar por categoría" as CU09
  usecase "CU-10\nBuscar por nombre" as CU10
  usecase "CU-11\nVer detalle desde mapa" as CU11
  usecase "CU-12\nAbrir en Google Maps" as CU12
}
rectangle "Módulo 4: Detalle y Reseñas" {
  usecase "CU-13\nVer detalle de local" as CU13
  usecase "CU-14\nDejar una reseña" as CU14
}
rectangle "Módulo 5: QR / PetPoints" {
  usecase "CU-15\nEscanear QR" as CU15
  usecase "CU-16\nVer puntos acumulados" as CU16
}
rectangle "Módulo 6: Recompensas" {
  usecase "CU-17\nVer catálogo" as CU17
  usecase "CU-18\nBuscar recompensas" as CU18
  usecase "CU-19\nCanjear recompensa" as CU19
  usecase "CU-20\nVer historial canjes" as CU20
}
rectangle "Módulo 7: Perfil" {
  usecase "CU-21\nVer perfil" as CU21
  usecase "CU-22\nEditar datos personales" as CU22
  usecase "CU-23\nCambiar foto de perfil" as CU23
}
rectangle "Módulo 8: Configuración" {
  usecase "CU-24\nConfigurar notificaciones" as CU24
  usecase "CU-25\nConfigurar privacidad" as CU25
  usecase "CU-26\nVer centro de ayuda" as CU26
  usecase "CU-27\nVer términos" as CU27
  usecase "CU-28\nVer política privacidad" as CU28
}
rectangle "Módulo 9: Backend" {
  usecase "CU-29\nEmail bienvenida" as CU29
  usecase "CU-30\nCalcular distancias" as CU30
  usecase "CU-31\nCalcular puntos" as CU31
}

actor "Firebase Auth" as FB <<service>>
actor "Resend" as RS <<service>>
actor "Cloudflare R2" as R2 <<service>>

U --> CU01
U --> CU02
U --> CU03
U --> CU04
U --> CU05
U --> CU06
U --> CU07
U --> CU08
U --> CU09
U --> CU10
U --> CU11
U --> CU12
U --> CU13
U --> CU14
U --> CU15
U --> CU16
U --> CU17
U --> CU18
U --> CU19
U --> CU20
U --> CU21
U --> CU22
U --> CU23
U --> CU24
U --> CU25
U --> CU26
U --> CU27
U --> CU28
CU01 .> CU29 : <<include>>
CU02 .> CU29 : <<include>>
CU08 .> CU30 : <<include>>
CU11 .> CU13 : <<include>>
CU15 .> CU31 : <<include>>
FB --> CU01
RS --> CU29
R2 --> CU23
@enduml
```

---

## CU-01: Registrarse con Google

```plantuml
@startuml CU-01
left to right direction
title CU-01 — Registrarse con Google

actor "Usuario" as U

rectangle "Guander" {
  usecase "Registrarse\ncon Google" as CU01
  usecase "Autenticar con\ncuenta de Google" as UC1
  usecase "Persistir sesión\nen dispositivo" as UC2
  usecase "Crear perfil\nde usuario" as UC3
  usecase "Enviar email\nde bienvenida" as UC4
}

actor "Firebase Auth" as FB <<service>>
actor "Sistema" as Srv <<service>>
actor "Resend" as Resend <<service>>

U --> CU01
CU01 .> UC1 : <<include>>
CU01 .> UC2 : <<include>>
UC3 .> CU01 : <<extend>>
UC4 .> CU01 : <<extend>>
FB --> UC1
Srv --> UC3
Resend --> UC4
@enduml
```

---

## CU-02: Registrarse con email y contraseña

```plantuml
@startuml CU-02
left to right direction
title CU-02 — Registrarse con email y contraseña

actor "Usuario" as U

rectangle "Guander" {
  usecase "Registrarse con\nemail y contraseña" as CU02
  usecase "Validar campos\ndel formulario" as UC1
  usecase "Cifrar contraseña\nde forma segura" as UC2
  usecase "Crear perfil\nde usuario" as UC3
  usecase "Persistir sesión\nen dispositivo" as UC4
  usecase "Enviar email\nde bienvenida" as UC5
}

actor "Sistema" as Srv <<service>>
actor "Resend" as Resend <<service>>

U --> CU02
CU02 .> UC1 : <<include>>
CU02 .> UC2 : <<include>>
CU02 .> UC3 : <<include>>
CU02 .> UC4 : <<include>>
CU02 .> UC5 : <<include>>
Srv --> UC2
Srv --> UC3
Resend --> UC5
@enduml
```

---

## CU-03: Iniciar sesión con email y contraseña

```plantuml
@startuml CU-03
left to right direction
title CU-03 — Iniciar sesión con email y contraseña

actor "Usuario" as U

rectangle "Guander" {
  usecase "Iniciar sesión con\nemail y contraseña" as CU03
  usecase "Verificar credenciales\nen el servidor" as UC1
  usecase "Persistir sesión\nen dispositivo" as UC2
}

actor "Sistema" as Srv <<service>>

U --> CU03
CU03 .> UC1 : <<include>>
CU03 .> UC2 : <<include>>
Srv --> UC1
@enduml
```

---

## CU-04: Cerrar sesión

```plantuml
@startuml CU-04
left to right direction
title CU-04 — Cerrar sesión

actor "Usuario" as U

rectangle "Guander" {
  usecase "Cerrar sesión" as CU04
  usecase "Confirmar cierre\nde sesión" as UC1
  usecase "Cerrar sesión\nen Firebase Auth" as UC2
  usecase "Eliminar datos locales\ndel dispositivo" as UC3
}

actor "Firebase Auth" as FB <<service>>

U --> CU04
CU04 .> UC1 : <<include>>
CU04 .> UC2 : <<include>>
CU04 .> UC3 : <<include>>
FB --> UC2
@enduml
```

---

## CU-05: Mantener sesión entre reinicios

```plantuml
@startuml CU-05
left to right direction
title CU-05 — Mantener sesión entre reinicios

actor "Usuario" as U

rectangle "Guander" {
  usecase "Mantener sesión\nentre reinicios" as CU05
  usecase "Verificar sesión\nguardada en dispositivo" as UC1
  usecase "Verificar sesión\nde Firebase activa" as UC2
  usecase "Redirigir\nal Dashboard" as UC3
  usecase "Redirigir\na pantalla de Login" as UC4
}

actor "Firebase Auth" as FB <<service>>

U --> CU05
CU05 .> UC1 : <<include>>
CU05 .> UC2 : <<include>>
UC3 .> CU05 : <<extend>>
UC4 .> CU05 : <<extend>>
FB --> UC2
@enduml
```

---

## CU-06: Ver Dashboard

```plantuml
@startuml CU-06
left to right direction
title CU-06 — Ver Dashboard

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver Dashboard" as CU06
  usecase "Mostrar saludo\npersonalizado" as UC1
  usecase "Consultar saldo\nde PetPoints" as UC2
  usecase "Consultar últimas\n5 notificaciones" as UC3
}

actor "Sistema" as Srv <<service>>

U --> CU06
CU06 .> UC1 : <<include>>
CU06 .> UC2 : <<include>>
CU06 .> UC3 : <<include>>
Srv --> UC2
Srv --> UC3
@enduml
```

---

## CU-07: Navegar entre secciones

```plantuml
@startuml CU-07
left to right direction
title CU-07 — Navegar entre secciones

actor "Usuario" as U

rectangle "Guander" {
  usecase "Navegar entre\nsecciones" as CU07
  usecase "Ver Dashboard\n(CU-06)" as UC1
  usecase "Ver Mapa\n(CU-08)" as UC2
  usecase "Escanear QR\n(CU-15)" as UC3
  usecase "Ver Recompensas\n(CU-17)" as UC4
  usecase "Ver Perfil\n(CU-21)" as UC5
}

U --> CU07
UC1 .> CU07 : <<extend>>
UC2 .> CU07 : <<extend>>
UC3 .> CU07 : <<extend>>
UC4 .> CU07 : <<extend>>
UC5 .> CU07 : <<extend>>
@enduml
```

---

## CU-08: Explorar locales en el mapa

```plantuml
@startuml CU-08
left to right direction
title CU-08 — Explorar locales en el mapa

actor "Usuario" as U

rectangle "Guander" {
  usecase "Explorar locales\nen el mapa" as CU08
  usecase "Obtener ubicación\nGPS del usuario" as UC1
  usecase "Consultar establecimientos\ncercanos" as UC2
  usecase "Calcular distancias\n(CU-30)" as UC3
  usecase "Mostrar marcadores\ny lista ordenada" as UC4
  usecase "Usar ubicación\npredeterminada (CABA)" as UC5
}

actor "Sistema" as Srv <<service>>

U --> CU08
CU08 .> UC1 : <<include>>
CU08 .> UC2 : <<include>>
UC2 .> UC3 : <<include>>
CU08 .> UC4 : <<include>>
UC5 .> CU08 : <<extend>>
Srv --> UC2
Srv --> UC3
@enduml
```

---

## CU-09: Filtrar locales por categoría

```plantuml
@startuml CU-09
left to right direction
title CU-09 — Filtrar locales por categoría

actor "Usuario" as U

rectangle "Guander" {
  usecase "Filtrar locales\npor categoría" as CU09
  usecase "Seleccionar categoría\n(Todos / Locales /\nRestaurantes /\nProfesionales / Servicios)" as UC1
  usecase "Actualizar lista\nen tiempo real" as UC2
  usecase "Actualizar marcadores\nen el mapa" as UC3
}

U --> CU09
CU09 .> UC1 : <<include>>
CU09 .> UC2 : <<include>>
CU09 .> UC3 : <<include>>
@enduml
```

---

## CU-10: Buscar local por nombre

```plantuml
@startuml CU-10
left to right direction
title CU-10 — Buscar local por nombre

actor "Usuario" as U

rectangle "Guander" {
  usecase "Buscar local\npor nombre" as CU10
  usecase "Filtrar lista\nen tiempo real" as UC1
  usecase "Actualizar marcadores\nen el mapa" as UC2
}

U --> CU10
CU10 .> UC1 : <<include>>
CU10 .> UC2 : <<include>>
@enduml
```

---

## CU-11: Ver detalle de local desde el mapa

```plantuml
@startuml CU-11
left to right direction
title CU-11 — Ver detalle de local desde el mapa

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver detalle de local\ndesde el mapa" as CU11
  usecase "Mostrar panel emergente\n(nombre, distancia, estado)" as UC1
  usecase "Ver detalle completo\ndel local (CU-13)" as UC2
}

U --> CU11
CU11 .> UC2 : <<include>>
UC1 .> CU11 : <<extend>>
@enduml
```

---

## CU-12: Abrir establecimiento en Google Maps

```plantuml
@startuml CU-12
left to right direction
title CU-12 — Abrir establecimiento en Google Maps

actor "Usuario" as U

rectangle "Guander" {
  usecase "Abrir establecimiento\nen Google Maps" as CU12
  usecase "Enviar intent\nde ubicación" as UC1
  usecase "Seleccionar app\nde mapas" as UC2
}

actor "App de Mapas" as Mapas <<service>>

U --> CU12
CU12 .> UC1 : <<include>>
CU12 .> UC2 : <<include>>
Mapas --> UC2
@enduml
```

---

## CU-13: Ver detalle de un local

```plantuml
@startuml CU-13
left to right direction
title CU-13 — Ver detalle de un local

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver detalle\nde un local" as CU13
  usecase "Consultar datos\ndel establecimiento" as UC1
  usecase "Consultar reseñas\ndel establecimiento" as UC2
  usecase "Mostrar información\ncompleta" as UC3
  usecase "Mostrar lista\nde reseñas" as UC4
}

actor "Sistema" as Srv <<service>>

U --> CU13
CU13 .> UC1 : <<include>>
CU13 .> UC2 : <<include>>
CU13 .> UC3 : <<include>>
CU13 .> UC4 : <<include>>
Srv --> UC1
Srv --> UC2
@enduml
```

---

## CU-14: Dejar una reseña

```plantuml
@startuml CU-14
left to right direction
title CU-14 — Dejar una reseña

actor "Usuario" as U

rectangle "Guander" {
  usecase "Dejar una\nreseña" as CU14
  usecase "Verificar compra\nprevia en el local" as UC1
  usecase "Publicar reseña\n(estrellas + comentario)" as UC2
  usecase "Bloquear reseña:\nsin compra previa" as UC3
  usecase "Bloquear reseña:\nreseña ya existente" as UC4
}

actor "Sistema" as Srv <<service>>

U --> CU14
CU14 .> UC1 : <<include>>
UC2 .> CU14 : <<extend>>
UC3 .> CU14 : <<extend>>
UC4 .> CU14 : <<extend>>
Srv --> UC1
Srv --> UC2
@enduml
```

---

## CU-15: Escanear código QR

```plantuml
@startuml CU-15
left to right direction
title CU-15 — Escanear código QR

actor "Usuario" as U

rectangle "Guander" {
  usecase "Escanear\ncódigo QR" as CU15
  usecase "Solicitar permiso\nde cámara" as UC1
  usecase "Leer y validar\ncódigo QR" as UC2
  usecase "Registrar compra y\ncalcular puntos (CU-31)" as UC3
  usecase "Mostrar resultado\ndel escaneo" as UC4
  usecase "Mostrar error:\nsin permiso de cámara" as UC5
}

actor "Sistema" as Srv <<service>>

U --> CU15
CU15 .> UC1 : <<include>>
CU15 .> UC2 : <<include>>
UC3 .> CU15 : <<extend>>
UC3 .> UC4 : <<include>>
UC5 .> CU15 : <<extend>>
Srv --> UC2
Srv --> UC3
@enduml
```

---

## CU-16: Visualizar puntos acumulados

```plantuml
@startuml CU-16
left to right direction
title CU-16 — Visualizar puntos acumulados

actor "Usuario" as U

rectangle "Guander" {
  usecase "Visualizar puntos\nacumulados" as CU16
  usecase "Consultar saldo\nde PetPoints" as UC1
  usecase "Mostrar en\nDashboard (CU-06)" as UC2
  usecase "Mostrar en\nRecompensas (CU-17)" as UC3
}

actor "Sistema" as Srv <<service>>

U --> CU16
CU16 .> UC1 : <<include>>
CU16 .> UC2 : <<include>>
CU16 .> UC3 : <<include>>
Srv --> UC1
@enduml
```

---

## CU-17: Ver catálogo de recompensas

```plantuml
@startuml CU-17
left to right direction
title CU-17 — Ver catálogo de recompensas

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver catálogo\nde recompensas" as CU17
  usecase "Consultar catálogo\nal sistema" as UC1
  usecase "Filtrar recompensas\nexpiradas y ya canjeadas" as UC2
  usecase "Indicar disponibilidad\npor puntos" as UC3
}

actor "Sistema" as Srv <<service>>

U --> CU17
CU17 .> UC1 : <<include>>
UC1 .> UC2 : <<include>>
CU17 .> UC3 : <<include>>
Srv --> UC1
@enduml
```

---

## CU-18: Buscar recompensas

```plantuml
@startuml CU-18
left to right direction
title CU-18 — Buscar recompensas

actor "Usuario" as U

rectangle "Guander" {
  usecase "Buscar\nrecompensas" as CU18
  usecase "Filtrar catálogo en\ntiempo real por\nnombre o descripción" as UC1
}

U --> CU18
CU18 .> UC1 : <<include>>
@enduml
```

---

## CU-19: Canjear una recompensa

```plantuml
@startuml CU-19
left to right direction
title CU-19 — Canjear una recompensa

actor "Usuario" as U

rectangle "Guander" {
  usecase "Canjear una\nrecompensa" as CU19
  usecase "Confirmar\nel canje" as UC1
  usecase "Verificar sin\ncanje previo" as UC2
  usecase "Deducir puntos y\nregistrar canje" as UC3
  usecase "Generar y mostrar\ncódigo de canje" as UC4
  usecase "Bloquear:\nrecompensa ya canjeada" as UC5
  usecase "Deshabilitar botón:\npuntos insuficientes" as UC6
}

actor "Sistema" as Srv <<service>>

U --> CU19
CU19 .> UC1 : <<include>>
CU19 .> UC2 : <<include>>
UC3 .> CU19 : <<extend>>
UC3 .> UC4 : <<include>>
UC5 .> CU19 : <<extend>>
UC6 .> CU19 : <<extend>>
Srv --> UC2
Srv --> UC3
Srv --> UC4
@enduml
```

---

## CU-20: Ver historial de canjes

```plantuml
@startuml CU-20
left to right direction
title CU-20 — Ver historial de canjes

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver historial\nde canjes" as CU20
  usecase "Consultar historial\nal sistema" as UC1
  usecase "Mostrar canjes con\nícono, fecha, puntos\ny código" as UC2
}

actor "Sistema" as Srv <<service>>

U --> CU20
CU20 .> UC1 : <<include>>
CU20 .> UC2 : <<include>>
Srv --> UC1
@enduml
```

---

## CU-21: Ver perfil de usuario

```plantuml
@startuml CU-21
left to right direction
title CU-21 — Ver perfil de usuario

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver perfil\nde usuario" as CU21
  usecase "Consultar datos\ndel perfil" as UC1
  usecase "Consultar estadísticas\ndel usuario" as UC2
  usecase "Mostrar avatar o\ninicial del nombre" as UC3
}

actor "Sistema" as Srv <<service>>

U --> CU21
CU21 .> UC1 : <<include>>
CU21 .> UC2 : <<include>>
CU21 .> UC3 : <<include>>
Srv --> UC1
Srv --> UC2
@enduml
```

---

## CU-22: Editar datos personales

```plantuml
@startuml CU-22
left to right direction
title CU-22 — Editar datos personales

actor "Usuario" as U

rectangle "Guander" {
  usecase "Editar datos\npersonales" as CU22
  usecase "Precargar formulario\ncon datos actuales" as UC1
  usecase "Guardar cambios\nen el sistema" as UC2
}

actor "Sistema" as Srv <<service>>

U --> CU22
CU22 .> UC1 : <<include>>
CU22 .> UC2 : <<include>>
Srv --> UC1
Srv --> UC2
@enduml
```

---

## CU-23: Cambiar foto de perfil

```plantuml
@startuml CU-23
left to right direction
title CU-23 — Cambiar foto de perfil

actor "Usuario" as U

rectangle "Guander" {
  usecase "Cambiar foto\nde perfil" as CU23
  usecase "Seleccionar imagen\nde la galería" as UC1
  usecase "Obtener firma segura\npara subida" as UC2
  usecase "Subir imagen al\nalmacenamiento" as UC3
  usecase "Actualizar foto\nen el perfil" as UC4
  usecase "Mostrar inicial del\nnombre como avatar" as UC5
}

actor "Sistema" as Srv <<service>>
actor "Cloudflare R2" as R2 <<service>>

U --> CU23
CU23 .> UC1 : <<include>>
CU23 .> UC2 : <<include>>
UC2 .> UC3 : <<include>>
UC3 .> UC4 : <<include>>
UC5 .> CU23 : <<extend>>
Srv --> UC2
R2 --> UC3
@enduml
```

---

## CU-24: Configurar notificaciones

```plantuml
@startuml CU-24
left to right direction
title CU-24 — Configurar notificaciones

actor "Usuario" as U

rectangle "Guander" {
  usecase "Configurar\nnotificaciones" as CU24
  usecase "Activar/desactivar:\nGeneral / Puntos /\nCupones / Lugares / Promo" as UC1
  usecase "Guardar preferencias\nen el dispositivo" as UC2
}

U --> CU24
CU24 .> UC1 : <<extend>>
CU24 .> UC2 : <<include>>
@enduml
```

---

## CU-25: Configurar privacidad

```plantuml
@startuml CU-25
left to right direction
title CU-25 — Configurar privacidad

actor "Usuario" as U

rectangle "Guander" {
  usecase "Configurar\nprivacidad" as CU25
  usecase "Activar/desactivar:\nUbicación / Datos /\nContenido personalizado" as UC1
  usecase "Guardar preferencias\nen el dispositivo" as UC2
}

U --> CU25
CU25 .> UC1 : <<extend>>
CU25 .> UC2 : <<include>>
@enduml
```

---

## CU-26: Ver centro de ayuda

```plantuml
@startuml CU-26
left to right direction
title CU-26 — Ver centro de ayuda

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver centro\nde ayuda" as CU26
  usecase "Ver preguntas\nfrecuentes por categoría" as UC1
  usecase "Ver información\nde contacto de privacidad" as UC2
}

U --> CU26
CU26 .> UC1 : <<include>>
CU26 .> UC2 : <<include>>
@enduml
```

---

## CU-27: Ver términos y condiciones

```plantuml
@startuml CU-27
left to right direction
title CU-27 — Ver términos y condiciones

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver términos\ny condiciones" as CU27
  usecase "Mostrar texto legal\ncompleto con scroll" as UC1
}

U --> CU27
CU27 .> UC1 : <<include>>
@enduml
```

---

## CU-28: Ver política de privacidad

```plantuml
@startuml CU-28
left to right direction
title CU-28 — Ver política de privacidad

actor "Usuario" as U

rectangle "Guander" {
  usecase "Ver política\nde privacidad" as CU28
  usecase "Mostrar texto completo\ncon info de contacto" as UC1
}

U --> CU28
CU28 .> UC1 : <<include>>
@enduml
```

---

## CU-29: Enviar email de bienvenida

```plantuml
@startuml CU-29
left to right direction
title CU-29 — Enviar email de bienvenida

actor "Sistema" as Srv

rectangle "Guander" {
  usecase "Enviar email\nde bienvenida" as CU29
  usecase "Llamar al servicio\nde email (asíncrono)" as UC1
  usecase "Confirmar registro\nsin esperar al email" as UC2
}

actor "Resend" as Resend <<service>>

Srv --> CU29
CU29 .> UC1 : <<include>>
CU29 .> UC2 : <<include>>
Resend --> UC1
@enduml
```

---

## CU-30: Calcular distancia a locales

```plantuml
@startuml CU-30
left to right direction
title CU-30 — Calcular distancia a locales

actor "Sistema" as Srv

rectangle "Guander — Backend" {
  usecase "Calcular distancia\na locales" as CU30
  usecase "Calcular distancia\na cada establecimiento" as UC1
  usecase "Ordenar resultados\npor distancia" as UC2
  usecase "Incluir distancia\nen la respuesta" as UC3
}

Srv --> CU30
CU30 .> UC1 : <<include>>
UC1 .> UC2 : <<include>>
CU30 .> UC3 : <<include>>
@enduml
```

---

## CU-31: Calcular puntos por compra

```plantuml
@startuml CU-31
left to right direction
title CU-31 — Calcular puntos por compra

actor "Sistema" as Srv

rectangle "Guander — Backend" {
  usecase "Calcular puntos\npor compra" as CU31
  usecase "Calcular puntos\nsegún monto (mín. 1)" as UC1
  usecase "Sumar puntos al\nsaldo del usuario" as UC2
  usecase "Registrar operación\ncon fecha y hora" as UC3
}

Srv --> CU31
CU31 .> UC1 : <<include>>
CU31 .> UC2 : <<include>>
CU31 .> UC3 : <<include>>
@enduml
```
