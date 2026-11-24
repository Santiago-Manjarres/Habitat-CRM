package vistas;

import datos.BaseDeDatos;
import estructuras.ListaEnlazada;
import estructuras.Nodo;
import json.ObjetoJson;
import modelo.Cliente;
import modelo.Propiedad;
import modelo.Usuario;
import servicios.Catalogos;

/**
 * La sección "Clientes": el tablero por etapas (columnas), la ficha del cliente
 * con su formulario y la ventana para confirmar una eliminación.
 */
public class PaginaClientes {

    private PaginaClientes() {
    }

    /**
     * El tablero. Recibe el arreglo de listas que arma ServicioClientes.columnas():
     * una lista enlazada de clientes por cada columna.
     */
    public static String contenido(BaseDeDatos db, ListaEnlazada<Cliente>[] columnas) {
        String boton = "<a class=\"btn btn--primary\" href=\"/clientes/nuevo\">" + Html.icono("plus", "") + "Nuevo cliente</a>";
        String cuerpo;
        if (db.getClientes().tamano() == 0) {
            cuerpo = Html.vacio("funnel", "Aún no hay clientes",
                    "Cada persona que llame, escriba o visite la oficina por una propiedad se registra aquí para seguirla hasta el cierre.",
                    "<a class=\"btn btn--primary\" href=\"/clientes/nuevo\">" + Html.icono("plus", "") + "Registrar el primer cliente</a>");
        } else {
            StringBuilder html = new StringBuilder("<div class=\"kanban\">");
            for (int i = 0; i < columnas.length; i++) {
                html.append(columna(db, Catalogos.COLUMNAS[i], Catalogos.NOMBRES_COLUMNAS[i], columnas[i]));
            }
            cuerpo = html.append("</div>").toString();
        }
        return Plantilla.seccion("Clientes", "Cada tarjeta es un cliente interesado. Cuando avance, cámbiale la etapa.", boton, cuerpo);
    }

    /** Una columna del tablero: título, cantidad y sus tarjetas. */
    private static String columna(BaseDeDatos db, String clave, String nombre, ListaEnlazada<Cliente> clientes) {
        StringBuilder tarjetas = new StringBuilder();
        for (Nodo<Cliente> n = clientes.getCabeza(); n != null; n = n.getSiguiente()) {
            tarjetas.append(tarjeta(db, n.getDato()));
        }
        if (clientes.estaVacia()) {
            tarjetas.append("<p class=\"kanban__empty\">Nadie en esta etapa</p>");
        }
        return "<section class=\"kanban__column\" aria-labelledby=\"kanban-" + clave + "\">"
                + "<header class=\"kanban__header\"><h2 class=\"kanban__title\" id=\"kanban-" + clave + "\">" + nombre + "</h2>"
                + "<span class=\"kanban__count\" aria-label=\"" + Formato.plural(clientes.tamano(), "cliente", "clientes") + "\">"
                + clientes.tamano() + "</span></header>"
                + tarjetas + "</section>";
    }

