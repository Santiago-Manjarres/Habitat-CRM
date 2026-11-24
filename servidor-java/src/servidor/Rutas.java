package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import datos.EstructuraDeDatos;
import datos.Datos;
import datos.Validador;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import vistas.PaginaClientes;
import vistas.PaginaLogin;
import vistas.PaginaPropiedades;
import vistas.PaginaResumen;

/**
 * Atiende todas las páginas del CRM. Cada dirección hace una cosa:
 *
 *   /entrar                  login (GET: mostrar, POST: revisar usuario y contraseña)
 *   /salir                   cerrar sesión
 *   /resumen                 cifras del negocio
 *   /propiedades             tabla de propiedades (con ?buscar=texto)
 *   /propiedades/nueva       formulario vacío (POST: crear)
 *   /propiedades/editar      formulario con datos, ?id=3 (POST: guardar)
 *   /propiedades/eliminar    POST: eliminar
 *   /clientes ... igual que propiedades: /clientes/nuevo, /clientes/editar, /clientes/eliminar
 *
 * GET = pedir una página.  POST = enviar un formulario.
 */
public class Rutas implements HttpHandler {

    private final EstructuraDeDatos estructura;

    public Rutas(EstructuraDeDatos estructura) {
        this.estructura = estructura;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        // 1. Leemos la petición
        String ruta = intercambio.getRequestURI().getPath();                  // ej. "/propiedades/editar"
        boolean esPost = intercambio.getRequestMethod().equals("POST");
        Datos formulario = leerFormulario(intercambio, esPost);                // lo que escribió la persona
        Datos usuario = estructura.buscarPorSesion(leerCookie(intercambio));           // el asesor con sesión, o null

        // 2. El login es la única página que no pide sesión
        if (ruta.equals("/entrar")) {
            entrar(intercambio, esPost, formulario, usuario);
            return;
        }
        // 3. Todas las demás exigen haber iniciado sesión
        if (usuario == null) {
            redirigir(intercambio, "/entrar");
            return;
        }
        // 4. Cada dirección hace una cosa
        switch (ruta) {
            case "/salir":
                usuario.poner("sesion", "");                                   // olvidamos su código de sesión
                redirigir(intercambio, "/entrar");
                break;
            case "/resumen":
                enviar(intercambio, PaginaResumen.html(estructura, usuario));
                break;
            case "/propiedades":
                enviar(intercambio, PaginaPropiedades.lista(estructura, usuario, formulario.get("buscar")));
                break;
            case "/propiedades/nueva":
                nuevaPropiedad(intercambio, esPost, formulario, usuario);
                break;
            case "/propiedades/editar":
                editarPropiedad(intercambio, esPost, formulario, usuario);
                break;
            case "/propiedades/eliminar":
                eliminar(intercambio, esPost, estructura.buscar(estructura.propiedades, formulario.get("id")), true);
                break;
            case "/clientes":
                enviar(intercambio, PaginaClientes.tablero(estructura, usuario));
                break;
            case "/clientes/nuevo":
                nuevoCliente(intercambio, esPost, formulario, usuario);
                break;
            case "/clientes/editar":
                editarCliente(intercambio, esPost, formulario, usuario);
                break;
            case "/clientes/eliminar":
                eliminar(intercambio, esPost, estructura.buscar(estructura.clientes, formulario.get("id")), false);
                break;
            default:
                redirigir(intercambio, "/resumen");
        }
    }

    /* ---------- Login ---------- */

    private void entrar(HttpExchange intercambio, boolean esPost, Datos formulario, Datos usuario) throws IOException {
        if (usuario != null) {
            redirigir(intercambio, "/resumen");                     // ya tenía sesión
            return;
        }
        if (!esPost) {
            enviar(intercambio, PaginaLogin.html("", new Datos(), ""));      // formulario vacío
            return;
        }
        String nombre = formulario.get("usuario");
        String clave = formulario.get("clave");
        // 1. ¿Están vacíos?
        Datos errores = Validador.login(nombre, clave);
        if (!errores.estaVacio()) {
            enviar(intercambio, PaginaLogin.html(nombre, errores, ""));
            return;
        }
        // 2. ¿Existe el usuario y la contraseña coincide?
        Datos encontrado = estructura.buscarUsuario(nombre);
        if (encontrado == null || !encontrado.get("clave").equals(clave)) {
            enviar(intercambio, PaginaLogin.html(nombre, new Datos(), "Usuario o contraseña incorrectos"));
            return;
        }
        // 3. Todo bien: creamos un código de sesión, se lo damos al navegador en una cookie y entramos
        String codigo = UUID.randomUUID().toString();
        encontrado.poner("sesion", codigo);
        intercambio.getResponseHeaders().add("Set-Cookie", "sesion=" + codigo + "; Path=/; HttpOnly");
        redirigir(intercambio, "/resumen");
    }

    /* ---------- Propiedades ---------- */

    private void nuevaPropiedad(HttpExchange intercambio, boolean esPost, Datos formulario, Datos usuario) throws IOException {
        if (!esPost) {
            enviar(intercambio, PaginaPropiedades.formulario(usuario, new Datos(), new Datos(), false));
            return;
        }
        Datos errores = Validador.propiedad(formulario);
        if (!errores.estaVacio()) {          // hay errores: se muestra el formulario otra vez, con los mensajes
            enviar(intercambio, PaginaPropiedades.formulario(usuario, formulario, errores, false));
            return;
        }
        Datos propiedad = new Datos();
        propiedad.poner("id", estructura.nuevoId(estructura.propiedades));
        copiar(formulario, propiedad, EstructuraDeDatos.CAMPOS_PROPIEDAD);
        estructura.propiedades.agregar(propiedad);   // se agrega a la lista...
        estructura.guardar();                        // ...y se escribe el archivo .json
        redirigir(intercambio, "/propiedades");
    }

