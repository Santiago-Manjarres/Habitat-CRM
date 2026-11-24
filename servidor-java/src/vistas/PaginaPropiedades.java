package vistas;

import datos.EstructuraDeDatos;
import datos.Datos;
import estructuras.Nodo;

/** La sección "Propiedades": la tabla con buscador y el formulario para crear o editar. */
public class PaginaPropiedades {

    /** La tabla. Si "buscar" tiene texto, solo muestra las propiedades cuyo título o ubicación lo contienen. */
    public static String lista(EstructuraDeDatos estructura, Datos usuario, String buscar) {
        String filas = "";
        int encontradas = 0;
        for (Nodo<Datos> n = estructura.propiedades.getCabeza(); n != null; n = n.siguiente) {
            Datos p = n.dato;
            String dondeBuscar = (p.get("titulo") + " " + p.get("ubicacion")).toLowerCase();
            if (dondeBuscar.contains(buscar.toLowerCase())) {          // con "buscar" vacío, todas coinciden
                filas += fila(p);
                encontradas++;
            }
        }
        String buscador = "<form method=\"get\" action=\"/propiedades\" class=\"search-form\">"
                + "<div class=\"field search-field\"><label class=\"field__label\" for=\"buscar\">Buscar propiedad</label>"
                + "<input class=\"field__control\" id=\"buscar\" name=\"buscar\" placeholder=\"Título o ubicación\" value=\""
                + Plantilla.escapar(buscar) + "\"></div>"
                + "<button type=\"submit\" class=\"btn btn--secondary\">Buscar</button></form>";
        String nueva = "<a class=\"btn btn--primary\" href=\"/propiedades/nueva\">" + Plantilla.icono("plus") + "Nueva propiedad</a>";
        String tabla = encontradas == 0
                ? "<p class=\"text-muted\">No hay propiedades para mostrar.</p>"
                : "<div class=\"card table-card\"><table class=\"data-table\"><thead><tr><th>Propiedad</th><th>Tipo</th>"
                + "<th>Operación</th><th class=\"is-numeric\">Precio</th><th>Estado</th><th></th></tr></thead>"
                + "<tbody>" + filas + "</tbody></table></div>";

        String contenido = "<section class=\"page\">" + Plantilla.seccion("Propiedades", "Los inmuebles que ofrece la inmobiliaria", "")
                + "<div class=\"section-stack\"><div class=\"toolbar\">" + buscador + nueva + "</div>"
                + "<p class=\"results-count\">" + encontradas + " propiedades</p>" + tabla + "</div></section>";
        return Plantilla.pagina("Propiedades", usuario, "propiedades", contenido);
    }

    /** Una fila de la tabla. */
    private static String fila(Datos p) {
        String foto = p.get("foto").isEmpty() ? "/assets/img/property-placeholder.svg" : p.get("foto");
        return "<tr><td class=\"is-full\"><span class=\"property-cell\">"
                + "<img class=\"property-cell__thumb\" src=\"" + Plantilla.escapar(foto) + "\" alt=\"\" width=\"48\" height=\"36\">"
                + "<span class=\"property-cell__text\"><span class=\"property-cell__title\">" + Plantilla.escapar(p.get("titulo")) + "</span>"
                + "<span class=\"text-sm text-muted\">" + Plantilla.escapar(p.get("ubicacion")) + "</span></span></span></td>"
                + "<td data-label=\"Tipo\">" + Plantilla.escapar(p.get("tipo")) + "</td>"
                + "<td data-label=\"Operación\">" + Plantilla.escapar(p.get("operacion")) + "</td>"
                + "<td data-label=\"Precio\" class=\"is-numeric\">" + Plantilla.dinero(p.get("precio")) + "</td>"
                + "<td data-label=\"Estado\">" + Plantilla.insignia(p.get("estado")) + "</td>"
                + "<td class=\"is-full\"><a class=\"btn btn--secondary btn--sm\" href=\"/propiedades/editar?id=" + p.get("id") + "\">Editar</a></td></tr>";
    }

    /**
     * El formulario para crear (editar = false) o editar (editar = true) una propiedad.
     * @param valores lo que va escrito en cada campo
     * @param errores los mensajes del Validador (vacío si no hay errores)
     */
    public static String formulario(Datos usuario, Datos valores, Datos errores, boolean editar) {
        String accion = editar ? "/propiedades/editar" : "/propiedades/nueva";
        String eliminar = !editar ? ""
                : "<form method=\"post\" action=\"/propiedades/eliminar\"><input type=\"hidden\" name=\"id\" value=\"" + Plantilla.escapar(valores.get("id")) + "\">"
                + "<button type=\"submit\" class=\"btn btn--danger\">Eliminar propiedad</button></form>";
        String contenido = "<section class=\"page\">" + Plantilla.seccion(editar ? "Editar propiedad" : "Nueva propiedad", "Llena los datos y guarda.", "")
                + "<div class=\"card form-card\">"
                + "<form method=\"post\" action=\"" + accion + "\" class=\"form-grid\" novalidate>"
                + "<input type=\"hidden\" name=\"id\" value=\"" + Plantilla.escapar(valores.get("id")) + "\">"      // el id viaja escondido
                + Plantilla.campo("titulo", "Título", "text", valores, errores)
                + Plantilla.lista("tipo", "Tipo de inmueble", EstructuraDeDatos.TIPOS, valores)
                + Plantilla.lista("operacion", "Operación", EstructuraDeDatos.OPERACIONES, valores)
                + Plantilla.campo("precio", "Precio", "text", valores, errores)
                + Plantilla.campo("ubicacion", "Ubicación", "text", valores, errores)
                + Plantilla.lista("estado", "Estado", EstructuraDeDatos.ESTADOS, valores)
                + Plantilla.campo("foto", "Enlace de la foto (opcional)", "url", valores, errores)
                + "<div class=\"form-botones\"><button type=\"submit\" class=\"btn btn--primary\">Guardar</button>"
                + "<a class=\"btn btn--secondary\" href=\"/propiedades\">Cancelar</a></div>"
                + "</form>" + eliminar + "</div></section>";
        return Plantilla.pagina("Propiedades", usuario, "propiedades", contenido);
    }
}
