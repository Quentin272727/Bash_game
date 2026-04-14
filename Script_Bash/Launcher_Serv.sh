#!/usr/bin/env bash

# Move to project root (parent of Script_Bash/)
cd "$(dirname "$0")/.." || exit 1

# Make Play.sh executable (needed on Linux)
chmod +x Script_Bash/Play.sh

# Compile — classpath must include Src/ so the 'server' package resolves
javac -cp Src Src/server/Server.java Src/main.java

if [ $? -ne 0 ]; then
  echo "Compilation failed. Check errors above."
  exit 1
fi

echo "Starting Java server..."
# Run from project root; classpath points to Src/ where .class files landed
java -cp Src main
