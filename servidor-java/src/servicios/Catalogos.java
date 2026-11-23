package servicios;

/**
 * Valores permitidos en el CRM (los mismos que usa la interfaz).
 */
public class Catalogos {

    private Catalogos() {
    }

    public static final String[] TIPOS = {"Apartamento", "Casa", "Local", "Oficina", "Lote"};
    public static final String[] OPERACIONES = {"sale", "rent"};
    public static final String[] ESTADOS_PROPIEDAD = {"available", "negotiation", "sold"};
    public static final String[] ORIGENES = {"web", "phone", "whatsapp", "walk_in", "referral"};
    public static final String[] ETAPAS = {"new", "contacted", "visit", "negotiation", "closed_won", "closed_lost"};

    /** Columnas del tablero de clientes. La última junta las dos etapas de cierre. */
    public static final String[] COLUMNAS = {"new", "contacted", "visit", "negotiation", "closed"};
    public static final String[] NOMBRES_COLUMNAS = {"Nuevo", "Contactado", "Visita", "Negociación", "Cerrado"};

    /** Límites de largo y tamaño de los campos. */
    public static final int MAX_TITULO = 120;
    public static final int MAX_UBICACION = 120;
    public static final int MAX_DESCRIPCION = 2000;
    public static final int MAX_NOTAS = 1000;
    public static final int MAX_NOMBRE = 80;
    public static final double MAX_PRECIO = 1e13;
    public static final double MAX_AREA = 1e6;
    public static final int MAX_CUARTOS = 99;

    /** Columna del tablero a la que pertenece una etapa. */
    public static String columnaDeEtapa(String etapa) {
        if (etapa.equals("closed_won") || etapa.equals("closed_lost")) {
            return "closed";
        }
        return etapa;
    }

    public static boolean estaCerrada(String etapa) {
        return columnaDeEtapa(etapa).equals("closed");
    }
}
