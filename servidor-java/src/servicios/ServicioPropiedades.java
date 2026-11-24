package servicios;

import datos.BaseDeDatos;
import estructuras.ArbolBinarioBusqueda;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;

/**
 * Inventario de propiedades: listar, buscar, crear, editar, cambiar estado y eliminar.
 *
 * Patrón que siguen crear, actualizar y eliminar:
 *  1. validar los datos  ->  2. cambiar la tabla en memoria  ->  3. guardar el archivo .json
 */
public class ServicioPropiedades {

    private final BaseDeDatos db;

    public ServicioPropiedades(BaseDeDatos db) {
        this.db = db;
    }

    /**
     * Propiedades de la más nueva a la más antigua.
     * 1) Se insertan en un árbol binario usando la fecha de creación como clave.
     * 2) El recorrido en orden las da de la más antigua a la más nueva.
     * 3) Una pila las invierte para que la más nueva quede primero.
     */
    public ListaEnlazada<Propiedad> recientesPrimero() {
        ArbolBinarioBusqueda<Propiedad> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Propiedad> n = db.getPropiedades().getPrimero(); n != null; n = n.getSiguiente()) {
            Propiedad p = n.getDato();
            arbol.insertar(p.getFechaCreacion() + p.getId(), p);
        }
        return Herramientas.invertir(arbol.recorrerEnOrden());
    }

    /** Busca por título o ubicación, sin importar tildes ni mayúsculas. Todas las palabras deben aparecer. */
    public ListaEnlazada<Propiedad> buscar(String texto) {
        // Separamos lo que escribió la persona en palabras: "casa bogota" -> ["casa", "bogota"]
        String[] palabras = Herramientas.normalizar(texto).split("\\s+");
        ListaEnlazada<Propiedad> resultado = new ListaEnlazada<>();
        for (Nodo<Propiedad> n = recientesPrimero().getCabeza(); n != null; n = n.getSiguiente()) {
            // El texto donde se busca es el título y la ubicación juntos
            String dondeBuscar = Herramientas.normalizar(n.getDato().getTitulo() + " " + n.getDato().getUbicacion());
            boolean coinciden = true;
            for (String palabra : palabras) {
                if (!dondeBuscar.contains(palabra)) {
                    coinciden = false;      // si falta una sola palabra, la propiedad no sirve
                }
            }
            if (coinciden) {
                resultado.agregar(n.getDato());
            }
        }
        return resultado;
    }

    /** Cuántos clientes están interesados en una propiedad. */
    public int contarClientes(String propiedadId) {
        int cantidad = 0;
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getPropiedadId().equals(propiedadId)) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /** Crea una propiedad nueva. */
    public ObjetoJson crear(ObjetoJson datos, String asesorId) {
        ObjetoJson errores = Validador.validarPropiedad(datos);          // 1. validar
        if (errores.tamano() > 0) {
            return Herramientas.conErrores(errores);                     //    hay errores: se devuelven y no se guarda nada
        }
        Propiedad nueva = Propiedad.nueva(Herramientas.nuevoId("prop"), datos, asesorId, Herramientas.fechaActual());
        db.getPropiedades().agregar(nueva);                              // 2. agregar a la tabla en memoria
        db.guardarPropiedades();                                         // 3. escribir properties.json
        return Herramientas.exito("property", nueva.aJson());
    }

    /** Edita una propiedad que ya existe. */
    public ObjetoJson actualizar(String id, ObjetoJson datos) {
        Propiedad propiedad = db.getPropiedades().buscarPorId(id);
        if (propiedad == null) {
            return Herramientas.conErrores(new ObjetoJson().poner("_form", "La propiedad ya no existe. Puede que otro asesor la haya eliminado"));
        }
        ObjetoJson errores = Validador.validarPropiedad(datos);
        if (errores.tamano() > 0) {
            return Herramientas.conErrores(errores);
        }
        propiedad.copiarDatos(datos);
        db.guardarPropiedades();
        return Herramientas.exito("property", propiedad.aJson());
    }

    /** Cambia solo el estado (disponible, en negociación o vendida). */
    public ObjetoJson cambiarEstado(String id, String estado) {
        if (!Herramientas.estaEn(estado, Catalogos.ESTADOS_PROPIEDAD)) {
            return Herramientas.fallo("Estado no válido");
        }
        Propiedad propiedad = db.getPropiedades().buscarPorId(id);
        if (propiedad == null) {
            return Herramientas.fallo("La propiedad ya no existe");
        }
        propiedad.setEstado(estado);
        db.guardarPropiedades();
        return Herramientas.exito("property", propiedad.aJson());
    }

    /** Elimina una propiedad y también los clientes interesados en ella. */
    public ObjetoJson eliminar(String id) {
        if (!db.getPropiedades().existe(id)) {
            return Herramientas.fallo("La propiedad ya no existe");
        }
        // 1. primero anotamos los ids de los clientes de esta propiedad
        //    (no se borra mientras se recorre la lista, para no romper el recorrido)
        ListaEnlazada<String> idsClientes = new ListaEnlazada<>();
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getPropiedadId().equals(id)) {
                idsClientes.agregar(n.getDato().getId());
            }
        }
        // 2. después eliminamos esos clientes uno por uno
        for (Nodo<String> n = idsClientes.getCabeza(); n != null; n = n.getSiguiente()) {
            db.getClientes().eliminarPorId(n.getDato());
        }
        if (!idsClientes.estaVacia()) {
            db.guardarClientes();
        }
        db.getPropiedades().eliminarPorId(id);
        db.guardarPropiedades();
        return Herramientas.exito("removedLeads", idsClientes.tamano());
    }
}
