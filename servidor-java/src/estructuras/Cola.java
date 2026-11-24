package estructuras;

/**
 * Cola (FIFO: el primero en entrar es el primero en salir), hecha con nodos.
 *
 * Imagina la fila de un banco: quien llega primero es atendido primero.
 *
 *   frente -> [1] -> [2] -> [3] -> null
 *                           ^
 *                          fin
 *
 * En el CRM se usa para los clientes "por contactar": el que llegó primero
 * queda al frente de la cola y es al que hay que llamar primero.
 */
public class Cola<T> {

    private Nodo<T> frente;   // el primero de la fila (el próximo en salir)
    private Nodo<T> fin;      // el último de la fila (el que acaba de llegar)
    private int tamano;       // cuántos elementos hay

    /** Agrega un elemento al final de la cola. */
    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);   // 1. creamos el nodo
        if (fin == null) {
            frente = nuevo;                 // 2a. si la cola estaba vacía, es el primero
        } else {
            fin.setSiguiente(nuevo);        // 2b. si no, el último actual apunta al nuevo
        }
        fin = nuevo;                        // 3. el nuevo queda al final de la fila
        tamano++;
    }

    /** Saca y devuelve el elemento del frente (o null si la cola está vacía). */
    public T desencolar() {
        if (frente == null) {
            return null;                    // la cola está vacía
        }
        T dato = frente.getDato();          // 1. guardamos el dato del primero
        frente = frente.getSiguiente();     // 2. el segundo pasa a ser el primero
        if (frente == null) {
            fin = null;                     // si ya no queda nadie, el final también se vacía
        }
        tamano--;
        return dato;                        // 3. devolvemos el dato
    }

    /** Devuelve el elemento del frente sin sacarlo. */
    public T verFrente() {
        return frente == null ? null : frente.getDato();
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }
}
