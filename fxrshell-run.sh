#!/bin/bash

set -e

# --- Config
JAVAFX_VERSION="21.0.6"
JAVAFX_ZIP="openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip"
JAVAFX_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/${JAVAFX_ZIP}"
JAVAFX_DIR="javafx-sdk-${JAVAFX_VERSION}"
JAVAFX_LIB="./${JAVAFX_DIR}/lib"
OUT_DIR="out"

echo "[*] FX-Rshell runner"

# --- JavaFX SDK download
if [ ! -d "$JAVAFX_DIR" ]; then
    echo "[*] JavaFX SDK not found. Downloading..."
    wget -q --show-progress "$JAVAFX_URL" -O "$JAVAFX_ZIP"
    echo "[*] Unzipping JavaFX SDK..."
    unzip -q "$JAVAFX_ZIP"
    rm "$JAVAFX_ZIP"
else
    echo "[*] JavaFX SDK already present."
fi

# --- Compilation
if [ ! -d "$OUT_DIR" ] || [ -z "$(find $OUT_DIR -name '*.class' 2>/dev/null)" ]; then
    echo "[*] Compiling source files..."
    mkdir -p "$OUT_DIR"
    javac \
        --module-path "$JAVAFX_LIB" \
        --add-modules javafx.controls,javafx.fxml \
        -d "$OUT_DIR" $(find src -name "*.java")
else
    echo "[*] Compiled classes already exist. Skipping compilation."
fi

# --- Execution
echo "[*] Running FX-Rshell..."
java \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls,javafx.fxml \
    -cp "$OUT_DIR" Main
