# Habitat CRM

CRM web para inmobiliarias, construido como proyecto de aula de **Estructuras de Datos**. No usa un motor de base de datos: un **servidor en Java** simula la base de datos con archivos `.json` y guarda los registros en memoria con estructuras hechas a mano (lista enlazada, árbol binario de búsqueda, tabla hash, cola y pila).

**El proyecto no usa JavaScript.** Java arma cada página del CRM completa (HTML) y se la envía al navegador; los formularios se envían con HTML normal (`<form method="post">`). La página de inicio es HTML y CSS. El navegador solo muestra HTML y CSS.

El proyecto tiene dos partes:

- **Página de inicio (`index.html`):** presenta el producto a la inmobiliaria en lenguaje sencillo y lleva al CRM con el botón **«Entrar al CRM»**. Es solo HTML y CSS, sin efectos especiales.
- **CRM (`/entrar`, `/resumen`, `/propiedades`, `/clientes`):** login y panel del equipo de asesores. Hay **un solo tipo de usuario**, el asesor, y todos ven la misma interfaz:
  - **Resumen:** 4 indicadores, gráfico de clientes por etapa, inventario por estado y últimos movimientos.
  - **Propiedades:** buscar, crear, editar, cambiar estado y eliminar.
  - **Clientes:** tablero por etapa. Cada cliente (un *lead* en los datos) es una persona interesada que registra un asesor. Se puede crear, editar, cambiar de etapa y eliminar.

La interfaz habla el idioma de una inmobiliaria: dice «clientes», «etapa» y «ficha», nunca «lead» o «JSON».

---

## Cómo ejecutarlo

**Con NetBeans (Java with Ant):**

1. *File → Open Project…* y elige la carpeta `servidor-java` (NetBeans la reconoce como proyecto con el ícono de taza).
2. Clic derecho en el proyecto → *Run* (o F6). La clase principal es `servidor.Principal`.
3. En la ventana *Output* aparece `Habitat CRM está funcionando`. Abre **http://localhost:8080** para ver la página de inicio, o **http://localhost:8080/entrar** para ir directo al CRM.

Al abrir el proyecto por primera vez, NetBeans crea solo el archivo `nbproject/build-impl.xml`. Si avisa que lo regeneró, es normal.

**Sin NetBeans (terminal):**

```bash
cd "HABITAT CRM/servidor-java"
javac -encoding UTF-8 -d build/classes $(find src -name "*.java")
java -cp build/classes servidor.Principal ..
```

El servidor busca la página en la carpeta de arriba (`..`), donde están `index.html`, `css/` y `data/`. Se le puede pasar otra carpeta y otro puerto: `java -cp build/classes servidor.Principal <carpeta> <puerto>`.

---

## Credenciales de prueba

| Asesor | Usuario | Contraseña |
| --- | --- | --- |
| Laura Mendoza | `admin` | `admin` |
| Andrés Rojas | `andres` | `1234` |
| Valentina Ríos | `valentina` | `1234` |

El usuario no distingue mayúsculas; la contraseña sí. No hay registro público: los asesores vienen en `data/users.json`.

---

## Arquitectura

```
HABITAT CRM/
├── index.html              Página de inicio
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
│       ├── vistas/         Las páginas en HTML: Plantilla, PaginaLogin, PaginaResumen,
│       │                   PaginaPropiedades, PaginaClientes, Html, Formato, Etiquetas
│       └── servidor/       Principal (main), ManejadorPaginas (las rutas del CRM),
│                           ManejadorArchivos (CSS e imágenes), Formulario, Respuesta
├── assets/                 Imágenes, fuente Geist, íconos y los estilos base de la página de inicio
└── css/                    variables.css (valores de diseño), base.css, components.css, layout.css, landing.css
```

### Las capas de Java

| Capa | Qué hace |
| --- | --- |
| `servidor` | Recibe cada petición del navegador, lee los formularios y la cookie de sesión, decide qué hacer y responde. |
| `vistas` | Arma el HTML de cada página con los datos. No decide nada: solo convierte datos en HTML. |
| `servicios` | Las reglas del negocio: validar, buscar, ordenar, armar el tablero y calcular las cifras. |
| `datos` | Las tablas en memoria y la lectura y escritura de los `.json`. |
| `modelo` | Las clases `Usuario`, `Propiedad` y `Cliente`. |
| `estructuras` | Las estructuras de datos hechas a mano. |
| `json` | Leer y escribir JSON sin librerías. |