    /** La tarjeta de un cliente. */
    private static String tarjeta(BaseDeDatos db, Cliente c) {
        String id = Html.escapar(c.getId());
        Propiedad p = db.getPropiedades().buscarPorId(c.getPropiedadId());    // búsquedas por id en la tabla hash
        Usuario asesor = db.getUsuarios().buscarPorId(c.getAsesorId());
        String notas = c.getNotas().isEmpty() ? "Sin notas" : c.getNotas();
        return "<article class=\"card lead-card\" aria-label=\"" + Html.escapar(c.getNombre()) + "\">"
                + "<div class=\"lead-card__head\"><span class=\"avatar\" aria-hidden=\"true\">" + Html.escapar(Formato.iniciales(c.getNombre())) + "</span>"
                + "<div class=\"property-cell__text\"><span class=\"lead-card__name\">" + Html.escapar(c.getNombre()) + "</span>"
                + "<span class=\"lead-card__property\">" + Html.escapar(p == null ? "Propiedad eliminada" : p.getTitulo()) + "</span></div></div>"
                + Html.insignia(Etiquetas.etapa(c.getEtapa()), Etiquetas.tonoEtapa(c.getEtapa()))
                + "<p class=\"lead-card__notes" + (c.getNotas().isEmpty() ? " text-muted" : "") + "\">" + Html.escapar(notas) + "</p>"
                + "<div class=\"lead-card__meta\"><span>" + Html.escapar(Etiquetas.origen(c.getOrigen())) + " · "
                + Formato.tiempoRelativo(c.getFechaActualizacion()) + "</span>"
                + "<span class=\"avatar avatar--xs\" title=\"" + Html.escapar(asesor == null ? "" : asesor.getNombre())
                + "\" aria-label=\"Asesor: " + Html.escapar(asesor == null ? "sin asignar" : asesor.getNombre()) + "\">"
                + Html.escapar(asesor == null ? "?" : asesor.getAvatar()) + "</span></div>"
                + "<div class=\"lead-card__actions\">"
                // Cambiar la etapa: la lista está en un formulario y el botón "Mover" lo envía
                // (el atributo form="..." une el botón con su formulario aunque esté fuera de él)
                + "<form method=\"post\" action=\"/clientes/" + id + "/etapa\" id=\"etapa-" + id + "\" class=\"lead-card__stage\">"
                + "<label class=\"field__label\" for=\"etapa-lista-" + id + "\">Cambiar etapa</label>"
                + "<div class=\"field__select field__select--sm\">"
                + "<select class=\"field__control field__control--sm\" id=\"etapa-lista-" + id + "\" name=\"stage\">"
                + Html.opciones(Catalogos.ETAPAS, Etiquetas.ETAPAS, c.getEtapa()) + "</select></div></form>"
                + "<div class=\"lead-card__buttons\">"
                + "<button type=\"submit\" class=\"btn btn--secondary btn--sm\" form=\"etapa-" + id + "\">Mover</button>"
                + "<a class=\"btn btn--secondary btn--sm\" href=\"/clientes/" + id + "\">" + Html.icono("note", "icon--sm") + "Ver ficha</a>"
                + "</div>"
                + "</div></article>";
    }

    /** Los valores con que empieza el formulario de un cliente nuevo (el asesor es quien lo registra). */
    public static ObjetoJson valoresNuevos(Usuario asesor) {
        return new ObjetoJson().poner("source", "phone").poner("stage", "new").poner("agentId", asesor.getId());
    }

    /** Enlaces para llamar o escribir al cliente (solo en la ficha de un cliente que ya existe). */
    private static String contacto(Cliente c) {
        String telefono = c.getTelefono().replaceAll("[^\\d+]", "");       // deja solo números y el +
        String enlaces = (telefono.isEmpty() ? "" : "<a class=\"link-btn\" href=\"tel:" + Html.escapar(telefono) + "\">"
                + Html.icono("phone", "icon--sm") + Html.escapar(c.getTelefono()) + "</a>")
                + (c.getCorreo().isEmpty() ? "" : "<a class=\"link-btn\" href=\"mailto:" + Html.escapar(c.getCorreo()) + "\">"
                + Html.icono("mail", "icon--sm") + Html.escapar(c.getCorreo()) + "</a>");
        return "<div class=\"contact-card\"><span class=\"contact-card__text\"><span class=\"contact-card__label\">Contactar</span></span>"
                + "<span class=\"contact-card__links\">" + (enlaces.isEmpty() ? "<span class=\"text-muted\">Sin datos de contacto</span>" : enlaces)
                + "</span></div>";
    }

