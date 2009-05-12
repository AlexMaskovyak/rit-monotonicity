#!/bin/bash
# Setup the Java Classpath to include the
# Proper Jars and Class Files.
# Author: Joseph Pecoraro
# Date: Tuesday May 12, 2009

# The current directory should be the root of the project
PROJ_ROOT=$(pwd)
BIN_DIR="bin"

# Include the Jar Files
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/collections-generic-4.01.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/colt-1.2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/concurrent-1.3.4.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/FreePastry-2.1.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-3d-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-algorithms-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-api-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-graph-impl-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-io-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-jai-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/jung-visualization-2.0.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/junit.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/stax-api-1.0.1.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/wstx-asl-3.2.6.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/xmlpull.jar"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/jars/xpp.jar"

# Include the Project Build Files (make sure it at least exists)
mkdir -p "$PROJ_ROOT/$BIN_DIR"
CLASSPATH="$CLASSPATH:$PROJ_ROOT/$BIN_DIR"

# Propagate the Classpath
export CLASSPATH
