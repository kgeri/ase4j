#!/bin/bash

CWD=$(dirname $0)

java -Xmx2048M -XX:+HeapDumpOnOutOfMemoryError -DdataDir=$CWD/data -Dschema=$CWD/schema.xml -Dlogback.configurationFile=$CWD/logback.xml -jar ase4j-server.jar