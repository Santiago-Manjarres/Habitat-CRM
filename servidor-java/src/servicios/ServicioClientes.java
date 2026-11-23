package servicios;

import datos.BaseDeDatos;
import estructuras.ArbolBinarioBusqueda;
import estructuras.Cola;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;
import modelo.Usuario;

/**
 * Clientes interesados: crear, editar, cambiar de etapa, eliminar y armar el tablero.
 */
public class ServicioClientes {

    private final BaseDeDatos db;

    public ServicioClientes(BaseDeDatos db) {
        this.db = db;
    }

    /** Cliente con su asesor y un resumen de la propiedad, listo para mostrarlo. */
    public ObjetoJson conDetalles(Cliente c) {
        Usuario asesor = db.getUsuarios().buscarPorId(c.getAsesorId());
        Propiedad propiedad = db.getPropiedades().buscarPorId(c.getPropiedadId());
        ObjetoJson json = c.aJson();
        json.poner("agent", asesor == null ? null : new ObjetoJson()
                .poner("id", asesor.getId())
                .poner("name", asesor.getNombre())
                .poner("avatar", asesor.getAvatar()));
        json.poner("property", propiedad == null ? null : propiedad.aJsonResumen());
        return json;
    }

    public ListaEnlazada<Object> aListaJson(ListaEnlazada<Cliente> clientes) {
        ListaEnlazada<Object> lista = new ListaEnlazada<>();
        for (Nodo<Cliente> n = clientes.getCabeza(); n != null; n = n.getSiguiente()) {
            lista.agregar(conDetalles(n.getDato()));
        }
        return lista;
    }

    /**
     * Clientes del cambio más reciente al más antiguo: árbol binario ordenado
     * por fecha de actualización y una pila para invertir el orden.
     */
    public ListaEnlazada<Cliente> recientesPrimero() {
        ArbolBinarioBusqueda<Cliente> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            Cliente c = n.getDato();
            arbol.insertar(c.getFechaActualizacion() + c.getId(), c);
        }
        return Herramientas.invertir(arbol.recorrerEnOrden());
    }

    /** Los últimos movimientos del resumen: los primeros "cantidad" clientes más recientes. */
    public ListaEnlazada<Cliente> ultimosMovimientos(int cantidad) {
        ListaEnlazada<Cliente> resultado = new ListaEnlazada<>();
        Nodo<Cliente> n = recientesPrimero().getCabeza();
        while (n != null && resultado.tamano() < cantidad) {
            resultado.agregar(n.getDato());
            n = n.getSiguiente();
        }
        return resultado;
    }

    /**
     * Clientes nuevos que todavía nadie ha llamado, en una cola:
     * el que llegó primero queda al frente y es el primero que hay que contactar.
     */
    public Cola<Cliente> porContactar() {
        ArbolBinarioBusqueda<Cliente> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            Cliente c = n.getDato();
            if (c.getEtapa().equals("new")) {
                arbol.insertar(c.getFechaCreacion() + c.getId(), c);
            }
        }
        Cola<Cliente> cola = new Cola<>();
        for (Nodo<Cliente> n = arbol.recorrerEnOrden().getCabeza(); n != null; n = n.getSiguiente()) {
            cola.encolar(n.getDato());
        }
        return cola;
    }

    /**
     * Tablero de clientes: un arreglo con una lista enlazada por columna
     * (Nuevo, Contactado, Visita, Negociación, Cerrado).
     */
    @SuppressWarnings("unchecked")
    public ListaEnlazada<Object> tablero() {
        ListaEnlazada<Cliente>[] columnas = new ListaEnlazada[Catalogos.COLUMNAS.length];
        for (int i = 0; i < columnas.length; i++) {
            columnas[i] = new ListaEnlazada<>();
        }
        for (Nodo<Cliente> n = recientesPrimero().getCabeza(); n != null; n = n.getSiguiente()) {
            String columna = Catalogos.columnaDeEtapa(n.getDato().getEtapa());
            for (int i = 0; i < columnas.length; i++) {
                if (Catalogos.COLUMNAS[i].equals(columna)) {
                    columnas[i].agregar(n.getDato());
                }
            }
        }
        ListaEnlazada<Object> resultado = new ListaEnlazada<>();
        for (int i = 0; i < columnas.length; i++) {
            resultado.agregar(new ObjetoJson()
                    .poner("key", Catalogos.COLUMNAS[i])
                    .poner("label", Catalogos.NOMBRES_COLUMNAS[i])
                    .poner("leads", aListaJson(columnas[i])));
        }
        return resultado;
    }

    /** Valida el formulario y revisa que la propiedad y el asesor existan. */
    public ObjetoJson revisar(ObjetoJson datos) {
        ObjetoJson errores = Validador.validarCliente(datos);
        if (errores.tamano() > 0) {
            return errores;
        }
        if (!db.getPropiedades().existe(datos.texto("propertyId"))) {
            errores.poner("propertyId", "Esa propiedad ya no existe. Elige otra");
        } else if (!db.getUsuarios().existe(datos.texto("agentId"))) {
            errores.poner("agentId", "Ese asesor ya no existe. Elige otro");
        }
        return errores;
    }

    public ObjetoJson crear(ObjetoJson datos) {
        ObjetoJson errores = revisar(datos);
        if (errores.tamano() > 0) {
            return Herramientas.conErrores(errores);
        }
        Cliente nuevo = Cliente.nuevo(Herramientas.nuevoId("lead"), datos, Herramientas.fechaActual());
        db.getClientes().agregar(nuevo);
        db.guardarClientes();
        return Herramientas.exito("lead", nuevo.aJson());
    }

    public ObjetoJson actualizar(String id, ObjetoJson datos) {
        Cliente cliente = db.getClientes().buscarPorId(id);
        if (cliente == null) {
            return Herramientas.conErrores(new ObjetoJson().poner("_form", "Este cliente ya no existe. Puede que otro asesor lo haya eliminado"));
        }
        ObjetoJson errores = revisar(datos);
        if (errores.tamano() > 0) {
            return Herramientas.conErrores(errores);
        }
        cliente.copiarDatos(datos);
        cliente.setFechaActualizacion(Herramientas.fechaActual());
        db.guardarClientes();
        return Herramientas.exito("lead", cliente.aJson());
    }

    public ObjetoJson cambiarEtapa(String id, String etapa) {
        if (!Herramientas.estaEn(etapa, Catalogos.ETAPAS)) {
            return Herramientas.fallo("Etapa no válida");
        }
        Cliente cliente = db.getClientes().buscarPorId(id);
        if (cliente == null) {
            return Herramientas.fallo("Este cliente ya no existe. Puede que otro asesor lo haya eliminado");
        }
        cliente.setEtapa(etapa);
        cliente.setFechaActualizacion(Herramientas.fechaActual());
        db.guardarClientes();
        return Herramientas.exito("lead", cliente.aJson());
    }

    public ObjetoJson eliminar(String id) {
        if (!db.getClientes().eliminarPorId(id)) {
            return Herramientas.fallo("Este cliente ya no existe");
        }
        db.guardarClientes();
        return new ObjetoJson().poner("ok", true);
    }
}
