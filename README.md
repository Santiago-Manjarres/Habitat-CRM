# Habitat CRM

CRM web para inmobiliarias, construido como proyecto universitario **sin base de datos**: los datos iniciales viven en archivos `.json`, el estado se guarda en el navegador (`localStorage`) y las pestañas abiertas se sincronizan al instante con `BroadcastChannel`.

El proyecto tiene dos páginas:

- **Landing (`index.html`):** presenta el producto a la inmobiliaria en lenguaje sencillo, sin términos técnicos, y lleva al CRM con el botón **«Ingresar al CRM»** del encabezado.
- **CRM (`app.html`):** login y panel del equipo de asesores. Hay **un solo tipo de usuario**, el asesor de la inmobiliaria, y todos ven la misma interfaz:
  - **Resumen:** un aviso con el siguiente paso («Tienes 2 clientes nuevos por contactar»), 4 indicadores, gráfico de clientes por etapa, inventario por estado y últimos movimientos.
  - **Propiedades:** crear, editar, cambiar estado y eliminar, con búsqueda.
  - **Clientes:** tablero por etapa. Cada cliente (un *lead* en el código) es una persona interesada que registra un asesor: nombre, teléfono o correo, cómo llegó, propiedad de interés y asesor responsable. Se puede crear, editar, cambiar de etapa y eliminar.

La interfaz habla el idioma de una inmobiliaria: dice «clientes», «etapa» y «ficha», nunca «lead», «JSON» o «pestaña». La opción de respaldo (exportar, importar y restaurar) está desactivada en la interfaz.

Está hecho con HTML, CSS y JavaScript nativos (ES Modules). No usa frameworks ni npm, y no requiere compilar nada.

---

## Cómo ejecutarlo

```bash
cd "HABITAT CRM"
python3 -m http.server 8000
```

Luego abre **http://localhost:8000** para ver la landing, o **http://localhost:8000/app.html** para ir directo al CRM. Cualquier otro servidor estático sirve igual (Live Server, `npx serve`).

**Con doble clic (`file://`):** Chrome, Edge y Brave bloquean los módulos de JavaScript en ese modo. El CRM muestra entonces un aviso con los pasos para levantar el servidor, en lugar de quedar en blanco. Firefox sí permite módulos en `file://` y usa los datos semilla incluidos en `js/config.js`; ese caso no se probó.

---

## Credenciales de prueba

| Asesor | Usuario | Contraseña |
| --- | --- | --- |
| Laura Mendoza | `admin` | `admin` |
| Andrés Rojas | `andres` | `1234` |
| Valentina Ríos | `valentina` | `1234` |

La pantalla de acceso tiene dos botones de cuenta de prueba (Laura Mendoza y Andrés Rojas) que llenan el usuario y la contraseña. El usuario no distingue mayúsculas; la contraseña sí. No hay registro público: los asesores vienen en `data/users.json`.

**Para probar la sincronización**, inicia sesión como `admin` en una pestaña y como `andres` en otra. Cada pestaña conserva su sesión (`sessionStorage`). Si registras un cliente en una pestaña, aparece al instante en el tablero, los indicadores y los últimos movimientos de la otra.

---

## Arquitectura

```
HABITAT CRM/
├── index.html              Landing del producto (taste-skill)
├── app.html                CRM: login + panel del asesor
├── data/                   Datos semilla: users.json (3 asesores), properties.json (9), leads.json (8)
├── assets/
│   ├── img/                logo.png, login-house.jpg, property-placeholder.svg
│   ├── icons/sprite.svg    Iconos SVG del CRM (copia en línea dentro de app.html)
│   ├── fonts/              Geist (variable, .woff2, licencia OFL)
│   └── system.css, system.js, ui/, motion/, layout/, moments/, backgrounds/
│                           Kit de taste-skill, usado solo por la landing (sin editar)
├── css/
│   ├── variables.css       Tokens del CRM: el ÚNICO archivo con colores, radios, sombras y duraciones
│   ├── base.css            Fuente, reset, tipografía, foco visible, prefers-reduced-motion
│   ├── components.css      Botones, campos, badges, KPIs, paneles, gráfico, tablas, modal, toasts, kanban
│   ├── layout.css          Login, sidebar, barra superior, navegación móvil
│   └── landing.css         Solo para la landing: la fuente y el interior de sus visuales
└── js/
    ├── app.js              Arranque del CRM, guardia de sesión, navegación por #hash, eventos de datos
    ├── config.js           Claves, canal de sincronización, etiquetas, datos semilla embebidos
    ├── services/           Lo único que toca localStorage, sessionStorage y fetch
    │   ├── storageService.js   Carga semilla y lectura/escritura (exportar/importar siguen aquí, sin uso en la UI)
    │   ├── syncService.js      BroadcastChannel (+ evento storage como respaldo)
    │   ├── authService.js      Login de asesores, sesión por pestaña, lista de asesores
    │   ├── propertyService.js  CRUD de propiedades y filtros
    │   └── leadService.js      CRUD de clientes (leads), etapas y cifras del resumen
    ├── ui/
    │   ├── toastUI.js          Toasts y diálogos (<dialog>)
    │   ├── authUI.js           Login, cierre de sesión y helpers de formularios
    │   └── adminUI.js          Resumen, propiedades y clientes
    └── utils/                  validators.js, formatters.js (escapeHTML, moneda COP, fechas)
```

### Modelo de datos

