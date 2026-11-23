package servidor;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import json.EscritorJson;
import json.ObjetoJson;

/**
 * Envía respuestas al navegador.
 */
public class Respuesta {

    private Respuesta() {
    }

    /** Envía un objeto JSON con el código de estado HTTP indicado (200 = todo bien). */
    public static void json(HttpExchange intercambio, int codigo, ObjetoJson cuerpo) throws IOException {
        byte[] bytes = EscritorJson.escribir(cuerpo, false).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        enviar(intercambio, codigo, bytes);
    }

    /** Envía bytes (una página, una imagen...) con su tipo de contenido. */
    public static void archivo(HttpExchange intercambio, byte[] bytes, String tipo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        enviar(intercambio, 200, bytes);
    }

    public static void texto(HttpExchange intercambio, int codigo, String mensaje) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        enviar(intercambio, codigo, mensaje.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange intercambio, int codigo, byte[] bytes) throws IOException {
        intercambio.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
