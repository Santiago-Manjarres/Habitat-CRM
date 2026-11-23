package pruebas;

import estructuras.ArbolBinarioBusqueda;
import estructuras.Cola;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import estructuras.Pila;
import estructuras.TablaHash;
import json.EscritorJson;
import json.LectorJson;
import json.ObjetoJson;

/**
 * Programa de prueba de las estructuras de datos del proyecto.
 * En NetBeans: clic derecho sobre este archivo y "Run File" (Shift + F6).
 * Cada línea imprime BIEN o FALLA.
 */
public class PruebaEstructuras {

    private static int fallas = 0;

    public static void main(String[] args) {
        probarLista();
        probarPilaYCola();
        probarArbol();
        probarTablaHash();
        probarJson();
        System.out.println(fallas == 0 ? "\nTodas las pruebas pasaron." : "\nPruebas con falla: " + fallas);
    }

    private static void revisar(String nombre, boolean resultado) {
        System.out.println((resultado ? "BIEN   " : "FALLA  ") + nombre);
        if (!resultado) fallas++;
    }

    private static void probarLista() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("b");
        lista.agregar("c");
        lista.agregarAlInicio("a");
        revisar("Lista: agregar al final y al inicio", lista.tamano() == 3 && lista.obtener(0).equals("a") && lista.obtener(2).equals("c"));
        lista.eliminar(lista.obtener(1));
        revisar("Lista: eliminar del medio", lista.tamano() == 2 && lista.obtener(1).equals("c"));
        lista.eliminar(lista.obtener(1));
        lista.agregar("d");
        revisar("Lista: eliminar el último y volver a agregar", lista.tamano() == 2 && lista.obtener(1).equals("d"));
    }

    private static void probarPilaYCola() {
        Pila<Integer> pila = new Pila<>();
        Cola<Integer> cola = new Cola<>();
        for (int i = 1; i <= 3; i++) {
            pila.apilar(i);
            cola.encolar(i);
        }
        revisar("Pila: sale primero el último (3, 2, 1)", pila.desapilar() == 3 && pila.desapilar() == 2 && pila.desapilar() == 1 && pila.estaVacia());
        revisar("Cola: sale primero el primero (1, 2, 3)", cola.desencolar() == 1 && cola.desencolar() == 2 && cola.desencolar() == 3 && cola.estaVacia());
    }

    private static void probarArbol() {
        ArbolBinarioBusqueda<String> arbol = new ArbolBinarioBusqueda<>();
        String[] fechas = {"2026-11-05", "2026-10-15", "2026-11-12", "2026-10-22"};
        for (String fecha : fechas) {
            arbol.insertar(fecha, "propiedad del " + fecha);
        }
        StringBuilder orden = new StringBuilder();
        for (Nodo<String> n = arbol.recorrerEnOrden().getCabeza(); n != null; n = n.getSiguiente()) {
            orden.append(n.getDato().substring(14)).append(' ');
        }
        revisar("Árbol: el recorrido en orden ordena las fechas", orden.toString().trim().equals("2026-10-15 2026-10-22 2026-11-05 2026-11-12"));
        revisar("Árbol: buscar una clave", arbol.buscar("2026-11-12").equals("propiedad del 2026-11-12") && arbol.buscar("2020-01-01") == null);
        revisar("Árbol: tamaño y altura", arbol.tamano() == 4 && arbol.altura() == 3);
    }

    private static void probarTablaHash() {
        TablaHash<String> tabla = new TablaHash<>();
        for (int i = 0; i < 100; i++) {
            tabla.poner("id_" + i, "registro " + i);
        }
        revisar("Tabla hash: 100 claves, con colisiones resueltas", tabla.tamano() == 100 && tabla.obtener("id_57").equals("registro 57"));
        tabla.eliminar("id_57");
        revisar("Tabla hash: eliminar una clave", !tabla.contiene("id_57") && tabla.tamano() == 99);
    }

    private static void probarJson() {
        String texto = "{\"title\":\"Casa en Bogotá\",\"price\":450000000,\"tags\":[\"a\",\"b\"],\"ok\":true,\"x\":null}";
        ObjetoJson objeto = (ObjetoJson) LectorJson.leer(texto);
        revisar("JSON: leer texto, número, arreglo, booleano y null",
                objeto.texto("title").equals("Casa en Bogotá") && objeto.numero("price") == 450000000
                && ((ListaEnlazada<?>) objeto.obtener("tags")).tamano() == 2 && objeto.obtener("ok") == Boolean.TRUE);
        revisar("JSON: escribir y volver a leer da lo mismo", EscritorJson.escribir(objeto, false).equals(texto));
    }
}
