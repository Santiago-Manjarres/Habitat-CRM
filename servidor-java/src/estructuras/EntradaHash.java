package estructuras;

/**
 * Una pareja clave-valor guardada dentro de la tabla hash.
 */
public class EntradaHash<T> {

    private String clave;
    private T valor;

    public EntradaHash(String clave, T valor) {
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
}
