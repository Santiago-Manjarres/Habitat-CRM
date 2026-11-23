package json;

import estructuras.ListaEnlazada;

/**
 * Convierte un texto JSON en objetos de Java.
 *
 * Lee el texto carácter por carácter:
 *   {   -> ObjetoJson
 *   [   -> ListaEnlazada
 *   "   -> String
 *   número -> Double
 *   true / false -> Boolean
 *   null -> null
 */
public class LectorJson {

    private final String texto;
    private int posicion;

    private LectorJson(String texto) {
        this.texto = texto;
        this.posicion = 0;
    }

    /** Punto de entrada: lee un texto JSON completo. */
    public static Object leer(String texto) {
        LectorJson lector = new LectorJson(texto);
        Object valor = lector.leerValor();
        lector.saltarEspacios();
        if (lector.posicion < texto.length()) {
            throw lector.error("Hay texto de más al final");
        }
        return valor;
    }

    private Object leerValor() {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw error("El texto terminó antes de tiempo");
        }
        char c = texto.charAt(posicion);
        if (c == '{') return leerObjeto();
        if (c == '[') return leerArreglo();
        if (c == '"') return leerTexto();
        if (texto.startsWith("true", posicion)) { posicion += 4; return Boolean.TRUE; }
        if (texto.startsWith("false", posicion)) { posicion += 5; return Boolean.FALSE; }
        if (texto.startsWith("null", posicion)) { posicion += 4; return null; }
        return leerNumero();
    }

    private ObjetoJson leerObjeto() {
        ObjetoJson objeto = new ObjetoJson();
        posicion++; // salta {
        saltarEspacios();
        if (siguienteEs('}')) return objeto;
        while (true) {
            saltarEspacios();
            String clave = leerTexto();
            saltarEspacios();
            esperar(':');
            objeto.poner(clave, leerValor());
            saltarEspacios();
            if (siguienteEs('}')) return objeto;
            esperar(',');
        }
    }

    private ListaEnlazada<Object> leerArreglo() {
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        posicion++; // salta [
        saltarEspacios();
        if (siguienteEs(']')) return lista;
        while (true) {
            lista.agregar(leerValor());
            saltarEspacios();
            if (siguienteEs(']')) return lista;
            esperar(',');
        }
    }

    private String leerTexto() {
        esperar('"');
        StringBuilder resultado = new StringBuilder();
        while (posicion < texto.length()) {
            char c = texto.charAt(posicion++);
            if (c == '"') {
                return resultado.toString();
            }
            if (c == '\\') {
                resultado.append(leerEscape());
            } else {
                resultado.append(c);
            }
        }
        throw error("Falta cerrar un texto con comillas");
    }

    /** Traduce las secuencias de escape: \n, \t, \", \\, á, etc. */
    private char leerEscape() {
        char c = texto.charAt(posicion++);
        switch (c) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            case 'b': return '\b';
            case 'f': return '\f';
            case 'u':
                char unicode = (char) Integer.parseInt(texto.substring(posicion, posicion + 4), 16);
                posicion += 4;
                return unicode;
            default: return c; // \" \\ \/
        }
    }

    private Double leerNumero() {
        int inicio = posicion;
        while (posicion < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(posicion)) >= 0) {
            posicion++;
        }
        if (inicio == posicion) {
            throw error("Se esperaba un valor");
        }
        return Double.parseDouble(texto.substring(inicio, posicion));
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    /** Si el siguiente carácter es c, lo consume y devuelve true. */
    private boolean siguienteEs(char c) {
        if (posicion < texto.length() && texto.charAt(posicion) == c) {
            posicion++;
            return true;
        }
        return false;
    }

    private void esperar(char c) {
        if (!siguienteEs(c)) {
            throw error("Se esperaba '" + c + "'");
        }
    }

    private IllegalArgumentException error(String mensaje) {
        return new IllegalArgumentException("JSON no válido en la posición " + posicion + ": " + mensaje);
    }
}
