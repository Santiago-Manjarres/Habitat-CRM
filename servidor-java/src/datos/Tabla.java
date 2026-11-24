package datos;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import estructuras.TablaHash;
import modelo.Registro;

/**
 * Una "tabla" de la base de datos simulada, guardada en memoria.
 *
 * Usa dos estructuras a la vez:
 *  - una ListaEnlazada con todos los registros, en el mismo orden del archivo;
 *  - una TablaHash que funciona como índice: dado un id, encuentra el
 *    registro de inmediato sin recorrer la lista.
 */
public class Tabla<T extends Registro> {

    private final ListaEnlazada<T> registros = new ListaEnlazada<>();   // todos los registros, en orden
    private final TablaHash<T> indicePorId = new TablaHash<>();         // índice para buscar por id rápido

    /** Agrega un registro en las dos estructuras a la vez. */
    public void agregar(T registro) {
        registros.agregar(registro);                        // a la lista (mantiene el orden)
        indicePorId.poner(registro.getId(), registro);      // a la tabla hash (para buscar por id)
    }

    /** Busca un registro por su id usando la tabla hash (no recorre la lista). */
    public T buscarPorId(String id) {
        return indicePorId.obtener(id);
    }

    public boolean existe(String id) {
        return indicePorId.contiene(id);
    }

    /** Elimina un registro de la lista y del índice. */
    public boolean eliminarPorId(String id) {
        T registro = indicePorId.obtener(id);   // 1. lo buscamos por id
        if (registro == null) {
            return false;                       // no existe
        }
        registros.eliminar(registro);           // 2. lo quitamos de la lista
        indicePorId.eliminar(id);               // 3. lo quitamos del índice
        return true;
    }

    public ListaEnlazada<T> getRegistros() {
        return registros;
    }

    /** Primer nodo de la lista, para recorrer todos los registros con un while. */
    public Nodo<T> getPrimero() {
        return registros.getCabeza();
    }

    public int tamano() {
        return registros.tamano();
    }
}
