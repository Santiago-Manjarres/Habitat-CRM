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
    private final TablaHash<Usuario> porNombreDeUsuario = new TablaHash<>();   // "laura" -> el asesor Laura
    private final TablaHash<String> sesiones = new TablaHash<>();              // código de sesión -> id del asesor

    public ServicioUsuarios(BaseDeDatos db) {
        this.db = db;
        // Al arrancar, llenamos la tabla hash con todos los asesores para encontrarlos rápido al iniciar sesión
        for (Nodo<Usuario> n = db.getUsuarios().getPrimero(); n != null; n = n.getSiguiente()) {
            porNombreDeUsuario.poner(n.getDato().getUsuario().toLowerCase(), n.getDato());
        }
    }

    /**
     * El usuario no distingue mayúsculas; la contraseña sí.
     * Si todo está bien devuelve { ok: true, user, token }.
     * Si faltan datos devuelve { ok: false, fieldErrors }; si no coinciden, { ok: false, error }.
     */
    public ObjetoJson iniciarSesion(ObjetoJson datos) {
        String usuario = datos.texto("username");
        String contrasena = datos.obtener("password") == null ? "" : datos.obtener("password").toString();
        // 1. revisamos que no vengan vacíos
        ObjetoJson errores = Validador.validarInicioSesion(usuario, contrasena);
        if (errores.tamano() > 0) {
            return new ObjetoJson().poner("ok", false).poner("fieldErrors", errores);
        }
        // 2. buscamos al asesor en la tabla hash y comparamos la contraseña
        Usuario encontrado = porNombreDeUsuario.obtener(usuario.toLowerCase());
        if (encontrado == null || !encontrado.getContrasena().equals(contrasena)) {
            return Herramientas.fallo("Usuario o contraseña incorrectos");
        }
        // 3. creamos un código de sesión único y lo guardamos en la tabla de sesiones
        String token = UUID.randomUUID().toString();
        sesiones.poner(token, encontrado.getId());
        // 4. le devolvemos al navegador el asesor (sin contraseña) y su código de sesión
        return Herramientas.exito("user", encontrado.aJsonPublico()).poner("token", token);
    }

    /** Cierra la sesión: borra el código de la tabla de sesiones. */
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
    public ListaEnlazada<Usuario> asesoresOrdenados() {
        ArbolBinarioBusqueda<Usuario> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Usuario> n = db.getUsuarios().getPrimero(); n != null; n = n.getSiguiente()) {
            Usuario u = n.getDato();
            // La clave del árbol es el nombre (sin tildes ni mayúsculas) + el id, para que dos asesores
            // con el mismo nombre no tengan la misma clave y se pisen.
            arbol.insertar(Herramientas.normalizar(u.getNombre()) + u.getId(), u);
        }
        // El recorrido en orden entrega los asesores ya ordenados alfabéticamente
        return arbol.recorrerEnOrden();
    }
}
