package servicios;

import datos.BaseDeDatos;
import estructuras.ArbolBinarioBusqueda;
import estructuras.Cola;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;

/**
 * Clientes interesados: crear, editar, cambiar de etapa, eliminar y armar el tablero.
 *
 * Aquí se usan casi todas las estructuras: árbol (ordenar por fecha), pila
 * (invertir el orden), cola (clientes por contactar) y arreglo de listas (tablero).
 */
public class ServicioClientes {

    private final BaseDeDatos db;

    public ServicioClientes(BaseDeDatos db) {
        this.db = db;
    }

    /**
     * Clientes del cambio más reciente al más antiguo: árbol binario ordenado
     * por fecha de actualización y una pila para invertir el orden.
     */
    public ListaEnlazada<Cliente> recientesPrimero() {
        ArbolBinarioBusqueda<Cliente> arbol = new ArbolBinarioBusqueda<>();
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            Cliente c = n.getDato();
            // clave = fecha + id (el id evita que dos clientes con la misma fecha se pisen)
            arbol.insertar(c.getFechaActualizacion() + c.getId(), c);
        }
        // el árbol los entrega del más antiguo al más nuevo; la pila los invierte
        return Herramientas.invertir(arbol.recorrerEnOrden());
    }

    /** Los últimos movimientos del resumen: los primeros "cantidad" clientes más recientes. */
    public ListaEnlazada<Cliente> ultimosMovimientos(int cantidad) {
        ListaEnlazada<Cliente> resultado = new ListaEnlazada<>();
        Nodo<Cliente> n = recientesPrimero().getCabeza();
        // nos detenemos cuando ya juntamos la cantidad pedida
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
            if (c.getEtapa().equals("new")) {       // solo los clientes en etapa "nuevo"
                arbol.insertar(c.getFechaCreacion() + c.getId(), c);   // ordenados por fecha de creación
            }
        }
        // Pasamos del más antiguo al más nuevo a la cola: el más antiguo queda al frente
        Cola<Cliente> cola = new Cola<>();
        for (Nodo<Cliente> n = arbol.recorrerEnOrden().getCabeza(); n != null; n = n.getSiguiente()) {
            cola.encolar(n.getDato());
        }
        return cola;
    }

    /**
     * Tablero de clientes: un arreglo con una lista enlazada por columna
     * (Nuevo, Contactado, Visita, Negociación, Cerrado).
     * La columna "Nuevo" se llena desde la cola de clientes por contactar:
     * el primero de la columna es el que lleva más tiempo esperando una llamada.
     */
    @SuppressWarnings("unchecked")
    public ListaEnlazada<Cliente>[] columnas() {
        // 1. un arreglo con 5 posiciones; cada posición guarda la lista de clientes de esa columna
        ListaEnlazada<Cliente>[] columnas = new ListaEnlazada[Catalogos.COLUMNAS.length];
        for (int i = 0; i < columnas.length; i++) {
            columnas[i] = new ListaEnlazada<>();
        }
        // 2. columna 0 (Nuevo): sacamos los clientes de la cola, del que llegó primero al último
        Cola<Cliente> cola = porContactar();
        while (!cola.estaVacia()) {
            columnas[0].agregar(cola.desencolar());
        }
        // 3. las demás columnas: repartimos cada cliente (del más reciente al más antiguo) en la suya
        for (Nodo<Cliente> n = recientesPrimero().getCabeza(); n != null; n = n.getSiguiente()) {
            String columna = Catalogos.columnaDeEtapa(n.getDato().getEtapa());
            for (int i = 1; i < columnas.length; i++) {
                if (Catalogos.COLUMNAS[i].equals(columna)) {
                    columnas[i].agregar(n.getDato());
                }
            }
        }
        return columnas;
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

    /** Crea un cliente nuevo: validar, agregar a la tabla y guardar leads.json. */
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

    /** Edita un cliente y actualiza su fecha de último cambio. */
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

    /** Mueve al cliente a otra etapa del embudo. */
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

    /** Elimina un cliente de la tabla y guarda el archivo. */
    public ObjetoJson eliminar(String id) {
        if (!db.getClientes().eliminarPorId(id)) {
            return Herramientas.fallo("Este cliente ya no existe");
        }
        db.guardarClientes();
        return new ObjetoJson().poner("ok", true);
    }
}