- **Usuario (asesor):** `id`, `username`, `password`, `name`, `email`, `phone`, `role: "admin"`, `avatar`.
- **Propiedad:** igual que en la especificación original, con `agentId` (el asesor que la gestiona).
- **Lead:** `contactName`, `contactPhone`, `contactEmail`, `source` (`web`, `phone`, `whatsapp`, `walk_in`, `referral`), `propertyId`, `agentId`, `stage` (`new` a `closed_won` / `closed_lost`), `notes`, `createdAt`, `updatedAt`. Debe tener teléfono o correo. Al eliminar una propiedad se eliminan sus leads.

### Flujo de datos

La UI llama a los servicios, y los servicios leen y escriben en `localStorage`. Cada guardado avisa por `inmocrm_sync_channel`, y `app.js` re-renderiza solo los bloques afectados, en esta pestaña y en las demás. Todo texto de usuario pasa por `escapeHTML()` o `textContent`.

### Diseño

- **CRM:** inspirado en las referencias CRMPro y Valtro. Usa Geist; fondo gris azulado claro con tarjetas blancas de sombra suave; el azul del logo (`#2667CC`) como único acento en botones píldora, navegación activa y gráficos. Movimiento máximo de 280 ms y solo fundidos con `prefers-reduced-motion`.
- **Landing:** construida con la skill *taste-skill* (tipo `software`).
  - **Hero `centred`:** la vista de clientes del CRM dibujada en HTML vivo.
  - **`app-frame` sobre fondo oscuro:** el inventario junto a una propiedad con sus clientes interesados.
  - **Resto:** funciones en 2x2, proceso en 3 pasos con el momento `marquee-row`, preguntas frecuentes y cierre `close-word`.
  - **Contenido:** escrito para el dueño de una inmobiliaria, sin términos técnicos; describe solo funciones reales del CRM, sin clientes, cifras ni testimonios inventados.

---

## Diferencias con la especificación original

| Tema | Decisión |
| --- | --- |
| Interfaz del cliente | **Retirada.** Hay un solo tipo de usuario, el asesor. Los leads ya no los crea un cliente con «Me interesa»: el asesor registra cada contacto con «Nuevo lead». No hay registro público. |
| Chat en tiempo real | Retirado. El asesor contacta al cliente con los enlaces de correo y teléfono de su ficha. |
| Respaldo (exportar / importar / restaurar) | Desactivado en la interfaz a pedido del cliente: la sección «Datos» ya no aparece. Las funciones siguen en `storageService.js` por si se reactiva. |
| Vocabulario | La interfaz dice «Clientes» en lugar de «Leads»; el modelo de datos conserva el nombre `leads`. |
| Landing | Nueva (`index.html`); el CRM pasó a `app.html`. |
| Fuente | Geist (OFL) en lugar de Inter o SF Pro. |
| Dashboard | Suma un gráfico de leads por etapa, el inventario por estado y una tabla de leads recientes, tomados de las referencias. |
| QA-11 (tokens) | Aplica al CSS del CRM. La landing usa el kit de taste-skill y `landing.css`, que tienen sus propios valores. |
| Doble clic (`file://`) | En Chrome no es posible con ES Modules sin un paso de build; se muestra un aviso. |

---

## Pruebas

**CRM (`app.html`):** verificado en Chrome sin interfaz, con dos pestañas reales, a 360, 768 y 1280 px.

| ID | Caso (adaptado al modelo de un solo usuario) | Resultado |
| --- | --- | --- |
| QA-01 | Carga inicial con el almacenamiento vacío | Pasa por HTTP. En `file://` con Chrome muestra el aviso. |
| QA-02 | `admin`/`1234` bloqueado; `admin` y `andres` entran al panel | Pasa |
| QA-03 | Dos asesores en dos pestañas, sesiones independientes | Pasa |
| QA-04 | La propiedad creada persiste tras recargar | Pasa |
| QA-05 / 06 | Chat | No aplica |
| QA-07 | Un asesor registra un cliente y la otra pestaña lo ve al instante | Pasa |
| QA-08 | Propiedad y cliente inválidos bloqueados, los datos intactos | Pasa |
| QA-09 | XSS en nombre y notas del cliente y en el título de propiedad | Pasa: se muestra como texto |
| QA-10 | Respaldo | No aplica: desactivado. Se verifica que no quede ningún control de respaldo en la interfaz |
| QA-11 | Tokens y estilos en línea (CRM) | Pasa |
| QA-12 / 13 | Foco visible, teclado, modal con `Esc`, contraste AA | Pasa (334 textos AA) |
| QA-14 | Responsivo, sin scroll lateral, objetivos de 44 px | Pasa |
| QA-15 | Movimiento y movimiento reducido | Pasa |
| QA-16 | Estados vacío, carga y error | Pasa |

**Landing (`index.html`):** el *gate* de taste-skill terminó en **OPEN 2** tras sus 3 rondas. Quedan abiertos dos avisos `bleed-one-side` del momento `marquee-row`: su riel animado se mide más allá del contenedor aunque está recortado. Además se registraron 4 excepciones justificadas en `.taste-skill/waivers.json`:
- Las filas de la lista del `app-frame` que se cortan bajo el fundido, como pide la receta (2).
- La sección de preguntas sin imagen, que la guía permite.
- El riel del marquee.

### Limitaciones conocidas

- Las contraseñas están en texto plano (proyecto académico sin backend).
- Los datos viven en el navegador de cada equipo y, con el respaldo desactivado, no hay forma de pasarlos a otro equipo desde la interfaz.
- Las fotos de las propiedades vienen de Unsplash. Sin internet se muestra la imagen de respaldo; la foto del login está guardada en el proyecto.
