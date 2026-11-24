package vistas;

import datos.BaseDeDatos;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;
import modelo.Usuario;
import servicios.ServicioClientes;
import servicios.ServicioResumen;

/**
 * La sección "Resumen": los 4 indicadores, el gráfico de clientes por etapa,
 * el inventario por estado y los últimos movimientos.
 * Todas las cifras las calcula ServicioResumen; aquí solo se convierten en HTML.
 */
public class PaginaResumen {

    private PaginaResumen() {
    }

    public static String contenido(BaseDeDatos db, ServicioResumen resumen, ServicioClientes clientes) {
        String boton = "<a class=\"btn btn--primary\" href=\"/clientes/nuevo\">" + Html.icono("plus", "") + "Nuevo cliente</a>";
        String cuerpo = "<div class=\"section-stack\">"
                + indicadores(resumen.indicadores(), clientes.porContactar().tamano())
                + "<div class=\"dash-grid\">"
                + panel("Clientes por etapa", grafico(resumen.clientesPorEtapa()), "panel--chart", "/clientes", "Ver clientes")
                + panel("Inventario por estado", inventario(resumen.inventarioPorEstado()), "", "/propiedades", "Ver propiedades")
                + "</div>"
                + panel("Últimos movimientos", ultimos(db, clientes.ultimosMovimientos(5)), "panel--table", "/clientes", "Ver todos")
                + "</div>";
        return Plantilla.seccion("Resumen", "Cómo va el negocio hoy", boton, cuerpo);
    }

    /** Un panel: título con un enlace a la derecha y su contenido debajo. */
    private static String panel(String titulo, String cuerpo, String clase, String enlace, String textoEnlace) {
        return "<section class=\"card panel " + clase + "\">"
                + "<header class=\"panel__header\"><h2 class=\"panel__title\">" + titulo + "</h2>"
                + "<a class=\"link-btn\" href=\"" + enlace + "\">" + textoEnlace + "</a></header>"
                + cuerpo + "</section>";
    }

    /** Una tarjeta de indicador: nombre, cifra grande y una línea de ayuda. */
    private static String indicador(String nombre, String valor, String ayuda) {
        return "<article class=\"kpi\"><h2 class=\"kpi__label\">" + nombre + "</h2>"
                + "<p class=\"kpi__value\">" + Html.escapar(valor) + "</p>"
                + "<p class=\"kpi__hint\">" + Html.escapar(ayuda) + "</p></article>";
    }

    /**
     * Los 4 indicadores de arriba.
     * @param porContactar tamaño de la cola de clientes nuevos que nadie ha llamado
     */
    private static String indicadores(ObjetoJson k, int porContactar) {
        double cerrados = k.numero("closedDeals");
        String tasa = k.obtener("winRate") == null ? "—" : Formato.numero(k.numero("winRate")) + " %";
        return "<div class=\"kpi-grid\">"
                + indicador("Propiedades disponibles", Formato.numero(k.numero("availableProperties")),
                        "de " + Formato.plural(k.numero("totalProperties"), "propiedad", "propiedades") + " en el inventario")
                + indicador("Valor del inventario en venta", Formato.dineroCorto(k.numero("availableInventoryValue")),
                        Formato.dinero(k.numero("availableInventoryValue")) + " en "
                                + Formato.plural(k.numero("availableForSaleCount"), "inmueble", "inmuebles"))
                + indicador("Clientes en seguimiento", Formato.numero(k.numero("activeLeads")),
                        porContactar > 0 ? Formato.plural(porContactar, "nuevo", "nuevos") + " por contactar" : "Todos contactados")
                + indicador("Tasa de cierre", tasa,
                        cerrados > 0 ? Formato.plural(k.numero("wonDeals"), "negocio cerrado", "negocios cerrados")
                                + " de " + Formato.plural(cerrados, "terminado", "terminados") : "Aún no hay negocios terminados")
                + "</div>";
    }

    /**
     * Gráfico de barras dibujado con SVG: una barra por etapa.
     * La altura de cada barra es proporcional a su cantidad comparada con la más alta.
     */
    private static String grafico(ListaEnlazada<Object> etapas) {
        final int ancho = 680, alto = 220, barra = 40, arriba = 24;
        // 1. buscamos la cantidad más grande (la barra más alta ocupa toda la altura)
        double mayor = 1;
        for (Nodo<Object> n = etapas.getCabeza(); n != null; n = n.getSiguiente()) {
            mayor = Math.max(mayor, ((ObjetoJson) n.getDato()).numero("count"));
        }
        double paso = (double) ancho / etapas.tamano();      // espacio horizontal de cada barra
        StringBuilder barras = new StringBuilder();
        StringBuilder resumen = new StringBuilder();
        boolean destacada = false;
        int i = 0;
        // 2. dibujamos cada barra: el fondo gris, la barra azul, el número y el nombre
        for (Nodo<Object> n = etapas.getCabeza(); n != null; n = n.getSiguiente(), i++) {
            ObjetoJson etapa = (ObjetoJson) n.getDato();
            double cantidad = etapa.numero("count");
            int h = (int) Math.round((alto - arriba - 28) * cantidad / mayor);
            int x = (int) Math.round(i * paso + (paso - barra) / 2);
            int y = alto - 28 - h;
            String clase = "chart__bar";
            if (!destacada && cantidad == mayor && cantidad > 0) {      // la barra más alta va en azul fuerte
                clase += " chart__bar--top";
                destacada = true;
            }
            barras.append("<rect class=\"chart__track\" x=\"").append(x).append("\" y=\"").append(arriba)
                    .append("\" width=\"").append(barra).append("\" height=\"").append(alto - 28 - arriba).append("\" rx=\"6\"></rect>")
                    .append("<rect class=\"").append(clase).append("\" x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" width=\"").append(barra).append("\" height=\"").append(Math.max(h, 0)).append("\" rx=\"6\"></rect>")
                    .append("<text class=\"chart__value\" x=\"").append(x + barra / 2).append("\" y=\"").append(Math.max(y - 8, 14))
                    .append("\" text-anchor=\"middle\">").append(Formato.numero(cantidad)).append("</text>")
                    .append("<text class=\"chart__label\" x=\"").append(x + barra / 2).append("\" y=\"").append(alto - 6)
                    .append("\" text-anchor=\"middle\">").append(Html.escapar(etapa.texto("label"))).append("</text>");
            resumen.append(i > 0 ? ", " : "").append(etapa.texto("label")).append(": ").append(Formato.numero(cantidad));
        }
        // aria-label: un lector de pantalla lee el gráfico como texto
        return "<svg class=\"chart\" viewBox=\"0 0 " + ancho + " " + alto + "\" role=\"img\" aria-label=\"Clientes por etapa. "
                + Html.escapar(resumen.toString()) + "\">" + barras + "</svg>";
    }

