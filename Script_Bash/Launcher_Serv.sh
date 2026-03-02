#!/usr/bin/env bash

# Se placer à la racine du projet (dossier du script)
cd "$(dirname "$0")/.." || exit 1

# Compiler la classe Server (et ses dépendances) si nécessaire
javac -d . server/Server.java Src/main.java

if [ $? -ne 0 ]; then
  echo "Échec de la compilation. Vérifie les erreurs ci-dessus."
  exit 1
fi

echo "Lancement du serveur Java..."
java main
