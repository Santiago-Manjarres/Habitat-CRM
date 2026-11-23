package modelo;

import json.ObjetoJson;

/**
 * Un cliente interesado en una propiedad (en la especificación se llama "lead").
 * Lo registra un asesor y avanza por etapas: nuevo, contactado, visita,
 * negociación y cerrado. Se guarda en data/leads.json.
 */
public class Cliente implements Registro {

    private String id;
    private String nombre;
    private String telefono;
    private String correo;
    private String origen;       // "web", "phone", "whatsapp", "walk_in" o "referral"
    private String propiedadId;
    private String asesorId;
    private String etapa;        // "new", "contacted", "visit", "negotiation", "closed_won" o "closed_lost"
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
