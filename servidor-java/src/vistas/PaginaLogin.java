package vistas;

import json.ObjetoJson;

/**
 * La pantalla de inicio de sesión: la foto a la izquierda y el formulario a la derecha.
 * El formulario se envía con method="post" a /entrar, donde Java revisa los datos.
 */
public class PaginaLogin {

    private PaginaLogin() {
    }

    /**
     * @param usuario      lo que la persona ya había escrito (para no hacerla escribir de nuevo)
     * @param errores      errores por campo, por ejemplo { "username": "Escribe tu usuario" }
     * @param errorGeneral mensaje de la alerta roja de arriba, o "" si no hay
     * @param aviso        aviso de abajo (por ejemplo "Cerraste sesión"), o ""
     */
    public static String html(String usuario, ObjetoJson errores, String errorGeneral, String aviso) {
        String alerta = errorGeneral.isEmpty() ? ""
                : Html.icono("alert-circle", "icon--sm") + "<span>" + Html.escapar(errorGeneral) + "</span>";
        String cuerpo = "<main class=\"view view-login\">"
                // Mitad izquierda: la foto
                + "<div class=\"login-media\"><img src=\"/assets/img/login-house.jpg\" alt=\"Casa de dos pisos con fachada de madera y jardín al atardecer\" width=\"1200\" height=\"799\"></div>"
                // Mitad derecha: la tarjeta con el formulario
                + "<div class=\"login-panel\"><section class=\"auth-card\" aria-labelledby=\"login-title\">"
                + "<header class=\"auth-card__header\">"
                + "<a class=\"auth-brand\" href=\"/\"><img src=\"/assets/img/logo.png\" alt=\"\" width=\"28\" height=\"26\" class=\"brand__logo\">Habitat CRM</a>"
                + "<div><h1 id=\"login-title\" class=\"page__title\">Inicia sesión</h1>"
                + "<p class=\"page__subtitle\">Acceso para el equipo de asesores de la inmobiliaria.</p></div>"
                + "</header>"
                // novalidate: las reglas las revisa Java, no el navegador
                + "<form method=\"post\" action=\"/entrar\" class=\"auth-form\" novalidate>"
                + "<div class=\"alert alert--danger\" role=\"alert\">" + alerta + "</div>"
                + Html.campo("login-username", "username", "Usuario", usuario, "text",
                        "autocomplete=\"username\" autocapitalize=\"none\" spellcheck=\"false\" autofocus",
                        "", errores.texto("username"), false)
                + Html.campo("login-password", "password", "Contraseña", "", "password",
                        "autocomplete=\"current-password\"", "", errores.texto("password"), false)
                + "<button type=\"submit\" class=\"btn btn--primary btn--block\">Iniciar sesión</button>"
                + "</form>"
                + "<a class=\"link-btn auth-back\" href=\"/\">" + Html.icono("chevron-left", "icon--sm") + "Volver al sitio</a>"
                + "</section></div></main>";
        return Plantilla.pagina("Iniciar sesión", "login", cuerpo, aviso);
    }
}
