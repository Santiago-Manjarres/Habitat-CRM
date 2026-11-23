package estructuras;

/**
 * Cola (FIFO: el primero en entrar es el primero en salir), hecha con nodos.
 *
 * En el CRM se usa para los clientes "por contactar": el que llegó primero
 * queda al frente de la cola y es al que hay que llamar primero.
 */
public class Cola<T> {

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamano;

    /** Agrega un elemento al final de la cola. */
    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.setSiguiente(nuevo);
        }
        fin = nuevo;
        tamano++;
    }

    /** Saca y devuelve el elemento del frente (o null si la cola está vacía). */
    public T desencolar() {
        if (frente == null) {
            return null;
        }
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        if (frente == null) {
            fin = null;
        }
        tamano--;
        return dato;
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
