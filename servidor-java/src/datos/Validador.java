package datos;

/**
 * Revisa los formularios antes de guardar.
 * Cada método devuelve un Datos con un mensaje por cada campo que tiene error
 * (por ejemplo  titulo -> "Escribe el título"). Si viene vacío, todo está bien.
 */
public class Validador {

    public static Datos login(String usuario, String clave) {
        Datos errores = new Datos();
        if (usuario.isEmpty()) errores.poner("usuario", "Escribe tu usuario");
        if (clave.isEmpty()) errores.poner("clave", "Escribe tu contraseña");
        return errores;
    }

    public static Datos propiedad(Datos formulario) {
        Datos errores = new Datos();
        if (formulario.get("titulo").isEmpty()) errores.poner("titulo", "Escribe el título");
        if (formulario.get("ubicacion").isEmpty()) errores.poner("ubicacion", "Escribe la ubicación");
        if (leerNumero(formulario.get("precio")) <= 0) errores.poner("precio", "Escribe un precio mayor que 0");
        return errores;
    }

    public static Datos cliente(Datos formulario) {
        Datos errores = new Datos();
        if (formulario.get("nombre").isEmpty()) errores.poner("nombre", "Escribe el nombre del cliente");
        if (formulario.get("telefono").isEmpty() && formulario.get("correo").isEmpty()) {
            errores.poner("telefono", "Escribe un teléfono o un correo");
        }
        if (formulario.get("propiedad").isEmpty()) errores.poner("propiedad", "Primero registra una propiedad");
        return errores;
    }

    /** "$ 1.350.000" -> 1350000. Se queda solo con los dígitos. Devuelve -1 si no hay ningún número. */
    public static long leerNumero(String texto) {
        String digitos = texto.replaceAll("[^0-9]", "");      // borra todo lo que no sea un dígito
        if (digitos.isEmpty() || digitos.length() > 15) {
            return -1;
        }
        return Long.parseLong(digitos);
    }
}
