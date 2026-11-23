package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import datos.BaseDeDatos;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import json.LectorJson;
import json.ObjetoJson;
import modelo.Usuario;
import servicios.Herramientas;
import servicios.ServicioClientes;
import servicios.ServicioPropiedades;
import servicios.ServicioResumen;
import servicios.ServicioUsuarios;
import servicios.Validador;

/**
 * Atiende las peticiones de la interfaz (todo lo que empieza por /api/).
 *
 *   GET    /api/version                     número de versión de los datos
 *   POST   /api/sesion                      iniciar sesión
 *   DELETE /api/sesion                      cerrar sesión
 *   GET    /api/sesion                      asesor de la sesión actual
 *   GET    /api/estado                      todo lo que la interfaz necesita para dibujarse
 *   GET    /api/propiedades?q=texto         buscar propiedades
 *   POST   /api/propiedades                 crear propiedad
 *   PUT    /api/propiedades/{id}            editar propiedad
 *   PATCH  /api/propiedades/{id}/estado     cambiar estado
 *   DELETE /api/propiedades/{id}            eliminar propiedad (y sus clientes)
 *   POST   /api/clientes                    crear cliente
 *   PUT    /api/clientes/{id}               editar cliente
 *   PATCH  /api/clientes/{id}/etapa         cambiar etapa
 *   DELETE /api/clientes/{id}               eliminar cliente
 *   POST   /api/validar/propiedad           revisar un formulario mientras se escribe
 *   POST   /api/validar/cliente             revisar un formulario mientras se escribe
 *
 * El navegador envía el código de sesión en la cabecera "X-Sesion".
 */
public class ManejadorApi implements HttpHandler {

    private final BaseDeDatos db;
    private final ServicioUsuarios usuarios;
    private final ServicioPropiedades propiedades;
    private final ServicioClientes clientes;
    private final ServicioResumen resumen;

