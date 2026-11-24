package servicios;

/**
 * Lee números escritos como en Colombia: "1.350.000.000" o "115,5".
 * El punto separa miles y la coma separa decimales.
 */
public class Numeros {

    private Numeros() {
    }

    /** Convierte un valor del formulario o del JSON en número. Si no se puede, devuelve NaN. */
    public static double leer(Object valor) {
        if (valor instanceof Double) {
            return (Double) valor;
        }
        if (valor == null) {
            return Double.NaN;
        }
        // Dejamos solo dígitos y el signo menos; la coma decimal se cambia por punto
        // (que es lo que entiende Java). Los puntos de miles y los símbolos ($, espacios) se descartan.
        StringBuilder limpio = new StringBuilder();
        String texto = valor.toString().trim();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isDigit(c) || c == '-') {
                limpio.append(c);
            } else if (c == ',') {
                limpio.append('.');
            }
        }
        if (limpio.length() == 0 || limpio.toString().equals("-")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(limpio.toString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** true si el número es entero y no es negativo (0, 1, 2...). */
    public static boolean esEnteroNoNegativo(double numero) {
        return !Double.isNaN(numero) && numero >= 0 && numero == Math.rint(numero);
    }
}
