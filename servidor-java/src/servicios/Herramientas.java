package servicios;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import estructuras.Pila;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import json.ObjetoJson;

/**
 * Funciones pequeñas que usan varios servicios.
 */
public class Herramientas {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Herramientas() {
    }

    /** Fecha y hora actual, como en los archivos: 2026-11-20T14:30:00.000Z */
    public static String fechaActual() {
        return FORMATO_FECHA.format(Instant.now());
    }

    /** Id único con un prefijo, por ejemplo "prop_3f2a...". */
    public static String nuevoId(String prefijo) {
        return prefijo + "_" + UUID.randomUUID();
    }

    /** Quita tildes, pasa a minúsculas y quita espacios a los lados: "Bogotá " -> "bogota". */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD);
        return sinTildes.replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    /** true si el valor está dentro del arreglo. */
    public static boolean estaEn(String valor, String[] opciones) {
        for (String opcion : opciones) {
            if (opcion.equals(valor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve una lista al revés usando una pila: se apilan todos los
     * elementos y al desapilarlos salen en orden contrario.
     */
    public static <T> ListaEnlazada<T> invertir(ListaEnlazada<T> lista) {
        Pila<T> pila = new Pila<>();
        for (Nodo<T> n = lista.getCabeza(); n != null; n = n.getSiguiente()) {
            pila.apilar(n.getDato());
        }
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        while (!pila.estaVacia()) {
            resultado.agregar(pila.desapilar());
        }
        return resultado;
    }

    /* ---------- Respuestas que se le envían al navegador ---------- */

    public static ObjetoJson exito(String clave, Object valor) {
        return new ObjetoJson().poner("ok", true).poner(clave, valor);
    }

    public static ObjetoJson fallo(String mensaje) {
        return new ObjetoJson().poner("ok", false).poner("error", mensaje);
    }

    public static ObjetoJson conErrores(ObjetoJson errores) {
        return new ObjetoJson().poner("ok", false).poner("errors", errores);
    }
}
