package datos;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import java.io.IOException;
import java.nio.file.Path;

/**
 * La estructura de datos del CRM: tres listas enlazadas que se leen de los .json al
 * arrancar y se vuelven a escribir cada vez que algo cambia.
 *
 *   data/usuarios.json     -> usuarios     (usuario, clave, nombre)
 *   data/propiedades.json  -> propiedades  (id, titulo, tipo, operacion, precio, ubicacion, estado, foto)
 *   data/clientes.json     -> clientes     (id, nombre, telefono, correo, propiedad, etapa, notas)
 */
public class EstructuraDeDatos {

    // Opciones de las listas desplegables
    public static final String[] TIPOS = {"Apartamento", "Casa", "Local", "Oficina", "Lote"};
    public static final String[] OPERACIONES = {"Venta", "Arriendo"};
    public static final String[] ESTADOS = {"Disponible", "En negociación", "Vendida"};
    public static final String[] ETAPAS = {"Nuevo", "Contactado", "Visita", "Negociación", "Cerrado"};

    // Los campos que se pueden editar desde cada formulario
    public static final String[] CAMPOS_PROPIEDAD = {"titulo", "tipo", "operacion", "precio", "ubicacion", "estado", "foto"};
    public static final String[] CAMPOS_CLIENTE = {"nombre", "telefono", "correo", "propiedad", "etapa", "notas"};

    public final ListaEnlazada<Datos> usuarios;
    public final ListaEnlazada<Datos> propiedades;
    public final ListaEnlazada<Datos> clientes;
    private final Path carpeta;

    /** Lee los tres archivos y los carga en memoria. */
    public EstructuraDeDatos(Path carpeta) throws IOException {
        this.carpeta = carpeta;
        usuarios = Json.leer(carpeta.resolve("usuarios.json"));
        propiedades = Json.leer(carpeta.resolve("propiedades.json"));
        clientes = Json.leer(carpeta.resolve("clientes.json"));
    }

    /** Escribe las propiedades y los clientes en sus archivos (los usuarios no cambian). */
    public void guardar() throws IOException {
        Json.escribir(carpeta.resolve("propiedades.json"), propiedades);
        Json.escribir(carpeta.resolve("clientes.json"), clientes);
    }

    /** Busca en una lista el registro con ese id, recorriéndola de principio a fin. */
    public Datos buscar(ListaEnlazada<Datos> lista, String id) {
        for (Nodo<Datos> n = lista.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato.get("id").equals(id)) {
                return n.dato;
            }
        }
        return null;
    }

    /** Busca un asesor por su nombre de usuario (sin importar mayúsculas). */
    public Datos buscarUsuario(String usuario) {
        for (Nodo<Datos> n = usuarios.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato.get("usuario").equalsIgnoreCase(usuario)) {
                return n.dato;
            }
        }
        return null;
    }

    /** Busca el asesor que tiene ese código de sesión. */
    public Datos buscarPorSesion(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        for (Nodo<Datos> n = usuarios.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato.get("sesion").equals(codigo)) {
                return n.dato;
            }
        }
        return null;
    }

    /** El id para un registro nuevo: el id más alto de la lista + 1. */
    public String nuevoId(ListaEnlazada<Datos> lista) {
        int mayor = 0;
        for (Nodo<Datos> n = lista.getCabeza(); n != null; n = n.siguiente) {
            mayor = Math.max(mayor, Integer.parseInt(n.dato.get("id")));
        }
        return String.valueOf(mayor + 1);
    }

    /** Cuántos registros de la lista tienen ese valor en ese campo. Ej: contar(clientes, "etapa", "Nuevo"). */
    public int contar(ListaEnlazada<Datos> lista, String campo, String valor) {
        int cantidad = 0;
        for (Nodo<Datos> n = lista.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato.get(campo).equals(valor)) {
                cantidad++;
            }
        }
        return cantidad;
    }
}
