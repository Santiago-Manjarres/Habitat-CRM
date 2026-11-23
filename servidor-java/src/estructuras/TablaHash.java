package estructuras;

/**
 * Tabla hash con claves de texto.
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

    private static final int CAPACIDAD_INICIAL = 31;

    private ListaEnlazada<EntradaHash<T>>[] cubetas;
    private int tamano;

    @SuppressWarnings("unchecked")
    public TablaHash() {
        cubetas = new ListaEnlazada[CAPACIDAD_INICIAL];
        for (int i = 0; i < cubetas.length; i++) {
            cubetas[i] = new ListaEnlazada<>();
        }
        tamano = 0;
    }

    /** Función hash: suma los caracteres de la clave y la ajusta al tamaño del arreglo. */
    private int posicion(String clave) {
        int suma = 0;
        for (int i = 0; i < clave.length(); i++) {
            suma = suma * 31 + clave.charAt(i);
        }
        return Math.abs(suma % cubetas.length);
    }

    /** Busca la entrada de una clave dentro de su cubeta. */
    private EntradaHash<T> buscarEntrada(String clave) {
        Nodo<EntradaHash<T>> actual = cubetas[posicion(clave)].getCabeza();
        while (actual != null) {
            if (actual.getDato().getClave().equals(clave)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /** Guarda un valor con su clave. Si la clave ya existía, cambia el valor. */
    public void poner(String clave, T valor) {
        EntradaHash<T> existente = buscarEntrada(clave);
        if (existente != null) {
            existente.setValor(valor);
            return;
        }
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
            return false;
        }
        cubetas[posicion(clave)].eliminar(entrada);
        tamano--;
        return true;
    }

    public int tamano() {
        return tamano;
    }
}
