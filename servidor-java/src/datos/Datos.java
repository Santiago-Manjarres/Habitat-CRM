package datos;

import estructuras.ListaEnlazada;
import estructuras.Nodo;

/**
 * Un grupo de pares nombre -> valor (todos son texto), guardados en una lista enlazada.
 *
 *   id -> "3",  titulo -> "Casa en Laureles",  precio -> "450000000"
 *
 * Se usa para TODO:
 *  - cada usuario, propiedad y cliente (un objeto del archivo .json);
 *  - lo que llega de un formulario;
 *  - los errores de validación (campo -> mensaje).
 */
public class Datos {

    private final ListaEnlazada<String[]> pares = new ListaEnlazada<>();   // cada par es {nombre, valor}

    /** Guarda un valor. Si el nombre ya existe, cambia su valor; si no, lo agrega. */
    public void poner(String nombre, String valor) {
        for (Nodo<String[]> n = pares.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato[0].equals(nombre)) {
                n.dato[1] = valor;                       // ya existía: lo cambiamos
                return;
            }
        }
        pares.agregar(new String[] {nombre, valor});     // no existía: lo agregamos al final
    }

    /** Devuelve el valor de un nombre, o "" si no existe. */
    public String get(String nombre) {
        for (Nodo<String[]> n = pares.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato[0].equals(nombre)) {
                return n.dato[1];
            }
        }
        return "";
    }

    public boolean estaVacio() {
        return pares.tamano() == 0;
    }

    /** La lista de pares, para recorrerlos (la usa Json al escribir el archivo). */
    public ListaEnlazada<String[]> getPares() {
        return pares;
    }
}
