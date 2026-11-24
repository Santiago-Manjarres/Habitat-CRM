package estructuras;

/**
 * Una pareja clave-valor guardada dentro de la tabla hash.
 * Ejemplo: clave "prop_001" -> valor: la propiedad con ese id.
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
