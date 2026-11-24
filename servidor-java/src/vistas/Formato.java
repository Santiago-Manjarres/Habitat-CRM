package vistas;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Convierte números y fechas en texto fácil de leer.
 *   450000000            -> "$ 450.000.000"
 *   4870000000           -> "$ 4.870 M"
 *   "2026-11-20T14:30Z"  -> "hace 2 horas"
 *   "Laura Mendoza"      -> "LM"
 */
public class Formato {

    private Formato() {
    }

    /** 1234567 -> "1.234.567" (pone un punto cada tres cifras, contando desde la derecha). */
    public static String numero(double valor) {
        if (Double.isNaN(valor)) {
            return "—";
        }
        long entero = Math.round(valor);
        String digitos = String.valueOf(Math.abs(entero));
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            int quedan = digitos.length() - i;          // cuántas cifras faltan por escribir
            if (i > 0 && quedan % 3 == 0) {
                resultado.append('.');
            }
            resultado.append(digitos.charAt(i));
        }
        return (entero < 0 ? "-" : "") + resultado;
    }

    /** 450000000 -> "$ 450.000.000" */
    public static String dinero(double valor) {
        return Double.isNaN(valor) ? "—" : "$ " + numero(valor);
    }

    /** Dinero abreviado para los indicadores: 4870000000 -> "$ 4.870 M" (millones). */
    public static String dineroCorto(double valor) {
        if (Double.isNaN(valor)) {
            return "—";
        }
        if (Math.abs(valor) < 1000000) {
            return dinero(valor);
        }
        return "$ " + numero(Math.round(valor / 1000000)) + " M";
    }

    /** 115.5 -> "115,5". Se usa la coma decimal porque el punto es el separador de miles. */
    public static String decimal(double valor) {
        if (Double.isNaN(valor)) {
            return "";
        }
        if (valor == Math.rint(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor).replace('.', ',');
    }

    /** (12, "propiedad", "propiedades") -> "12 propiedades" */
    public static String plural(double cantidad, String singular, String plural) {
        return numero(cantidad) + " " + (cantidad == 1 ? singular : plural);
    }

    /** "Laura Mendoza" -> "LM": la primera letra de las dos primeras palabras. */
    public static String iniciales(String nombre) {
        String[] palabras = nombre == null ? new String[0] : nombre.trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < palabras.length && resultado.length() < 2; i++) {
            if (!palabras[i].isEmpty()) {
                resultado.append(Character.toUpperCase(palabras[i].charAt(0)));
            }
        }
        return resultado.length() == 0 ? "?" : resultado.toString();
    }

    /** Fecha guardada en el .json -> "justo ahora", "hace 5 minutos", "ayer", "hace 3 días"... */
    public static String tiempoRelativo(String fecha) {
        long segundos;
        try {
            segundos = Duration.between(Instant.parse(fecha), Instant.now()).getSeconds();
        } catch (Exception e) {
            return "—";
        }
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;
        if (segundos < 45) return "justo ahora";
        if (minutos < 60) return "hace " + plural(Math.max(minutos, 1), "minuto", "minutos");
        if (horas < 24) return "hace " + plural(horas, "hora", "horas");
        if (dias == 1) return "ayer";
        if (dias < 7) return "hace " + dias + " días";
        if (dias < 30) return "hace " + plural(dias / 7, "semana", "semanas");
        if (dias < 365) return "hace " + plural(dias / 30, "mes", "meses");
        return "hace " + plural(dias / 365, "año", "años");
    }

    /** La fecha de hoy para la barra superior: "Martes, 24 de noviembre". */
    public static String fechaDeHoy() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "CO"));
        String texto = LocalDate.now().format(formato);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
