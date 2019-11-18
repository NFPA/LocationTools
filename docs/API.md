---
id: APISearch
title: Search using API
sidebar_label: API Search
---

## Start the WebServer
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

## Enpoints - 

### Geocode  

    Request

```
http://localhost:8080/geocoder/v1?n=1&address=36%20john%20st%20worcwster
```
    Response
```json
"todo": "add details here"
```

### Reverse Geocode

    Request
```
http://localhost:8080/reverse-geocoder/v1?lat=42.23033786087573&lon=-71.02699395656802&radius=0.01&n=2
```
      Response
```json
"todo": "add details here"
```

## [TO-DO]
  - [ ] Authentication 
  - [ ] Pagination
  - [ ] Conditional Requests