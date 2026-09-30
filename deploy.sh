mkdir -p bin
javac -cp "lib/*:lib/*.jar" -d bin $(find src -name "*.java")
jar -cvf framework-jar.jar -C bin .
rm -rf bin