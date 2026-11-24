package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import datos.BaseDeDatos;
import estructuras.TablaHash;
import java.io.IOException;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;
import modelo.Usuario;
import servicios.ServicioClientes;
import servicios.ServicioPropiedades;
import servicios.ServicioResumen;
import servicios.ServicioUsuarios;
import vistas.Etiquetas;
import vistas.PaginaClientes;
import vistas.PaginaLogin;
import vistas.PaginaPropiedades;
import vistas.PaginaResumen;
import vistas.Plantilla;

/**
 * Atiende todas las páginas del CRM. Java arma cada página completa y se la
 * envía al navegador; el CRM no usa JavaScript.
 *
 *   GET  /entrar                         pantalla de inicio de sesión
 *   POST /entrar                         revisar usuario y contraseña
 *   POST /salir                          cerrar sesión
 *   GET  /resumen                        indicadores y gráficos
 *   GET  /propiedades?q=texto            tabla de propiedades (con búsqueda)
 *   GET  /propiedades/nueva              formulario vacío       POST: crear
 *   GET  /propiedades/{id}/editar        formulario con datos   POST: guardar cambios
 *   POST /propiedades/{id}/estado        cambiar el estado
 *   GET  /propiedades/{id}/eliminar      confirmar              POST: eliminar
 *   GET  /clientes                       tablero por etapas
 *   GET  /clientes/nuevo                 formulario vacío       POST: crear
 *   GET  /clientes/{id}                  ficha del cliente      POST: guardar cambios
 *   POST /clientes/{id}/etapa            cambiar la etapa
 *   GET  /clientes/{id}/eliminar         confirmar              POST: eliminar
 *
 * GET = pedir una página. POST = enviar un formulario (cambia algo).
 * Después de cada POST que sale bien se redirige a otra página (patrón
 * "Post/Redirect/Get"): así, si se recarga, el formulario no se envía dos veces.
 */
public class ManejadorPaginas implements HttpHandler {

    private final BaseDeDatos db;
    private final ServicioUsuarios usuarios;
    private final ServicioPropiedades propiedades;
    private final ServicioClientes clientes;
    private final ServicioResumen resumen;

    /**
     * Avisos pendientes: código de sesión -> "tipo|mensaje".
     * Después de guardar se redirige a otra página; el aviso ("Propiedad creada")
     * se guarda aquí y se muestra en la página siguiente. Luego se borra.
     */
    private final TablaHash<String> avisos = new TablaHash<>();

