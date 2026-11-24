package vistas;

/**
 * Piezas pequeñas de HTML que se repiten en todas las páginas:
 * íconos, etiquetas de estado, imágenes y campos de formulario.
 * Cada método devuelve un texto con HTML listo para pegar en la página.
 */
public class Html {

    private Html() {
    }

    /**
     * Limpia un texto antes de meterlo en el HTML. Cambia los caracteres especiales
     * (< > & " ') por su versión segura, así si alguien escribe código en un nombre,
     * se muestra como texto y no se ejecuta.
     */
    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '<': resultado.append("&lt;"); break;
                case '>': resultado.append("&gt;"); break;
                case '&': resultado.append("&amp;"); break;
                case '"': resultado.append("&quot;"); break;
                case '\'': resultado.append("&#39;"); break;
                default: resultado.append(c);
            }
        }
        return resultado.toString();
    }

    /** Un ícono del archivo assets/icons/sprite.svg, por ejemplo icono("plus", ""). */
    public static String icono(String nombre, String claseExtra) {
        return "<svg class=\"icon " + claseExtra + "\" aria-hidden=\"true\" focusable=\"false\">"
                + "<use href=\"/assets/icons/sprite.svg#i-" + nombre + "\"></use></svg>";
    }

    /** Etiqueta de estado de color. El texto siempre se ve; el color solo lo refuerza. */
    public static String insignia(String texto, String tono) {
        return "<span class=\"badge badge--" + tono + "\">" + escapar(texto) + "</span>";
    }

    /** Foto de una propiedad. Si no tiene enlace, se usa una imagen de relleno del proyecto. */
    public static String imagen(String url, String clase, int ancho, int alto) {
        String fuente = url == null || url.isEmpty() ? "/assets/img/property-placeholder.svg" : url;
        return "<img src=\"" + escapar(fuente) + "\" alt=\"\" width=\"" + ancho + "\" height=\"" + alto
                + "\" class=\"" + clase + "\" loading=\"lazy\" decoding=\"async\">";
    }

    /** Las opciones de una lista desplegable. "seleccionado" es la que aparece elegida. */
    public static String opciones(String[] valores, String[] textos, String seleccionado) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            resultado.append("<option value=\"").append(escapar(valores[i])).append('"')
                    .append(valores[i].equals(seleccionado) ? " selected" : "")
                    .append('>').append(escapar(textos[i])).append("</option>");
        }
        return resultado.toString();
    }

    /** El mensaje rojo que va debajo de un campo. Si no hay error, queda vacío (y el CSS lo oculta). */
    public static String error(String id, String mensaje) {
        String contenido = mensaje == null || mensaje.isEmpty()
                ? ""
                : icono("alert-circle", "icon--sm") + "<span>" + escapar(mensaje) + "</span>";
        return "<span class=\"field__error\" id=\"" + id + "-error\">" + contenido + "</span>";
    }

    /** aria-invalid="true" hace que el CSS pinte el campo de rojo. */
    private static String invalido(String mensaje) {
        return mensaje == null || mensaje.isEmpty() ? "" : " aria-invalid=\"true\"";
    }

    /**
     * Un campo de texto completo: etiqueta + caja + ayuda (opcional) + error.
     * @param id        identificador único del campo en la página
     * @param nombre    el "name": la clave con la que llega a Java (ej. "title")
     * @param etiqueta  el texto que se ve arriba del campo (puede tener HTML)
     * @param valor     lo que ya está escrito
     * @param tipo      text, email, tel, url...
     * @param atributos atributos extra (maxlength, inputmode...)
     * @param ayuda     texto gris de ayuda, o "" si no hay
     * @param error     mensaje de error, o "" si no hay
     * @param completo  true = ocupa las dos columnas del formulario
     */
    public static String campo(String id, String nombre, String etiqueta, String valor, String tipo,
                               String atributos, String ayuda, String error, boolean completo) {
        String descripcion = (ayuda.isEmpty() ? "" : id + "-help ") + id + "-error";
        return "<div class=\"field" + (completo ? " is-full" : "") + "\">"
                + "<label class=\"field__label\" for=\"" + id + "\">" + etiqueta + "</label>"
                + "<input class=\"field__control\" id=\"" + id + "\" name=\"" + nombre + "\" type=\"" + tipo
                + "\" value=\"" + escapar(valor) + "\" aria-describedby=\"" + descripcion + "\"" + invalido(error) + " " + atributos + ">"
                + (ayuda.isEmpty() ? "" : "<span class=\"field__help\" id=\"" + id + "-help\">" + ayuda + "</span>")
                + error(id, error)
                + "</div>";
    }

    /** Un campo de dinero: igual que campo(), pero con el símbolo $ dentro de la caja. */
    public static String campoDinero(String id, String nombre, String etiqueta, String valor, String ayuda, String error) {
        String descripcion = (ayuda.isEmpty() ? "" : id + "-help ") + id + "-error";
        return "<div class=\"field\">"
                + "<label class=\"field__label\" for=\"" + id + "\">" + etiqueta + "</label>"
                + "<div class=\"field__affix field__affix--prefix\"><span class=\"field__prefix\">$</span>"
                + "<input class=\"field__control\" id=\"" + id + "\" name=\"" + nombre + "\" type=\"text\" inputmode=\"numeric\" autocomplete=\"off\""
                + " value=\"" + escapar(valor) + "\" aria-describedby=\"" + descripcion + "\"" + invalido(error) + "></div>"
                + (ayuda.isEmpty() ? "" : "<span class=\"field__help\" id=\"" + id + "-help\">" + ayuda + "</span>")
                + error(id, error)
                + "</div>";
    }

    /** Un área de texto de varias líneas (descripción, notas). Siempre ocupa las dos columnas. */
    public static String areaDeTexto(String id, String nombre, String etiqueta, String valor, int maximo, String error) {
        return "<div class=\"field is-full\">"
                + "<label class=\"field__label\" for=\"" + id + "\">" + etiqueta + "</label>"
                + "<textarea class=\"field__control\" id=\"" + id + "\" name=\"" + nombre + "\" maxlength=\"" + maximo
                + "\" aria-describedby=\"" + id + "-error\"" + invalido(error) + ">" + escapar(valor) + "</textarea>"
                + error(id, error)
                + "</div>";
    }

    /** Una lista desplegable completa: etiqueta + lista + error. */
    public static String lista(String id, String nombre, String etiqueta, String[] valores, String[] textos,
                               String seleccionado, String error, boolean completo) {
        return "<div class=\"field" + (completo ? " is-full" : "") + "\">"
                + "<label class=\"field__label\" for=\"" + id + "\">" + etiqueta + "</label>"
                + "<div class=\"field__select\"><select class=\"field__control\" id=\"" + id + "\" name=\"" + nombre
                + "\" aria-describedby=\"" + id + "-error\"" + invalido(error) + ">" + opciones(valores, textos, seleccionado) + "</select></div>"
                + error(id, error)
                + "</div>";
    }

    /** Un estado vacío: ícono, título, texto y un botón (cuando no hay nada que mostrar). */
    public static String vacio(String icono, String titulo, String texto, String accion) {
        return "<div class=\"card empty-state\">" + icono(icono, "empty-state__icon")
                + "<h2 class=\"empty-state__title\">" + escapar(titulo) + "</h2>"
                + "<p class=\"empty-state__text\">" + escapar(texto) + "</p>" + accion + "</div>";
    }
}