    /**
     * La ventana con el formulario del cliente (nuevo o ficha de uno que ya existe).
     * @param existente el cliente que se está editando, o null si es nuevo
     * @param asesores  los asesores ordenados por nombre (salen del árbol binario)
     */
    public static String formulario(BaseDeDatos db, Cliente existente, ObjetoJson valores, ObjetoJson errores,
                                    ListaEnlazada<Usuario> asesores) {
        String accion = existente == null ? "/clientes/nuevo" : "/clientes/" + Html.escapar(existente.getId());

        // Lista de propiedades: no se ofrecen las vendidas (salvo la que el cliente ya tenía)
        ListaEnlazada<String> ids = new ListaEnlazada<>();
        ListaEnlazada<String> nombres = new ListaEnlazada<>();
        for (Nodo<Propiedad> n = db.getPropiedades().getPrimero(); n != null; n = n.getSiguiente()) {
            Propiedad p = n.getDato();
            if (!p.getEstado().equals("sold") || p.getId().equals(valores.texto("propertyId"))) {
                ids.agregar(p.getId());
                nombres.agregar(p.getTitulo() + " · " + Formato.dinero(p.getPrecio()));
            }
        }
        // Lista de asesores
        ListaEnlazada<String> idsAsesores = new ListaEnlazada<>();
        ListaEnlazada<String> nombresAsesores = new ListaEnlazada<>();
        for (Nodo<Usuario> n = asesores.getCabeza(); n != null; n = n.getSiguiente()) {
            idsAsesores.agregar(n.getDato().getId());
            nombresAsesores.agregar(n.getDato().getNombre());
        }

        String general = errores.texto("_form");
        String cuerpo = (existente == null ? "" : contacto(existente))
                + (general.isEmpty() ? "" : "<div class=\"alert alert--danger\" role=\"alert\">"
                + Html.icono("alert-circle", "icon--sm") + "<span>" + Html.escapar(general) + "</span></div>")
                + "<form id=\"lead-form\" method=\"post\" action=\"" + accion + "\" class=\"form-grid form-grid--2\" novalidate>"
                + Html.campo("lf-contactName", "contactName", "Nombre del cliente", valores.texto("contactName"), "text",
                        "maxlength=\"" + Catalogos.MAX_NOMBRE + "\" autocomplete=\"off\" autofocus", "", errores.texto("contactName"), true)
                + Html.campo("lf-contactPhone", "contactPhone", "Teléfono", valores.texto("contactPhone"), "tel",
                        "inputmode=\"tel\" autocomplete=\"off\"", "Teléfono o correo: al menos uno.", errores.texto("contactPhone"), false)
                + Html.campo("lf-contactEmail", "contactEmail", "Correo", valores.texto("contactEmail"), "email",
                        "inputmode=\"email\" autocomplete=\"off\"", "", errores.texto("contactEmail"), false)
                + Html.lista("lf-propertyId", "propertyId", "Propiedad de interés", aArreglo(ids), aArreglo(nombres),
                        valores.texto("propertyId"), errores.texto("propertyId"), true)
                + Html.lista("lf-source", "source", "¿Cómo llegó?", Catalogos.ORIGENES, Etiquetas.ORIGENES,
                        valores.texto("source"), errores.texto("source"), false)
                + Html.lista("lf-stage", "stage", "Etapa", Catalogos.ETAPAS, Etiquetas.ETAPAS,
                        valores.texto("stage"), errores.texto("stage"), false)
                + Html.lista("lf-agentId", "agentId", "Asesor responsable", aArreglo(idsAsesores), aArreglo(nombresAsesores),
                        valores.texto("agentId"), errores.texto("agentId"), true)
                + Html.areaDeTexto("lf-notes", "notes", "Notas <span class=\"field__optional\">(opcional)</span>",
                        valores.texto("notes"), Catalogos.MAX_NOTAS, errores.texto("notes"))
                + "</form>";
        String pie = (existente == null ? "" : "<a class=\"btn btn--ghost modal__footer-start\" href=\"" + accion + "/eliminar\">"
                + Html.icono("trash", "icon--sm") + "Eliminar</a>")
                + "<a class=\"btn btn--secondary\" href=\"/clientes\">Cancelar</a>"
                + "<button type=\"submit\" class=\"btn btn--primary\" form=\"lead-form\">"
                + (existente == null ? "Guardar cliente" : "Guardar cambios") + "</button>";
        String titulo = existente == null ? "Nuevo cliente" : existente.getNombre();
        return Plantilla.ventana(titulo, cuerpo, pie, "/clientes", false);
    }

    /** La ventana "¿Seguro que quieres eliminar?" de un cliente. */
    public static String confirmarEliminar(Cliente c) {
        String id = Html.escapar(c.getId());
        String cuerpo = "<p>Se eliminará la ficha de <strong>" + Html.escapar(c.getNombre()) + "</strong>. Esta acción no se puede deshacer.</p>";
        String pie = "<a class=\"btn btn--secondary\" href=\"/clientes/" + id + "\" autofocus>Cancelar</a>"
                + "<form method=\"post\" action=\"/clientes/" + id + "/eliminar\" class=\"form-en-linea\">"
                + "<button type=\"submit\" class=\"btn btn--danger\">Eliminar</button></form>";
        return Plantilla.ventana("Eliminar cliente", cuerpo, pie, "/clientes/" + id, true);
    }

    /** Pasa una lista enlazada de textos a un arreglo (lo necesita Html.lista). */
    private static String[] aArreglo(ListaEnlazada<String> lista) {
        String[] arreglo = new String[lista.tamano()];
        int i = 0;
        for (Nodo<String> n = lista.getCabeza(); n != null; n = n.getSiguiente()) {
            arreglo[i++] = n.getDato();
        }
        return arreglo;
    }
}
