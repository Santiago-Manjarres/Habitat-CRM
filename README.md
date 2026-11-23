# Habitat CRM

CRM web para inmobiliarias, construido como proyecto de aula de **Estructuras de Datos**. No usa un motor de base de datos: un **servidor en Java** simula la base de datos con archivos `.json` y guarda los registros en memoria con estructuras hechas a mano (lista enlazada, árbol binario de búsqueda, tabla hash, cola y pila).

El proyecto tiene dos páginas:

- **Landing (`index.html`):** presenta el producto a la inmobiliaria en lenguaje sencillo, sin términos técnicos, y lleva al CRM con el botón **«Ingresar al CRM»** del encabezado.
- **CRM (`app.html`):** login y panel del equipo de asesores. Hay **un solo tipo de usuario**, el asesor de la inmobiliaria, y todos ven la misma interfaz:
  - **Resumen:** un aviso con el siguiente paso («Tienes 2 clientes nuevos por contactar»), 4 indicadores, gráfico de clientes por etapa, inventario por estado y últimos movimientos.
  - **Propiedades:** crear, editar, cambiar estado y eliminar, con búsqueda.
  - **Clientes:** tablero por etapa. Cada cliente (un *lead* en el código) es una persona interesada que registra un asesor: nombre, teléfono o correo, cómo llegó, propiedad de interés y asesor responsable. Se puede crear, editar, cambiar de etapa y eliminar.

La interfaz habla el idioma de una inmobiliaria: dice «clientes», «etapa» y «ficha», nunca «lead», «JSON» o «pestaña». La opción de respaldo (exportar, importar y restaurar) está desactivada en la interfaz.

**Reparto del trabajo:**
- **Java (`servidor-java/`):** el servidor web, la base de datos simulada, el inicio de sesión, las validaciones, las búsquedas, el orden de las listas, el tablero y las cifras del resumen. Usa solo Java 17 estándar, sin librerías externas.
- **JavaScript:** solo dibuja la pantalla y le pide los datos a Java. El navegador únicamente sabe ejecutar JavaScript, por eso esa parte no puede ser Java.
- **HTML y CSS:** sin cambios visuales.

---

## Cómo ejecutarlo

**Con NetBeans (Java with Ant):**

1. *File → Open Project…* y elige la carpeta `servidor-java` (NetBeans la reconoce como proyecto con el ícono de taza).
2. Clic derecho en el proyecto → *Run* (o F6). La clase principal es `servidor.Principal`.
3. En la ventana *Output* aparece `Habitat CRM está funcionando`. Abre **http://localhost:8080** para ver la landing, o **http://localhost:8080/app.html** para ir directo al CRM.

Al abrir el proyecto por primera vez, NetBeans crea solo el archivo `nbproject/build-impl.xml`. Si avisa que lo regeneró, es normal.

**Sin NetBeans (terminal):**

```bash
cd "HABITAT CRM/servidor-java"
javac -encoding UTF-8 -d build/classes $(find src -name "*.java")
java -cp build/classes servidor.Principal ..
```

El servidor busca la página en la carpeta de arriba (`..`), donde están `app.html` y `data/`. Se le puede pasar otra carpeta y otro puerto: `java -cp build/classes servidor.Principal <carpeta> <puerto>`.

**Prueba de las estructuras:** en NetBeans, clic derecho sobre `src/pruebas/PruebaEstructuras.java` → *Run File* (Shift + F6). Imprime una línea `BIEN` por cada prueba de la lista, la pila, la cola, el árbol, la tabla hash y el lector de JSON.

**Con doble clic (`file://`):** la página necesita al servidor Java. Si se abre como archivo suelto, el CRM muestra un aviso con estos pasos en lugar de quedar en blanco.

---

## Credenciales de prueba

| Asesor | Usuario | Contraseña |
| --- | --- | --- |
| Laura Mendoza | `admin` | `admin` |
| Andrés Rojas | `andres` | `1234` |
| Valentina Ríos | `valentina` | `1234` |

La pantalla de acceso tiene dos botones de cuenta de prueba (Laura Mendoza y Andrés Rojas) que llenan el usuario y la contraseña. Java revisa los datos: el usuario no distingue mayúsculas; la contraseña sí. No hay registro público: los asesores vienen en `data/users.json`.

