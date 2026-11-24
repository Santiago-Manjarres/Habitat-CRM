package servicios;

import datos.BaseDeDatos;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;

/**
 * Cifras de la pantalla "Resumen": indicadores, clientes por etapa e inventario por estado.
 */
public class ServicioResumen {

    private final BaseDeDatos db;

    public ServicioResumen(BaseDeDatos db) {
        this.db = db;
    }

    /** Los cuatro indicadores de arriba del resumen. */
    public ObjetoJson indicadores() {
        // Primera pasada: recorremos las propiedades y contamos
        int disponibles = 0, enVenta = 0;
        double valorEnVenta = 0;
        for (Nodo<Propiedad> n = db.getPropiedades().getPrimero(); n != null; n = n.getSiguiente()) {
            Propiedad p = n.getDato();
            if (p.getEstado().equals("available")) {
                disponibles++;
                if (p.getOperacion().equals("sale")) {
                    enVenta++;
                    valorEnVenta += p.getPrecio();
                }
            }
        }
        // Segunda pasada: recorremos los clientes y contamos por etapa
        int activos = 0, nuevos = 0, ganados = 0, cerrados = 0;
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            String etapa = n.getDato().getEtapa();
            if (Catalogos.estaCerrada(etapa)) cerrados++; else activos++;
            if (etapa.equals("new")) nuevos++;
            if (etapa.equals("closed_won")) ganados++;
        }
        return new ObjetoJson()
                .poner("totalProperties", db.getPropiedades().tamano())
                .poner("availableProperties", disponibles)
                .poner("availableInventoryValue", valorEnVenta)
                .poner("availableForSaleCount", enVenta)
                .poner("activeLeads", activos)
                .poner("newLeads", nuevos)
                .poner("wonDeals", ganados)
                .poner("closedDeals", cerrados)
                // porcentaje de éxito = cerrados ganados / total cerrados (null si todavía no hay cerrados)
                .poner("winRate", cerrados == 0 ? null : (Object) Math.round(ganados * 100.0 / cerrados));
    }

    /** Cuántos clientes hay en cada columna del tablero (para el gráfico de barras). */
    public ListaEnlazada<Object> clientesPorEtapa() {
        int[] conteo = new int[Catalogos.COLUMNAS.length];    // un contador por cada columna
        for (Nodo<Cliente> n = db.getClientes().getPrimero(); n != null; n = n.getSiguiente()) {
            String columna = Catalogos.columnaDeEtapa(n.getDato().getEtapa());
            for (int i = 0; i < conteo.length; i++) {
                if (Catalogos.COLUMNAS[i].equals(columna)) conteo[i]++;
            }
        }
        ListaEnlazada<Object> resultado = new ListaEnlazada<>();
        for (int i = 0; i < conteo.length; i++) {
            resultado.agregar(new ObjetoJson()
                    .poner("key", Catalogos.COLUMNAS[i])
                    .poner("label", Catalogos.NOMBRES_COLUMNAS[i])
                    .poner("count", conteo[i]));
        }
        return resultado;
    }

    /** Cuántas propiedades hay en cada estado y qué porcentaje del total representan. */
    public ListaEnlazada<Object> inventarioPorEstado() {
        int total = db.getPropiedades().tamano();
        ListaEnlazada<Object> resultado = new ListaEnlazada<>();
        for (String estado : Catalogos.ESTADOS_PROPIEDAD) {
            int cantidad = 0;
            for (Nodo<Propiedad> n = db.getPropiedades().getPrimero(); n != null; n = n.getSiguiente()) {
                if (n.getDato().getEstado().equals(estado)) cantidad++;
            }
            resultado.agregar(new ObjetoJson()
                    .poner("status", estado)
                    .poner("count", cantidad)
                    .poner("pct", total == 0 ? 0 : Math.round(cantidad * 100.0 / total)));
        }
        return resultado;
    }
}
