package vistas;

import datos.Datos;

/** La pantalla de inicio de sesión: la foto a la izquierda y el formulario a la derecha. */
public class PaginaLogin {

    /**
     * @param usuario      lo que la persona ya había escrito (para no hacerla escribir de nuevo)
     * @param errores      errores por campo, por ejemplo  usuario -> "Escribe tu usuario"
     * @param errorGeneral el mensaje rojo de arriba, o "" si no hay
     */
    public static String html(String usuario, Datos errores, String errorGeneral) {
        Datos valores = new Datos();
        valores.poner("usuario", usuario);
        return Plantilla.cabeza("Iniciar sesión") + "<body><main class=\"view view-login\">"
                + "<div class=\"login-media\"><img src=\"/assets/img/login-house.jpg\" alt=\"Casa con jardín al atardecer\" width=\"1200\" height=\"799\"></div>"
                + "<div class=\"login-panel\"><section class=\"auth-card\"><header class=\"auth-card__header\">"
                + "<a class=\"auth-brand\" href=\"/\"><img src=\"/assets/img/logo.png\" alt=\"\" width=\"28\" height=\"26\" class=\"brand__logo\">Habitat CRM</a>"
                + "<div><h1 class=\"page__title\">Inicia sesión</h1><p class=\"page__subtitle\">Acceso para el equipo de asesores de la inmobiliaria.</p></div>"
                + "</header>"
                // El formulario se envía a /entrar con POST. novalidate: las reglas las revisa Java.
                + "<form method=\"post\" action=\"/entrar\" class=\"auth-form\" novalidate>"
                + (errorGeneral.isEmpty() ? "" : "<div class=\"alert alert--danger\">" + errorGeneral + "</div>")
                + Plantilla.campo("usuario", "Usuario", "text", valores, errores)
                + Plantilla.campo("clave", "Contraseña", "password", new Datos(), errores)
                + "<button type=\"submit\" class=\"btn btn--primary btn--block\">Iniciar sesión</button>"
                + "</form>"
                + "<a class=\"link-btn auth-back\" href=\"/\">Volver al sitio</a>"
                + "</section></div></main></body></html>";
    }
}
