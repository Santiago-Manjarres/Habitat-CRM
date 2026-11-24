package servidor;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Envía respuestas al navegador. Hay tres tipos:
 *  - html():       una página armada por Java
 *  - archivo():    un archivo del proyecto (CSS, imagen, la página de inicio...)
 *  - redirigir():  "ve a esta otra dirección" (se usa después de guardar)
 * El "código" es el número HTTP: 200 = todo bien, 303 = ve a otra página,
 * 404 = no existe, 500 = error del servidor.
 */
public class Respuesta {

    private Respuesta() {
    }

    /** Envía una página HTML armada por Java. */
    public static void html(HttpExchange intercambio, int codigo, String html) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");      // siempre datos frescos
        enviar(intercambio, codigo, html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Le dice al navegador que vaya a otra dirección (código 303).
     * Se usa después de guardar: así, si la persona recarga la página,
     * no se vuelve a enviar el formulario por error.
     */
    public static void redirigir(HttpExchange intercambio, String destino) throws IOException {
        intercambio.getResponseHeaders().set("Location", destino);
        intercambio.sendResponseHeaders(303, -1);      // -1 = sin contenido
        intercambio.close();
    }

    /** Envía bytes (una imagen, un CSS...) con su tipo de contenido. */
    public static void archivo(HttpExchange intercambio, byte[] bytes, String tipo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        enviar(intercambio, 200, bytes);
    }

    /** Envía un mensaje de texto simple (se usa para el error 404). */
    public static void texto(HttpExchange intercambio, int codigo, String mensaje) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        enviar(intercambio, codigo, mensaje.getBytes(StandardCharsets.UTF_8));
    }

    /** Envía primero la cabecera (código y tamaño) y después el contenido. */
    private static void enviar(HttpExchange intercambio, int codigo, byte[] bytes) throws IOException {
        intercambio.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