    public ManejadorPaginas(BaseDeDatos db) {
        this.db = db;
        this.usuarios = new ServicioUsuarios(db);
        this.propiedades = new ServicioPropiedades(db);
        this.clientes = new ServicioClientes(db);
        this.resumen = new ServicioResumen(db);
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            atender(intercambio);
        } catch (Exception e) {
            // cualquier error inesperado se imprime en la consola y se responde con un error 500
            e.printStackTrace();
            Respuesta.texto(intercambio, 500, "Error del servidor: " + e.getMessage());
        }
    }

    private void atender(HttpExchange intercambio) throws IOException {
        // 1. leemos qué pidió el navegador
        boolean esPost = intercambio.getRequestMethod().equals("POST");
        // la dirección "/propiedades/prop_001/editar" se parte en ["propiedades", "prop_001", "editar"]
        String[] partes = intercambio.getRequestURI().getPath().substring(1).split("/");
        ObjetoJson datos = esPost ? Formulario.leer(intercambio) : new ObjetoJson();   // campos del formulario
        String token = Formulario.cookie(intercambio, "sesion");                      // código de sesión
        Usuario usuario = usuarios.usuarioDeSesion(token);                              // null si no hay sesión

        // 2. el login es la única página que no pide sesión
        if (partes[0].equals("entrar")) {
            entrar(intercambio, esPost, datos, usuario);
            return;
        }
        // 3. todo lo demás exige una sesión válida
        if (usuario == null) {
            // si traía una cookie que ya no sirve (por ejemplo, se reinició el servidor), se le avisa
            Respuesta.redirigir(intercambio, token == null ? "/entrar" : "/entrar?terminada=1");
            return;
        }
        // 4. según la primera palabra de la dirección, atiende una sección u otra
        switch (partes[0]) {
            case "salir":
                salir(intercambio, token);
                break;
            case "resumen":
                mostrar(intercambio, usuario, token, "resumen", PaginaResumen.contenido(db, resumen, clientes), "");
                break;
            case "propiedades":
                propiedades(intercambio, esPost, partes, datos, usuario, token);
                break;
            case "clientes":
                clientes(intercambio, esPost, partes, datos, usuario, token);
                break;
            default:
                Respuesta.redirigir(intercambio, "/resumen");     // por ejemplo, la antigua dirección app.html
        }
    }

    /* ---------- Sesión ---------- */

    private void entrar(HttpExchange intercambio, boolean esPost, ObjetoJson datos, Usuario usuario) throws IOException {
        if (usuario != null) {
            Respuesta.redirigir(intercambio, "/resumen");     // ya tenía sesión: directo al panel
            return;
        }
        if (!esPost) {
            // Mostrar el formulario vacío (con un aviso si viene de cerrar sesión o de una sesión vencida)
            ObjetoJson parametros = Formulario.parametros(intercambio);
            String aviso = parametros.tiene("terminada") ? Plantilla.aviso("warning", "Tu sesión terminó. Inicia sesión de nuevo")
                    : parametros.tiene("salio") ? Plantilla.aviso("info", "Cerraste sesión") : "";
            Respuesta.html(intercambio, 200, PaginaLogin.html("", new ObjetoJson(), "", aviso));
            return;
        }
        ObjetoJson resultado = usuarios.iniciarSesion(datos);      // aquí se valida y se busca en la tabla hash
        if (exito(resultado)) {
            String token = resultado.texto("token");
            // La cookie guarda el código de sesión en el navegador. HttpOnly: ningún script puede leerla.
            intercambio.getResponseHeaders().add("Set-Cookie", "sesion=" + token + "; Path=/; HttpOnly; SameSite=Lax");
            String nombre = ((ObjetoJson) resultado.obtener("user")).texto("name").split(" ")[0];
            guardarAviso(token, "success", "Hola, " + nombre);
            Respuesta.redirigir(intercambio, "/resumen");
            return;
        }
        // Falló: se vuelve a mostrar el login con los errores y con el usuario que ya había escrito
        ObjetoJson errores = resultado.tiene("fieldErrors") ? (ObjetoJson) resultado.obtener("fieldErrors") : new ObjetoJson();
        Respuesta.html(intercambio, 200, PaginaLogin.html(datos.texto("username"), errores, resultado.texto("error"), ""));
    }

    private void salir(HttpExchange intercambio, String token) throws IOException {
        usuarios.cerrarSesion(token);         // se borra de la tabla hash de sesiones
        avisos.eliminar(token);
        // Max-Age=0 le dice al navegador que borre la cookie
        intercambio.getResponseHeaders().add("Set-Cookie", "sesion=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        Respuesta.redirigir(intercambio, "/entrar?salio=1");
    }

    /* ---------- Propiedades ---------- */

    private void propiedades(HttpExchange intercambio, boolean esPost, String[] partes, ObjetoJson datos,
                             Usuario usuario, String token) throws IOException {
        // /propiedades (la tabla, con la búsqueda si viene ?q=)
        if (partes.length == 1) {
            String busqueda = Formulario.parametros(intercambio).texto("q");
            mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(busqueda), "");
            return;
        }
        // /propiedades/nueva
        if (partes[1].equals("nueva")) {
            if (esPost) {
                ObjetoJson resultado = propiedades.crear(datos, usuario.getId());
                if (exito(resultado)) {
                    volver(intercambio, token, "success", "Propiedad creada", "/propiedades");
                } else {
                    mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(""),
                            PaginaPropiedades.formulario("Nueva propiedad", "/propiedades/nueva", datos, errores(resultado)));
                }
            } else {
                mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(""),
                        PaginaPropiedades.formulario("Nueva propiedad", "/propiedades/nueva", PaginaPropiedades.valoresNuevos(), new ObjetoJson()));
            }
            return;
        }
        // /propiedades/{id}/{accion}
        String id = partes[1];
        String accion = partes.length > 2 ? partes[2] : "";
        Propiedad propiedad = db.getPropiedades().buscarPorId(id);
        if (propiedad == null) {
            volver(intercambio, token, "warning", "La propiedad ya no existe", "/propiedades");
            return;
        }
        String direccion = "/propiedades/" + id + "/editar";
        switch (accion) {
            case "editar":
                if (!esPost) {
                    mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(""),
                            PaginaPropiedades.formulario("Editar propiedad", direccion, PaginaPropiedades.valoresDe(propiedad), new ObjetoJson()));
                    return;
                }
                ObjetoJson resultado = propiedades.actualizar(id, datos);
                if (exito(resultado)) {
                    volver(intercambio, token, "success", "Cambios guardados", "/propiedades");
                } else {
                    mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(""),
                            PaginaPropiedades.formulario("Editar propiedad", direccion, datos, errores(resultado)));
                }
                return;
            case "estado":
                if (esPost) {
                    ObjetoJson r = propiedades.cambiarEstado(id, datos.texto("status"));
                    guardarAviso(token, exito(r) ? "success" : "error",
                            exito(r) ? "Estado actualizado: " + Etiquetas.estadoPropiedad(datos.texto("status")) : r.texto("error"));
                }
                Respuesta.redirigir(intercambio, "/propiedades");
                return;
            case "eliminar":
                if (!esPost) {
                    mostrar(intercambio, usuario, token, "propiedades", tablaPropiedades(""),
                            PaginaPropiedades.confirmarEliminar(propiedad, propiedades.contarClientes(id)));
                    return;
                }
                ObjetoJson r = propiedades.eliminar(id);
                volver(intercambio, token, exito(r) ? "success" : "error", exito(r) ? "Propiedad eliminada" : r.texto("error"), "/propiedades");
                return;
            default:
                Respuesta.redirigir(intercambio, "/propiedades");
        }
    }

    /** La tabla de propiedades: todas (de la más nueva a la más antigua) o solo las que coinciden con la búsqueda. */
    private String tablaPropiedades(String busqueda) {
        return PaginaPropiedades.contenido(
                busqueda.isEmpty() ? propiedades.recientesPrimero() : propiedades.buscar(busqueda),
                db.getPropiedades().tamano(), busqueda);
    }

    /* ---------- Clientes ---------- */

    private void clientes(HttpExchange intercambio, boolean esPost, String[] partes, ObjetoJson datos,
                          Usuario usuario, String token) throws IOException {
        // /clientes (el tablero)
        if (partes.length == 1) {
            mostrar(intercambio, usuario, token, "clientes", tablero(), "");
            return;
        }
        // /clientes/nuevo
        if (partes[1].equals("nuevo")) {
            if (esPost) {
                ObjetoJson resultado = clientes.crear(datos);
                if (exito(resultado)) {
                    volver(intercambio, token, "success", "Cliente registrado: " + datos.texto("contactName"), "/clientes");
                } else {
                    mostrar(intercambio, usuario, token, "clientes", tablero(),
                            PaginaClientes.formulario(db, null, datos, errores(resultado), usuarios.asesoresOrdenados()));
                }
            } else {
                mostrar(intercambio, usuario, token, "clientes", tablero(),
                        PaginaClientes.formulario(db, null, PaginaClientes.valoresNuevos(usuario), new ObjetoJson(), usuarios.asesoresOrdenados()));
            }
            return;
        }
        // /clientes/{id}/{accion}
        String id = partes[1];
        String accion = partes.length > 2 ? partes[2] : "";
        Cliente cliente = db.getClientes().buscarPorId(id);
        if (cliente == null) {
            volver(intercambio, token, "warning", "Este cliente ya no existe", "/clientes");
            return;
        }
        switch (accion) {
            case "":        // la ficha del cliente
                if (!esPost) {
                    mostrar(intercambio, usuario, token, "clientes", tablero(),
                            PaginaClientes.formulario(db, cliente, cliente.aJson(), new ObjetoJson(), usuarios.asesoresOrdenados()));
                    return;
                }
                ObjetoJson resultado = clientes.actualizar(id, datos);
                if (exito(resultado)) {
                    volver(intercambio, token, "success", "Cambios guardados", "/clientes");
                } else {
                    mostrar(intercambio, usuario, token, "clientes", tablero(),
                            PaginaClientes.formulario(db, cliente, datos, errores(resultado), usuarios.asesoresOrdenados()));
                }
                return;
            case "etapa":
                if (esPost) {
                    ObjetoJson r = clientes.cambiarEtapa(id, datos.texto("stage"));
                    guardarAviso(token, exito(r) ? "success" : "error",
                            exito(r) ? "Etapa cambiada a «" + Etiquetas.etapa(datos.texto("stage")) + "»" : r.texto("error"));
                }
                Respuesta.redirigir(intercambio, "/clientes");
                return;
            case "eliminar":
                if (!esPost) {
                    mostrar(intercambio, usuario, token, "clientes", tablero(), PaginaClientes.confirmarEliminar(cliente));
                    return;
                }
                ObjetoJson r = clientes.eliminar(id);
                volver(intercambio, token, exito(r) ? "success" : "error", exito(r) ? "Cliente eliminado" : r.texto("error"), "/clientes");
                return;
            default:
                Respuesta.redirigir(intercambio, "/clientes");
        }
    }

    /** El tablero de clientes: un arreglo con una lista enlazada por columna. */
    private String tablero() {
        return PaginaClientes.contenido(db, clientes.columnas());
    }

    /* ---------- Ayudantes ---------- */

    /**
     * Envía una página del panel del asesor.
     * @param seccion   "resumen", "propiedades" o "clientes" (la que se marca en el menú)
     * @param contenido el centro de la página
     * @param ventana   una ventana emergente encima (formulario o confirmación), o ""
     */
    private void mostrar(HttpExchange intercambio, Usuario usuario, String token, String seccion,
                         String contenido, String ventana) throws IOException {
        String titulo = seccion.equals("resumen") ? "Resumen" : seccion.equals("propiedades") ? "Propiedades" : "Clientes";
        String html = Plantilla.pagina(titulo, "admin", Plantilla.panelAsesor(usuario, seccion, contenido, ventana), tomarAviso(token));
        Respuesta.html(intercambio, 200, html);
    }

    /** Guarda un aviso y lleva al navegador a otra página, donde se mostrará. */
    private void volver(HttpExchange intercambio, String token, String tipo, String mensaje, String destino) throws IOException {
        guardarAviso(token, tipo, mensaje);
        Respuesta.redirigir(intercambio, destino);
    }

    private void guardarAviso(String token, String tipo, String mensaje) {
        avisos.poner(token, tipo + "|" + mensaje);
    }

    /** Saca el aviso pendiente de esta sesión (y lo borra, para que se vea una sola vez). */
    private String tomarAviso(String token) {
        String aviso = avisos.obtener(token);
        if (aviso == null) {
            return "";
        }
        avisos.eliminar(token);
        String[] partes = aviso.split("\\|", 2);       // "success|Propiedad creada" -> ["success", "Propiedad creada"]
        return Plantilla.aviso(partes[0], partes[1]);
    }

    /** true si el servicio respondió { "ok": true }. */
    private boolean exito(ObjetoJson resultado) {
        return Boolean.TRUE.equals(resultado.obtener("ok"));
    }

    /** Los errores por campo que devolvió el servicio ({} si no hay). */
    private ObjetoJson errores(ObjetoJson resultado) {
        Object errores = resultado.obtener("errors");
        return errores instanceof ObjetoJson ? (ObjetoJson) errores : new ObjetoJson();
    }
}
