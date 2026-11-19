# Especificación Técnica y Guía de Implementación: CRM Inmobiliario Web (Sin Base de Datos)

## 1. Contexto y Objetivo del Proyecto

> **Referencia de diseño:** la interfaz debe seguir el sistema de diseño de la **Sección 9** (inspirado en las prácticas públicas de Stripe, Linear y Vercel: tokens propios, componentes accesibles, un único color de acento y movimiento sobrio), implementado solo con HTML, CSS y JavaScript nativos.

Construir una aplicación web de **CRM para una inmobiliaria** como proyecto universitario, operando **sin motor de base de datos tradicional** (sin SQL ni NoSQL externo) y utilizando exclusivamente **archivos `.json` como fuente inicial (semilla)** combinados con almacenamiento en el navegador (`localStorage`) y sincronización entre pestañas (`BroadcastChannel`).

La aplicación debe contar con una **pantalla de inicio de sesión (Login)** validada contra el archivo JSON de usuarios y ofrecer **dos interfaces diferenciadas por rol** dentro de la misma arquitectura:

1. **Interfaz del Cliente (`client`):** Exploración del catálogo de inmuebles con filtros, visualización de detalles, manifestación de interés en propiedades (generación de *lead*) y **chat en tiempo real** con el asesor asignado.

2. **Interfaz del Vendedor / Administrador (`agent` / `admin`):** Panel de control (Dashboard con KPIs), gestión completa (CRUD) del inventario de propiedades, gestión del embudo comercial de clientes (*leads* y estados de negociación), **bandeja de chat centralizada** para responder a los clientes en tiempo real, y herramientas de respaldo (**Exportar JSON actualizado** / **Restaurar datos semilla**).

## 2. Stack Tecnológico y Restricciones de Arquitectura

* **Lenguajes:** HTML5 semántico, CSS3 moderno (Variables CSS, Flexbox, CSS Grid, diseño responsivo) y JavaScript ES6+ (ES Modules nativos con `type="module"`).

* **Persistencia:** Archivos `.json` estáticos en `/data/` (carga inicial vía `fetch`) + `localStorage` (estado mutable de la aplicación) + `sessionStorage` (sesión activa por pestaña).

* **Comunicación en Tiempo Real (Chat y Estado):** API nativa `BroadcastChannel` + evento `window.addEventListener('storage')` como respaldo, permitiendo abrir **una pestaña logueada como Cliente** y **otra pestaña logueada como Administrador** con actualización instantánea de mensajes y cambios de inventario.

* **Diseño y UI:** sistema de diseño propio basado en **CSS Variables (tokens)**, componentes reutilizables en CSS puro, elementos nativos accesibles (`<dialog>`, `<button>`, `<details>`), iconos SVG inline y fuente **Inter** con *fallback* a `system-ui`. Detalle completo en la **Sección 9**.

* **Restricciones estrictas:**

  * **Cero frameworks pesados** (sin React, Angular, Vue ni jQuery) y **cero librerías de componentes o CSS** (sin Bootstrap, Tailwind ni Material). Los patrones de Stripe/Linear/Vercel se replican con CSS propio.

  * **Cero backends obligatorios** (debe funcionar sirviendo archivos estáticos con Live Server, `npx serve`, `python -m http.server` o similar).

  * **Seguridad básica:** Sanitizar cualquier texto ingresado por el usuario (especialmente en formularios y chat) antes de insertarlo en el DOM para evitar inyección XSS.

## 3. Estructura de Directorios y Archivos

El agente debe respetar exactamente esta organización modular para mantener la separación de responsabilidades (*Separation of Concerns*):

```
crm-inmobiliario/
├── index.html                  # Punto de entrada único (Vista Login + Vistas dinámicas por rol)
├── README.md                   # Instrucciones de ejecución, credenciales de acceso y arquitectura
├── data/                       # Datos semilla iniciales (solo lectura vía fetch)
│   ├── users.json              # Usuarios con credenciales (username, password, role)
│   ├── properties.json         # Catálogo inicial de propiedades inmobiliarias
│   ├── leads.json              # Relaciones de interés cliente <-> propiedad y etapa comercial
│   └── conversations.json      # Hilos de chat y mensajes iniciales
├── assets/
│   ├── img/                    # Imágenes locales de respaldo o placeholders SVG
│   ├── icons/
│   │   └── sprite.svg          # Sprite de iconos SVG (trazo 1.5px, 16/20px)
│   └── fonts/                  # (Opcional) Inter autoalojada en .woff2
├── css/
│   ├── variables.css           # Tokens de diseño (Sección 9.3): color, tipografía, espaciado, radios, sombras, movimiento
│   ├── base.css                # Reset, tipografía base, utilidades, foco visible, movimiento y `prefers-reduced-motion`
│   ├── components.css          # Componentes (Sección 9.5): botones, campos, badges, tarjetas, tablas, KPIs, modal, toast, pestañas, estados vacío/carga/error
│   ├── layout.css              # Estructura de login, navegación, sidebar del asesor, navbar del cliente y grids
│   └── chat.css                # Estilos dedicados al widget/bandeja de mensajería en tiempo real
└── js/
    ├── app.js                  # Inicializador principal, guardia de rutas/sesión y suscripción a eventos
    ├── config.js               # Constantes globales (claves de localStorage, BroadcastChannel, credenciales fallback)
    ├── services/
    │   ├── storageService.js   # Carga inicial de JSON, lectura/escritura en localStorage, exportar/resetear JSON
    │   ├── syncService.js      # Gestión de BroadcastChannel para sincronizar chat y cambios entre pestañas
    │   ├── authService.js      # Validación de login (username/password), sesión en sessionStorage y logout
    │   ├── propertyService.js  # Lógica de negocio y CRUD de propiedades + filtros
    │   ├── leadService.js      # Gestión de prospectos, etapas del embudo y métricas KPI
    │   └── chatService.js      # Creación de hilos de conversación, envío de mensajes y conteo de no leídos
    ├── ui/
    │   ├── toastUI.js          # Notificaciones visuales no intrusivas (éxito, error, advertencia)
    │   ├── authUI.js           # Formulario de Login, botones de autocompletado demo y cierre de sesión
    │   ├── clientUI.js         # Renderizado del catálogo cliente, detalle de inmueble y modal de contacto
    │   ├── adminUI.js          # Renderizado del dashboard, tabla/tarjetas CRUD de inmuebles y tablero de leads
    │   └── chatUI.js           # Renderizado compartido de la interfaz de chat (adaptada a cada rol)
    └── utils/
        ├── validators.js       # Validación de formularios (login, precios, campos requeridos, formatos)
        └── formatters.js       # Formateo de moneda, fechas/horas relativas y sanitización HTML (escapeHTML)

```

