package vistas;

import datos.Datos;
import datos.Validador;

/**
 * Las piezas de HTML que comparten todas las páginas: la cabecera con los CSS,
 * el marco del panel (menú y barra superior) y los campos de formulario.
 */
public class Plantilla {

    /** El <head>: título de la pestaña y los archivos CSS (el orden importa: primero las variables). */
    public static String cabeza(String titulo) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titulo + " · Habitat CRM</title>"
                + "<link rel=\"icon\" href=\"/assets/img/logo.png\">"
                + "<link rel=\"stylesheet\" href=\"/css/variables.css\">"
                + "<link rel=\"stylesheet\" href=\"/css/base.css\">"
                + "<link rel=\"stylesheet\" href=\"/css/components.css\">"
                + "<link rel=\"stylesheet\" href=\"/css/layout.css\"></head>";
    }

    /**
     * Una página completa del panel del asesor.
     * @param activa "resumen", "propiedades" o "clientes" (la sección que se marca en el menú)
     */
    public static String pagina(String titulo, Datos usuario, String activa, String contenido) {
        String logo = "<img src=\"/assets/img/logo.png\" alt=\"\" width=\"28\" height=\"26\" class=\"brand__logo\"><span>Habitat CRM</span>";
        String menu = enlace("resumen", "grid", "Resumen", activa)
                + enlace("propiedades", "building", "Propiedades", activa)
                + enlace("clientes", "user", "Clientes", activa);
        return cabeza(titulo) + "<body data-view=\"admin\"><div class=\"view admin-layout\">"
                // Menú lateral (computador)
                + "<aside class=\"sidebar\"><a class=\"brand\" href=\"/resumen\">" + logo + "</a>"
                + "<nav class=\"sidebar__nav\">" + menu.replace("MENU", "nav-link") + "</nav>"
                + "<a class=\"nav-link sidebar__site\" href=\"/\">" + icono("home") + "Ver el sitio</a></aside>"
                + "<div class=\"admin-main has-bottom-nav\">"
                // Barra superior: el asesor y el botón de salir
                + "<header class=\"app-header topbar\"><div class=\"container app-header__inner\">"
                + "<a class=\"brand topbar__brand\" href=\"/resumen\">" + logo + "</a><span class=\"app-header__spacer\"></span>"
                + "<div class=\"user-chip\"><span class=\"avatar\">" + iniciales(usuario.get("nombre")) + "</span>"
                + "<span class=\"user-chip__text\"><span class=\"user-chip__name\">" + escapar(usuario.get("nombre")) + "</span>"
                + "<span class=\"user-chip__role\">Asesor</span></span></div>"
                + "<a class=\"btn btn--secondary logout-btn\" href=\"/salir\">" + icono("logout") + "<span class=\"logout-btn__label\">Cerrar sesión</span></a>"
                + "</div></header>"
                // El contenido de la sección
                + "<main class=\"container\">" + contenido + "</main>"
                // Menú inferior (celular)
                + "<nav class=\"bottom-nav\">" + menu.replace("MENU", "bottom-nav__link") + "</nav>"
                + "</div></div></body></html>";
    }

    /** Un enlace del menú. aria-current="page" marca la sección activa (el CSS la pinta de azul). */
    private static String enlace(String ruta, String icono, String texto, String activa) {
        return "<a class=\"MENU\" href=\"/" + ruta + "\"" + (ruta.equals(activa) ? " aria-current=\"page\"" : "") + ">"
                + icono(icono) + texto + "</a>";
    }

    /** El título de una sección, con un botón opcional a la derecha. */
    public static String seccion(String titulo, String subtitulo, String boton) {
        return "<header class=\"page__header\"><div><h1 class=\"page__title\">" + titulo + "</h1>"
                + "<p class=\"page__subtitle\">" + subtitulo + "</p></div>" + boton + "</header>";
    }

    /** Un campo de texto con su etiqueta y su mensaje de error. */
    public static String campo(String nombre, String etiqueta, String tipo, Datos valores, Datos errores) {
        String error = errores.get(nombre);
        return "<div class=\"field\"><label class=\"field__label\" for=\"" + nombre + "\">" + etiqueta + "</label>"
                + "<input class=\"field__control\" id=\"" + nombre + "\" name=\"" + nombre + "\" type=\"" + tipo + "\""
                + " value=\"" + escapar(valores.get(nombre)) + "\"" + (error.isEmpty() ? "" : " aria-invalid=\"true\"") + ">"
                + "<span class=\"field__error\">" + error + "</span></div>";
    }

    /** Una lista desplegable con sus opciones. */
    public static String lista(String nombre, String etiqueta, String[] opciones, Datos valores) {
        String html = "";
        for (String opcion : opciones) {
            html += "<option" + (opcion.equals(valores.get(nombre)) ? " selected" : "") + ">" + opcion + "</option>";
        }
        return "<div class=\"field\"><label class=\"field__label\" for=\"" + nombre + "\">" + etiqueta + "</label>"
                + "<div class=\"field__select\"><select class=\"field__control\" id=\"" + nombre + "\" name=\"" + nombre + "\">"
                + html + "</select></div></div>";
    }

    /** Etiqueta de color: verde, ámbar, rojo o azul según el texto. */
    public static String insignia(String texto) {
        String color;
        switch (texto) {
            case "Disponible": case "Cerrado": color = "success"; break;
            case "En negociación": case "Negociación": case "Visita": color = "warning"; break;
            case "Vendida": color = "danger"; break;
            default: color = "info";
        }
        return "<span class=\"badge badge--" + color + "\">" + escapar(texto) + "</span>";
    }

    /** "450000000" -> "$ 450.000.000" (pone un punto cada tres cifras). */
    public static String dinero(String precio) {
        String digitos = String.valueOf(Math.max(Validador.leerNumero(precio), 0));
        String resultado = "";
        for (int i = 0; i < digitos.length(); i++) {
            if (i > 0 && (digitos.length() - i) % 3 == 0) {
                resultado += ".";
            }
            resultado += digitos.charAt(i);
        }
        return "$ " + resultado;
    }

    /** "Laura Mendoza" -> "LM" */
    public static String iniciales(String nombre) {
        String resultado = "";
        for (String palabra : nombre.trim().split(" ")) {
            if (!palabra.isEmpty() && resultado.length() < 2) {
                resultado += Character.toUpperCase(palabra.charAt(0));
            }
        }
        return resultado;
    }

    /** Un ícono del archivo de íconos del proyecto. */
    public static String icono(String nombre) {
        return "<svg class=\"icon\" aria-hidden=\"true\"><use href=\"/assets/icons/sprite.svg#i-" + nombre + "\"></use></svg>";
    }

    /**
     * Limpia un texto antes de meterlo en la página: cambia < > " & por su versión segura.
     * Así, si alguien escribe código en un nombre, se ve como texto y no se ejecuta.
     */
    public static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
