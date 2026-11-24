package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entrega los archivos del proyecto: index.html, los .css, las imágenes y la fuente.
 * Nunca entrega la carpeta data/ (tiene las contraseñas) ni el código del servidor.
 */
public class Archivos implements HttpHandler {

    private final Path carpeta;

    public Archivos(Path carpeta) {
        this.carpeta = carpeta.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        String ruta = URLDecoder.decode(intercambio.getRequestURI().getPath(), StandardCharsets.UTF_8);
        if (ruta.equals("/")) {
            ruta = "/index.html";                                // la dirección raíz es la página de inicio
        }
        Path archivo = carpeta.resolve(ruta.substring(1)).normalize();
        String relativa = archivo.startsWith(carpeta) ? carpeta.relativize(archivo).toString().replace('\\', '/') : "";
        boolean prohibido = relativa.isEmpty() || relativa.startsWith("data/") || relativa.startsWith("servidor-java/")
                || relativa.startsWith(".") || relativa.contains("/.");
        byte[] contenido;
        int codigo;
        if (prohibido || !Files.isRegularFile(archivo)) {
            contenido = "No encontrado".getBytes(StandardCharsets.UTF_8);    // 404: no existe o no se puede ver
            codigo = 404;
        } else {
            contenido = Files.readAllBytes(archivo);
            codigo = 200;
            intercambio.getResponseHeaders().set("Content-Type", tipo(relativa));
        }
        intercambio.sendResponseHeaders(codigo, contenido.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(contenido);
        }
    }

    /** Le dice al navegador qué tipo de archivo es, según su terminación. */
    private String tipo(String nombre) {
        if (nombre.endsWith(".html")) return "text/html; charset=utf-8";
        if (nombre.endsWith(".css")) return "text/css; charset=utf-8";
        if (nombre.endsWith(".svg")) return "image/svg+xml";
        if (nombre.endsWith(".png")) return "image/png";
        if (nombre.endsWith(".jpg")) return "image/jpeg";
        if (nombre.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}
