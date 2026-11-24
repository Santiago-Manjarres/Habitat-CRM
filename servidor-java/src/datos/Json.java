package datos;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lee y escribe los archivos .json de la carpeta data/ (donde se guardan las listas).
 *
 * Los archivos tienen siempre esta forma, con UN dato por línea:
 *   [
 *     {
 *       "id": "1",
 *       "titulo": "Casa en Laureles"
 *     },
 *     ...
 *   ]
 * Por eso se pueden leer línea por línea, sin un lector de JSON complicado.
 */
public class Json {

    /** Lee un archivo y devuelve una lista con un Datos por cada objeto { ... }. */
    public static ListaEnlazada<Datos> leer(Path archivo) throws IOException {
        ListaEnlazada<Datos> lista = new ListaEnlazada<>();
        Datos actual = null;
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            linea = linea.trim();
            if (linea.startsWith("{")) {
                actual = new Datos();                         // empieza un objeto nuevo
            } else if (linea.startsWith("}")) {
                lista.agregar(actual);                        // termina el objeto: lo guardamos en la lista
            } else if (linea.startsWith("\"")) {
                // una línea como:   "titulo": "Casa en Laureles",
                int separador = linea.indexOf("\":");
                String nombre = linea.substring(1, separador);
                String valor = linea.substring(separador + 2).trim();
                if (valor.endsWith(",")) {
                    valor = valor.substring(0, valor.length() - 1);      // quitamos la coma del final
                }
                actual.poner(nombre, quitarComillas(valor));
            }
        }
        return lista;
    }

    /** Escribe la lista completa en el archivo (reemplaza lo que había). */
    public static void escribir(Path archivo, ListaEnlazada<Datos> lista) throws IOException {
        StringBuilder texto = new StringBuilder("[\n");
        for (Nodo<Datos> n = lista.getCabeza(); n != null; n = n.siguiente) {
            texto.append("  {\n");
            for (Nodo<String[]> par = n.dato.getPares().getCabeza(); par != null; par = par.siguiente) {
                texto.append("    \"").append(par.dato[0]).append("\": \"").append(escapar(par.dato[1])).append("\"");
                texto.append(par.siguiente != null ? ",\n" : "\n");     // coma entre datos, no después del último
            }
            texto.append(n.siguiente != null ? "  },\n" : "  }\n");
        }
        texto.append("]\n");
        Files.writeString(archivo, texto, StandardCharsets.UTF_8);
    }

    /** "Casa \"bonita\"" -> Casa "bonita". Quita las comillas de los lados y traduce \" \\ \n. */
    private static String quitarComillas(String valor) {
        if (!valor.startsWith("\"")) {
            return valor;                                     // un número o null: se deja igual
        }
        valor = valor.substring(1, valor.length() - 1);
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\\' && i + 1 < valor.length()) {        // una barra invertida: miramos el siguiente carácter
                i++;
                resultado.append(valor.charAt(i) == 'n' ? '\n' : valor.charAt(i));
            } else {
                resultado.append(c);
            }
        }
        return resultado.toString();
    }

    /** Lo contrario: prepara un texto para escribirlo entre comillas en el archivo. */
    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }
}
