package modelo;

import json.ObjetoJson;

/**
 * Un cliente interesado en una propiedad (en la especificación se llama "lead").
 * Lo registra un asesor y avanza por etapas: nuevo, contactado, visita,
 * negociación y cerrado. Se guarda en data/leads.json.
 */
public class Cliente implements Registro {

    // Las variables están en español; las claves del JSON siguen en inglés ("contactName", "stage"...)

    private String id;
    private String nombre;
    private String telefono;
    private String correo;
    private String origen;       // cómo llegó: "web", "phone" (llamada), "whatsapp", "walk_in" (visita) o "referral" (recomendado)
    private String propiedadId;  // id de la propiedad que le interesa
    private String asesorId;     // id del asesor responsable
    private String etapa;        // "new" (nuevo), "contacted" (contactado), "visit" (visita), "negotiation" (negociación),
                             // "closed_won" (cerrado ganado) o "closed_lost" (cerrado perdido)
    private String notas;
    private String fechaCreacion;
    private String fechaActualizacion;

    /** Crea un cliente a partir de un objeto leído del archivo JSON. */
    public static Cliente desdeJson(ObjetoJson json) {
        Cliente c = new Cliente();
        c.id = json.texto("id");
        c.fechaCreacion = json.texto("createdAt");
        c.fechaActualizacion = json.texto("updatedAt");
        c.copiarDatos(json);
        return c;
    }

    /** Copia los datos que el asesor puede editar desde el formulario. */
    public void copiarDatos(ObjetoJson datos) {
        nombre = datos.texto("contactName");
        telefono = datos.texto("contactPhone");
        correo = datos.texto("contactEmail");
        origen = datos.texto("source");
        propiedadId = datos.texto("propertyId");
        asesorId = datos.texto("agentId");
        etapa = datos.texto("stage").isEmpty() ? "new" : datos.texto("stage");
        notas = datos.texto("notes");
    }

    /** Crea un cliente nuevo con los datos del formulario. */
    public static Cliente nuevo(String id, ObjetoJson datos, String fecha) {
        Cliente c = new Cliente();
        c.id = id;
        c.fechaCreacion = fecha;
        c.fechaActualizacion = fecha;
        c.copiarDatos(datos);
        return c;
    }

    @Override
    public ObjetoJson aJson() {
        return new ObjetoJson()
                .poner("id", id)
                .poner("contactName", nombre)
                .poner("contactPhone", telefono)
                .poner("contactEmail", correo)
                .poner("source", origen)
                .poner("propertyId", propiedadId)
                .poner("agentId", asesorId)
                .poner("stage", etapa)
                .poner("notes", notas)
                .poner("createdAt", fechaCreacion)
                .poner("updatedAt", fechaActualizacion);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getOrigen() {
        return origen;
    }

    public String getNotas() {
        return notas;
    }

    public String getPropiedadId() {
        return propiedadId;
    }

    public String getAsesorId() {
        return asesorId;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public String getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(String fecha) {
        this.fechaActualizacion = fecha;
    }
}
