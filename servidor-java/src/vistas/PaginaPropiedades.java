package vistas;

import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Propiedad;
import servicios.Catalogos;

/**
 * La sección "Propiedades": buscador, tabla del inventario, formulario para
 * crear o editar, y la ventana para confirmar una eliminación.
 */
public class PaginaPropiedades {

    private PaginaPropiedades() {
    }

    /**
     * La tabla de propiedades.
     * @param filas    las propiedades a mostrar (todas, o el resultado de la búsqueda)
     * @param total    cuántas propiedades hay en el inventario
     * @param busqueda lo que se escribió en el buscador ("" si no se buscó nada)
     */
    public static String contenido(ListaEnlazada<Propiedad> filas, int total, String busqueda) {
        String nueva = "<a class=\"btn btn--primary\" href=\"/propiedades/nueva\">" + Html.icono("plus", "") + "Nueva propiedad</a>";
        // El buscador es un formulario GET: al presionar Enter la dirección queda /propiedades?q=texto
        String buscador = "<form method=\"get\" action=\"/propiedades\" class=\"search-form\" role=\"search\">"
                + "<div class=\"field search-field\">"
                + "<label class=\"field__label\" for=\"buscar-propiedad\">Buscar propiedad</label>"
                + "<div class=\"field__affix field__affix--prefix\"><span class=\"field__prefix\">" + Html.icono("search", "icon--sm") + "</span>"
                + "<input class=\"field__control\" id=\"buscar-propiedad\" name=\"q\" type=\"search\" placeholder=\"Título o ubicación\" autocomplete=\"off\" value=\""
                + Html.escapar(busqueda) + "\"></div></div>"
                + "<button type=\"submit\" class=\"btn btn--secondary\">Buscar</button></form>";

        String tabla;
        if (total == 0) {
            tabla = Html.vacio("building", "Aún no hay propiedades", "Registra la primera para empezar a asociarle clientes interesados.",
                    "<a class=\"btn btn--primary\" href=\"/propiedades/nueva\">" + Html.icono("plus", "") + "Crear propiedad</a>");
        } else if (filas.estaVacia()) {
            tabla = Html.vacio("search", "Sin resultados", "Ninguna propiedad coincide con la búsqueda.",
                    "<a class=\"btn btn--secondary\" href=\"/propiedades\">Limpiar búsqueda</a>");
        } else {
            StringBuilder html = new StringBuilder();
            for (Nodo<Propiedad> n = filas.getCabeza(); n != null; n = n.getSiguiente()) {
                html.append(fila(n.getDato()));
            }
            tabla = "<div class=\"card table-card\"><table class=\"data-table\">"
                    + "<caption class=\"visually-hidden\">Inventario de propiedades</caption>"
                    + "<thead><tr><th scope=\"col\">Propiedad</th><th scope=\"col\">Tipo</th><th scope=\"col\">Operación</th>"
                    + "<th scope=\"col\" class=\"is-numeric\">Precio</th><th scope=\"col\">Estado</th>"
                    + "<th scope=\"col\"><span class=\"visually-hidden\">Acciones</span></th></tr></thead>"
                    + "<tbody>" + html + "</tbody></table></div>";
        }
        String cuerpo = "<div class=\"section-stack\">"
                + "<div class=\"toolbar\">" + buscador + nueva + "</div>"
                + "<p class=\"results-count\">" + Formato.plural(filas.tamano(), "propiedad", "propiedades") + "</p>"
                + tabla + "</div>";
        return Plantilla.seccion("Propiedades", "Los inmuebles que ofrece la inmobiliaria", "", cuerpo);
    }

    /** Una fila de la tabla. */
    private static String fila(Propiedad p) {
        String id = Html.escapar(p.getId());
        return "<tr><td class=\"is-full\"><span class=\"property-cell\">"
                + Html.imagen(p.getImagenUrl(), "property-cell__thumb", 48, 36)
                + "<span class=\"property-cell__text\"><span class=\"property-cell__title\">" + Html.escapar(p.getTitulo()) + "</span>"
                + "<span class=\"text-sm text-muted\">" + Html.escapar(p.getUbicacion()) + "</span></span></span></td>"
                + "<td data-label=\"Tipo\">" + Html.escapar(p.getTipo()) + "</td>"
                + "<td data-label=\"Operación\">" + Etiquetas.operacion(p.getOperacion()) + "</td>"
                + "<td data-label=\"Precio\" class=\"is-numeric\">" + Formato.dinero(p.getPrecio()) + "</td>"
                // Cambiar el estado: un formulario pequeño con la lista y un botón (en celular ocupa todo el ancho de la tarjeta)
                + "<td data-label=\"Estado\" class=\"is-full\"><form method=\"post\" action=\"/propiedades/" + id + "/estado\" class=\"cambio-rapido\">"
                + "<div class=\"field__select field__select--sm\"><select class=\"field__control field__control--sm\" name=\"status\" aria-label=\"Estado de "
                + Html.escapar(p.getTitulo()) + "\">" + Html.opciones(Catalogos.ESTADOS_PROPIEDAD, Etiquetas.ESTADOS_PROPIEDAD, p.getEstado()) + "</select></div>"
                + "<button type=\"submit\" class=\"btn btn--secondary btn--sm\">Cambiar</button></form></td>"
                + "<td class=\"is-full\"><span class=\"data-table__actions\">"
                + "<a class=\"btn btn--ghost btn--sm\" href=\"/propiedades/" + id + "/editar\">" + Html.icono("edit", "icon--sm") + "Editar</a>"
                + "<a class=\"btn btn--ghost btn--sm\" href=\"/propiedades/" + id + "/eliminar\">" + Html.icono("trash", "icon--sm") + "Eliminar</a>"
                + "</span></td></tr>";
    }