**Para probar la sincronización**, inicia sesión como `admin` en una pestaña y como `andres` en otra. Cada pestaña conserva su sesión. Si registras un cliente en una pestaña, aparece al instante en el tablero, los indicadores y los últimos movimientos de la otra. También funciona entre dos navegadores o dos computadores conectados al mismo servidor: cada 2 segundos la página le pregunta a Java si los datos cambiaron.

---

## Arquitectura

```
HABITAT CRM/
├── index.html              Landing del producto (taste-skill)
├── app.html                CRM: login + panel del asesor
├── data/                   LA BASE DE DATOS SIMULADA: users.json, properties.json, leads.json
│                           (Java los lee al arrancar y los reescribe con cada cambio)
├── servidor-java/          Proyecto NetBeans (Java with Ant)
│   ├── build.xml, manifest.mf, nbproject/
│   └── src/
│       ├── estructuras/    Nodo, ListaEnlazada, Cola, Pila, NodoArbol, ArbolBinarioBusqueda,
│       │                   EntradaHash, TablaHash
│       ├── json/           ObjetoJson, LectorJson, EscritorJson (leer y escribir JSON sin librerías)
│       ├── modelo/         Registro (interfaz), Usuario, Propiedad, Cliente
│       ├── datos/          Tabla (una tabla en memoria), BaseDeDatos (los archivos .json)
│       ├── servicios/      ServicioUsuarios, ServicioPropiedades, ServicioClientes, ServicioResumen,
│       │                   Validador, Catalogos, Numeros, Herramientas
│       ├── servidor/       Principal (main), ManejadorApi (/api/...), ManejadorArchivos, Respuesta
│       └── pruebas/        PruebaEstructuras (programa de prueba)
├── assets/                 Imágenes, fuente Geist e íconos; kit de taste-skill para la landing
├── css/                    variables.css (tokens), base.css, components.css, layout.css, landing.css
└── js/                     Solo interfaz:
    ├── app.js              Arranque, guardia de sesión y navegación por #hash
    ├── config.js           Etiquetas en español y ajustes de pantalla
    ├── landing.js          Selector de propiedades de la landing
    ├── services/
    │   ├── api.js          El único archivo que habla con Java (fetch + código de sesión)
    │   ├── dataService.js  Guarda la última copia de los datos que mandó Java y le envía los cambios
    │   ├── authService.js  Sesión de la pestaña (el usuario y la contraseña los revisa Java)
    │   └── syncService.js  Avisa a las otras pestañas y pregunta a Java si hubo cambios
    ├── ui/                 toastUI.js, authUI.js, adminUI.js: dibujan la pantalla
    └── utils/formatters.js escapeHTML, moneda COP, fechas
```

### Estructuras de datos (Java)

Todas están en `servidor-java/src/estructuras/`, hechas a mano con nodos. El proyecto no usa `java.util.ArrayList`, `HashMap` ni otras colecciones de Java.

| Estructura | Para qué se usa en el CRM | Dónde |
| --- | --- | --- |
| **Lista enlazada** (`ListaEnlazada`, `Nodo`) | Guarda en memoria cada tabla de la base de datos (usuarios, propiedades, clientes). Los arreglos JSON se leen como listas enlazadas. Las respuestas de búsqueda son listas. | `Tabla`, `LectorJson`, todos los servicios |
| **Tabla hash** (`TablaHash`, `EntradaHash`) | Índice por id: encuentra un registro al instante sin recorrer la lista. También busca al asesor por su nombre de usuario al iniciar sesión y guarda las sesiones abiertas (código → asesor). Las colisiones se resuelven con listas enlazadas en cada cubeta. | `Tabla`, `ServicioUsuarios` |
| **Árbol binario de búsqueda** (`ArbolBinarioBusqueda`, `NodoArbol`) | Ordena: las propiedades por fecha de creación, los clientes por fecha de su último cambio y los asesores por nombre. Se insertan con esa clave y el recorrido en orden los devuelve ordenados. | `ServicioPropiedades`, `ServicioClientes`, `ServicioUsuarios` |
| **Pila** (`Pila`) | Invierte el orden del árbol (de menor a mayor fecha) para mostrar primero lo más reciente: inventario, tablero y «Últimos movimientos». | `Herramientas.invertir` |
| **Cola** (`Cola`) | Clientes «por contactar»: los nuevos en orden de llegada. El primero en llegar es el primero al que hay que llamar. Su tamaño es el aviso «Tienes 2 clientes nuevos por contactar». | `ServicioClientes.porContactar` |
| **Arreglo de listas** | El tablero de clientes: un arreglo con una lista enlazada por columna (Nuevo, Contactado, Visita, Negociación, Cerrado). | `ServicioClientes.tablero` |

