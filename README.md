# Habitat CRM

CRM web para inmobiliarias, hecho como proyecto de aula de **Estructuras de Datos**.

- **Todo está hecho en Java.** No hay JavaScript: Java arma cada página (HTML) y el navegador solo la muestra con su CSS.
- **Una sola estructura de datos: la lista enlazada** (`ListaEnlazada` + `Nodo`), hecha a mano.
- **Los datos se guardan en tres archivos `.json`** de la carpeta `data/`: al encender, Java los carga en listas enlazadas.

## Cómo ejecutarlo

1. En NetBeans: *File → Open Project…* y elige la carpeta `servidor-java`.
2. Clic derecho en el proyecto → *Run* (F6).
3. Abre **http://localhost:8080**.

Sin NetBeans:
```bash
cd servidor-java
javac -encoding UTF-8 -d build/classes $(find src -name "*.java")
java -cp build/classes servidor.Principal
```

## Usuarios de prueba

| Asesor | Usuario | Contraseña |
| --- | --- | --- |
| Laura Mendoza | `admin` | `admin` |
| Andrés Rojas | `andres` | `1234` |
| Valentina Ríos | `valentina` | `1234` |

## Qué hace

- **Login:** usuario y contraseña. Sin sesión no se puede ver ninguna página del CRM.
- **Resumen:** 4 cifras (propiedades disponibles, su valor, clientes en seguimiento y negocios cerrados) y cuántos clientes hay en cada etapa y cuántas propiedades en cada estado.
- **Propiedades:** tabla con buscador, crear, editar y eliminar.
- **Clientes:** tablero con una columna por etapa (Nuevo, Contactado, Visita, Negociación, Cerrado), crear, editar y eliminar.

## Archivos

```
HABITAT CRM/
├── index.html                    Página de inicio (solo HTML y CSS)
├── css/                          Estilos
├── assets/                       Imágenes, fuente e íconos
├── data/                         LOS DATOS: usuarios.json, propiedades.json, clientes.json
└── servidor-java/src/
    ├── estructuras/
    │   ├── Nodo.java             Un dato + el enlace al siguiente nodo
    │   └── ListaEnlazada.java    agregar, eliminar, recorrer
    ├── datos/
    │   ├── Datos.java            Pares nombre -> valor (un registro, un formulario o unos errores)
    │   ├── Json.java             Lee y escribe los .json, línea por línea
    │   ├── EstructuraDeDatos.java Las 3 listas enlazadas: buscar, contar y guardar
    │   └── Validador.java        Las reglas de los formularios
    ├── servidor/
    │   ├── Principal.java        main: enciende el servidor
    │   ├── Rutas.java            Qué hace cada dirección (/entrar, /propiedades...)
    │   └── Archivos.java         Entrega index.html, CSS e imágenes
    └── vistas/
        ├── Plantilla.java        El marco de las páginas y los campos de formulario
        ├── PaginaLogin.java
        ├── PaginaResumen.java
        ├── PaginaPropiedades.java
        └── PaginaClientes.java
```

## Cómo viaja un dato (ejemplo: crear una propiedad)

1. Al encender, `EstructuraDeDatos` lee `propiedades.json` con `Json.leer` y guarda cada propiedad (un `Datos`) en una lista enlazada.
2. El asesor llena el formulario de `/propiedades/nueva` y lo envía (POST).
3. `Rutas.leerFormulario` convierte el texto `titulo=Casa&precio=450000000` en un `Datos`.
4. `Validador.propiedad` revisa los campos. Si hay errores, se vuelve a mostrar el formulario con los mensajes.
5. Si todo está bien, se crea la propiedad con un id nuevo, se agrega a la lista (`agregar`) y `Json.escribir` reescribe `propiedades.json`.

## Sesión

Al iniciar sesión, Java crea un código aleatorio, lo guarda en el usuario (campo `sesion`, solo en memoria) y se lo da al navegador en una **cookie**. En cada página, Java busca qué usuario tiene ese código recorriendo la lista de usuarios. Al salir, el código se borra.

## Reglas de los formularios

| Formulario | Regla |
| --- | --- |
| Login | Usuario y contraseña obligatorios; deben coincidir con `usuarios.json` |
| Propiedad | Título y ubicación obligatorios; precio mayor que 0 |
| Cliente | Nombre obligatorio; teléfono o correo (al menos uno); una propiedad |

## Limitaciones

- Las contraseñas están en texto plano (proyecto académico).
- Si se reinicia el servidor, hay que volver a iniciar sesión.
- Buscar distingue tildes: "bogotá" encuentra Bogotá, "bogota" no.
- Eliminar no pide confirmación, y al eliminar una propiedad sus clientes quedan con «Propiedad eliminada».
