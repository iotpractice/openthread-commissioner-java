#!/bin/bash

if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME is not set. Please set it before running the script."
    exit 1
fi

echo "JAVA_HOME is set to: $JAVA_HOME"

if [ -d ot-commissioner ]; then
    echo "Folder 'ot-commissioner' already exists. Skipping checkout."
else
    git clone https://github.com/openthread/ot-commissioner
fi

cd ot-commissioner

./script/bootstrap.sh

## create a hidden build folder.
readonly BUILD_DIR=".build"

mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"
cmake -DCMAKE_INSTALL_PREFIX=/usr/local -GNinja \
    -DBUILD_SHARED_LIBS=OFF \
    -DCMAKE_CXX_STANDARD=11 \
    -DCMAKE_CXX_STANDARD_REQUIRED=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DOT_COMM_JAVA_BINDING=ON \
    ../

ninja commissioner-java
## in ot-commissioner folder
cd ../

## Create JAR library
javac -source 8 -target 8 "$BUILD_DIR"/src/java/io/openthread/commissioner/*.java

cd "$BUILD_DIR"/src/java
find ./io/openthread/commissioner -name "*.class" | xargs jar cvf ../../../../libs/libotcommissioner.jar

cp libcommissioner-java.* ../../../../libs

cd ../../../../libs

echo "JNI libraries copied at `pwd`"