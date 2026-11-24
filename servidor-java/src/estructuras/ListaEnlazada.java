package estructuras;

/**
 * Lista enlazada simple.
 *
 * Es una cadena de nodos: cada elemento vive en un Nodo que apunta al siguiente.
 *
 *   cabeza -> [A] -> [B] -> [C] -> null
 *                            ^
 *                           cola
 *
 * Guardamos la cabeza (primer nodo) y la cola (último nodo). Con la cola
 * podemos agregar al final sin recorrer toda la lista.
 *
 * Diferencia con un arreglo: en una lista enlazada los elementos no están
 * juntos en memoria; solo están conectados por referencias.
 *
 * En el CRM se usa para guardar en memoria cada "tabla" de la base de datos
 * (usuarios, propiedades y clientes) y para devolver resultados de búsquedas.
 */
public class ListaEnlazada<T> {

    private Nodo<T> cabeza;   // primer nodo de la lista (null si está vacía)
    private Nodo<T> cola;     // último nodo de la lista
    private int tamano;       // cuántos elementos hay

    // Una lista nueva empieza vacía
    public ListaEnlazada() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    /** Agrega un elemento al final de la lista. */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);      // 1. creamos el nodo nuevo
        if (cabeza == null) {
            cabeza = nuevo;                     // 2a. si la lista estaba vacía, el nuevo es el primero
        } else {
            cola.setSiguiente(nuevo);           // 2b. si no, el último actual apunta al nuevo
        }
        cola = nuevo;                           // 3. el nuevo pasa a ser el último
        tamano++;
    }

    /** Agrega un elemento al inicio de la lista. */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);      // 1. creamos el nodo nuevo
        nuevo.setSiguiente(cabeza);             // 2. el nuevo apunta al que era el primero
        cabeza = nuevo;                         // 3. el nuevo pasa a ser el primero
        if (cola == null) {
            cola = nuevo;                       // si la lista estaba vacía, también es el último
        }
        tamano++;
    }

    /** Devuelve el elemento que está en la posición indicada (empieza en 0). */
    public T obtener(int posicion) {
        if (posicion < 0 || posicion >= tamano) {
            return null;                        // la posición no existe
        }
        Nodo<T> actual = cabeza;                // empezamos por el primero
        for (int i = 0; i < posicion; i++) {
            actual = actual.getSiguiente();     // avanzamos nodo por nodo hasta llegar
        }
        return actual.getDato();
    }

    /** Elimina el primer nodo que guarda exactamente este dato. Devuelve true si lo encontró. */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;                // el nodo que está justo antes del actual
        Nodo<T> actual = cabeza;                // empezamos por el primero
        while (actual != null) {
            if (actual.getDato() == dato) {     // ¿es el que buscamos? (se compara la misma referencia)
                if (anterior == null) {
                    cabeza = actual.getSiguiente();             // era el primero: la cabeza avanza
                } else {
                    anterior.setSiguiente(actual.getSiguiente()); // lo saltamos: el anterior apunta al siguiente
                }
                if (actual == cola) {
                    cola = anterior;            // era el último: la cola retrocede
                }
                tamano--;
                return true;
            }
            anterior = actual;                  // seguimos con el siguiente nodo
            actual = actual.getSiguiente();
        }
        return false;                           // recorrimos todo y no estaba
    }

    /** Vacía la lista. */
    public void limpiar() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    /** Devuelve el primer nodo, para poder recorrer la lista con un while. */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }
}
