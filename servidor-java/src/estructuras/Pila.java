package estructuras;

/**
 * Pila (LIFO: el último en entrar es el primero en salir), hecha con nodos.
 *
 * Imagina una pila de platos: pones uno encima de otro y solo puedes
 * sacar el de arriba.
 *
 *   cima -> [3] -> [2] -> [1] -> null
 *
 * En el CRM se usa para mostrar primero lo más reciente: se apilan los
 * registros del más antiguo al más nuevo y al desapilarlos salen del más
 * nuevo al más antiguo (por ejemplo, los "Últimos movimientos" del resumen).
 */
public class Pila<T> {

    private Nodo<T> cima;   // el nodo de arriba de la pila
    private int tamano;     // cuántos elementos hay

    /** Pone un elemento encima de la pila. */
    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);   // 1. creamos el nodo
        nuevo.setSiguiente(cima);           // 2. apunta al que estaba arriba
        cima = nuevo;                       // 3. ahora el nuevo es la cima
        tamano++;
    }

    /** Saca y devuelve el elemento de encima (o null si la pila está vacía). */
    public T desapilar() {
        if (cima == null) {
            return null;                    // la pila está vacía
        }
        T dato = cima.getDato();            // 1. guardamos el dato de arriba
        cima = cima.getSiguiente();         // 2. la cima baja al siguiente nodo
        tamano--;
        return dato;                        // 3. devolvemos el dato
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
