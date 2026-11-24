package estructuras;

/**
 * Nodo de un árbol binario de búsqueda.
 * Guarda una clave (con la que se ordena), un valor y sus dos hijos:
 *
 *            [clave]
 *            /     \
 *     izquierdo   derecho
 *   (claves menores) (claves mayores)
 */
public class NodoArbol<T> {

    private String clave;
    private T valor;
    private NodoArbol<T> izquierdo;
    private NodoArbol<T> derecho;

    public NodoArbol(String clave, T valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() {
        return clave;
    }

    public T getValor() {
        return valor;
    }

    public void setValor(T valor) {
        this.valor = valor;
    }

    public NodoArbol<T> getIzquierdo() {
        return izquierdo;
    }

    public void setIzquierdo(NodoArbol<T> izquierdo) {
        this.izquierdo = izquierdo;
    }

    public NodoArbol<T> getDerecho() {
        return derecho;
    }

    public void setDerecho(NodoArbol<T> derecho) {
        this.derecho = derecho;
    }
}
