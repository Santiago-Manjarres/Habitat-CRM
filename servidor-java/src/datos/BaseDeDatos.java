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
 *
 * También lleva un número de "versión" que sube con cada cambio. El navegador
 * lo consulta cada poco para saber si otro asesor modificó algo.
 */
public class BaseDeDatos {

    private final Path carpeta;
    private final Tabla<Usuario> usuarios = new Tabla<>();
    private final Tabla<Propiedad> propiedades = new Tabla<>();
    private final Tabla<Cliente> clientes = new Tabla<>();
    private int version = 1;

    public BaseDeDatos(Path carpeta) throws IOException {
        this.carpeta = carpeta;
        Nodo<Object> n;
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
        String texto = Files.readString(carpeta.resolve(nombre), StandardCharsets.UTF_8);
        Object valor = LectorJson.leer(texto);
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
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        for (Nodo<T> n = tabla.getPrimero(); n != null; n = n.getSiguiente()) {
            lista.agregar(n.getDato().aJson());
        }
        try {
            Path destino = carpeta.resolve(nombre);
            Path temporal = carpeta.resolve(nombre + ".tmp");
            Files.writeString(temporal, EscritorJson.escribir(lista, true) + "\n", StandardCharsets.UTF_8);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + nombre + ": " + e.getMessage());
        }
        version++;
    }

    public void guardarPropiedades() {
        guardar(propiedades, "properties.json");
    }

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

    public int getVersion() {
        return version;
    }
}
