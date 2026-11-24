package vistas;

import datos.EstructuraDeDatos;
import datos.Datos;
import datos.Validador;
import estructuras.Nodo;

/** La sección "Resumen": 4 cifras y dos listas con cantidades. */
public class PaginaResumen {

    public static String html(EstructuraDeDatos estructura, Datos usuario) {
        // 1. Recorremos las propiedades para sumar el valor de las disponibles
        long valorDisponible = 0;
        for (Nodo<Datos> n = estructura.propiedades.getCabeza(); n != null; n = n.siguiente) {
            if (n.dato.get("estado").equals("Disponible")) {
                valorDisponible += Validador.leerNumero(n.dato.get("precio"));
            }
        }
        int disponibles = estructura.contar(estructura.propiedades, "estado", "Disponible");
        int cerrados = estructura.contar(estructura.clientes, "etapa", "Cerrado");

        // 2. Las 4 cifras de arriba
        String cifras = "<div class=\"kpi-grid\">"
                + cifra("Propiedades disponibles", disponibles + "", "de " + estructura.propiedades.tamano() + " en el inventario")
                + cifra("Valor de lo disponible", Plantilla.dinero(valorDisponible + ""), "suma de sus precios")
                + cifra("Clientes en seguimiento", (estructura.clientes.tamano() - cerrados) + "", "sin contar los cerrados")
                + cifra("Negocios cerrados", cerrados + "", "de " + estructura.clientes.tamano() + " clientes")
                + "</div>";

        // 3. Cuántos clientes hay en cada etapa y cuántas propiedades en cada estado
        String etapas = "";
        for (String etapa : EstructuraDeDatos.ETAPAS) {
            etapas += fila(etapa, estructura.contar(estructura.clientes, "etapa", etapa));
        }
        String estados = "";
        for (String estado : EstructuraDeDatos.ESTADOS) {
            estados += fila(estado, estructura.contar(estructura.propiedades, "estado", estado));
        }

        String boton = "<a class=\"btn btn--primary\" href=\"/clientes/nuevo\">" + Plantilla.icono("plus") + "Nuevo cliente</a>";
        String contenido = "<section class=\"page\">" + Plantilla.seccion("Resumen", "Cómo va el negocio hoy", boton)
                + "<div class=\"section-stack\">" + cifras + "<div class=\"dash-grid\">"
                + panel("Clientes por etapa", etapas, "/clientes")
                + panel("Propiedades por estado", estados, "/propiedades")
                + "</div></div></section>";
        return Plantilla.pagina("Resumen", usuario, "resumen", contenido);
    }

    private static String cifra(String nombre, String valor, String ayuda) {
        return "<article class=\"kpi\"><h2 class=\"kpi__label\">" + nombre + "</h2>"
                + "<p class=\"kpi__value\">" + valor + "</p><p class=\"kpi__hint\">" + ayuda + "</p></article>";
    }

    private static String fila(String texto, int cantidad) {
        return "<li class=\"meter\"><span class=\"meter__top\">" + Plantilla.insignia(texto) + "<strong>" + cantidad + "</strong></span></li>";
    }

    private static String panel(String titulo, String filas, String enlace) {
        return "<section class=\"card panel\"><header class=\"panel__header\"><h2 class=\"panel__title\">" + titulo + "</h2>"
                + "<a class=\"link-btn\" href=\"" + enlace + "\">Ver todo</a></header>"
                + "<ul class=\"meter-list\" role=\"list\">" + filas + "</ul></section>";
    }
}
