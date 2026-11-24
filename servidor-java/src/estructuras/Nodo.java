package estructuras;

/**
 * Nodo: la "cajita" básica de una lista enlazada, una cola o una pila.
 *
 * Imagina un tren: cada vagón (nodo) lleva una carga (el dato) y está
 * enganchado al vagón que sigue. El último vagón no tiene siguiente (null).
 *
 *   [dato | siguiente] -> [dato | siguiente] -> [dato | null]
 *
 * <T> significa que el nodo puede guardar cualquier tipo de dato
 * (texto, propiedades, clientes, etc.).
 */
public class Nodo<T> {

    private T dato;               // lo que guarda el nodo
    private Nodo<T> siguiente;    // el nodo que viene después (null si es el último)

    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
