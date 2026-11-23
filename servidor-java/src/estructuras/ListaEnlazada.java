package estructuras;

/**
 * Lista enlazada simple.
 *
 * Cada elemento vive en un Nodo que apunta al siguiente. Guardamos la cabeza
 * (primer nodo) y la cola (último nodo) para poder agregar al final sin
 * recorrer toda la lista.
 *
 * En el CRM se usa para guardar en memoria cada "tabla" de la base de datos
 * (usuarios, propiedades y clientes) y para devolver resultados de búsquedas.
 */
public class ListaEnlazada<T> {

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public ListaEnlazada() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    /** Agrega un elemento al final de la lista. */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            cola.setSiguiente(nuevo);
        }
        cola = nuevo;
        tamano++;
    }

    /** Agrega un elemento al inicio de la lista. */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /** Devuelve el elemento que está en la posición indicada (empieza en 0). */
    public T obtener(int posicion) {
        if (posicion < 0 || posicion >= tamano) {
            return null;
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < posicion; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /** Elimina el primer nodo que guarda exactamente este dato. Devuelve true si lo encontró. */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (actual.getDato() == dato) {
                if (anterior == null) {
                    cabeza = actual.getSiguiente();
                } else {
                    anterior.setSiguiente(actual.getSiguiente());
                }
                if (actual == cola) {
                    cola = anterior;
                }
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /** Vacía la lista. */
    public void limpiar() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

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
