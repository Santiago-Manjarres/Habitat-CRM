# Habitat CRM

CRM web para una inmobiliaria, construido como proyecto universitario **sin base de datos**: los datos iniciales viven en archivos `.json`, el estado se guarda en el navegador (`localStorage`) y las pestañas abiertas se sincronizan al instante con `BroadcastChannel`.

Tiene dos interfaces según el rol:

- **Cliente:** catálogo con filtros, detalle de cada inmueble, botón **«Me interesa / Consultar»** y la sección **Mis consultas** con el estado de cada una.
- **Asesor (admin):** resumen con KPIs, gestión completa de propiedades (crear, editar, cambiar estado, eliminar), embudo de leads por etapa con notas comerciales y herramientas para **exportar, importar y restaurar** los datos en JSON.

Está hecho solo con HTML, CSS y JavaScript nativos (ES Modules). No usa frameworks, librerías ni npm, y no requiere compilar nada.

---

## Cómo ejecutarlo

Necesitas un servidor de archivos estáticos. Con Python (ya viene en macOS y en la mayoría de Linux):

```bash
cd "HABITAT CRM"
python3 -m http.server 8000
```

Luego abre **http://localhost:8000** en el navegador.

Cualquier otro servidor estático sirve igual: Live Server de VS Code o `npx serve`.

### ¿Y con doble clic en `index.html`?

- **Chrome, Edge y Brave** bloquean los módulos de JavaScript cuando la página se abre como archivo (`file://`). En ese caso la app muestra un aviso con los pasos para levantar el servidor, en lugar de quedar en blanco.
- **Firefox** permite módulos en `file://`. Ahí la app usa los **datos semilla incluidos en `js/config.js`**, porque `fetch` de los `.json` no funciona sin servidor. Este caso no se probó en este entorno.

---

## Credenciales de prueba

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| Asesor / Administrador | `admin` | `admin` |
| Cliente | `client` | `client` |
| Cliente (secundario) | `ana.gomez` | `1234` |

La pantalla de inicio tiene los botones **«Autocompletar Admin»** y **«Autocompletar Cliente»** para llenar los campos con un clic. También se puede crear una cuenta de cliente nueva en la pestaña **Crear cuenta**.

El usuario no distingue mayúsculas; la contraseña sí.

### Probar la sincronización entre pestañas

1. En la pestaña 1 inicia sesión como `client` / `client`.
2. En la pestaña 2 inicia sesión como `admin` / `admin`. Cada pestaña conserva su propia sesión (`sessionStorage`).
3. Como cliente, abre un inmueble y pulsa **«Me interesa / Consultar»**: el lead aparece al instante en la columna **Nuevo** del panel del asesor.
4. Como asesor, cambia el estado de una propiedad o la etapa de un lead: el catálogo y «Mis consultas» del cliente se actualizan sin recargar.

---

## Arquitectura

```
HABITAT CRM/
├── index.html              Punto de entrada único: login + vista cliente + vista asesor
├── README.md
├── data/                   Datos semilla (solo lectura vía fetch)
│   ├── users.json
│   ├── properties.json
│   └── leads.json
├── assets/
│   ├── img/                logo.png y property-placeholder.svg (imagen de respaldo)
│   ├── icons/sprite.svg    Iconos SVG (trazo 1.5px)
│   └── fonts/              SF Pro Display (400/500/700) en .woff, reducida a caracteres latinos
├── css/
│   ├── variables.css       Tokens de diseño: el ÚNICO archivo con colores, radios, sombras y duraciones
│   ├── base.css            Fuentes, reset, tipografía, foco visible, prefers-reduced-motion
│   ├── components.css      Botones, campos, badges, tarjetas, tablas, KPIs, modal, toasts, kanban, estados
│   └── layout.css          Login, barra del cliente, sidebar del asesor, navegación móvil, grids
├── js/
│   ├── app.js              Arranque, guardia de sesión/rol, navegación por #hash, eventos de datos
│   ├── config.js           Claves de almacenamiento, canal de sincronización, etiquetas, datos semilla embebidos
│   ├── services/           Lógica y datos (lo único que toca localStorage, sessionStorage y fetch)
│   │   ├── storageService.js   Carga semilla, lectura/escritura, exportar/importar/restaurar
│   │   ├── syncService.js      BroadcastChannel (+ evento storage como respaldo)
│   │   ├── authService.js      Login, sesión por pestaña, logout, registro de clientes
│   │   ├── propertyService.js  CRUD de propiedades y filtros
│   │   └── leadService.js      «Me interesa», etapas del embudo, notas y KPIs
│   ├── ui/                 Renderizado e interacción (nunca accede al almacenamiento directamente)
│   │   ├── toastUI.js          Toasts y diálogos (<dialog>) de confirmación
│   │   ├── authUI.js           Login, registro, cierre de sesión y helpers de formularios
│   │   ├── clientUI.js         Catálogo, filtros, detalle y «Mis consultas»
│   │   └── adminUI.js          KPIs, tabla de propiedades, kanban de leads y herramientas de datos
│   └── utils/
│       ├── validators.js       Validación de formularios y de respaldos importados
│       └── formatters.js       Moneda COP, fechas relativas, escapeHTML y marcado de iconos/badges
└── Resources/              Archivos originales de marca (logos y fuentes completas)
```

### Flujo de datos

```
 UI (js/ui)  ──llama──▶  services  ──lee/escribe──▶  localStorage
     ▲                       │
     │                       └─ storageService.saveCollection()
     │                              └─ syncService.notifyChange(entity)
     │                                     ├─ esta pestaña: aviso inmediato
     │                                     └─ otras pestañas: BroadcastChannel
     └──── app.js recibe el aviso y re-renderiza solo el bloque afectado
```

