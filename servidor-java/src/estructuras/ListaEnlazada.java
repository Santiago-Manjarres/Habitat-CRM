package estructuras;

/**
 * Lista enlazada simple: la ÚNICA estructura de datos del proyecto.
 *
 *   cabeza -> [A] -> [B] -> [C] -> null
 *
 * Guarda los usuarios, las propiedades, los clientes y los campos de cada uno.
 * Para recorrerla se empieza en la cabeza y se avanza con "siguiente":
 *
 *   for (Nodo<T> n = lista.getCabeza(); n != null; n = n.siguiente) { ... n.dato ... }
 */
public class ListaEnlazada<T> {

    private Nodo<T> cabeza;   // el primer nodo (null si la lista está vacía)
    private int tamano;       // cuántos datos hay

    /** Agrega un dato al final de la lista. */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;                     // la lista estaba vacía: el nuevo es el primero
        } else {
            Nodo<T> actual = cabeza;
            while (actual.siguiente != null) {  // avanzamos hasta el último nodo
                actual = actual.siguiente;
            }
            actual.siguiente = nuevo;           // el último ahora apunta al nuevo
        }
        tamano++;
    }

    /** Quita un dato de la lista. */
    public void eliminar(T dato) {
        if (cabeza == null) {
            return;                             // lista vacía: no hay nada que quitar
        }
        if (cabeza.dato == dato) {
            cabeza = cabeza.siguiente;          // era el primero: la cabeza pasa al segundo
            tamano--;
            return;
        }
        Nodo<T> actual = cabeza;
        while (actual.siguiente != null) {
            if (actual.siguiente.dato == dato) {
                actual.siguiente = actual.siguiente.siguiente;   // lo "saltamos": queda fuera de la cadena
                tamano--;
                return;
            }
            actual = actual.siguiente;
        }
    }

    public Nodo<T> getCabeza() {
        return cabeza;
    }

    public int tamano() {
        return tamano;
    }
}