### Modelo de datos

Los nombres de Java van en español; las claves de los archivos `.json` se mantienen como estaban para no cambiar los datos ni la interfaz.

- **Usuario (asesor):** `id`, `username`, `password`, `name`, `email`, `phone`, `role: "admin"`, `avatar`.
- **Propiedad:** `id`, `title`, `type`, `operation` (`sale`/`rent`), `price`, `currency`, `location`, `areaM2`, `bedrooms`, `bathrooms`, `parking`, `status` (`available`/`negotiation`/`sold`), `agentId`, `description`, `imageUrl`, `createdAt`.
- **Cliente (lead):** `contactName`, `contactPhone`, `contactEmail`, `source` (`web`, `phone`, `whatsapp`, `walk_in`, `referral`), `propertyId`, `agentId`, `stage` (`new` a `closed_won` / `closed_lost`), `notes`, `createdAt`, `updatedAt`. Debe tener teléfono o correo. Al eliminar una propiedad se eliminan sus clientes.

### Flujo de datos

1. Java arranca, lee los tres `.json` y arma cada tabla: una lista enlazada con los registros y una tabla hash como índice por id.
2. Al iniciar sesión, la página pide `GET /api/estado` y Java responde con todo listo para dibujar: inventario ordenado, clientes, tablero, cifras del resumen y cola de clientes por contactar.
3. Cuando el asesor guarda algo, la página lo envía a Java (`POST`, `PUT`, `PATCH` o `DELETE` en `/api/...`). Java valida, cambia la tabla en memoria y reescribe el archivo `.json` correspondiente.
4. Cada cambio sube un número de versión. Las demás pestañas y computadores lo notan y vuelven a pedir los datos.

Todo texto de usuario pasa por `escapeHTML()` o `textContent` en la interfaz. La carpeta `data/` no se puede descargar desde el navegador: el servidor la bloquea, porque contiene las contraseñas.

### Diseño

- **CRM:** al estilo del panel de Stripe.
  - Un solo lienzo blanco, con bordes finos en lugar de sombras y secciones planas: título, línea divisoria y contenido.
  - Controles pequeños: 32 px en escritorio (44 px de área táctil en el celular), radio de 6 px en botones y de 8 px en tarjetas.
  - Los cuatro indicadores van en una sola franja dividida por líneas finas.
  - Navegación lateral discreta: el ítem activo se marca solo con el color de acento.
  - Usa Geist, con el azul del logo (`#2667CC`) como único acento. Movimiento máximo de 280 ms y solo fundidos con `prefers-reduced-motion`.
- **Landing:** construida con la skill *taste-skill* (tipo `software`).
  - **Hero `centred`:** la vista de clientes del CRM dibujada en HTML vivo, sobre el efecto WebGL `fluted-glass` (vidrio estriado) repartido por la skill.
  - **`app-frame` sobre fondo oscuro, interactivo:** al hacer clic en una propiedad del inventario se abre su ficha con sus clientes interesados (`js/landing.js`).
  - **Inventario con fotos:** una lista de propiedades con miniaturas y una foto grande con el momento `liquid-hover`, que ondula como agua bajo el cursor.
  - **Resto:** funciones en 2x2 (con retratos de los asesores), proceso en 3 pasos con el momento `marquee-row`, preguntas frecuentes y cierre `close-word`.
  - **Fotos:** de Unsplash, enlazadas directamente; los créditos van en una sola línea del pie de página.
  - **Contenido:** escrito para el dueño de una inmobiliaria, sin términos técnicos; describe solo funciones reales del CRM, sin clientes, cifras ni testimonios inventados.

---

## Diferencias con la especificación original