    /**
     * Los datos de una propiedad guardada, listos para ponerlos en el formulario.
     * El precio va con puntos de miles y el área con coma decimal.
     */
    public static ObjetoJson valoresDe(Propiedad p) {
        ObjetoJson valores = p.aJson();
        valores.poner("price", Formato.numero(p.getPrecio()));
        valores.poner("areaM2", Formato.decimal(valores.numero("areaM2")));
        return valores;
    }

    /** Los valores con que empieza el formulario de una propiedad nueva. */
    public static ObjetoJson valoresNuevos() {
        return new ObjetoJson().poner("type", "Apartamento").poner("operation", "sale").poner("status", "available")
                .poner("bedrooms", "0").poner("bathrooms", "1").poner("parking", "0");
    }

    /**
     * La ventana con el formulario de propiedad (para crear o para editar).
     * @param accion  a dónde se envía: "/propiedades/nueva" o "/propiedades/{id}/editar"
     * @param valores lo que va escrito en cada campo
     * @param errores errores por campo que devolvió el Validador ({} si no hay)
     */
    public static String formulario(String titulo, String accion, ObjetoJson valores, ObjetoJson errores) {
        String general = errores.texto("_form");
        String cuerpo = (general.isEmpty() ? "" : "<div class=\"alert alert--danger\" role=\"alert\">"
                + Html.icono("alert-circle", "icon--sm") + "<span>" + Html.escapar(general) + "</span></div>")
                + "<form id=\"property-form\" method=\"post\" action=\"" + accion + "\" class=\"form-grid form-grid--2\" novalidate>"
                + Html.campo("pf-title", "title", "Título", valores.texto("title"), "text",
                        "maxlength=\"" + Catalogos.MAX_TITULO + "\" autofocus", "", errores.texto("title"), true)
                + Html.lista("pf-type", "type", "Tipo de inmueble", Catalogos.TIPOS, Catalogos.TIPOS, valores.texto("type"), errores.texto("type"), false)
                + Html.lista("pf-operation", "operation", "Operación", Catalogos.OPERACIONES, Etiquetas.OPERACIONES,
                        valores.texto("operation"), errores.texto("operation"), false)
                + Html.campoDinero("pf-price", "price", "Precio", valores.texto("price"),
                        valores.texto("operation").equals("rent") ? "Canon mensual" : "", errores.texto("price"))
                + Html.lista("pf-status", "status", "Estado", Catalogos.ESTADOS_PROPIEDAD, Etiquetas.ESTADOS_PROPIEDAD,
                        valores.texto("status"), errores.texto("status"), false)
                + Html.campo("pf-location", "location", "Ubicación", valores.texto("location"), "text",
                        "maxlength=\"" + Catalogos.MAX_UBICACION + "\"", "Barrio y ciudad, por ejemplo: Laureles, Medellín", errores.texto("location"), true)
                + Html.campo("pf-areaM2", "areaM2", "Área (m²)", valores.texto("areaM2"), "text", "inputmode=\"decimal\"", "", errores.texto("areaM2"), false)
                + Html.campo("pf-bedrooms", "bedrooms", "Habitaciones", valores.texto("bedrooms"), "text", "inputmode=\"numeric\"", "", errores.texto("bedrooms"), false)
                + Html.campo("pf-bathrooms", "bathrooms", "Baños", valores.texto("bathrooms"), "text", "inputmode=\"numeric\"", "", errores.texto("bathrooms"), false)
                + Html.campo("pf-parking", "parking", "Parqueaderos", valores.texto("parking"), "text", "inputmode=\"numeric\"", "", errores.texto("parking"), false)
                + Html.campo("pf-imageUrl", "imageUrl", "Enlace de la foto <span class=\"field__optional\">(opcional)</span>",
                        valores.texto("imageUrl"), "url", "",
                        "Copia y pega el enlace de una foto publicada en internet. Si no hay enlace, se muestra una imagen genérica.",
                        errores.texto("imageUrl"), true)
                + Html.areaDeTexto("pf-description", "description", "Descripción <span class=\"field__optional\">(opcional)</span>",
                        valores.texto("description"), Catalogos.MAX_DESCRIPCION, errores.texto("description"))
                + "</form>";
        // form="property-form": el botón está fuera del <form>, pero lo envía igual
        String pie = "<a class=\"btn btn--secondary\" href=\"/propiedades\">Cancelar</a>"
                + "<button type=\"submit\" class=\"btn btn--primary\" form=\"property-form\">Guardar propiedad</button>";
        return Plantilla.ventana(titulo, cuerpo, pie, "/propiedades", false);
    }

    /** La ventana "¿Seguro que quieres eliminar?". Avisa si también se borran clientes. */
    public static String confirmarEliminar(Propiedad p, int clientes) {
        String nota = clientes == 0 ? ""
                : " También se " + (clientes == 1 ? "eliminará " : "eliminarán ")
                + Formato.plural(clientes, "cliente interesado", "clientes interesados") + ".";
        String cuerpo = "<p>Se eliminará <strong>" + Html.escapar(p.getTitulo()) + "</strong>." + Html.escapar(nota)
                + " Esta acción no se puede deshacer.</p>";
        String pie = "<a class=\"btn btn--secondary\" href=\"/propiedades\" autofocus>Cancelar</a>"
                + "<form method=\"post\" action=\"/propiedades/" + Html.escapar(p.getId()) + "/eliminar\" class=\"form-en-linea\">"
                + "<button type=\"submit\" class=\"btn btn--danger\">Eliminar</button></form>";
        return Plantilla.ventana("Eliminar propiedad", cuerpo, pie, "/propiedades", true);
    }
}
