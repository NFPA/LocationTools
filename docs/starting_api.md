---
id: starting_api
title: Starting API server
sidebar_label: API Server
---

### Starting API Server
```
java -Dvertx.options.blockedThreadCheckInterval=9999 \
-Djava.library.path="/home/rpande/jniLibs" \
-jar target/location-tools-1.0-SNAPSHOT.jar \
-conf vertx-conf.json
```
### Arguments: 
  **Note** : Run `spark2-submit --help` for more configuration options.
  > **Djava.library.path** - Jpostal Library Location
  >
  > **JAR File (-jar)** - Jar File Location
  >
  > **Config File (-conf)** - Location of API Configuration File. Located in Project's src/conf folder. See [API Configuration](Configurations.md#api-configurations) For more Details.

