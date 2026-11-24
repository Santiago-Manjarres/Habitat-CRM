package servidor;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import json.ObjetoJson;

/**
 * Lee los datos que manda el navegador.
 *
 * Cuando se envía un formulario HTML, el navegador manda un texto así:
 *     title=Casa+en+Laureles&price=450.000.000&type=Casa
 * Cada pareja va separada por "&" y cada nombre de su valor por "=".
 * Los espacios llegan como "+" y las tildes como "%C3%A1"; URLDecoder los traduce.
 *
 * El resultado se guarda en un ObjetoJson con las mismas claves (title, price...),
 * así los servicios y el Validador lo reciben igual que antes.
 */
public class Formulario {

    private Formulario() {
    }

    /** Lee el cuerpo de un POST (los campos del formulario). */
    public static ObjetoJson leer(HttpExchange intercambio) throws IOException {
        String texto = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return separar(texto);
    }

    /** Lee los parámetros de la dirección: lo que va después del "?" (por ejemplo ?q=bogota). */
    public static ObjetoJson parametros(HttpExchange intercambio) {
        String consulta = intercambio.getRequestURI().getRawQuery();
        return separar(consulta == null ? "" : consulta);
    }

    /** Separa "a=1&b=2" en { "a": "1", "b": "2" }. */
    private static ObjetoJson separar(String texto) {
        ObjetoJson datos = new ObjetoJson();
        if (texto.isEmpty()) {
            return datos;
        }
        for (String pareja : texto.split("&")) {
            String[] partes = pareja.split("=", 2);                    // separa el nombre del valor
            String nombre = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            datos.poner(nombre, valor);
        }
        return datos;
    }

    /** Busca una cookie por su nombre. El navegador las manda así: "sesion=abc123; otra=xyz". */
    public static String cookie(HttpExchange intercambio, String nombre) {
        String cabecera = intercambio.getRequestHeaders().getFirst("Cookie");
        if (cabecera == null) {
            return null;
        }
        for (String pareja : cabecera.split(";")) {
            String[] partes = pareja.trim().split("=", 2);
            if (partes[0].equals(nombre) && partes.length == 2) {
                return partes[1];
            }
        }
        return null;
    }
}
