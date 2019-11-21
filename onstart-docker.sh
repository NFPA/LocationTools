#!/bin/sh

echo "Success"

java -Dvertx.options.blockedThreadCheckInterval=9999 -Djava.library.path=/usr/src/jniLibs -jar /usr/src/location-tools-1.0-SNAPSHOT.jar -conf /usr/src/conf/vertx-docker-conf.json
