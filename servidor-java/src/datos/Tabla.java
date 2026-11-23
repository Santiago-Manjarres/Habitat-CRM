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

    private final ListaEnlazada<T> registros = new ListaEnlazada<>();
    private final TablaHash<T> indicePorId = new TablaHash<>();

    public void agregar(T registro) {
        registros.agregar(registro);
        indicePorId.poner(registro.getId(), registro);
    }

    public T buscarPorId(String id) {
        return indicePorId.obtener(id);
    }

    public boolean existe(String id) {
        return indicePorId.contiene(id);
    }

    /** Elimina un registro de la lista y del índice. */
    public boolean eliminarPorId(String id) {
        T registro = indicePorId.obtener(id);
        if (registro == null) {
            return false;
        }
        registros.eliminar(registro);
        indicePorId.eliminar(id);
        return true;
    }

    public ListaEnlazada<T> getRegistros() {
        return registros;
    }

    public Nodo<T> getPrimero() {
        return registros.getCabeza();
    }

    public int tamano() {
        return registros.tamano();
    }
}
