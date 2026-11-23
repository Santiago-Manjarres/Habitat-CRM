package servicios;

import datos.BaseDeDatos;
import estructuras.ArbolBinarioBusqueda;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import estructuras.TablaHash;
import java.util.UUID;
import json.ObjetoJson;
import modelo.Usuario;

/**
 * Inicio y cierre de sesión de los asesores.
 *
 * Usa dos tablas hash:
 *  - porNombreDeUsuario: encuentra al asesor que escribió su usuario al iniciar sesión;
 *  - sesiones: guarda las sesiones abiertas (código de sesión -> id del asesor).
 */
public class ServicioUsuarios {

    private final BaseDeDatos db;
    private final TablaHash<Usuario> porNombreDeUsuario = new TablaHash<>();
    private final TablaHash<String> sesiones = new TablaHash<>();

    public ServicioUsuarios(BaseDeDatos db) {
        this.db = db;
        for (Nodo<Usuario> n = db.getUsuarios().getPrimero(); n != null; n = n.getSiguiente()) {
            porNombreDeUsuario.poner(n.getDato().getUsuario().toLowerCase(), n.getDato());
        }
    }

    /**
     * El usuario no distingue mayúsculas; la contraseña sí.
     * Si todo está bien devuelve { ok, user, token }.
     */
    public ObjetoJson iniciarSesion(ObjetoJson datos) {
        String usuario = datos.texto("username");
        String contrasena = datos.obtener("password") == null ? "" : datos.obtener("password").toString();
        ObjetoJson errores = Validador.validarInicioSesion(usuario, contrasena);
        if (errores.tamano() > 0) {
            return new ObjetoJson().poner("ok", false).poner("fieldErrors", errores);
        }
        Usuario encontrado = porNombreDeUsuario.obtener(usuario.toLowerCase());
        if (encontrado == null || !encontrado.getContrasena().equals(contrasena)) {
            return Herramientas.fallo("Usuario o contraseña incorrectos");
        }
        String token = UUID.randomUUID().toString();
        sesiones.poner(token, encontrado.getId());
        return Herramientas.exito("user", encontrado.aJsonPublico()).poner("token", token);
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.eliminar(token);
        }
    }

    /** Devuelve el asesor dueño de una sesión, o null si la sesión no existe. */
    public Usuario usuarioDeSesion(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String id = sesiones.obtener(token);
        return id == null ? null : db.getUsuarios().buscarPorId(id);
    }

    /** Todos los asesores ordenados por nombre, usando un árbol binario de búsqueda. */
    public ListaEnlazada<Object> asesoresOrdenados() {
        ArbolBinarioBusqueda<Usuario> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Usuario> n = db.getUsuarios().getPrimero(); n != null; n = n.getSiguiente()) {
            Usuario u = n.getDato();
            arbol.insertar(Herramientas.normalizar(u.getNombre()) + u.getId(), u);
        }
        ListaEnlazada<Object> resultado = new ListaEnlazada<>();
        for (Nodo<Usuario> n = arbol.recorrerEnOrden().getCabeza(); n != null; n = n.getSiguiente()) {
            resultado.agregar(n.getDato().aJsonPublico());
        }
        return resultado;
    }
}
