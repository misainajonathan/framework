mkdir -p bin
javac -cp "lib/*:lib/servlet-api.jar" -d bin $(find src -name "*.java")
jar -cvf framework-jar.jar -C bin .
rm -rf bin