package vistas;

import datos.EstructuraDeDatos;
import datos.Datos;
import estructuras.Nodo;

/** La sección "Clientes": el tablero por etapas y el formulario para crear o editar. */
public class PaginaClientes {

    /** El tablero: una columna por etapa. En cada columna van los clientes que están en esa etapa. */
    public static String tablero(EstructuraDeDatos estructura, Datos usuario) {
        String columnas = "";
        for (String etapa : EstructuraDeDatos.ETAPAS) {
            String tarjetas = "";
            // Recorremos la lista de clientes y nos quedamos con los de esta etapa
            for (Nodo<Datos> n = estructura.clientes.getCabeza(); n != null; n = n.siguiente) {
                if (n.dato.get("etapa").equals(etapa)) {
                    tarjetas += tarjeta(estructura, n.dato);
                }
            }
            columnas += "<section class=\"kanban__column\"><header class=\"kanban__header\"><h2 class=\"kanban__title\">" + etapa + "</h2>"
                    + "<span class=\"kanban__count\">" + estructura.contar(estructura.clientes, "etapa", etapa) + "</span></header>"
                    + (tarjetas.isEmpty() ? "<p class=\"kanban__empty\">Nadie en esta etapa</p>" : tarjetas) + "</section>";
        }
        String boton = "<a class=\"btn btn--primary\" href=\"/clientes/nuevo\">" + Plantilla.icono("plus") + "Nuevo cliente</a>";
        String contenido = "<section class=\"page\">" + Plantilla.seccion("Clientes", "Cada tarjeta es un cliente interesado en una propiedad.", boton)
                + "<div class=\"kanban\">" + columnas + "</div></section>";
        return Plantilla.pagina("Clientes", usuario, "clientes", contenido);
    }

    /** La tarjeta de un cliente. */
    private static String tarjeta(EstructuraDeDatos estructura, Datos c) {
        Datos propiedad = estructura.buscar(estructura.propiedades, c.get("propiedad"));
        String notas = c.get("notas").isEmpty() ? "Sin notas" : c.get("notas");
        return "<article class=\"card lead-card\"><div class=\"lead-card__head\">"
                + "<span class=\"avatar\">" + Plantilla.iniciales(c.get("nombre")) + "</span>"
                + "<div class=\"property-cell__text\"><span class=\"lead-card__name\">" + Plantilla.escapar(c.get("nombre")) + "</span>"
                + "<span class=\"lead-card__property\">" + (propiedad == null ? "Propiedad eliminada" : Plantilla.escapar(propiedad.get("titulo"))) + "</span></div></div>"
                + Plantilla.insignia(c.get("etapa"))
                + "<p class=\"lead-card__notes\">" + Plantilla.escapar(notas) + "</p>"
                + "<a class=\"btn btn--secondary btn--sm\" href=\"/clientes/editar?id=" + c.get("id") + "\">Editar</a></article>";
    }

    /** El formulario para crear (editar = false) o editar (editar = true) un cliente. */
    public static String formulario(EstructuraDeDatos estructura, Datos usuario, Datos valores, Datos errores, boolean editar) {
        // La lista de propiedades para elegir: el valor es el id y se ve el título
        String opciones = "";
        for (Nodo<Datos> n = estructura.propiedades.getCabeza(); n != null; n = n.siguiente) {
            String id = n.dato.get("id");
            opciones += "<option value=\"" + id + "\"" + (id.equals(valores.get("propiedad")) ? " selected" : "") + ">"
                    + Plantilla.escapar(n.dato.get("titulo")) + "</option>";
        }
        String accion = editar ? "/clientes/editar" : "/clientes/nuevo";
        String eliminar = !editar ? ""
                : "<form method=\"post\" action=\"/clientes/eliminar\"><input type=\"hidden\" name=\"id\" value=\"" + Plantilla.escapar(valores.get("id")) + "\">"
                + "<button type=\"submit\" class=\"btn btn--danger\">Eliminar cliente</button></form>";
        String contenido = "<section class=\"page\">" + Plantilla.seccion(editar ? "Editar cliente" : "Nuevo cliente", "Llena los datos y guarda.", "")
                + "<div class=\"card form-card\">"
                + "<form method=\"post\" action=\"" + accion + "\" class=\"form-grid\" novalidate>"
                + "<input type=\"hidden\" name=\"id\" value=\"" + Plantilla.escapar(valores.get("id")) + "\">"
                + Plantilla.campo("nombre", "Nombre del cliente", "text", valores, errores)
                + Plantilla.campo("telefono", "Teléfono", "tel", valores, errores)
                + Plantilla.campo("correo", "Correo", "email", valores, errores)
                + "<div class=\"field\"><label class=\"field__label\" for=\"propiedad\">Propiedad de interés</label>"
                + "<div class=\"field__select\"><select class=\"field__control\" id=\"propiedad\" name=\"propiedad\">" + opciones + "</select></div>"
                + "<span class=\"field__error\">" + errores.get("propiedad") + "</span></div>"
                + Plantilla.lista("etapa", "Etapa", EstructuraDeDatos.ETAPAS, valores)
                + "<div class=\"field\"><label class=\"field__label\" for=\"notas\">Notas (opcional)</label>"
                + "<textarea class=\"field__control\" id=\"notas\" name=\"notas\">" + Plantilla.escapar(valores.get("notas")) + "</textarea></div>"
                + "<div class=\"form-botones\"><button type=\"submit\" class=\"btn btn--primary\">Guardar</button>"
                + "<a class=\"btn btn--secondary\" href=\"/clientes\">Cancelar</a></div>"
                + "</form>" + eliminar + "</div></section>";
        return Plantilla.pagina("Clientes", usuario, "clientes", contenido);
    }
}
