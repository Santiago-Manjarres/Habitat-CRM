package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entrega los archivos de la página web: index.html, app.html, css, js,
 * imágenes y fuentes. Es lo que hace cualquier servidor web básico.
 */
public class ManejadorArchivos implements HttpHandler {

    private final Path carpetaWeb;

    public ManejadorArchivos(Path carpetaWeb) {
        this.carpetaWeb = carpetaWeb.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        String ruta = URLDecoder.decode(intercambio.getRequestURI().getPath(), StandardCharsets.UTF_8);
        if (ruta.equals("/")) {
            ruta = "/index.html";
        }
        Path archivo = carpetaWeb.resolve(ruta.substring(1)).normalize();
        if (!estaPermitido(archivo) || !Files.isRegularFile(archivo)) {
            Respuesta.texto(intercambio, 404, "No encontrado");
            return;
        }
        Respuesta.archivo(intercambio, Files.readAllBytes(archivo), tipoDeContenido(archivo.toString()));
    }

    /**
     * Solo se entregan archivos que estén dentro de la carpeta web, y nunca
     * los datos, el código del servidor ni carpetas ocultas (.git...).
     */
    private boolean estaPermitido(Path archivo) {
        if (!archivo.startsWith(carpetaWeb)) {
            return false;
        }
        String relativa = carpetaWeb.relativize(archivo).toString().replace('\\', '/');
        return !relativa.startsWith("data/") && !relativa.startsWith("servidor-java/")
                && !relativa.startsWith(".") && !relativa.contains("/.");
    }

    private String tipoDeContenido(String nombre) {
        String n = nombre.toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css")) return "text/css; charset=utf-8";
        if (n.endsWith(".js") || n.endsWith(".mjs")) return "text/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".woff2")) return "font/woff2";
        if (n.endsWith(".woff")) return "font/woff";
        if (n.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
