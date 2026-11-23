package estructuras;

/**
 * Árbol binario de búsqueda con claves de texto.
 *
 * Regla del árbol: en cada nodo, las claves menores quedan a la izquierda y
 * las mayores a la derecha. Por eso, al recorrerlo "en orden" (izquierda,
 * nodo, derecha) los valores salen ordenados por su clave.
 *
 * En el CRM se usa para ordenar: las propiedades por fecha de creación, los
 * clientes por fecha de su último cambio y los asesores por nombre.
 */
public class ArbolBinarioBusqueda<T> {

    private NodoArbol<T> raiz;
    private int tamano;

    /** Inserta un valor con su clave. Si la clave ya existe, reemplaza el valor. */
    public void insertar(String clave, T valor) {
        raiz = insertar(raiz, clave, valor);
    }

    private NodoArbol<T> insertar(NodoArbol<T> nodo, String clave, T valor) {
        if (nodo == null) {
            tamano++;
            return new NodoArbol<>(clave, valor);
        }
        int comparacion = clave.compareTo(nodo.getClave());
        if (comparacion < 0) {
            nodo.setIzquierdo(insertar(nodo.getIzquierdo(), clave, valor));
        } else if (comparacion > 0) {
            nodo.setDerecho(insertar(nodo.getDerecho(), clave, valor));
        } else {
            nodo.setValor(valor);
        }
        return nodo;
    }

    /** Busca el valor de una clave. Devuelve null si no está. */
    public T buscar(String clave) {
        NodoArbol<T> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.getClave());
            if (comparacion == 0) {
                return actual.getValor();
            }
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        return null;
    }

    /** Devuelve todos los valores ordenados de menor a mayor clave (recorrido en orden). */
    public ListaEnlazada<T> recorrerEnOrden() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        recorrerEnOrden(raiz, resultado);
        return resultado;
    }

    private void recorrerEnOrden(NodoArbol<T> nodo, ListaEnlazada<T> resultado) {
        if (nodo == null) {
            return;
        }
        recorrerEnOrden(nodo.getIzquierdo(), resultado);
        resultado.agregar(nodo.getValor());
        recorrerEnOrden(nodo.getDerecho(), resultado);
    }

    /** Altura del árbol: cuántos niveles tiene (un árbol vacío mide 0). */
    public int altura() {
        return altura(raiz);
    }

    private int altura(NodoArbol<T> nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + Math.max(altura(nodo.getIzquierdo()), altura(nodo.getDerecho()));
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }
}
