#!/bin/sh

#BUILD can be sample or all
BUILD=sample

echo "Downloading lucene index....."
curl https://tiger-lucene-index.s3.us-east-2.amazonaws.com/lucene-{$BUILD}-states-index.zip -o lucene-index.zip

echo "Unzipping lucene index....."
unzip lucene-index.zip -d /usr/src/

java -Dvertx.options.blockedThreadCheckInterval=9999 -Djava.library.path=/usr/src/jniLibs -jar /usr/src/location-tools-1.0-SNAPSHOT.jar -conf /usr/src/conf/vertx-docker-conf.json