### Estructuras de datos

Todas están en `servidor-java/src/estructuras/`, hechas a mano con nodos. El proyecto no usa `java.util.ArrayList`, `HashMap` ni otras colecciones de Java.

| Estructura | Para qué se usa en el CRM | Dónde |
| --- | --- | --- |
| **Lista enlazada** (`ListaEnlazada`, `Nodo`) | Guarda en memoria cada tabla de la base de datos (usuarios, propiedades, clientes). Los arreglos JSON se leen como listas enlazadas. Los resultados de búsqueda son listas. | `Tabla`, `LectorJson`, todos los servicios |
| **Tabla hash** (`TablaHash`, `EntradaHash`) | Índice por id: encuentra un registro sin recorrer la lista. También busca al asesor por su nombre de usuario al iniciar sesión, guarda las sesiones abiertas (código → asesor) y los avisos pendientes de cada sesión. Las colisiones se resuelven con listas enlazadas en cada cubeta. | `Tabla`, `ServicioUsuarios`, `ManejadorPaginas` |
| **Árbol binario de búsqueda** (`ArbolBinarioBusqueda`, `NodoArbol`) | Ordena: las propiedades por fecha de creación, los clientes por fecha de su último cambio y los asesores por nombre. | `ServicioPropiedades`, `ServicioClientes`, `ServicioUsuarios` |
| **Pila** (`Pila`) | Invierte el orden del árbol (de menor a mayor fecha) para mostrar primero lo más reciente. | `Herramientas.invertir` |
| **Cola** (`Cola`) | Clientes «por contactar»: los nuevos en orden de llegada. Llena la columna «Nuevo» del tablero (arriba, el que lleva más tiempo esperando) y da la cifra «2 nuevos por contactar» del resumen. | `ServicioClientes.porContactar` |
| **Arreglo de listas** | El tablero de clientes: un arreglo con una lista enlazada por columna (Nuevo, Contactado, Visita, Negociación, Cerrado). | `ServicioClientes.columnas` |

### Modelo de datos

Los nombres de Java van en español; las claves de los archivos `.json` se mantienen en inglés para no cambiar los datos.

- **Usuario (asesor):** `id`, `username`, `password`, `name`, `email`, `phone`, `role`, `avatar`.
- **Propiedad:** `id`, `title`, `type`, `operation` (`sale`/`rent`), `price`, `currency`, `location`, `areaM2`, `bedrooms`, `bathrooms`, `parking`, `status` (`available`/`negotiation`/`sold`), `agentId`, `description`, `imageUrl`, `createdAt`.
- **Cliente (lead):** `contactName`, `contactPhone`, `contactEmail`, `source`, `propertyId`, `agentId`, `stage` (`new` a `closed_won` / `closed_lost`), `notes`, `createdAt`, `updatedAt`. Debe tener teléfono o correo. Al eliminar una propiedad se eliminan sus clientes.

### Cómo viaja un dato (ejemplo: crear una propiedad)

1. El asesor abre `/propiedades/nueva`. Java responde la página de propiedades con el formulario encima.
2. Llena el formulario y presiona «Guardar». El navegador envía los campos a `POST /propiedades/nueva` (texto como `title=Casa&price=450.000.000`).
3. `Formulario` convierte ese texto en un `ObjetoJson`. `ServicioPropiedades` lo pasa por el `Validador`.
4. **Si hay errores**, Java vuelve a armar el formulario con lo escrito y un mensaje rojo debajo de cada campo.
5. **Si está bien**, agrega la propiedad a la tabla en memoria (lista enlazada + tabla hash), reescribe `data/properties.json`, guarda el aviso «Propiedad creada» y redirige a `/propiedades`. Ahí se muestra el aviso.

Después de cada cambio exitoso se redirige a otra página («Post/Redirect/Get»), así recargar no envía el formulario dos veces.

