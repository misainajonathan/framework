#!/bin/bash

#  Arret de Tomcat
cd /home/jonathan/Bureau/tomcat/tomcat/bin
./shutdown.sh
cd -

# Definition des variables
APP_NAME="framework"
SRC_DIR="src/main/java"
WEB_DIR="src/main/webapp"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/home/jonathan/Bureau/tomcat/tomcat/webapps"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"
MY_FRAMEWORK_JAR="$LIB_DIR/framework-jar.jar" # Votre JAR actuel

#  Nettoyage et creation du repertoire temporaire
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/WEB-INF/classes
mkdir -p $BUILD_DIR/WEB-INF/lib

#  Copie de votre framework JAR dans le dossier build de l'application
cp -f $MY_FRAMEWORK_JAR $BUILD_DIR/WEB-INF/lib/

#  Compilation des fichiers Java (Test.java, Main.java) en incluant votre JAR
find $SRC_DIR -name "*.java" > sources.txt
javac -cp "$SERVLET_API_JAR:$MY_FRAMEWORK_JAR" -d $BUILD_DIR/WEB-INF/classes @sources.txt
rm sources.txt

#  Copier les fichiers web (web.xml)
cp -r $WEB_DIR/* $BUILD_DIR/

#  Generer le fichier .war
cd $BUILD_DIR || exit
jar -cvf $APP_NAME.war *
cd ..

#  Nettoyage et deploiement dans Tomcat
rm -rf $TOMCAT_WEBAPPS/$APP_NAME
rm -f $TOMCAT_WEBAPPS/$APP_NAME.war
cp -f $BUILD_DIR/$APP_NAME.war $TOMCAT_WEBAPPS/

echo ""
echo "Deploiement termine. Redemarrage de Tomcat..."
echo ""

#  Redemarrage de Tomcat
cd /home/jonathan/Bureau/tomcat/tomcat/bin
./startup.sh
cd -