#!/bin/bash

# This script decompiles an app's apk to retrieve the sources.

# =============================================================================

# Read command-line arguments.
SRC_DIR="$1"
RES_DIR="$2"
CLS_DIR="$3"
APK_DIR="$4"
DEX2JAR_DIR="$5"
JDEC_DIR="$6"

# Create directories to store the app's sources, class files and resources.
mkdir -p "$SRC_DIR" "$CLS_DIR" "$RES_DIR"

# Unpack the apk.
cd "$APK_DIR"
unzip "$(ls | grep '^.*\.apk$' | head -1)" -d "$RES_DIR"
cd "$RES_DIR"
mv classes.dex "$DEX2JAR_DIR"

# Dedex the app's code into a jar file.
cd "$DEX2JAR_DIR"
./dex2jar.sh classes.dex
rm classes.dex
mv classes_dex2jar.jar "$CLS_DIR/classes.jar"

# Extract classes from the jar file.
cd "$CLS_DIR"
jar xf classes.jar

# Decompile the class files to retrieve the app's source.
cd "$JDEC_DIR"
java -cp .:jdec.jar net.sf.jdec.main.ConsoleLauncher \
    -jar "$CLS_DIR/classes.jar" -outputFolder "$SRC_DIR"

# Remove the jar file.
rm -f "$CLS_DIR/classes.jar"
