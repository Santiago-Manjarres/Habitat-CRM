package vistas;

import modelo.Usuario;

/**
 * La estructura que comparten todas las páginas del CRM.
 *
 *  pagina()      -> el documento completo: <head> con los CSS y <body>
 *  panelAsesor() -> menú lateral, barra superior, contenido y menú inferior (celular)
 *  ventana()     -> una ventana emergente encima de la página (formularios y confirmaciones)
 *  aviso()       -> el mensaje pequeño de abajo a la derecha ("Propiedad creada")
 */
public class Plantilla {

    private Plantilla() {
    }

    /**
     * El documento HTML completo.
     * @param titulo título de la pestaña del navegador
     * @param vista  "login" o "admin" (el CSS usa este dato para ubicar los avisos)
     * @param cuerpo el contenido de la página
     * @param aviso  un aviso ya armado con aviso(), o "" si no hay
     */
    public static String pagina(String titulo, String vista, String cuerpo, String aviso) {
        return "<!doctype html>\n<html lang=\"es\">\n<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">\n"
                + "  <meta name=\"description\" content=\"Habitat CRM: propiedades, clientes y negocios del equipo de asesores.\">\n"
                + "  <meta name=\"robots\" content=\"noindex\">\n"
                + "  <title>" + Html.escapar(titulo) + " · Habitat CRM</title>\n"
                + "  <link rel=\"icon\" type=\"image/png\" href=\"/assets/img/logo.png\">\n"
                // El orden importa: primero las variables, después lo que las usa
                + "  <link rel=\"stylesheet\" href=\"/css/variables.css\">\n"
                + "  <link rel=\"stylesheet\" href=\"/css/base.css\">\n"
                + "  <link rel=\"stylesheet\" href=\"/css/components.css\">\n"
                + "  <link rel=\"stylesheet\" href=\"/css/layout.css\">\n"
                + "</head>\n<body data-view=\"" + vista + "\">\n"
                + cuerpo
                + "\n<div class=\"toast-region\" aria-live=\"polite\">" + aviso + "</div>\n"
                + "</body>\n</html>\n";
    }

    /**
     * Un aviso que aparece y se va solo a los pocos segundos (lo hace el CSS, sin JavaScript).
     * @param tipo "success", "error", "warning" o "info"
     */
    public static String aviso(String tipo, String mensaje) {
        String icono = tipo.equals("success") ? "check-circle"
                : tipo.equals("error") ? "alert-circle"
                : tipo.equals("warning") ? "alert-triangle" : "info";
        return "<div class=\"toast toast--" + tipo + " toast--aviso\" role=\"" + (tipo.equals("error") ? "alert" : "status") + "\">"
                + "<span class=\"toast__icon\">" + Html.icono(icono, "") + "</span>"
                + "<p class=\"toast__message\">" + Html.escapar(mensaje) + "</p></div>";
    }

    /** Un enlace del menú. aria-current="page" marca la sección activa (el CSS la pinta de azul). */
    private static String enlace(String clase, String ruta, String icono, String texto, String activa) {
        String actual = ruta.equals(activa) ? " aria-current=\"page\"" : "";
        return "<a class=\"" + clase + "\" href=\"/" + ruta + "\"" + actual + ">" + Html.icono(icono, "") + texto + "</a>";
    }

