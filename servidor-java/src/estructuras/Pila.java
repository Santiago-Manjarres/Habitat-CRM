package estructuras;

/**
 * Pila (LIFO: el último en entrar es el primero en salir), hecha con nodos.
 *
 * En el CRM se usa para mostrar primero lo más reciente: se apilan los
 * registros del más antiguo al más nuevo y al desapilarlos salen del más
 * nuevo al más antiguo (por ejemplo, los "Últimos movimientos" del resumen).
 */
public class Pila<T> {

    private Nodo<T> cima;
    private int tamano;

    /** Pone un elemento encima de la pila. */
    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cima);
        cima = nuevo;
        tamano++;
    }

    /** Saca y devuelve el elemento de encima (o null si la pila está vacía). */
    public T desapilar() {
        if (cima == null) {
            return null;
        }
        T dato = cima.getDato();
        cima = cima.getSiguiente();
        tamano--;
        return dato;
    }

    /** Devuelve el elemento de encima sin sacarlo. */
    public T verCima() {
        return cima == null ? null : cima.getDato();
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }
}