## 4. Esquema de Datos JSON (Contratos de Datos)

Todos los servicios deben trabajar bajo estos esquemas estrictos. Si `localStorage` está vacío al iniciar, `storageService.js` debe hacer `fetch` a los 4 archivos en `/data/` y persistirlos.

### 4.1. `data/users.json` (Credenciales Oficiales)

Las cuentas principales obligatorias del sistema son:
* **Administrador / Vendedor:** usuario `admin` | contraseña `admin`
* **Cliente:** usuario `client` | contraseña `client`

```
[
  {
    "id": "usr_admin_01",
    "username": "admin",
    "password": "admin",
    "name": "Laura Mendoza (Asesora)",
    "email": "admin@inmocrm.com",
    "phone": "+57 300 111 2233",
    "role": "admin",
    "avatar": "LM"
  },
  {
    "id": "usr_client_01",
    "username": "client",
    "password": "client",
    "name": "Carlos Pérez",
    "email": "carlos.perez@correo.com",
    "phone": "+57 310 444 5566",
    "role": "client",
    "avatar": "CP"
  },
  {
    "id": "usr_client_02",
    "username": "ana.gomez",
    "password": "1234",
    "name": "Ana Gómez",
    "email": "ana.gomez@correo.com",
    "phone": "+57 320 777 8899",
    "role": "client",
    "avatar": "AG"
  }
]

```

### 4.2. `data/properties.json`

*Estados válidos (`status`):* `"available"` (Disponible), `"negotiation"` (En negociación), `"sold"` (Vendida/Arrendada).
*Operaciones válidas (`operation`):* `"sale"` (Venta), `"rent"` (Arriendo).

```
[
  {
    "id": "prop_001",
    "title": "Apartamento Moderno con Vista al Mar",
    "type": "Apartamento",
    "operation": "sale",
    "price": 450000000,
    "currency": "COP",
    "location": "Bocagrande, Cartagena",
    "areaM2": 115,
    "bedrooms": 3,
    "bathrooms": 2,
    "parking": 1,
    "status": "available",
    "agentId": "usr_admin_01",
    "description": "Amplio apartamento iluminado con balcón panorámico, cocina integral y zonas sociales completas.",
    "imageUrl": "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
    "createdAt": "2026-10-15T10:00:00.000Z"
  }
]

```

*(Incluir al menos 6 propiedades variadas en el archivo semilla: casas, apartamentos, locales, en venta y arriendo).*

### 4.3. `data/leads.json`

Vincula a un cliente con una propiedad específica y rastrea su avance comercial.
*Etapas válidas (`stage`):* `"new"` (Nuevo interés), `"contacted"` (Contactado), `"visit"` (Visita agendada), `"negotiation"` (Negociación), `"closed_won"` (Cerrado ganado), `"closed_lost"` (Descartado).

```
[
  {
    "id": "lead_001",
    "clientId": "usr_client_01",
    "agentId": "usr_admin_01",
    "propertyId": "prop_001",
    "stage": "contacted",
    "notes": "Cliente interesado en financiar el 70% con crédito hipotecario.",
    "createdAt": "2026-11-10T14:30:00.000Z",
    "updatedAt": "2026-11-12T09:15:00.000Z"
  }
]

```

### 4.4. `data/conversations.json`

Cada conversación está vinculada a un cliente, un asesor y opcionalmente a la propiedad que originó la consulta.

```
[
  {
    "id": "conv_001",
    "clientId": "usr_client_01",
    "agentId": "usr_admin_01",
    "propertyId": "prop_001",
    "lastUpdated": "2026-11-18T16:20:00.000Z",
    "messages": [
      {
        "id": "msg_001",
        "senderId": "usr_client_01",
        "senderRole": "client",
        "text": "Hola, me interesa el apartamento en Bocagrande. ¿Sigue disponible?",
        "timestamp": "2026-11-18T16:15:00.000Z",
        "read": true
      },
      {
        "id": "msg_002",
        "senderId": "usr_admin_01",
        "senderRole": "admin",
        "text": "¡Hola Carlos! Sí, está disponible. ¿Te gustaría agendar una visita esta semana?",
        "timestamp": "2026-11-18T16:20:00.000Z",
        "read": false
      }
    ]
  }
]

```

## 5. Requerimientos Funcionales por Módulo

### 5.1. Sistema de Autenticación y Control de Sesión (`authService.js` / `authUI.js`)

