#!/bin/bash
# Setup the Java Classpath to include the
# Proper Jars and Class Files.  Also compile
# the java files, generate javadocs, and
# output some usage help.
# Author: Joseph Pecoraro
# Date: Tuesday May 12, 2009

# The current directory should be the root of the project
PROJ_ROOT=$(pwd)
BIN_DIR="bin"
SRC_DIR="src"

# Setup the Classpath
source $PROJ_ROOT/shell/classpath.sh

# Compile the java files into the BIN directory
javac -d bin/                    \
	$SRC_DIR/chunker/*.java      \
	$SRC_DIR/eve/*.java          \
	$SRC_DIR/examples/*.java     \
	$SRC_DIR/raids/*.java        \
	$SRC_DIR/tests/*.java        \
	$SRC_DIR/util/*.java

# Run Javadocs
javadoc -private -d javadocs/    \
	$SRC_DIR/chunker/*.java      \
	$SRC_DIR/eve/*.java          \
	$SRC_DIR/examples/*.java     \
	$SRC_DIR/raids/*.java        \
	$SRC_DIR/tests/*.java        \
	$SRC_DIR/util/*.java


echo
echo "------------------------"
echo "  Classpath is Setup"
echo "  Java Files Compiled"
echo "   Javadocs Created"
echo "------------------------"
echo
echo " You can run eve with:"
echo "   shell> java eve.Eve 9999"
echo
echo " You can run the client with:"
echo "   shell> java raids.Client 9000 localhost 9000 axel 10 localhost 9999"
echo
echo " You can view the Javadocs with:"
echo "   shell> open javadocs/index.html"
echo
