package json;

import estructuras.EntradaHash;
import estructuras.ListaEnlazada;
import estructuras.Nodo;

/**
 * Un objeto JSON: { "clave": valor, ... }.
 *
 * Guarda sus parejas clave-valor en una lista enlazada para conservar el
 * orden en que se escribieron (así los archivos .json quedan legibles).
 * Los valores pueden ser: String, Double, Boolean, null, otro ObjetoJson
 * o una ListaEnlazada (un arreglo JSON).
 */
public class ObjetoJson {

    // Los campos viven en una lista de parejas clave-valor (en el orden en que se agregaron)
    private final ListaEnlazada<EntradaHash<Object>> campos = new ListaEnlazada<>();

    /** Recorre la lista de campos buscando una clave. Devuelve null si no existe. */
    private EntradaHash<Object> buscar(String clave) {
        Nodo<EntradaHash<Object>> actual = campos.getCabeza();
        while (actual != null) {
            if (actual.getDato().getClave().equals(clave)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /** Guarda un valor. Si la clave ya existe, lo reemplaza. Devuelve el mismo objeto. */
    public ObjetoJson poner(String clave, Object valor) {
        EntradaHash<Object> existente = buscar(clave);
        if (existente != null) {
            existente.setValor(valor);                          // ya existía: cambiamos el valor
        } else {
            campos.agregar(new EntradaHash<>(clave, valor));    // no existía: lo agregamos al final
        }
        return this;                                            // devolver "this" permite encadenar: .poner(...).poner(...)
    }

    /** Devuelve el valor de un campo (o null si no existe). */
    public Object obtener(String clave) {
        EntradaHash<Object> entrada = buscar(clave);
        return entrada == null ? null : entrada.getValor();
    }

    /** Dice si el objeto tiene ese campo. */
    public boolean tiene(String clave) {
        return buscar(clave) != null;
    }

    /** Lee un campo como texto (sin espacios a los lados). Si no existe, devuelve "". */
    public String texto(String clave) {
        Object valor = obtener(clave);
        if (valor == null) {
            return "";
        }
        if (valor instanceof Double) {
            return EscritorJson.numeroATexto((Double) valor);
        }
        return valor.toString().trim();
    }

    /** Lee un campo numérico. Si no es un número, devuelve NaN. */
    public double numero(String clave) {
        Object valor = obtener(clave);
        // Number incluye Double, Integer y Long (los tres tipos de número que guardamos)
        return valor instanceof Number ? ((Number) valor).doubleValue() : Double.NaN;
    }

    /** Devuelve la lista de campos, para recorrerlos (lo usa EscritorJson). */
    public ListaEnlazada<EntradaHash<Object>> getCampos() {
        return campos;
    }

    public int tamano() {
        return campos.tamano();
    }
}
