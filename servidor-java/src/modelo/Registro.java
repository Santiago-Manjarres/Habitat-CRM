package modelo;

import json.ObjetoJson;

/**
 * Interfaz: un "contrato" que cumplen todas las clases que se guardan en una tabla
 * (Usuario, Propiedad y Cliente). Obliga a que cada una tenga:
 *  - un id único (getId), con el que se busca en la tabla hash;
 *  - una forma de convertirse a JSON (aJson), para escribirla en su archivo.
 *
 * Gracias a esto, la clase Tabla<T> sirve para las tres sin repetir código.
 */
public interface Registro {

    String getId();

    ObjetoJson aJson();
}
