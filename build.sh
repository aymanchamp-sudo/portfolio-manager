#!/bin/bash
# PortfolioX — Build & Run Script
# Usage:  ./build.sh          (compile + run on port 8080)
#         ./build.sh compile  (compile only)
#         ./build.sh clean    (remove compiled output)
#         ./build.sh run 9090 (run on custom port)

set -e

# Detect javac / java
if command -v javac &>/dev/null; then
  JAVAC=javac; JAVA=java
else
  JDK=/usr/lib/jvm/java-21-openjdk-amd64/bin
  JAVAC="$JDK/javac"; JAVA="$JDK/java"
fi

SRC_DIR="src/main/java"
OUT_DIR="out"
MAIN_CLASS="com.portfolio.PortfolioServer"

case "${1:-run}" in
  clean)
    echo "[Build] Cleaning output..."
    rm -rf "$OUT_DIR"
    echo "[Build] Done."
    ;;
  compile)
    echo "[Build] Compiling Java sources..."
    mkdir -p "$OUT_DIR"
    find "$SRC_DIR" -name "*.java" > sources.txt
    $JAVAC -d "$OUT_DIR" @sources.txt
    rm sources.txt
    echo "[Build] Compilation successful."
    ;;
  run|*)
    echo "[Build] Compiling..."
    mkdir -p "$OUT_DIR" data
    find "$SRC_DIR" -name "*.java" > sources.txt
    $JAVAC -d "$OUT_DIR" @sources.txt
    rm sources.txt
    echo "[Build] Starting server on port ${2:-8080}..."
    $JAVA -cp "$OUT_DIR" "$MAIN_CLASS" "${2:-8080}"
    ;;
esac
