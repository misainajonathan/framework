#!/bin/sh
set -e

rm -rf bin
mkdir -p bin
javac -cp "lib/*" -d bin $(find src -name "*.java")

# Include Gson in the framework JAR so applications can load it at runtime.
(cd bin && jar -xf ../lib/gson-2.11.0.jar)
jar -cvf framework-jar.jar -C bin .
rm -rf bin