    private void editarPropiedad(HttpExchange intercambio, boolean esPost, Datos formulario, Datos usuario) throws IOException {
        Datos propiedad = estructura.buscar(estructura.propiedades, formulario.get("id"));
        if (propiedad == null) {
            redirigir(intercambio, "/propiedades");
            return;
        }
        if (!esPost) {
            enviar(intercambio, PaginaPropiedades.formulario(usuario, propiedad, new Datos(), true));
            return;
        }
        Datos errores = Validador.propiedad(formulario);
        if (!errores.estaVacio()) {
            enviar(intercambio, PaginaPropiedades.formulario(usuario, formulario, errores, true));
            return;
        }
        copiar(formulario, propiedad, EstructuraDeDatos.CAMPOS_PROPIEDAD);
        estructura.guardar();
        redirigir(intercambio, "/propiedades");
    }

    /* ---------- Clientes ---------- */

    private void nuevoCliente(HttpExchange intercambio, boolean esPost, Datos formulario, Datos usuario) throws IOException {
        if (!esPost) {
            enviar(intercambio, PaginaClientes.formulario(estructura, usuario, new Datos(), new Datos(), false));
            return;
        }
        Datos errores = Validador.cliente(formulario);
        if (!errores.estaVacio()) {
            enviar(intercambio, PaginaClientes.formulario(estructura, usuario, formulario, errores, false));
            return;
        }
        Datos cliente = new Datos();
        cliente.poner("id", estructura.nuevoId(estructura.clientes));
        copiar(formulario, cliente, EstructuraDeDatos.CAMPOS_CLIENTE);
        estructura.clientes.agregar(cliente);
        estructura.guardar();
        redirigir(intercambio, "/clientes");
    }

    private void editarCliente(HttpExchange intercambio, boolean esPost, Datos formulario, Datos usuario) throws IOException {
        Datos cliente = estructura.buscar(estructura.clientes, formulario.get("id"));
        if (cliente == null) {
            redirigir(intercambio, "/clientes");
            return;
        }
        if (!esPost) {
            enviar(intercambio, PaginaClientes.formulario(estructura, usuario, cliente, new Datos(), true));
            return;
        }
        Datos errores = Validador.cliente(formulario);
        if (!errores.estaVacio()) {
            enviar(intercambio, PaginaClientes.formulario(estructura, usuario, formulario, errores, true));
            return;
        }
        copiar(formulario, cliente, EstructuraDeDatos.CAMPOS_CLIENTE);
        estructura.guardar();
        redirigir(intercambio, "/clientes");
    }

    /* ---------- Eliminar (propiedad o cliente) ---------- */

    private void eliminar(HttpExchange intercambio, boolean esPost, Datos registro, boolean esPropiedad) throws IOException {
        if (esPost && registro != null) {
            if (esPropiedad) {
                estructura.propiedades.eliminar(registro);       // se quita de la lista...
            } else {
                estructura.clientes.eliminar(registro);
            }
            estructura.guardar();                                // ...y se reescribe el archivo
        }
        redirigir(intercambio, esPropiedad ? "/propiedades" : "/clientes");
    }

    /* ---------- Ayudantes ---------- */

    /** Copia los campos indicados del formulario al registro. */
    private void copiar(Datos desde, Datos hacia, String[] campos) {
        for (String campo : campos) {
            hacia.poner(campo, desde.get(campo));
        }
    }

    /**
     * Lee los datos que manda el navegador. Llegan como texto así:
     *     titulo=Casa+en+Laureles&precio=450.000.000
     * En un POST vienen en el cuerpo; en un GET, en la dirección después del "?".
     */
    private Datos leerFormulario(HttpExchange intercambio, boolean esPost) throws IOException {
        String texto = esPost
                ? new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                : intercambio.getRequestURI().getRawQuery();
        Datos datos = new Datos();
        if (texto == null || texto.isEmpty()) {
            return datos;
        }
        for (String pareja : texto.split("&")) {                  // "titulo=Casa+en+Laureles"
            String[] partes = pareja.split("=", 2);               // ["titulo", "Casa+en+Laureles"]
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            datos.poner(URLDecoder.decode(partes[0], StandardCharsets.UTF_8), valor.trim());   // "+" -> espacio, "%C3%A1" -> "á"
        }
        return datos;
    }

    /** Busca la cookie "sesion". El navegador manda sus cookies así: "sesion=abc123; otra=xyz". */
    private String leerCookie(HttpExchange intercambio) {
        String cookies = intercambio.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String cookie : cookies.split(";")) {
            cookie = cookie.trim();
            if (cookie.startsWith("sesion=")) {
                return cookie.substring("sesion=".length());
            }
        }
        return null;
    }

    /** Envía una página HTML al navegador. */
    private void enviar(HttpExchange intercambio, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        intercambio.sendResponseHeaders(200, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    /** Le dice al navegador que vaya a otra dirección (después de guardar, así recargar no repite el envío). */
    private void redirigir(HttpExchange intercambio, String destino) throws IOException {
        intercambio.getResponseHeaders().set("Location", destino);
        intercambio.sendResponseHeaders(303, -1);
        intercambio.close();
    }
}