1. **Pantalla de Inicio de Sesión (Vista por defecto al entrar sin sesión):**

   * Formulario limpio y profesional con campos **Usuario** (`username`) y **Contraseña** (`password`), además de un botón para mostrar/ocultar contraseña.

   * **Validación:** `authService.login(username, password)` busca coincidencia exacta (insensible a mayúsculas en `username`, sensible en `password`) dentro de la lista `users` almacenada en `storageService`.

   * Si las credenciales son incorrectas, muestra un mensaje de error claro (`"Usuario o contraseña incorrectos"`) sin recargar la página.

   * **Ayuda para evaluación universitaria (Botones de acceso rápido en la tarjeta de login):** Incluir dos botones discretos debajo del formulario: *"Autocompletar Admin (`admin` / `admin`)"* y *"Autocompletar Cliente (`client` / `client`)"* para facilitar las pruebas rápidas durante la sustentación.

2. **Registro de Nuevos Clientes (Opcional en pestaña dentro del Login):**

   * Permitir que un visitante cree una cuenta nueva con `role: "client"`, guardándola en `users` dentro de `localStorage` y sincronizando con el panel del administrador.

3. **Aislamiento de Sesión por Pestaña (`sessionStorage`):**

   * Al iniciar sesión exitosamente, guardar el objeto del usuario autenticado (sin incluir el campo `password`) en `sessionStorage` (`inmocrm_active_session`).

   * **Por qué usar `sessionStorage`:** Permite abrir **Pestaña 1** e iniciar sesión con `client` / `client`, y abrir **Pestaña 2** e iniciar sesión con `admin` / `admin` en el mismo navegador sin que una sesión cierre o pise a la otra.

4. **Protección de Vistas y Cierre de Sesión:**

   * Al cargar `app.js`, verificar si existe sesión activa en `sessionStorage`:

     * Si **no hay sesión**: mostrar únicamente la vista de Login.

     * Si `role === 'client'`: renderizar exclusivamente la Interfaz del Cliente.

     * Si `role === 'admin'`: renderizar exclusivamente la Interfaz del Administrador/Vendedor.

   * Incluir en la barra superior de ambas interfaces el nombre del usuario logueado, su rol y un botón **"Cerrar Sesión"** (`authService.logout()`) que limpia `sessionStorage` y regresa al Login.

### 5.2. Interfaz del Cliente (`clientUI.js`)

1. **Catálogo de Inmuebles:**

   * Tarjetas visuales limpias con imagen, badge de estado (`Disponible`, `En negociación`, `Vendida`), operación (`Venta` / `Arriendo`), precio formateado, ubicación y características principales ($m^2$, habitaciones, baños).

   * **Filtros en tiempo real:** Búsqueda por texto (título/ubicación), filtro por tipo de inmueble, operación, rango de precio máximo y estado.

2. **Detalle de Propiedad (Modal o Vista Detallada):**

   * Muestra la descripción completa, datos del asesor responsable y botón de acción principal: **"Consultar / Me interesa"**.

   * Al hacer clic en *"Consultar / Me interesa"*:

     * Crea automáticamente un registro en `leads` (si no existía uno activo para ese cliente y esa propiedad).

     * Abre o crea el hilo correspondiente en `conversations` asociado a esa propiedad y abre el panel de chat listo para escribir.

3. **Mis Consultas y Chat (`chatUI.js`):**

   * Lista de conversaciones activas del cliente logueado (indicando de qué propiedad es cada chat y si tiene mensajes sin leer).

   * Ventana de chat con burbujas diferenciadas (enviados a la derecha, recibidos a la izquierda), hora del mensaje y envío con `Enter` o botón.

### 5.3. Interfaz del Vendedor / Administrador (`adminUI.js`)

1. **Dashboard de Resumen (KPIs):**

   * Tarjetas dinámicas calculadas desde el JSON actual: Total de propiedades, Propiedades disponibles, Valor total del inventario disponible, Leads activos y Conversaciones con mensajes sin leer.

2. **Gestión de Propiedades (CRUD completo):**

   * Listado con buscador y acciones rápidas: **Crear nueva propiedad**, **Editar propiedad**, **Cambiar estado rápido** (Disponible / En negociación / Vendida) y **Eliminar propiedad** (con confirmación).

   * Cualquier cambio se refleja inmediatamente en la vista del cliente.

3. **Gestión de Clientes / Leads (CRM):**

   * Vista en tabla o columnas por etapa del embudo (`Nuevo`, `Contactado`, `Visita`, `Negociación`, `Cerrado`).

   * Permite cambiar la etapa del lead, agregar notas comerciales y abrir directamente el chat con ese cliente.

4. **Centro de Mensajería del Asesor (`chatUI.js`):**

   * Bandeja unificada con todos los clientes que han escrito, mostrando el nombre del cliente, la propiedad de referencia, el último mensaje y un contador de mensajes no leídos.

   * Al abrir un chat, marca automáticamente los mensajes del cliente como leídos (`read: true`).

5. **Herramientas de Datos JSON (`storageService.js`):**

   * **Exportar Base de Datos JSON:** Descarga un archivo `crm_backup_YYYY-MM-DD.json` con el estado completo actual (`users`, `properties`, `leads`, `conversations`).

   * **Importar JSON:** Permite cargar un archivo `.json` previamente exportado y validar su estructura antes de aplicarlo.

   * **Restaurar Datos de Fábrica:** Limpia `localStorage` y vuelve a cargar los archivos originales de `/data/`.

### 5.4. Sincronización en Tiempo Real (`syncService.js`)

* Instanciar `const channel = new BroadcastChannel('inmocrm_sync_channel');`.

