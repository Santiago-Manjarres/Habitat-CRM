package estructuras;

/**
 * Tabla hash con claves de texto.
 *
 *   posición 0: [ ]
 *   posición 1: [clave A] -> [clave D]   <- colisión: dos claves en la misma cubeta
 *   posición 2: [clave B]
 *   ...
 *
 * Es un arreglo de "cubetas". La función hash convierte la clave en una
 * posición del arreglo. Si dos claves caen en la misma cubeta (una colisión),
 * se guardan juntas en una lista enlazada dentro de esa cubeta.
 *
 * Buscar por clave es muy rápido porque se va directo a la cubeta, sin
 * recorrer todos los datos. En el CRM se usa para encontrar un registro por
 * su id, un usuario por su nombre de usuario y una sesión por su código.
 */
public class TablaHash<T> {

    private static final int CAPACIDAD_INICIAL = 31;   // cuántas cubetas tiene el arreglo

    private ListaEnlazada<EntradaHash<T>>[] cubetas;    // cada cubeta es una lista enlazada
    private int tamano;                                  // cuántas claves hay guardadas

    @SuppressWarnings("unchecked")
    public TablaHash() {
        cubetas = new ListaEnlazada[CAPACIDAD_INICIAL];
        for (int i = 0; i < cubetas.length; i++) {
            cubetas[i] = new ListaEnlazada<>();     // cada cubeta empieza como una lista vacía
        }
        tamano = 0;
    }

    /**
     * Función hash: convierte una clave de texto en un número de cubeta.
     * Va letra por letra mezclando su código en un número (multiplica por 31 y suma),
     * y al final usa el resto de dividir entre el número de cubetas para que
     * el resultado siempre caiga dentro del arreglo (de 0 a 30).
     */
    private int posicion(String clave) {
        int suma = 0;
        for (int i = 0; i < clave.length(); i++) {
            suma = suma * 31 + clave.charAt(i);     // mezcla cada letra en el número
        }
        return Math.abs(suma % cubetas.length);     // abs porque el número puede salir negativo
    }

    /** Busca la entrada de una clave dentro de su cubeta. */
    private EntradaHash<T> buscarEntrada(String clave) {
        // 1. la función hash nos dice en qué cubeta buscar (no revisamos las demás)
        Nodo<EntradaHash<T>> actual = cubetas[posicion(clave)].getCabeza();
        // 2. dentro de esa cubeta recorremos la lista hasta hallar la clave exacta
        while (actual != null) {
            if (actual.getDato().getClave().equals(clave)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;                                // la clave no está
    }

    /** Guarda un valor con su clave. Si la clave ya existía, cambia el valor. */
    public void poner(String clave, T valor) {
        EntradaHash<T> existente = buscarEntrada(clave);
        if (existente != null) {
            existente.setValor(valor);              // la clave ya estaba: solo cambiamos su valor
            return;
        }
        // clave nueva: se agrega al final de la lista de su cubeta
        cubetas[posicion(clave)].agregar(new EntradaHash<>(clave, valor));
        tamano++;
    }

    /** Devuelve el valor de una clave, o null si no está. */
    public T obtener(String clave) {
        EntradaHash<T> entrada = buscarEntrada(clave);
        return entrada == null ? null : entrada.getValor();
    }

    public boolean contiene(String clave) {
        return buscarEntrada(clave) != null;
    }

    /** Quita una clave de la tabla. Devuelve true si estaba. */
    public boolean eliminar(String clave) {
        EntradaHash<T> entrada = buscarEntrada(clave);
        if (entrada == null) {
            return false;                           // no estaba, no hay nada que quitar
        }
        cubetas[posicion(clave)].eliminar(entrada); // se quita de la lista de su cubeta
        tamano--;
        return true;
    }

    public int tamano() {
        return tamano;
    }
}
