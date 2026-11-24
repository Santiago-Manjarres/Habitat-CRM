package servidor;

import com.sun.net.httpserver.HttpServer;
import datos.EstructuraDeDatos;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Punto de entrada: enciende el servidor web en http://localhost:8080
 * Usa el servidor HTTP que ya trae Java, así que no necesita librerías.
 */
public class Principal {

    public static void main(String[] args) throws IOException {
        // 1. La carpeta del proyecto (donde está index.html). NetBeans arranca dentro de servidor-java/, por eso se mira arriba.
        Path carpeta = Files.exists(Path.of("index.html")) ? Path.of(".") : Path.of("..");

        // 2. Se leen los .json y quedan en memoria, en listas enlazadas
        EstructuraDeDatos estructura = new EstructuraDeDatos(carpeta.resolve("data"));

        // 3. Se crea el servidor y se dice quién atiende cada dirección
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);
        Rutas rutas = new Rutas(estructura);
        String[] paginasDelCrm = {"/entrar", "/salir", "/resumen", "/propiedades", "/clientes"};
        for (String pagina : paginasDelCrm) {
            servidor.createContext(pagina, rutas);                 // las páginas del CRM las arma Java
        }
        servidor.createContext("/", new Archivos(carpeta));       // lo demás: página de inicio, CSS, imágenes
        servidor.start();

        System.out.println("Habitat CRM está funcionando en http://localhost:8080");
    }
}