* Cada vez que `storageService.js` guarde una modificación (nuevo mensaje, nuevo lead, propiedad creada/editada), debe emitir un evento por el canal:

  ```
  channel.postMessage({ type: 'DATA_UPDATED', entity: 'conversations', timestamp: Date.now() });
  
  ```

* Todas las pestañas abiertas escuchan `channel.onmessage`, recargan el estado desde `localStorage` y vuelven a renderizar únicamente el componente afectado (por ejemplo, la ventana de chat activa y el contador de no leídos), haciendo scroll automático al último mensaje.

## 6. Estándares de Código Limpio y Manejo de Errores

El agente debe cumplir obligatoriamente con las siguientes prácticas:

1. **Arquitectura desacoplada:** Los archivos en `/js/ui/` **nunca** deben llamar directamente a `localStorage`, `sessionStorage` ni `fetch`. Toda lectura o escritura debe pasar por `/js/services/`.

2. **Prevención de XSS:** Todo texto generado por usuarios (mensajes de chat, notas de leads, títulos de propiedades) debe pasar por `escapeHTML(str)` antes de usarse en `innerHTML`, o inyectarse mediante `textContent`.

3. **Manejo robusto de errores (`try / catch`):**

   * Si el usuario abre `index.html` con doble clic (`file://`) en lugar de un servidor HTTP local, `fetch('./data/users.json')` fallará por CORS. El agente debe incluir un **fallback de datos semilla embebidos** en `config.js` (incluyendo los usuarios `admin`/`admin` y `client`/`client`) para que el login y la aplicación funcionen incluso sin servidor HTTP.

   * Validar que las imágenes externas tengan un manejador `onerror` que cargue un placeholder SVG local en caso de fallo de red o URL inválida.

   * Validar campos numéricos (precio > 0, área > 0, habitaciones >= 0) y campos obligatorios antes de guardar en `propertyService.js`.

4. **Identificadores únicos:** Generar IDs consistentes usando `crypto.randomUUID()` (con fallback a `Date.now().toString(36)`).

5. **Estilos con tokens:** ningún archivo CSS fuera de `variables.css` puede declarar colores, radios, sombras o duraciones literales; todo se consume con `var(--token)`. Prohibidos los estilos en línea y `alert()`/`confirm()`/`prompt()` nativos (usar toasts y `<dialog>`).

6. **Código autodocumentado:** Funciones pequeñas (máximo 30-40 líneas), nombres descriptivos en inglés para código (`loginUser`, `createProperty`, `sendMessage`, `renderChatWindow`) y textos de interfaz completamente en español natural.

## 7. Orden Recomendado de Implementación para el Agente

Ejecutar la construcción en estas 6 iteraciones verificables:

* **Paso 1 (Base, Datos y Tokens):** Crear la estructura de carpetas, los 4 archivos `.json` en `/data/` (con `admin`/`admin` y `client`/`client` en `users.json`), `css/variables.css` (tokens de la Sección 9.3), `css/base.css`, `css/components.css` (botones, campos y badges como mínimo) y `js/config.js` (incluyendo el fallback de datos semilla).

* **Paso 2 (Capa de Servicios y Auth):** Implementar `utils/formatters.js`, `utils/validators.js`, `services/storageService.js`, `services/syncService.js` y `services/authService.js`.

* **Paso 3 (Lógica de Negocio):** Implementar `propertyService.js`, `leadService.js` y `chatService.js`.

* **Paso 4 (Shell UI y Pantalla de Login):** Construir `index.html` (con `lang="es"` y viewport), `css/layout.css`, el sprite de iconos, `ui/toastUI.js` y `ui/authUI.js` con el formulario de inicio de sesión, botones de autocompletado demo y control de cierre de sesión.

* **Paso 5 (Vistas Principales):** Implementar `ui/clientUI.js` (catálogo + filtros + detalle) y `ui/adminUI.js` (KPIs + CRUD propiedades + gestión de leads + exportar/resetear JSON).

* **Paso 6 (Chat en Tiempo Real y Pulido):** Implementar `css/chat.css` y `ui/chatUI.js` conectado a `syncService.js`, aplicar la revisión de diseño (Secciones 9.6 a 9.9: movimiento, accesibilidad y responsivo) y ejecutar la matriz de pruebas.

## 8. Matriz de Pruebas de Error y Validación (QA Checklist)

Antes de dar por terminado el proyecto, el agente debe verificar que se cumplan todos estos casos de prueba:

