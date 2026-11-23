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
        String[] palabras = Herramientas.normalizar(texto).split("\\s+");
        ListaEnlazada<Propiedad> resultado = new ListaEnlazada<>();
        for (Nodo<Propiedad> n = recientesPrimero().getCabeza(); n != null; n = n.getSiguiente()) {
            String dondeBuscar = Herramientas.normalizar(n.getDato().getTitulo() + " " + n.getDato().getUbicacion());
            boolean coinciden = true;
            for (String palabra : palabras) {
                if (!dondeBuscar.contains(palabra)) {
                    coinciden = false;
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

    /** JSON de una propiedad con el número de clientes interesados (para la interfaz). */
    public ObjetoJson aJsonConClientes(Propiedad p) {
        return p.aJson().poner("linkedLeads", contarClientes(p.getId()));
    }

    public ListaEnlazada<Object> aListaJson(ListaEnlazada<Propiedad> propiedades) {
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        for (Nodo<Propiedad> n = propiedades.getCabeza(); n != null; n = n.getSiguiente()) {
            lista.agregar(aJsonConClientes(n.getDato()));
        }
        return lista;
    }

    public ObjetoJson crear(ObjetoJson datos, String asesorId) {
        ObjetoJson errores = Validador.validarPropiedad(datos);
        if (errores.tamano() > 0) {
            return Herramientas.conErrores(errores);
        }
        Propiedad nueva = Propiedad.nueva(Herramientas.nuevoId("prop"), datos, asesorId, Herramientas.fechaActual());
        db.getPropiedades().agregar(nueva);
        db.guardarPropiedades();
        return Herramientas.exito("property", nueva.aJson());
    }

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
        ListaEnlazada<String> idsClientes = new ListaEnlazada<>();
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getPropiedadId().equals(id)) {
                idsClientes.agregar(n.getDato().getId());
            }
        }
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
