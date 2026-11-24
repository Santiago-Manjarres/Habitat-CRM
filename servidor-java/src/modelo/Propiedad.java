package modelo;

import json.ObjetoJson;
import servicios.Numeros;

/**
 * Un inmueble del inventario. Se guarda en data/properties.json.
 *
 * Nota: las variables están en español, pero las claves del JSON ("title",
 * "price", "location"...) siguen en inglés para no cambiar los archivos de datos.
 */
public class Propiedad implements Registro {

    private String id;
    private String titulo;
    private String tipo;
    private String operacion;    // "sale" (venta) o "rent" (arriendo)
    private double precio;      // precio en pesos
    private String moneda;
    private String ubicacion;
    private double area;
    private int habitaciones;
    private int banos;
    private int parqueaderos;
    private String estado;       // "available" (disponible), "negotiation" (en negociación) o "sold" (vendida)
    private String asesorId;     // id del asesor que la registró
    private String descripcion;
    private String imagenUrl;
    private String fechaCreacion;

    /** Crea una propiedad a partir de un objeto leído del archivo JSON. */
    public static Propiedad desdeJson(ObjetoJson json) {
        Propiedad p = new Propiedad();
        p.id = json.texto("id");
        p.asesorId = json.texto("agentId");
        p.fechaCreacion = json.texto("createdAt");
        p.moneda = json.texto("currency");
        p.copiarDatos(json);
        return p;
    }

    /**
     * Copia los datos que el asesor puede editar desde el formulario.
     * Los números pueden venir como texto ("1.350.000.000"), por eso se usa Numeros.leer.
     */
    public void copiarDatos(ObjetoJson datos) {
        titulo = datos.texto("title");
        tipo = datos.texto("type");
        operacion = datos.texto("operation");
        precio = Numeros.leer(datos.obtener("price"));
        ubicacion = datos.texto("location");
        area = Numeros.leer(datos.obtener("areaM2"));
        habitaciones = (int) Numeros.leer(datos.obtener("bedrooms"));
        banos = (int) Numeros.leer(datos.obtener("bathrooms"));
        parqueaderos = (int) Numeros.leer(datos.obtener("parking"));
        estado = datos.texto("status");
        descripcion = datos.texto("description");
        imagenUrl = datos.texto("imageUrl");
        if (moneda == null || moneda.isEmpty()) {
            moneda = "COP";
        }
    }

    /** Crea una propiedad nueva con los datos del formulario. */
    public static Propiedad nueva(String id, ObjetoJson datos, String asesorId, String fecha) {
        Propiedad p = new Propiedad();
        p.id = id;
        p.asesorId = asesorId;
        p.fechaCreacion = fecha;
        p.copiarDatos(datos);
        return p;
    }

    @Override
    public ObjetoJson aJson() {
        return new ObjetoJson()
                .poner("id", id)
                .poner("title", titulo)
                .poner("type", tipo)
                .poner("operation", operacion)
                .poner("price", precio)
                .poner("currency", moneda)
                .poner("location", ubicacion)
                .poner("areaM2", area)
                .poner("bedrooms", habitaciones)
                .poner("bathrooms", banos)
                .poner("parking", parqueaderos)
                .poner("status", estado)
                .poner("agentId", asesorId)
                .poner("description", descripcion)
                .poner("imageUrl", imagenUrl)
                .poner("createdAt", fechaCreacion);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getOperacion() {
        return operacion;
    }

    public double getPrecio() {
        return precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }
}