| ID | Caso de Prueba | Acción a Realizar | Resultado Esperado | 
| ----- | ----- | ----- | ----- | 
| **QA-01** | Carga inicial y resiliencia | Iniciar la app con `localStorage` vacío (tanto en servidor local como en `file://`). | Muestra la pantalla de Login sin errores en consola y carga los datos semilla en segundo plano. | 
| **QA-02** | Autenticación y control de errores | Intentar iniciar sesión con credenciales erróneas (`admin` / `1234`), luego probar `admin` / `admin` y `client` / `client`. | Bloquea el acceso erróneo con alerta clara; `admin`/`admin` carga el panel Administrador y `client`/`client` carga el portal Cliente. | 
| **QA-03** | Sesiones simultáneas (2 pestañas) | Iniciar sesión en Pestaña 1 con `client`/`client` y en Pestaña 2 con `admin`/`admin`. | Ambas pestañas mantienen sus roles independientes sin cerrarse ni sobrescribirse entre sí. | 
| **QA-04** | Persistencia tras recarga | Crear una propiedad como Admin y recargar la página (`F5`). | Mantiene la sesión activa del Admin y la propiedad creada permanece en el inventario y en el catálogo del cliente. | 
| **QA-05** | Chat en tiempo real (2 pestañas) | Desde la Pestaña 1 (`client`), enviar un mensaje en el chat. | El mensaje aparece instantáneamente en la Pestaña 2 (`admin`) sin recargar, incrementando el indicador de no leídos. | 
| **QA-06** | Respuesta y estado leído | Responder desde la Pestaña 2 (`admin`) al cliente. | El mensaje llega al instante a la Pestaña 1 (`client`) y el contador de no leídos del asesor baja a `0`. | 
| **QA-07** | Flujo de Lead desde catálogo | Como Cliente (`client`), abrir una propiedad nueva y pulsar *"Me interesa / Consultar"*. | Se crea el lead en estado `"new"`, se abre el chat vinculado a esa propiedad y aparece en el panel del Admin. | 
| **QA-08** | Validación de formularios | Intentar guardar una propiedad con precio negativo, título vacío o URL rota. | El sistema bloquea el guardado, muestra un mensaje claro de error y no corrompe el JSON. | 
| **QA-09** | Prevención XSS en chat | Enviar `<script>alert('xss')</script>` o `<img src=x onerror=alert(1)>` en el chat. | Se renderiza como texto plano literal sin ejecutar código JavaScript. | 
| **QA-10** | Exportación y Restauración | Exportar el JSON, eliminar todas las propiedades y luego importar el archivo o pulsar *"Restaurar datos"*. | El sistema recupera íntegro todo el catálogo, leads y conversaciones sin errores. | 
| **QA-11** | Tokens y consistencia visual | Buscar en `css/` valores literales de color, radio o duración fuera de `variables.css`, y estilos en línea en el HTML/JS. | No existen valores literales ni `style=""`; todo usa `var(--token)`. |
| **QA-12** | Estados de componentes | Navegar con teclado por botones, campos y tarjetas; forzar errores, carga y estado deshabilitado. | Todo elemento interactivo muestra foco visible, y los botones tienen estados hover, activo, deshabilitado y carga. |
| **QA-13** | Accesibilidad | Recorrer login, catálogo y chat solo con teclado; revisar contraste y lectura de `aria-live`. | Contraste AA, `Esc` cierra los modales, el foco vuelve al disparador y los mensajes nuevos se anuncian. |
| **QA-14** | Responsivo | Probar a 360px, 768px y 1280px de ancho. | Sin scroll horizontal inesperado; catálogo 1/2/3+ columnas; objetivos táctiles de 44px en móvil; campos a 16px. |
| **QA-15** | Movimiento | Revisar transiciones y activar `prefers-reduced-motion`. | Ninguna animación supera 300ms (modales hasta 500ms), no hay `transition: all` ni `ease-in`; con movimiento reducido solo quedan fundidos. |
| **QA-16** | Estados vacío, carga y error | Vaciar el catálogo con filtros, simular un fallo de imagen y entrar sin conversaciones. | Se muestran estados vacíos con acción, *skeletons* y placeholder SVG; nunca una pantalla en blanco. |

## 9. Sistema de Diseño y Estándares de Interfaz (Referencia: Stripe, Linear y Vercel)

Esta sección es **normativa**: el agente debe construir la interfaz con estos criterios. El objetivo es que el CRM se vea y se sienta como un producto SaaS profesional (sobrio, denso en información, muy legible), manteniendo la restricción de **cero frameworks**: todo se resuelve con HTML, CSS y JavaScript nativos.

### 9.1. Qué usan las empresas de referencia (y su equivalente en este proyecto)

Las empresas de referencia no usan un kit visual genérico. Construyen un **sistema de diseño propio** (tokens + componentes) sobre React y TypeScript, apoyándose en primitivas accesibles sin estilo. Los datos de la tabla provienen de fuentes públicas (ofertas de empleo, blogs de ingeniería y análisis de terceros del sitio), por lo que son referencia orientativa y no documentación oficial.

| Empresa | Qué usan (público) | Equivalente en este proyecto (sin frameworks) |
| --- | --- | --- |
| **Stripe** | React + TypeScript. Sistema de diseño interno para el producto (*Sail*) y otro para stripe.com (*Helm*). Tipografía propia (Söhne), color de marca índigo. | Sistema propio en `variables.css` + `components.css`; una sola fuente sans; un único color de acento. |
| **Linear** | Primitivas de Radix UI como base de componentes, fuente Inter, modo oscuro como protagonista, un solo acento violeta. | Componentes nativos accesibles (`<dialog>`, `<button>`, `<details>`) + atributos ARIA; Inter con *fallback* a `system-ui`. |
| **Vercel** | Sistema y fuente propios (Geist), monocromático, sombras finas en lugar de bordes gruesos. | Escala de grises neutra + sombras de 1px como borde (`box-shadow`). |

**Lección común:** (1) tokens propios en lugar de valores sueltos, (2) componentes accesibles sin reinventar foco, teclado ni diálogos, (3) una paleta mínima con un solo color de acento, (4) tipografía y espaciado consistentes, (5) cuidado en el detalle de las interacciones.

**Regla de este proyecto:** está prohibido usar librerías de componentes o CSS (Bootstrap, Tailwind, Material, etc.). Sí se permite cargar la fuente Inter (autoalojada en `assets/fonts/` o desde Google Fonts); si no carga (por ejemplo, en `file://` sin internet), debe verse bien con el *fallback* del sistema.

### 9.2. Principios de diseño

