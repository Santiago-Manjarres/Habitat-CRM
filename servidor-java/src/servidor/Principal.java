package servidor;

import com.sun.net.httpserver.HttpServer;
import datos.BaseDeDatos;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada de Habitat CRM.
 *
 * Arranca un servidor web en http://localhost:8080 que:
 *  - entrega la página (landing y CRM) desde la carpeta del proyecto;
 *  - atiende las peticiones /api/ con la base de datos simulada en data/*.json.
 *
 * Usa el servidor HTTP que ya viene con Java (com.sun.net.httpserver),
 * así que no necesita librerías externas.
 */
public class Principal {

    private static final int PUERTO = 8080;

    public static void main(String[] args) throws IOException {
        Path carpetaWeb = buscarCarpetaWeb(args);
        BaseDeDatos db = new BaseDeDatos(carpetaWeb.resolve("data"));
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO;

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ManejadorApi(db));
        servidor.createContext("/", new ManejadorArchivos(carpetaWeb));
        servidor.start();

        System.out.println("Habitat CRM está funcionando.");
        System.out.println("  Página:  http://localhost:" + puerto);
        System.out.println("  Datos:   " + carpetaWeb.resolve("data").toAbsolutePath().normalize());
        System.out.println("Para detenerlo, cierra esta ventana o presiona Ctrl + C.");
    }

    /**
     * La carpeta web es la que contiene app.html. Si se ejecuta desde NetBeans,
     * el programa arranca dentro de servidor-java/, así que se busca en la carpeta de arriba.
     */
    private static Path buscarCarpetaWeb(String[] args) {
        if (args.length > 0) {
            return Paths.get(args[0]);
        }
        if (Files.exists(Paths.get("app.html"))) {
            return Paths.get(".");
        }
        return Paths.get("..");
    }
}
