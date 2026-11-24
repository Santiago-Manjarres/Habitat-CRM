package vistas;

import servicios.Catalogos;

/**
 * Traduce los valores que se guardan en los .json a los textos que ve el asesor.
 * Ejemplo: "closed_won" -> "Negocio cerrado", "available" -> "Disponible".
 *
 * Cada arreglo va en el MISMO ORDEN que su lista en Catalogos.java:
 * la posición 0 de uno corresponde a la posición 0 del otro.
 */
public class Etiquetas {

    private Etiquetas() {
    }

    public static final String[] OPERACIONES = {"Venta", "Arriendo"};

    public static final String[] ESTADOS_PROPIEDAD = {"Disponible", "En negociación", "Vendida"};
    public static final String[] TONOS_PROPIEDAD = {"success", "warning", "danger"};     // verde, ámbar, rojo

    public static final String[] ETAPAS = {"Nuevo", "Contactado", "Visita", "Negociación", "Negocio cerrado", "Descartado"};
    public static final String[] TONOS_ETAPA = {"info", "info", "warning", "warning", "success", "neutral"};

    public static final String[] ORIGENES = {"Portal web", "Llamada", "WhatsApp", "Visita a la oficina", "Referido"};

    /** Busca un valor en su lista y devuelve el texto que está en la misma posición. */
    public static String buscar(String valor, String[] valores, String[] textos) {
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equals(valor)) {
                return textos[i];
            }
        }
        return "—";
    }

    public static String operacion(String valor) {
        return buscar(valor, Catalogos.OPERACIONES, OPERACIONES);
    }

    public static String estadoPropiedad(String valor) {
        return buscar(valor, Catalogos.ESTADOS_PROPIEDAD, ESTADOS_PROPIEDAD);
    }

    public static String tonoPropiedad(String valor) {
        return buscar(valor, Catalogos.ESTADOS_PROPIEDAD, TONOS_PROPIEDAD);
    }

    public static String etapa(String valor) {
        return buscar(valor, Catalogos.ETAPAS, ETAPAS);
    }

    public static String tonoEtapa(String valor) {
        return buscar(valor, Catalogos.ETAPAS, TONOS_ETAPA);
    }

    public static String origen(String valor) {
        return buscar(valor, Catalogos.ORIGENES, ORIGENES);
    }
}
