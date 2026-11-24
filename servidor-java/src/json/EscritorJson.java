package json;

import estructuras.EntradaHash;
import estructuras.ListaEnlazada;
import estructuras.Nodo;

/**
 * Convierte objetos de Java en texto JSON.
 * Con "bonito" en true agrega saltos de línea y sangría (para los archivos .json).
 */
public class EscritorJson {

    private EscritorJson() {
    }

    /** Convierte cualquier valor (texto, número, objeto, lista...) en texto JSON. */
    public static String escribir(Object valor, boolean bonito) {
        StringBuilder salida = new StringBuilder();
        escribirValor(valor, salida, bonito, 0);
        return salida.toString();
    }

    @SuppressWarnings("unchecked")
    // Revisa de qué tipo es el valor y usa la forma correcta de escribirlo.
    // "nivel" cuenta cuántos objetos o listas hay dentro de otros (sirve para la sangría).
    private static void escribirValor(Object valor, StringBuilder salida, boolean bonito, int nivel) {
        if (valor == null) {
            salida.append("null");
        } else if (valor instanceof String) {
            escribirTexto((String) valor, salida);
        } else if (valor instanceof Double) {
            salida.append(numeroATexto((Double) valor));
        } else if (valor instanceof Integer || valor instanceof Long) {
            salida.append(valor);
        } else if (valor instanceof Boolean) {
            salida.append(valor);
        } else if (valor instanceof ObjetoJson) {
            escribirObjeto((ObjetoJson) valor, salida, bonito, nivel);
        } else if (valor instanceof ListaEnlazada) {
            escribirLista((ListaEnlazada<Object>) valor, salida, bonito, nivel);
        } else {
            escribirTexto(valor.toString(), salida);
        }
    }

    private static void escribirObjeto(ObjetoJson objeto, StringBuilder salida, boolean bonito, int nivel) {
        if (objeto.tamano() == 0) {
            salida.append("{}");
            return;
        }
        salida.append('{');
        Nodo<EntradaHash<Object>> actual = objeto.getCampos().getCabeza();
        while (actual != null) {                                    // recorremos cada campo
            saltoDeLinea(salida, bonito, nivel + 1);
            escribirTexto(actual.getDato().getClave(), salida);     // "clave"
            salida.append(bonito ? ": " : ":");                     // :
            escribirValor(actual.getDato().getValor(), salida, bonito, nivel + 1);  // valor
            actual = actual.getSiguiente();
            if (actual != null) {
                salida.append(',');                                 // coma entre campos (no después del último)
            }
        }
        saltoDeLinea(salida, bonito, nivel);
        salida.append('}');
    }

    private static void escribirLista(ListaEnlazada<Object> lista, StringBuilder salida, boolean bonito, int nivel) {
        if (lista.estaVacia()) {
            salida.append("[]");
            return;
        }
        salida.append('[');
        Nodo<Object> actual = lista.getCabeza();
        while (actual != null) {
            saltoDeLinea(salida, bonito, nivel + 1);
            escribirValor(actual.getDato(), salida, bonito, nivel + 1);
            actual = actual.getSiguiente();
            if (actual != null) {
                salida.append(',');
            }
        }
        saltoDeLinea(salida, bonito, nivel);
        salida.append(']');
    }

    /** En modo "bonito": pone un salto de línea y 2 espacios por cada nivel de profundidad. */
    private static void saltoDeLinea(StringBuilder salida, boolean bonito, int nivel) {
        if (!bonito) {
            return;
        }
        salida.append('\n');
        for (int i = 0; i < nivel; i++) {
            salida.append("  ");
        }
    }

    /** Escribe un texto entre comillas, escapando los caracteres especiales. */
    private static void escribirTexto(String texto, StringBuilder salida) {
        salida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"': salida.append("\\\""); break;
                case '\\': salida.append("\\\\"); break;
                case '\n': salida.append("\\n"); break;
                case '\r': salida.append("\\r"); break;
                case '\t': salida.append("\\t"); break;
                default:
                    if (c < 32) {
                        salida.append(String.format("\\u%04x", (int) c));
                    } else {
                        salida.append(c);
                    }
            }
        }
        salida.append('"');
    }

    /** Escribe 450000000 en vez de 4.5E8, y 115.5 tal cual. */
    public static String numeroATexto(double numero) {
        if (Double.isNaN(numero) || Double.isInfinite(numero)) {
            return "null";
        }
        if (numero == Math.rint(numero) && Math.abs(numero) < 1e15) {
            return String.valueOf((long) numero);   // es un entero: se escribe sin decimales
        }
        return String.valueOf(numero);
    }
}
