package modelo;

import json.ObjetoJson;

/**
 * Un asesor de la inmobiliaria (el único tipo de usuario del CRM).
 * Se guarda en data/users.json.
 *
 * Nota: las variables están en español, pero las claves del JSON ("username",
 * "name", "email"...) siguen en inglés para no cambiar los archivos de datos
 * ni la interfaz que ya las usa.
 */
public class Usuario implements Registro {

    private String id;
    private String usuario;
    private String contrasena;
    private String nombre;
    private String correo;
    private String telefono;
    private String rol;
    private String avatar;

    /** Crea un usuario a partir de un objeto leído del archivo JSON. */
    public static Usuario desdeJson(ObjetoJson json) {
        Usuario u = new Usuario();
        u.id = json.texto("id");
        u.usuario = json.texto("username");
        u.contrasena = json.texto("password");
        u.nombre = json.texto("name");
        u.correo = json.texto("email");
        u.telefono = json.texto("phone");
        u.rol = json.texto("role");
        u.avatar = json.texto("avatar");
        return u;
    }

    /** JSON completo, con contraseña: solo para guardarlo en el archivo. */
    @Override
    public ObjetoJson aJson() {
        return aJsonPublico().poner("password", contrasena);
    }

    /** JSON sin la contraseña (por ejemplo, para mostrar sus datos sin exponer la clave). */
    public ObjetoJson aJsonPublico() {
        return new ObjetoJson()
                .poner("id", id)
                .poner("username", usuario)
                .poner("name", nombre)
                .poner("email", correo)
                .poner("phone", telefono)
                .poner("role", rol)
                .poner("avatar", avatar);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAvatar() {
        return avatar;
    }
}