1. **Sobriedad primero:** fondo neutro, texto oscuro, **un único color de acento** para la acción principal. El resto son grises. Los colores semánticos (verde, ámbar, rojo) se reservan para estados.
2. **Densidad legible:** en el panel del asesor el tamaño base de texto es **14px** (no 16px); en el catálogo del cliente puede ser 15-16px. Los números (precios, KPIs, tablas) usan `font-variant-numeric: tabular-nums`.
3. **Jerarquía por peso y tono**, no por colores: títulos en peso 600, texto secundario en gris medio.
4. **Una acción principal por pantalla o tarjeta.** Las demás acciones son secundarias o discretas.
5. **Todo estado tiene diseño:** vacío, cargando, error, deshabilitado, foco y selección. Ninguna pantalla puede quedar en blanco.
6. **Bordes sutiles y sombras suaves:** profundidad con sombras de baja opacidad, no con bordes oscuros.

### 9.3. Tokens de diseño (`css/variables.css`)

Todos los estilos deben usar estas variables; está prohibido escribir colores, radios o duraciones directamente en otros archivos CSS.

```css
:root {
  /* Marca: un solo acento (inspirado en el índigo de Stripe; ajustable a la marca de la inmobiliaria) */
  --color-accent: #4f46e5;
  --color-accent-hover: #4338ca;
  --color-accent-soft: #eef2ff;      /* fondos suaves, selección, foco */

  /* Neutros */
  --color-bg: #f7f8fa;               /* fondo de la app */
  --color-surface: #ffffff;          /* tarjetas, modales */
  --color-surface-muted: #f1f3f6;    /* cabeceras de tabla, hover de filas */
  --color-border: #e3e6eb;
  --color-text: #1a1f2b;
  --color-text-muted: #5b6475;       /* contraste >= 4.5:1 sobre --color-surface */
  --color-text-faint: #8a93a3;       /* solo para metadatos no críticos */

  /* Semánticos (estados de propiedad y de lead) */
  --color-success: #0e7a4b;  --color-success-bg: #e6f6ee;   /* Disponible, Cerrado ganado */
  --color-warning: #9a5b00;  --color-warning-bg: #fff4dc;   /* En negociación */
  --color-danger:  #b42318;  --color-danger-bg:  #fdeceb;   /* Vendida/Descartado, errores */
  --color-info:    #1d5fd0;  --color-info-bg:    #e8f0fe;   /* Nuevo interés, Visita */

  /* Tipografía */
  --font-sans: "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-mono: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
  --text-xs: 12px;  --text-sm: 13px;  --text-base: 14px;
  --text-md: 16px;  --text-lg: 20px;  --text-xl: 28px;

  /* Espaciado (múltiplos de 4px) */
  --space-1: 4px;  --space-2: 8px;   --space-3: 12px;  --space-4: 16px;
  --space-5: 24px; --space-6: 32px;  --space-7: 48px;

  /* Radios */
  --radius-sm: 6px;   /* botones, inputs, badges */
  --radius-md: 10px;  /* tarjetas */
  --radius-lg: 14px;  /* modales */
  --radius-full: 999px;

  /* Sombras (el "borde" es una sombra de 1px) */
  --shadow-border: 0 0 0 1px var(--color-border);
  --shadow-sm: 0 1px 2px rgba(16, 24, 40, .06), 0 0 0 1px var(--color-border);
  --shadow-md: 0 4px 12px rgba(16, 24, 40, .08), 0 0 0 1px var(--color-border);
  --shadow-lg: 0 16px 40px rgba(16, 24, 40, .16);
  --focus-ring: 0 0 0 2px var(--color-surface), 0 0 0 4px var(--color-accent);

  /* Movimiento */
  --ease-out: cubic-bezier(0.23, 1, 0.32, 1);
  --ease-in-out: cubic-bezier(0.77, 0, 0.175, 1);
  --duration-press: 120ms;   /* feedback al pulsar */
  --duration-fast: 160ms;    /* hover, tooltips, popovers */
  --duration-base: 220ms;    /* menús, desplegables */
  --duration-modal: 280ms;   /* modales y panel de chat */

  /* Capas */
  --z-nav: 100;  --z-modal: 1000;  --z-toast: 1100;
}
```

### 9.4. Tipografía

* **Una sola familia sans** (`--font-sans`) más una monoespaciada para códigos (`--font-mono`). No mezclar fuentes de display.
* Escala: títulos de página 20-28px/peso 600 con `letter-spacing: -0.01em`; subtítulos 16px/600; cuerpo 14px/400; metadatos 12-13px.
* Precios en el catálogo: 20px/600, tabulares. KPIs del dashboard: 28px/600.
* `line-height`: 1.5 en cuerpo; 1.2 en títulos. Longitud máxima de línea en descripciones: 65-70 caracteres.
* Prohibido usar Arial, Times o Roboto como fuente principal visible.

### 9.5. Catálogo de componentes (`css/base.css` y `css/components.css`)

Cada componente debe implementarse **una sola vez** como clase reutilizable y mostrar todos sus estados.

#### Botones

| Variante | Clase | Uso | Estilo |
| --- | --- | --- | --- |
| Primario | `.btn .btn--primary` | Una acción principal por vista ("Me interesa", "Guardar propiedad", "Iniciar sesión") | Fondo `--color-accent`, texto blanco |
| Secundario | `.btn .btn--secondary` | Acciones alternas ("Cancelar", "Editar") | Fondo blanco, `--shadow-border` |
| Fantasma | `.btn .btn--ghost` | Acciones discretas (iconos, "Autocompletar Admin") | Sin fondo; fondo `--color-surface-muted` al hover |
| Peligro | `.btn .btn--danger` | Eliminar (siempre con confirmación) | Texto/fondo rojo semántico |