    public ManejadorApi(BaseDeDatos db) {
        this.db = db;
        this.usuarios = new ServicioUsuarios(db);
        this.propiedades = new ServicioPropiedades(db);
        this.clientes = new ServicioClientes(db);
        this.resumen = new ServicioResumen(db);
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            String metodo = intercambio.getRequestMethod();
            String[] partes = intercambio.getRequestURI().getPath().substring("/api/".length()).split("/");
            ObjetoJson cuerpo = leerCuerpo(intercambio);
            String token = intercambio.getRequestHeaders().getFirst("X-Sesion");
            ObjetoJson publica = rutaPublica(metodo, partes, cuerpo, token);
            if (publica != null) {
                Respuesta.json(intercambio, 200, publica);
                return;
            }
            Usuario usuario = usuarios.usuarioDeSesion(token);
            if (usuario == null) {
                Respuesta.json(intercambio, 401, Herramientas.fallo("Tu sesión terminó. Inicia sesión de nuevo").poner("sessionExpired", true));
                return;
            }
            ObjetoJson resultado = rutaPrivada(metodo, partes, cuerpo, usuario, intercambio);
            Respuesta.json(intercambio, resultado == null ? 404 : 200, resultado == null ? Herramientas.fallo("Ruta no encontrada") : resultado);
        } catch (Exception e) {
            e.printStackTrace();
            Respuesta.json(intercambio, 500, Herramientas.fallo("Error del servidor: " + e.getMessage()));
        }
    }

    /** Rutas que no necesitan sesión. */
    private ObjetoJson rutaPublica(String metodo, String[] partes, ObjetoJson cuerpo, String token) {
        if (partes[0].equals("version") && metodo.equals("GET")) {
            return new ObjetoJson().poner("ok", true).poner("version", db.getVersion());
        }
        if (partes[0].equals("sesion") && metodo.equals("POST")) {
            return usuarios.iniciarSesion(cuerpo);
        }
        if (partes[0].equals("sesion") && metodo.equals("DELETE")) {
            usuarios.cerrarSesion(token);
            return new ObjetoJson().poner("ok", true);
        }
        return null;
    }

    /** Rutas que necesitan un asesor con sesión. Devuelve null si la ruta no existe. */
    private ObjetoJson rutaPrivada(String metodo, String[] partes, ObjetoJson cuerpo, Usuario usuario, HttpExchange intercambio) {
        switch (partes[0]) {
            case "sesion":
                return Herramientas.exito("user", usuario.aJsonPublico());
            case "estado":
                return estado();
            case "propiedades":
                return rutaPropiedades(metodo, partes, cuerpo, usuario, intercambio);
            case "clientes":
                return rutaClientes(metodo, partes, cuerpo);
            case "validar":
                ObjetoJson errores = partes.length > 1 && partes[1].equals("propiedad")
                        ? Validador.validarPropiedad(cuerpo) : clientes.revisar(cuerpo);
                return new ObjetoJson().poner("ok", true).poner("errors", errores);
            default:
                return null;
        }
    }

    private ObjetoJson rutaPropiedades(String metodo, String[] partes, ObjetoJson cuerpo, Usuario usuario, HttpExchange intercambio) {
        String id = partes.length > 1 ? partes[1] : "";
        if (metodo.equals("GET")) {
            String q = leerParametro(intercambio, "q");
            return Herramientas.exito("properties", propiedades.aListaJson(q.isEmpty() ? propiedades.recientesPrimero() : propiedades.buscar(q)));
        }
        if (metodo.equals("POST")) return propiedades.crear(cuerpo, usuario.getId());
        if (metodo.equals("PUT")) return propiedades.actualizar(id, cuerpo);
        if (metodo.equals("PATCH")) return propiedades.cambiarEstado(id, cuerpo.texto("status"));
        if (metodo.equals("DELETE")) return propiedades.eliminar(id);
        return null;
    }

    private ObjetoJson rutaClientes(String metodo, String[] partes, ObjetoJson cuerpo) {
        String id = partes.length > 1 ? partes[1] : "";
        if (metodo.equals("POST")) return clientes.crear(cuerpo);
        if (metodo.equals("PUT")) return clientes.actualizar(id, cuerpo);
        if (metodo.equals("PATCH")) return clientes.cambiarEtapa(id, cuerpo.texto("stage"));
        if (metodo.equals("DELETE")) return clientes.eliminar(id);
        return null;
    }

    /** Todo lo que la interfaz necesita para dibujar el resumen, el inventario y el tablero. */
    private ObjetoJson estado() {
        ObjetoJson cifras = new ObjetoJson()
                .poner("kpis", resumen.indicadores())
                .poner("funnel", resumen.clientesPorEtapa())
                .poner("inventory", resumen.inventarioPorEstado())
                .poner("recent", clientes.aListaJson(clientes.ultimosMovimientos(5)))
                .poner("pending", clientes.porContactar().tamano());
        return new ObjetoJson()
                .poner("ok", true)
                .poner("version", db.getVersion())
                .poner("advisors", usuarios.asesoresOrdenados())
                .poner("properties", propiedades.aListaJson(propiedades.recientesPrimero()))
                .poner("leads", clientes.aListaJson(clientes.recientesPrimero()))
                .poner("summary", cifras)
                .poner("board", clientes.tablero());
    }

    /** Lee el cuerpo de la petición como JSON (si viene vacío, devuelve un objeto vacío). */
    private ObjetoJson leerCuerpo(HttpExchange intercambio) throws IOException {
        String texto = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (texto.isEmpty()) {
            return new ObjetoJson();
        }
        Object valor = LectorJson.leer(texto);
        return valor instanceof ObjetoJson ? (ObjetoJson) valor : new ObjetoJson();
    }

    /** Lee un parámetro de la dirección, por ejemplo el "q" de /api/propiedades?q=bogota */
    private String leerParametro(HttpExchange intercambio, String nombre) {
        String consulta = intercambio.getRequestURI().getRawQuery();
        if (consulta == null) {
            return "";
        }
        for (String par : consulta.split("&")) {
            String[] claveValor = par.split("=", 2);
            if (claveValor[0].equals(nombre) && claveValor.length == 2) {
                return URLDecoder.decode(claveValor[1], StandardCharsets.UTF_8).trim();
            }
        }
        return "";
    }
}