    /**
     * El panel del asesor con todo su marco.
     * @param usuario   el asesor con sesión (su nombre va en la barra superior)
     * @param activa    "resumen", "propiedades" o "clientes"
     * @param contenido lo que va en el centro
     * @param ventana   una ventana emergente encima de todo, o "" si no hay
     */
    public static String panelAsesor(Usuario usuario, String activa, String contenido, String ventana) {
        String logo = "<img src=\"/assets/img/logo.png\" alt=\"\" width=\"28\" height=\"26\" class=\"brand__logo\">";
        return "<div class=\"view admin-layout\">\n"
                // 1. Menú lateral (solo en computador)
                + "<aside class=\"sidebar\" aria-label=\"Menú del asesor\">"
                + "<a class=\"brand\" href=\"/resumen\">" + logo + "<span>Habitat CRM</span></a>"
                + "<nav class=\"sidebar__nav\" aria-label=\"Principal\">"
                + enlace("nav-link", "resumen", "grid", "Resumen", activa)
                + enlace("nav-link", "propiedades", "building", "Propiedades", activa)
                + enlace("nav-link", "clientes", "user", "Clientes", activa)
                + "</nav>"
                + "<a class=\"nav-link sidebar__site\" href=\"/\">" + Html.icono("home", "") + "Ver el sitio</a>"
                + "</aside>\n"
                + "<div class=\"admin-main has-bottom-nav\">\n"
                // 2. Barra superior: fecha, asesor y botón de salir
                + "<header class=\"app-header topbar\"><div class=\"container app-header__inner\">"
                + "<a class=\"brand topbar__brand\" href=\"/resumen\">" + logo + "<span>Habitat CRM</span></a>"
                + "<p class=\"topbar__date\">" + Formato.fechaDeHoy() + "</p>"
                + "<span class=\"app-header__spacer\"></span>"
                + "<div class=\"user-chip\"><span class=\"avatar\" aria-hidden=\"true\">" + Html.escapar(usuario.getAvatar()) + "</span>"
                + "<span class=\"user-chip__text\"><span class=\"user-chip__name\">" + Html.escapar(usuario.getNombre()) + "</span>"
                + "<span class=\"user-chip__role\">Asesor</span></span></div>"
                // Cerrar sesión es un formulario: así es un POST (cambia algo en el servidor)
                + "<form method=\"post\" action=\"/salir\" class=\"form-en-linea\">"
                + "<button type=\"submit\" class=\"btn btn--secondary btn--icon logout-btn\" aria-label=\"Cerrar sesión\">"
                + Html.icono("logout", "") + "<span class=\"logout-btn__label\">Cerrar sesión</span></button></form>"
                + "</div></header>\n"
                // 3. El contenido de la sección
                + "<main class=\"container\">" + contenido + "</main>\n"
                // 4. Menú inferior (solo en celular)
                + "<nav class=\"bottom-nav\" aria-label=\"Principal (móvil)\">"
                + enlace("bottom-nav__link", "resumen", "grid", "Resumen", activa)
                + enlace("bottom-nav__link", "propiedades", "building", "Propiedades", activa)
                + enlace("bottom-nav__link", "clientes", "user", "Clientes", activa)
                + "</nav>\n"
                + "</div>\n</div>\n"
                + ventana;
    }

    /** El encabezado de una sección: título, subtítulo y un botón opcional a la derecha. */
    public static String seccion(String titulo, String subtitulo, String boton, String contenido) {
        return "<section class=\"page\" aria-labelledby=\"titulo-seccion\">"
                + "<header class=\"page__header\"><div>"
                + "<h1 id=\"titulo-seccion\" class=\"page__title\">" + Html.escapar(titulo) + "</h1>"
                + "<p class=\"page__subtitle\">" + Html.escapar(subtitulo) + "</p></div>" + boton + "</header>"
                + contenido + "</section>";
    }

    /**
     * Una ventana emergente sin JavaScript. Es una capa oscura que tapa la página
     * con la ventana en el centro. Para cerrarla hay enlaces (la X, "Cancelar" y el
     * fondo oscuro) que vuelven a la página de atrás.
     * @param cerrar dirección a la que se vuelve al cerrar, por ejemplo "/propiedades"
     * @param chica  true para las confirmaciones (ventana más angosta)
     */
    public static String ventana(String titulo, String cuerpo, String pie, String cerrar, boolean chica) {
        return "<div class=\"modal-capa\">"
                + "<a class=\"modal-capa__fondo\" href=\"" + cerrar + "\" tabindex=\"-1\" aria-hidden=\"true\"></a>"
                + "<section class=\"modal" + (chica ? " modal--sm" : "") + "\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"titulo-ventana\">"
                + "<div class=\"modal__inner\">"
                + "<header class=\"modal__header\"><h2 class=\"modal__title\" id=\"titulo-ventana\">" + Html.escapar(titulo) + "</h2>"
                + "<a class=\"btn btn--ghost btn--icon\" href=\"" + cerrar + "\" aria-label=\"Cerrar\">" + Html.icono("close", "") + "</a></header>"
                + "<div class=\"modal__body\">" + cuerpo + "</div>"
                + "<footer class=\"modal__footer\">" + pie + "</footer>"
                + "</div></section></div>";
    }
}