Reglas comunes: altura 36px (32px en tablas, 44px mínimo en móvil), `padding: 0 var(--space-4)`, `border-radius: var(--radius-sm)`, peso 500.
Estados obligatorios: `:hover` (solo bajo `@media (hover: hover) and (pointer: fine)`), `:focus-visible` (anillo `--focus-ring`), `:active` (`transform: scale(0.97)`), `:disabled` (opacidad .5, sin eventos) y `.is-loading` (spinner a la izquierda, texto mantenido para no cambiar el ancho; el botón se deshabilita mientras tanto).

#### Campos de formulario

* Etiqueta siempre visible encima del campo (nunca solo *placeholder*). Ayuda o error debajo, en 12-13px.
* Altura 36-40px, borde por `--shadow-border`, radio `--radius-sm`. Foco: anillo `--focus-ring`. Error: borde y mensaje en `--color-danger` más `aria-invalid="true"` y `aria-describedby`.
* Tamaño de fuente mínimo **16px en móvil** para evitar el zoom automático de iOS al enfocar un campo.
* Contraseña con botón mostrar/ocultar (icono, con `aria-label` y `aria-pressed`). Precio con prefijo `$` y separadores de miles al salir del campo.
* Validación al enviar y, después del primer error, en vivo. Mensajes en español natural y accionables ("El precio debe ser mayor que 0").

#### Badges de estado

Píldoras (`--radius-full`), 12px/500, fondo suave + texto del color semántico, con un punto de color de 6px a la izquierda:

| Dominio | Valor | Color |
| --- | --- | --- |
| Propiedad | `available` | success |
| Propiedad | `negotiation` | warning |
| Propiedad | `sold` | danger |
| Lead | `new` / `contacted` | info |
| Lead | `visit` / `negotiation` | warning |
| Lead | `closed_won` | success |
| Lead | `closed_lost` | neutro (gris) |

El estado **nunca se comunica solo con color**: siempre incluye texto.

#### Tarjeta de propiedad (catálogo)

* Imagen con relación 4:3 (`aspect-ratio`), `object-fit: cover`, esquinas superiores redondeadas, `loading="lazy"` y `onerror` hacia el placeholder SVG.
* Contenido: badge de operación y estado, **precio** (jerarquía más alta), título en 1-2 líneas con *clamp*, ubicación en gris y una fila de características con iconos SVG (m², habitaciones, baños, parqueadero).
* Superficie `--color-surface`, `--shadow-sm`; al hover (solo dispositivos con puntero) `--shadow-md` y elevación de 2px con `transform`.
* Toda la tarjeta es clicable y accesible por teclado (`<a>` o `<button>` con foco visible).

#### Tablas y listados (panel del asesor)

* Cabecera en `--color-surface-muted`, 12px, mayúsculas suaves y peso 500; filas de 48px; separadores de 1px; hover de fila con `--color-surface-muted`.
* Columnas numéricas alineadas a la derecha con cifras tabulares. Acciones por fila en botones fantasma o menú de tres puntos.
* En pantallas estrechas, la tabla pasa a tarjetas apiladas (no scroll horizontal oculto).

#### KPIs (dashboard)

Tarjeta con etiqueta de 13px en gris, valor de 28px/600 tabular y, si aplica, una variación en texto pequeño. Cuadrícula de 4 columnas en escritorio, 2 en tablet y 1 en móvil. Sin gráficos decorativos innecesarios.

#### Modal (detalle de propiedad, formularios, confirmaciones)

* Usar el elemento nativo `<dialog>` con `showModal()`: aporta *focus trap*, cierre con `Esc` y capa de fondo sin código extra.
* Fondo con `backdrop` a baja opacidad, contenedor `--radius-lg` y `--shadow-lg`, ancho máximo 640px (96vw en móvil).
* Al cerrar, devolver el foco al elemento que lo abrió. Título con `aria-labelledby`.
* El modal **no** escala desde el punto de origen: se queda centrado (entrada con opacidad y `scale(0.96)` → `1`).
* Confirmación de eliminación: texto claro ("Se eliminará *Apartamento en Bocagrande*. Esta acción no se puede deshacer"), botón de peligro a la derecha y botón secundario a la izquierda, con el foco inicial en **Cancelar**.

#### Toasts (`toastUI.js`)

Esquina inferior derecha (centrada en móvil), máximo 3 visibles, duración 4s (errores: 6s). Icono + texto corto; contenedor `role="status"` y `aria-live="polite"` (errores: `role="alert"`). Entran desde abajo con opacidad y `translateY` (transición, no *keyframes*), se pueden cerrar con un botón y se pausan al pasar el puntero. Nunca bloquean la interacción.

#### Pestañas, filtros y navegación

* Barra de navegación del cliente: logo, enlaces, nombre del usuario, indicador de chat con contador y "Cerrar sesión". Sidebar del asesor de 240px, con ítem activo resaltado en `--color-accent-soft` y `aria-current="page"`.
* Filtros del catálogo como una barra horizontal compacta (búsqueda + selects + rango de precio), con botón "Limpiar filtros" y contador de resultados ("12 propiedades"). Debounce de 200ms en la búsqueda.
* Embudo de leads en columnas (kanban) con encabezado por etapa y contador; en móvil, columnas en carrusel horizontal con *scroll-snap*.

#### Chat (`css/chat.css`)