    /** Inventario por estado: una etiqueta, la cantidad y una barrita con el porcentaje. */
    private static String inventario(ListaEnlazada<Object> estados) {
        StringBuilder filas = new StringBuilder();
        for (Nodo<Object> n = estados.getCabeza(); n != null; n = n.getSiguiente()) {
            ObjetoJson e = (ObjetoJson) n.getDato();
            String estado = e.texto("status");
            String tono = Etiquetas.tonoPropiedad(estado);
            filas.append("<li class=\"meter\"><span class=\"meter__top\">")
                    .append(Html.insignia(Etiquetas.estadoPropiedad(estado), tono))
                    .append("<strong class=\"tabular\">").append(Formato.numero(e.numero("count"))).append("</strong></span>")
                    .append("<svg class=\"meter__bar\" viewBox=\"0 0 100 6\" preserveAspectRatio=\"none\" aria-hidden=\"true\">")
                    .append("<rect class=\"meter__track\" width=\"100\" height=\"6\"></rect>")
                    .append("<rect class=\"meter__fill meter__fill--").append(tono).append("\" width=\"")
                    .append(Formato.decimal(e.numero("pct"))).append("\" height=\"6\"></rect></svg></li>");
        }
        return "<ul class=\"meter-list\" role=\"list\">" + filas + "</ul>";
    }

    /** Tabla con los 5 clientes que se movieron más recientemente. */
    private static String ultimos(BaseDeDatos db, ListaEnlazada<Cliente> lista) {
        if (lista.estaVacia()) {
            return "<p class=\"text-muted\">Aún no hay clientes. Registra el primero con «Nuevo cliente».</p>";
        }
        StringBuilder filas = new StringBuilder();
        for (Nodo<Cliente> n = lista.getCabeza(); n != null; n = n.getSiguiente()) {
            Cliente c = n.getDato();
            Propiedad p = db.getPropiedades().buscarPorId(c.getPropiedadId());   // búsqueda en la tabla hash por id
            Usuario a = db.getUsuarios().buscarPorId(c.getAsesorId());
            filas.append("<tr><td class=\"is-full\"><span class=\"contact-cell\">")
                    .append("<span class=\"avatar avatar--sm\" aria-hidden=\"true\">").append(Html.escapar(Formato.iniciales(c.getNombre()))).append("</span>")
                    .append("<span class=\"property-cell__text\"><span class=\"property-cell__title\">").append(Html.escapar(c.getNombre())).append("</span>")
                    .append("<span class=\"text-sm text-muted\">").append(Html.escapar(Etiquetas.origen(c.getOrigen()))).append("</span></span></span></td>")
                    .append("<td data-label=\"Propiedad\">").append(Html.escapar(p == null ? "Propiedad eliminada" : p.getTitulo())).append("</td>")
                    .append("<td data-label=\"Asesor\">").append(Html.escapar(a == null ? "—" : a.getNombre())).append("</td>")
                    .append("<td data-label=\"Etapa\">").append(Html.insignia(Etiquetas.etapa(c.getEtapa()), Etiquetas.tonoEtapa(c.getEtapa()))).append("</td>")
                    .append("<td data-label=\"Actualizado\" class=\"text-muted\">").append(Formato.tiempoRelativo(c.getFechaActualizacion())).append("</td>")
                    .append("<td class=\"is-full\"><span class=\"data-table__actions\">")
                    .append("<a class=\"btn btn--ghost btn--sm\" href=\"/clientes/").append(Html.escapar(c.getId())).append("\">Ver ficha</a></span></td></tr>");
        }
        return "<table class=\"data-table\"><caption class=\"visually-hidden\">Clientes con actividad reciente</caption>"
                + "<thead><tr><th scope=\"col\">Cliente</th><th scope=\"col\">Propiedad</th><th scope=\"col\">Asesor</th>"
                + "<th scope=\"col\">Etapa</th><th scope=\"col\">Actualizado</th><th scope=\"col\"><span class=\"visually-hidden\">Acciones</span></th></tr></thead>"
                + "<tbody>" + filas + "</tbody></table>";
    }
}
