---
id: console_example
title: Single Mode
sidebar_label: Console Examples
---

## Geocoding Single Address
Change the `input.address` in the `[geocode]` section of `driver.ini` configuration file and execute

```
java -cp target/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --geocode driver.ini
```

## Reverse Geocoding Sinlge Geocode
Change the `ip.lat` and `ip.lon` in the `[reverse-geocode]` section of  `driver.ini` configuration file and execute

```
java -cp target/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --reverse-geocode driver.ini
```