* Burbujas: enviadas a la derecha con fondo `--color-accent` y texto blanco; recibidas a la izquierda con fondo `--color-surface-muted`. Radio 14px con la esquina del lado del remitente en 4px. Ancho máximo 75%.
* Agrupar mensajes consecutivos del mismo remitente (hora solo en el último). Separadores por día ("Hoy", "Ayer").
* Cabecera del hilo con nombre del interlocutor y propiedad vinculada (con enlace). Campo de texto que crece hasta 4 líneas; `Enter` envía y `Shift+Enter` agrega salto de línea.
* Contador de no leídos como píldora de acento. Scroll automático al último mensaje solo si el usuario ya estaba al final; si no, mostrar el botón "Nuevos mensajes ↓".
* Los mensajes se insertan siempre con `textContent` o `escapeHTML`.

#### Estados vacíos, cargando y error

* **Vacío:** icono SVG grande en gris, título breve, una frase de ayuda y una acción ("Aún no tienes consultas. Explora el catálogo").
* **Cargando:** *skeletons* (bloques grises con pulso suave) con las mismas medidas de la tarjeta o fila; nunca texto "Cargando..." suelto.
* **Error:** mensaje claro, causa probable y botón "Reintentar".

#### Iconografía

Usar **SVG inline** en un sprite local (`assets/icons/sprite.svg`) con trazo de 1.5px y tamaño 16/20px. Prohibido usar emojis como iconos de interfaz. Los iconos decorativos llevan `aria-hidden="true"`; los botones solo de icono llevan `aria-label`.

### 9.6. Movimiento y microinteracciones (`css/base.css`)

Principio: **el movimiento comunica, no decora**. Se anima solo lo que el usuario ve pocas veces al día o lo que confirma una acción.

1. **Sin animación** en acciones repetitivas o de teclado: cambio de filtros, filtrado de lista, enviar con `Enter`, cambio de pestañas del panel.
2. **Solo `transform` y `opacity`.** Nunca animar `width`, `height`, `margin` o `padding`.
3. **Curvas:** `--ease-out` para entradas y salidas; `--ease-in-out` para movimientos en pantalla. Prohibido `ease-in` y `transition: all`: siempre listar las propiedades.
4. **Duraciones:** pulsación 120ms, hover/tooltips 160ms, menús 220ms, modales y panel de chat 280ms. Ninguna animación de interfaz supera 300ms (los modales llegan como máximo a 500ms).
5. **Pulsación:** todo elemento pulsable usa `transform: scale(0.97)` en `:active`.
6. **Entradas:** nunca partir de `scale(0)`; usar `scale(0.95)` con `opacity: 0`. Mensajes nuevos del chat: `opacity` + `translateY(8px)`, 160ms.
7. **Preferir transiciones a *keyframes*** en elementos que pueden dispararse varias veces seguidas (toasts, mensajes). Usar `@starting-style` o el atributo `data-mounted` como alternativa.
8. **Hover solo con puntero fino:** `@media (hover: hover) and (pointer: fine)`.
9. **`prefers-reduced-motion: reduce`:** eliminar desplazamientos y escalas; conservar únicamente fundidos de opacidad cortos.
10. **Escalonado (stagger)** opcional en la primera carga del catálogo: 30-60ms entre tarjetas, máximo 8 tarjetas, sin bloquear la interacción.

```css
.btn { transition: transform var(--duration-press) var(--ease-out),
                   background-color var(--duration-fast) var(--ease-out),
                   box-shadow var(--duration-fast) var(--ease-out); }
.btn:active { transform: scale(0.97); }

@media (hover: hover) and (pointer: fine) {
  .card--property:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
```

### 9.7. Accesibilidad (nivel WCAG AA)

* Contraste de texto >= 4.5:1 y de componentes de interfaz >= 3:1.
* Todo es operable con teclado; el foco siempre es visible (`:focus-visible` con `--focus-ring`). Orden de tabulación lógico.
* Estructura semántica: `header`, `nav`, `main`, `section`, un solo `h1` por vista, `button` para acciones y `a` para navegación. Formularios con `label for`.
* Zonas dinámicas (toasts, mensajes nuevos de chat) con `aria-live`. Contadores de no leídos con texto accesible ("3 mensajes sin leer").
* Objetivos táctiles de al menos 44x44px en móvil. No depender solo del color para transmitir estado.
* Atributo `lang="es"` en `<html>` y `<meta name="viewport" content="width=device-width, initial-scale=1">`.

### 9.8. Diseño responsivo

* Enfoque *mobile-first* con puntos de quiebre en **640px**, **1024px** y **1280px**.
* Catálogo: 1 columna (móvil), 2 (tablet), 3-4 (escritorio) con `grid-template-columns: repeat(auto-fill, minmax(280px, 1fr))`.
* Panel del asesor: sidebar fija en escritorio; en móvil, menú inferior o botón de menú con panel deslizable.
* Chat: en escritorio, lista de conversaciones + ventana en dos columnas; en móvil, vistas alternadas con botón "Volver".
* Respetar áreas seguras (`env(safe-area-inset-*)`) y evitar `100vh`; usar `100dvh`.
* Alto contraste y legibilidad por encima de adornos. Modo oscuro opcional mediante `[data-theme="dark"]` redefiniendo solo los tokens de color.

### 9.9. Antipatrones prohibidos

* Más de un color de acento, degradados llamativos o sombras pesadas.
* Texto gris claro sobre fondo blanco que no cumpla el contraste.
* `alert()`, `confirm()` y `prompt()` nativos del navegador (usar toasts y `<dialog>`).
* Estilos en línea (`style=""`) y valores mágicos fuera de los tokens.
* Botones sin estado de foco, de carga o deshabilitado.
* Animaciones en cada elemento, en acciones de teclado o con duraciones superiores a 300ms.
* Iconos con emojis, imágenes sin dimensiones (provocan saltos de diseño) y tablas con scroll horizontal oculto en móvil.
* Texto de interfaz en inglés. Todo el texto visible va en español natural.
