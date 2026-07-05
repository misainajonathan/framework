mkdir -p build-classes
javac -cp "lib/servlet-api.jar" -d build-classes src/main/java/*/*.java
jar -cvf framework-jar.jar -C build-classes .
rm -rf build-classes