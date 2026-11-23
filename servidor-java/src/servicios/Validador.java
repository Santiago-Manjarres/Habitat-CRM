package servicios;

import java.net.URI;
import json.ObjetoJson;

/**
 * Revisa los datos de los formularios antes de guardarlos.
 * Cada método devuelve un ObjetoJson con un mensaje por campo con error
 * (por ejemplo { "title": "El título es obligatorio" }). Si está vacío, todo está bien.
 */
public class Validador {

    private Validador() {
    }

    public static ObjetoJson validarInicioSesion(String usuario, String contrasena) {
        ObjetoJson errores = new ObjetoJson();
        if (usuario.isEmpty()) errores.poner("username", "Escribe tu usuario");
        if (contrasena.isEmpty()) errores.poner("password", "Escribe tu contraseña");
        return errores;
    }

    /** Revisa los textos y las opciones del formulario de propiedad. */
    private static void revisarTextosPropiedad(ObjetoJson datos, ObjetoJson errores) {
        String titulo = datos.texto("title");
        String ubicacion = datos.texto("location");
        if (titulo.isEmpty()) errores.poner("title", "El título es obligatorio");
        else if (titulo.length() > Catalogos.MAX_TITULO) errores.poner("title", "Usa máximo " + Catalogos.MAX_TITULO + " caracteres");
        if (ubicacion.isEmpty()) errores.poner("location", "La ubicación es obligatoria");
        else if (ubicacion.length() > Catalogos.MAX_UBICACION) errores.poner("location", "Usa máximo " + Catalogos.MAX_UBICACION + " caracteres");
        if (datos.texto("description").length() > Catalogos.MAX_DESCRIPCION) {
            errores.poner("description", "Usa máximo " + Catalogos.MAX_DESCRIPCION + " caracteres");
        }
        if (!Herramientas.estaEn(datos.texto("type"), Catalogos.TIPOS)) errores.poner("type", "Elige un tipo de inmueble");
        if (!Herramientas.estaEn(datos.texto("operation"), Catalogos.OPERACIONES)) errores.poner("operation", "Elige si es venta o arriendo");
        if (!Herramientas.estaEn(datos.texto("status"), Catalogos.ESTADOS_PROPIEDAD)) errores.poner("status", "Elige un estado válido");
        String foto = datos.texto("imageUrl");
        if (!foto.isEmpty() && !esEnlaceValido(foto)) {
            errores.poner("imageUrl", "El enlace de la foto no es válido. Cópialo completo, debe empezar por https://");
        }
    }

    /** Revisa precio, área, habitaciones, baños y parqueaderos. */
    private static void revisarNumerosPropiedad(ObjetoJson datos, ObjetoJson errores) {
        double precio = Numeros.leer(datos.obtener("price"));
        double area = Numeros.leer(datos.obtener("areaM2"));
        if (Double.isNaN(precio) || precio <= 0) errores.poner("price", "El precio debe ser mayor que 0");
        else if (precio > Catalogos.MAX_PRECIO) errores.poner("price", "El precio es demasiado alto");
        if (Double.isNaN(area) || area <= 0) errores.poner("areaM2", "El área debe ser mayor que 0");
        else if (area > Catalogos.MAX_AREA) errores.poner("areaM2", "El área es demasiado grande");
        String[] campos = {"bedrooms", "bathrooms", "parking"};
        String[] nombres = {"habitaciones", "baños", "parqueaderos"};
        for (int i = 0; i < campos.length; i++) {
            double cantidad = Numeros.leer(datos.obtener(campos[i]));
            if (!Numeros.esEnteroNoNegativo(cantidad)) {
                errores.poner(campos[i], "El número de " + nombres[i] + " debe ser 0 o más (sin decimales)");
            } else if (cantidad > Catalogos.MAX_CUARTOS) {
                errores.poner(campos[i], "Máximo " + Catalogos.MAX_CUARTOS + " " + nombres[i]);
            }
        }
    }

    public static ObjetoJson validarPropiedad(ObjetoJson datos) {
        ObjetoJson errores = new ObjetoJson();
        revisarTextosPropiedad(datos, errores);
        revisarNumerosPropiedad(datos, errores);
        return errores;
    }

    /** Cliente: nombre, al menos un teléfono o un correo, propiedad, asesor y etapa. */
    public static ObjetoJson validarCliente(ObjetoJson datos) {
        ObjetoJson errores = new ObjetoJson();
        String nombre = datos.texto("contactName");
        String telefono = datos.texto("contactPhone");
        String correo = datos.texto("contactEmail");
        String etapa = datos.texto("stage").isEmpty() ? "new" : datos.texto("stage");
        if (nombre.isEmpty()) errores.poner("contactName", "Escribe el nombre del cliente");
        else if (nombre.length() > Catalogos.MAX_NOMBRE) errores.poner("contactName", "Usa máximo " + Catalogos.MAX_NOMBRE + " caracteres");
        if (!telefono.isEmpty() && !telefono.matches("\\+?[\\d\\s-]{7,20}")) {
            errores.poner("contactPhone", "Escribe un teléfono válido, por ejemplo +57 300 123 4567");
        }
        if (!correo.isEmpty() && !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}")) {
            errores.poner("contactEmail", "Escribe un correo válido, por ejemplo nombre@correo.com");
        }
        if (telefono.isEmpty() && correo.isEmpty()) errores.poner("contactPhone", "Escribe un teléfono o un correo para poder contactarlo");
        if (!Herramientas.estaEn(datos.texto("source"), Catalogos.ORIGENES)) errores.poner("source", "Elige cómo llegó el cliente");
        if (datos.texto("propertyId").isEmpty()) errores.poner("propertyId", "Elige la propiedad que le interesa");
        if (datos.texto("agentId").isEmpty()) errores.poner("agentId", "Elige el asesor responsable");
        if (!Herramientas.estaEn(etapa, Catalogos.ETAPAS)) errores.poner("stage", "Elige una etapa válida");
        if (datos.texto("notes").length() > Catalogos.MAX_NOTAS) errores.poner("notes", "Usa máximo " + Catalogos.MAX_NOTAS + " caracteres");
        return errores;
    }

    /** Solo se aceptan enlaces completos que empiecen por http:// o https:// */
    private static boolean esEnlaceValido(String texto) {
        try {
            URI enlace = new URI(texto);
            String esquema = enlace.getScheme();
            String servidor = enlace.getHost();
            return ("http".equals(esquema) || "https".equals(esquema)) && servidor != null && servidor.contains(".");
        } catch (Exception e) {
            return false;
        }
    }
}
