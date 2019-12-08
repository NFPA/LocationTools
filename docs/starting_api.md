---
id: starting_api
title: Starting API server
sidebar_label: API Server
---

## Starting API Server

The API is built with [vertx](https://vertx.io/)

```
java -Dvertx.options.blockedThreadCheckInterval=9999 \
-Djava.library.path="/path/to/jniLibs" \
-jar /path/to/location-tools-1.0-SNAPSHOT.jar \
-conf vertx-conf.json
```

#### Parameters

- -Djava.library.path : jpostal shared library location
- -jar : jar file location
- -conf : path to of Vertex API configuration file. Located in project's src/conf folder.
