package datos;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import json.EscritorJson;
import json.LectorJson;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;
import modelo.Registro;
import modelo.Usuario;

/**
 * Base de datos simulada con archivos JSON.
 *
 * Al arrancar, lee data/users.json, data/properties.json y data/leads.json y
 * carga cada archivo en una Tabla en memoria. Cada vez que algo cambia, vuelve
 * a escribir el archivo de esa tabla, así los datos no se pierden al cerrar.
 */
public class BaseDeDatos {

    private final Path carpeta;                                  // carpeta donde están los .json (data/)
    private final Tabla<Usuario> usuarios = new Tabla<>();       // tabla de users.json
    private final Tabla<Propiedad> propiedades = new Tabla<>();  // tabla de properties.json
    private final Tabla<Cliente> clientes = new Tabla<>();       // tabla de leads.json

    /** Al crear la base de datos se leen los tres archivos y se llenan las tablas. */
    public BaseDeDatos(Path carpeta) throws IOException {
        this.carpeta = carpeta;
        Nodo<Object> n;
        // Cada archivo es un arreglo JSON: lo recorremos y convertimos cada objeto en su clase Java
        for (n = leerArchivo("users.json").getCabeza(); n != null; n = n.getSiguiente()) {
            usuarios.agregar(Usuario.desdeJson((ObjetoJson) n.getDato()));
        }
        for (n = leerArchivo("properties.json").getCabeza(); n != null; n = n.getSiguiente()) {
            propiedades.agregar(Propiedad.desdeJson((ObjetoJson) n.getDato()));
        }
        for (n = leerArchivo("leads.json").getCabeza(); n != null; n = n.getSiguiente()) {
            clientes.agregar(Cliente.desdeJson((ObjetoJson) n.getDato()));
        }
    }

    /** Lee un archivo .json que contiene un arreglo y lo devuelve como lista enlazada. */
    @SuppressWarnings("unchecked")
    private ListaEnlazada<Object> leerArchivo(String nombre) throws IOException {
        String texto = Files.readString(carpeta.resolve(nombre), StandardCharsets.UTF_8);  // 1. leemos el archivo como texto
        Object valor = LectorJson.leer(texto);                                              // 2. lo convertimos en objetos Java
        if (!(valor instanceof ListaEnlazada)) {
            throw new IOException("El archivo " + nombre + " debe contener un arreglo [ ... ]");
        }
        return (ListaEnlazada<Object>) valor;
    }

    /**
     * Escribe una tabla completa en su archivo. Primero escribe un archivo
     * temporal y después lo renombra, así nunca queda un archivo a medias.
     */
    private <T extends Registro> void guardar(Tabla<T> tabla, String nombre) {
        // 1. convertimos cada registro de la tabla en un objeto JSON
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        for (Nodo<T> n = tabla.getPrimero(); n != null; n = n.getSiguiente()) {
            lista.agregar(n.getDato().aJson());
        }
        try {
            Path destino = carpeta.resolve(nombre);
            Path temporal = carpeta.resolve(nombre + ".tmp");
            // 2. escribimos todo en un archivo temporal
            Files.writeString(temporal, EscritorJson.escribir(lista, true) + "\n", StandardCharsets.UTF_8);
            // 3. el temporal reemplaza al archivo real (si falla antes, el archivo real queda intacto)
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + nombre + ": " + e.getMessage());
        }
    }

    /** Guarda la tabla de propiedades en properties.json. */
    public void guardarPropiedades() {
        guardar(propiedades, "properties.json");
    }

    /** Guarda la tabla de clientes en leads.json. */
    public void guardarClientes() {
        guardar(clientes, "leads.json");
    }

    public Tabla<Usuario> getUsuarios() {
        return usuarios;
    }

    public Tabla<Propiedad> getPropiedades() {
        return propiedades;
    }

    public Tabla<Cliente> getClientes() {
        return clientes;
    }
}
