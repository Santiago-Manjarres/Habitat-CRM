package estructuras;

/**
 * Árbol binario de búsqueda con claves de texto.
 *
 * Es un árbol donde cada nodo tiene máximo dos hijos.
 *
 * Regla del árbol: en cada nodo, las claves menores quedan a la izquierda y
 * las mayores a la derecha.
 *
 *            [M]
 *           /   \
 *         [C]   [T]      <- C < M, T > M
 *         / \
 *       [A] [F] Por eso, al recorrerlo "en orden" (izquierda,
 * nodo, derecha) los valores salen ordenados por su clave.
 *
 * En el CRM se usa para ordenar: las propiedades por fecha de creación, los
 * clientes por fecha de su último cambio y los asesores por nombre.
 */
public class ArbolBinarioBusqueda<T> {

    private NodoArbol<T> raiz;   // el nodo de arriba del árbol (null si está vacío)
    private int tamano;          // cuántos nodos tiene el árbol

    /** Inserta un valor con su clave. Si la clave ya existe, reemplaza el valor. */
    public void insertar(String clave, T valor) {
        raiz = insertar(raiz, clave, valor);   // empezamos a buscar el lugar desde la raíz
    }

    // Método recursivo: se llama a sí mismo bajando por el árbol hasta hallar el lugar vacío.
    private NodoArbol<T> insertar(NodoArbol<T> nodo, String clave, T valor) {
        if (nodo == null) {                       // llegamos a un lugar vacío: aquí va el nodo nuevo
            tamano++;
            return new NodoArbol<>(clave, valor);
        }
        int comparacion = clave.compareTo(nodo.getClave());   // negativo = menor, positivo = mayor, 0 = igual
        if (comparacion < 0) {
            nodo.setIzquierdo(insertar(nodo.getIzquierdo(), clave, valor));  // menor: vamos a la izquierda
        } else if (comparacion > 0) {
            nodo.setDerecho(insertar(nodo.getDerecho(), clave, valor));      // mayor: vamos a la derecha
        } else {
            nodo.setValor(valor);                 // la clave ya existía: solo cambiamos el valor
        }
        return nodo;
    }

    /** Busca el valor de una clave. Devuelve null si no está. */
    public T buscar(String clave) {
        NodoArbol<T> actual = raiz;                 // empezamos en la raíz
        while (actual != null) {
            int comparacion = clave.compareTo(actual.getClave());
            if (comparacion == 0) {
                return actual.getValor();           // la encontramos
            }
            // si es menor buscamos a la izquierda; si es mayor, a la derecha
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        return null;                                // llegamos al final y no estaba
    }

    /** Devuelve todos los valores ordenados de menor a mayor clave (recorrido en orden). */
    public ListaEnlazada<T> recorrerEnOrden() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        recorrerEnOrden(raiz, resultado);           // llena la lista de menor a mayor
        return resultado;
    }

    private void recorrerEnOrden(NodoArbol<T> nodo, ListaEnlazada<T> resultado) {
        if (nodo == null) {
            return;                                 // no hay nada que recorrer aquí
        }
        recorrerEnOrden(nodo.getIzquierdo(), resultado);   // 1. primero todo lo menor (izquierda)
        resultado.agregar(nodo.getValor());                // 2. luego el nodo actual
        recorrerEnOrden(nodo.getDerecho(), resultado);     // 3. al final todo lo mayor (derecha)
    }

    /** Altura del árbol: cuántos niveles tiene (un árbol vacío mide 0). */
    public int altura() {
        return altura(raiz);
    }

    private int altura(NodoArbol<T> nodo) {
        if (nodo == null) {
            return 0;                               // sin nodo no hay niveles
        }
        // este nodo cuenta 1 nivel, más el lado más profundo de sus dos hijos
        return 1 + Math.max(altura(nodo.getIzquierdo()), altura(nodo.getDerecho()));
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }
}
