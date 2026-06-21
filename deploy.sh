mkdir -p build-classes
javac -cp "lib/servlet-api.jar" -d build-classes src/main/java/annotation/Controller.java src/main/java/controler/FrameworkServlet.java
jar -cvf framework-jar.jar -C build-classes .
rm -rf build-classes