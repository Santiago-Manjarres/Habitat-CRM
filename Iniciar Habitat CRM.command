#!/bin/bash
# Doble clic para iniciar Habitat CRM: compila, enciende el servidor y abre el navegador.
cd "$(dirname "$0")" || exit 1
if ! command -v javac >/dev/null 2>&1; then
  echo "No se encontró Java (JDK 17 o superior). Instálalo y vuelve a intentar."
  read -n 1 -s -r -p "Presiona una tecla para cerrar..."; exit 1
fi
mkdir -p servidor-java/build/classes
javac -encoding UTF-8 -d servidor-java/build/classes $(find servidor-java/src -name "*.java") || {
  read -n 1 -s -r -p "Error al compilar. Presiona una tecla para cerrar..."; exit 1; }
echo "Para detener el servidor cierra esta ventana (o Ctrl+C)."
java -cp servidor-java/build/classes servidor.Principal