| Tema | Decisión |
| --- | --- |
| Interfaz del cliente | **Retirada.** Hay un solo tipo de usuario, el asesor. Los leads ya no los crea un cliente con «Me interesa»: el asesor registra cada contacto con «Nuevo lead». No hay registro público. |
| Chat en tiempo real | Retirado. El asesor contacta al cliente con los enlaces de correo y teléfono de su ficha. |
| Respaldo (exportar / importar / restaurar) | Desactivado a pedido del cliente: la sección «Datos» ya no aparece. Los archivos `data/*.json` son el respaldo: basta con copiarlos. |
| Persistencia | La especificación pedía `localStorage`. Por el enfoque del curso, la lógica y los datos pasaron a un servidor Java que simula la base de datos con los archivos `.json`. |
| Vocabulario | La interfaz dice «Clientes» en lugar de «Leads»; el modelo de datos conserva el nombre `leads`. |
| Landing | Nueva (`index.html`); el CRM pasó a `app.html`. |
| Fuente | Geist (OFL) en lugar de Inter o SF Pro. |
| Dashboard | Suma un gráfico de leads por etapa, el inventario por estado y una tabla de leads recientes, tomados de las referencias. |
| QA-11 (tokens) | Aplica al CSS del CRM. La landing usa el kit de taste-skill y `landing.css`, que tienen sus propios valores. |
| Doble clic (`file://`) | La página necesita al servidor Java; se muestra un aviso con los pasos. |

---

## Pruebas

**CRM (`app.html`):** verificado contra el servidor Java en Chrome sin interfaz, con dos pestañas reales, a 360, 768 y 1280 px. Además, `PruebaEstructuras` pasa sus 12 pruebas, y al reescribir `properties.json` Java produce un archivo idéntico al original.

| ID | Caso (adaptado al modelo de un solo usuario) | Resultado |
| --- | --- | --- |
| QA-01 | Carga inicial: Java lee 3 asesores, 9 propiedades y 8 clientes | Pasa. En `file://` muestra el aviso. |
| QA-02 | `admin`/`1234` bloqueado; `admin` y `andres` entran al panel | Pasa |
| QA-03 | Dos asesores en dos pestañas, sesiones independientes | Pasa |
| QA-04 | La propiedad creada persiste tras recargar | Pasa |
| QA-05 / 06 | Chat | No aplica |
| QA-07 | Un asesor registra un cliente y la otra pestaña lo ve al instante | Pasa |
| QA-08 | Propiedad y cliente inválidos bloqueados, los datos intactos | Pasa |
| QA-09 | XSS en nombre y notas del cliente y en el título de propiedad | Pasa: se muestra como texto |
| QA-10 | Respaldo | No aplica: desactivado. Se verifica que no quede ningún control de respaldo en la interfaz |
| QA-11 | Tokens y estilos en línea (CRM) | Pasa |
| QA-12 / 13 | Foco visible, teclado, modal con `Esc`, contraste AA | Pasa (357 textos AA) |
| QA-14 | Responsivo, sin scroll lateral, objetivos de 44 px | Pasa |
| QA-15 | Movimiento y movimiento reducido | Pasa |
| QA-16 | Estados vacío, carga y error | Pasa |

**Landing (`index.html`):** el *gate* de taste-skill pasó de 26 fallos a 1 en sus 3 rondas. El último («Funciones» medía 2,01 pantallas en el celular) se corrigió después, y `check.mjs` lo confirma sin hallazgos a 1440, 1024, 820, 768 y 390 px. Hay 4 excepciones justificadas en `.taste-skill/waivers.json`:
- La sección de preguntas sin imagen, que la guía permite.
- El riel del marquee (`row-lines`) y sus dos pistas (`bleed-one-side`): están recortadas dentro del contenedor y se desvanecen en ambos bordes.

### Limitaciones conocidas

- Las contraseñas están en texto plano en `data/users.json` (proyecto académico).
- Las sesiones viven en la memoria del servidor: si se reinicia Java, cada asesor debe iniciar sesión otra vez.
- El servidor atiende una petición a la vez, suficiente para un equipo pequeño de asesores.
- Las fotos de las propiedades vienen de Unsplash. Sin internet se muestra la imagen de respaldo; la foto del login está guardada en el proyecto.
