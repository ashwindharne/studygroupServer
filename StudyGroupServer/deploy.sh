#!/bin/bash

# compile java servlet codes and assemble files into build/libs/editor.war
gradle assemble

# deploy the war file to tomcat
rm -f $CATALINA_BASE/webapps/StudyGroupServer.war
cp build/libs/StudyGroupServer.war $CATALINA_BASE/webapps