1. **Arranque:** `storageService.initStorage()` revisa `localStorage`. Si falta alguna colección, hace `fetch` a `/data/*.json`; si eso falla (por ejemplo en `file://`), usa los datos embebidos en `config.js`.
2. **Sesión:** se guarda en `sessionStorage` (`inmocrm_active_session`) y nunca incluye la contraseña. Por eso dos pestañas pueden tener sesiones distintas.
3. **Cambios:** cada guardado emite `{ type: 'DATA_UPDATED', entity }` por el canal `inmocrm_sync_channel`. Las demás pestañas vuelven a leer `localStorage` y re-renderizan solo lo que depende de esa entidad, conservando el foco del teclado.
4. **Seguridad básica:** todo texto ingresado por usuarios se inserta con `escapeHTML()` o `textContent`. Las URLs de imagen solo aceptan `http(s)`, y las imágenes que fallan se reemplazan por un SVG local.

### Sistema de diseño

- **Tokens:** todos los estilos usan `var(--token)` de `css/variables.css`. Hay un solo color de acento, el azul del logo `#2667CC`; el verde, el ámbar y el rojo se reservan para estados.
- **Tipografía:** texto base de 14px. Los campos de formulario usan 16px solo en móvil para evitar el zoom automático de iOS.
- **Componentes nativos y accesibles:** `<dialog>` con cierre por `Esc` y retorno del foco, foco visible en todo elemento interactivo, regiones `aria-live` y contraste AA verificado.
- **Movimiento sobrio:** solo `transform` y `opacity`, con un máximo de 280ms. Con `prefers-reduced-motion` solo quedan fundidos.
- **Diseño responsivo, pensado primero para móvil:** puntos de quiebre en 640, 1024 y 1280px. En móvil hay barra de navegación inferior, la tabla de propiedades pasa a tarjetas y el kanban se desplaza en horizontal.

---

## Diferencias con la especificación original

| Tema | Decisión |
| --- | --- |
| Chat en tiempo real | **Retirado del alcance.** No existen `chatService.js`, `chatUI.js`, `chat.css` ni `conversations.json`. «Me interesa» crea el lead y el asesor contacta al cliente por correo o teléfono, con enlaces en el modal de notas. El KPI de «conversaciones sin leer» se reemplazó por **Leads nuevos**. |
| Doble clic (`file://`) | En Chrome, Edge y Brave no es posible con ES Modules sin un paso de build. Se muestra un aviso con instrucciones. |
| Color de acento | Azul del logo de Habitat (`#2667CC`) en lugar del índigo de ejemplo. |
| Fuente | SF Pro Display autoalojada, en lugar de Inter, con respaldo `system-ui`. Su licencia solo permite usarla en plataformas Apple; si el proyecto se publica, conviene cambiarla por Inter. |
| Peso 600 | SF Pro Display no incluye Semibold vertical; los títulos usan Bold (700). |
| Modo oscuro | No incluido (solo tema claro). |
| KPIs | Son 5 tarjetas: 5 columnas desde 1280px, 3 en escritorio, 2 en tablet y 1 en móvil. |
| Eliminar propiedad | También elimina sus leads, y la confirmación indica cuántos. |
| Diálogos | `openModal` y `confirmDialog` viven en `toastUI.js`, porque la estructura de carpetas no incluye un archivo propio para modales. |

### Mantenimiento

- **Datos semilla:** si editas `data/*.json`, replica el cambio en `SEED_DATA` de `js/config.js`, que es el respaldo para `file://`.
- **Iconos:** si editas `assets/icons/sprite.svg`, copia los `<symbol>` entre los comentarios `SPRITE:START` y `SPRITE:END` de `index.html`. La copia en línea existe porque las referencias SVG externas no cargan en `file://`.

---

## Pruebas (matriz QA de la especificación)

Verificadas en Chrome sin interfaz, con dos pestañas reales, a 360, 768 y 1280px.

| ID | Caso | Resultado |
| --- | --- | --- |
| QA-01 | Carga inicial y resiliencia | Pasa por HTTP. En `file://` con Chrome se muestra el aviso (ver arriba). |
| QA-02 | Autenticación y errores | Pasa |
| QA-03 | Sesiones simultáneas | Pasa |
| QA-04 | Persistencia tras recarga | Pasa |
| QA-05 | Chat en tiempo real | No aplica (chat retirado) |
| QA-06 | Respuesta y estado leído | No aplica (chat retirado) |
| QA-07 | Lead desde el catálogo | Pasa (sin la parte del chat) |
| QA-08 | Validación de formularios | Pasa |
| QA-09 | Prevención de XSS | Pasa, probado en notas, títulos y ubicaciones |
| QA-10 | Exportar / importar / restaurar | Pasa |
| QA-11 | Tokens y estilos en línea | Pasa |
| QA-12 | Estados de componentes | Pasa |
| QA-13 | Accesibilidad | Pasa: 431 textos revisados con contraste AA |
| QA-14 | Diseño responsivo | Pasa |
| QA-15 | Movimiento | Pasa |
| QA-16 | Estados vacío, carga y error | Pasa |

### Limitaciones conocidas

- Las contraseñas se guardan en texto plano en `localStorage` y en los JSON. Es aceptable solo para un proyecto académico sin backend.
- Los datos viven en el navegador: cada navegador o perfil tiene su propia copia. Usa **Exportar** e **Importar** para moverlos.
- Las fotos de las propiedades semilla vienen de Unsplash. Sin internet se muestra la imagen de respaldo.
