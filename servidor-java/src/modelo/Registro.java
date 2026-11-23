package modelo;

import json.ObjetoJson;

/**
 * Todo lo que se guarda en una tabla de la base de datos: tiene un id único
 * y se puede convertir a JSON para escribirlo en su archivo.
 */
public interface Registro {

    String getId();

    ObjetoJson aJson();
}
