package estructuras;

/**
 * Un nodo de la lista enlazada: guarda un dato y apunta al nodo que sigue.
 *
 *   [dato | siguiente] -> [dato | siguiente] -> null
 */
public class Nodo<T> {

    public T dato;              // lo que guarda el nodo
    public Nodo<T> siguiente;   // el nodo que viene después (null si es el último)

    public Nodo(T dato) {
        this.dato = dato;
    }
}
