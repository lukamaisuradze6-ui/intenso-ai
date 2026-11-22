#!/bin/bash
set -e
mvn clean package dependency:copy-dependencies -DoutputDirectory=target/libs
mkdir -p target/input && cp target/my-javafx-app-1.0-SNAPSHOT.jar target/input/
rm -rf dist && jpackage --type deb --name "Intenso AI" --app-version "1.0.0" --vendor "Kane" --icon src/main/resources/icon.png --input target/input --main-jar my-javafx-app-1.0-SNAPSHOT.jar --main-class com.example.App --module-path target/libs --add-modules javafx.controls,javafx.fxml,java.net.http,java.desktop,jdk.crypto.ec --java-options "-Djava.net.preferIPv4Stack=true" --dest dist --linux-shortcut
sudo apt install --reinstall ./dist/intenso-ai_1.0.0_amd64.deb