**Sesión:** al iniciar sesión, Java crea un código aleatorio, lo guarda en la tabla hash de sesiones y se lo da al navegador en una **cookie** (`HttpOnly`). El navegador la manda sola en cada petición.

**Seguridad:** todo texto escrito por un usuario pasa por `Html.escapar()` antes de entrar al HTML, así no se puede colar código. La carpeta `data/` y el código del servidor no se pueden descargar desde el navegador.

### Diseño

- **CRM:** al estilo del panel de Stripe: lienzo blanco, bordes finos, controles pequeños (32 px en computador, 44 px en celular), Geist y el azul del logo (`#2667CC`) como único acento. Las ventanas emergentes y los avisos que desaparecen solos están hechos solo con HTML y CSS.
- **Página de inicio:** solo HTML y CSS. Tiene la ventana del CRM dibujada en HTML, el inventario de ejemplo con fotos de Unsplash, las funciones, los tres pasos, las preguntas frecuentes y un contenido escrito para el dueño de una inmobiliaria.

---

## Diferencias con la especificación original

| Tema | Decisión |
| --- | --- |
| Lenguaje | La especificación pedía HTML, CSS y JavaScript. Por el enfoque del curso, todo pasó a Java: Java arma las páginas del CRM. El proyecto no tiene JavaScript. |
| Persistencia | La especificación pedía `localStorage`. Java simula la base de datos con los archivos `.json`. |
| Interfaz del cliente | Retirada. Hay un solo tipo de usuario, el asesor, que registra cada contacto. No hay registro público. |
| Chat en tiempo real | Retirado. El asesor contacta al cliente con los enlaces de correo y teléfono de su ficha. |
| Respaldo | Desactivado a pedido del cliente. Los archivos `data/*.json` son el respaldo: basta con copiarlos. |
| Vocabulario | La interfaz dice «Clientes» en lugar de «Leads». |
| Fuente | Geist (OFL) en lugar de Inter o SF Pro. |

---

## Pruebas

Verificado contra el servidor Java en Chrome sin interfaz, a 390 y 1440 px. Ninguna página (ni el CRM ni la de inicio) carga JavaScript, ninguna se sale por los lados y la consola no muestra errores.

| Caso | Resultado |
| --- | --- |
| Entrar sin sesión a `/resumen` | Redirige a `/entrar` |
| Login vacío / contraseña incorrecta / correcto | Mensajes por campo / alerta general / entra con «Hola, Laura» |
| Buscar «bogota» | 3 propiedades (sin importar tildes) |
| Crear propiedad con errores | Mensajes debajo de título, precio, ubicación y área; nada se guarda |
| Crear propiedad con «1.350.000.000» y área «115,5» | Se guarda como 1350000000 y 115.5 |
| Cambiar estado, editar, mover de etapa | Se guarda y muestra su aviso |
| Cliente sin teléfono ni correo | «Escribe un teléfono o un correo para poder contactarlo» |
| Texto con `<b>` en las notas | Se muestra como texto |
| Eliminar propiedad con un cliente | Avisa «También se eliminará 1 cliente interesado» y borra los dos |
| Cerrar sesión / cookie vencida | Vuelve al login con «Cerraste sesión» / «Tu sesión terminó» |
| `/data/users.json` | 404 |

### Limitaciones conocidas

- Las contraseñas están en texto plano en `data/users.json` (proyecto académico).
- Las sesiones viven en la memoria del servidor: si se reinicia Java, cada asesor debe iniciar sesión otra vez.
- Cada acción recarga la página. Para ver lo que hizo otro asesor hay que recargar o cambiar de sección.
- Las ventanas emergentes no se cierran con la tecla Esc (eso necesitaría JavaScript): se cierran con la X, «Cancelar» o tocando el fondo oscuro.
- Las fotos de las propiedades vienen de internet. Si un enlace está roto, el navegador muestra la imagen rota.
- En el celular, la página de inicio no tiene menú desplegable (necesitaría JavaScript): el botón «Entrar al CRM» siempre se ve y los enlaces quedan en el pie de página.
