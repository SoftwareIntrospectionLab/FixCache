#!/bin/bash

JARS=Jar/dbunit-2.4.8.jar:Jar/junit-4.8.1.jar:Jar/log4j-1.2.16.jar:Jar/mysql-connector-java-5.1.13-bin.jar:Jar/slf4j-api-1.6.1.jar:Jar/slf4j-simple-1.6.1.jar:Jar/joda-time-1.6.2/joda-time-1.6.2-sources.jar:Jar/joda-time-1.6.2/joda-time-1.6.2.jar:Jar/javacsv.jar

./CLEAN
mkdir bin
javac -cp $JARS -d bin `find . -name "*.java" `
        
        
