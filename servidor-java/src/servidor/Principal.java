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
 *  - arma las páginas del CRM en Java (login, resumen, propiedades y clientes)
 *    con los datos de la base de datos simulada en data/*.json;
 *  - entrega los archivos del proyecto: la página de inicio, los CSS y las imágenes.
 *
 * Usa el servidor HTTP que ya viene con Java (com.sun.net.httpserver),
 * así que no necesita librerías externas.
 */
public class Principal {

    private static final int PUERTO = 8080;   // número de puerto por defecto (http://localhost:8080)

    public static void main(String[] args) throws IOException {
        Path carpetaWeb = buscarCarpetaWeb(args);                      // 1. buscamos la carpeta con la página web
        BaseDeDatos db = new BaseDeDatos(carpetaWeb.resolve("data"));  // 2. leemos los .json y los cargamos en memoria
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO;

        // 3. creamos el servidor y le decimos quién atiende cada tipo de dirección
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        ManejadorPaginas paginas = new ManejadorPaginas(db);             // las páginas del CRM (las arma Java)
        String[] rutasDelCrm = {"/entrar", "/salir", "/resumen", "/propiedades", "/clientes", "/app.html"};
        for (String ruta : rutasDelCrm) {
            servidor.createContext(ruta, paginas);
        }
        servidor.createContext("/", new ManejadorArchivos(carpetaWeb));   // todo lo demás -> archivos (CSS, imágenes, página de inicio)
        servidor.start();                                              // 4. el servidor queda escuchando

        System.out.println("Habitat CRM está funcionando.");
        System.out.println("  Página:  http://localhost:" + puerto);
        System.out.println("  Datos:   " + carpetaWeb.resolve("data").toAbsolutePath().normalize());
        System.out.println("Para detenerlo, cierra esta ventana o presiona Ctrl + C.");
    }

    /**
     * La carpeta web es la que contiene index.html. Si se ejecuta desde NetBeans,
     * el programa arranca dentro de servidor-java/, así que se busca en la carpeta de arriba.
     */
    private static Path buscarCarpetaWeb(String[] args) {
        if (args.length > 0) {
            return Paths.get(args[0]);
        }
        if (Files.exists(Paths.get("index.html"))) {
            return Paths.get(".");
        }
        return Paths.get("..");
    }
}
