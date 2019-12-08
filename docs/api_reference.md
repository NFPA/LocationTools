---
id: api_reference
title: API reference
sidebar_label: API reference
---

## Geocoding API

Endpoint:

```bash
/geocoder/v1
```
Paramters:
  - address : The address string to geocode
  - n : number of results to return

Response:
```json
{
	"response" : "TO-DO"
}
```

Sample Geocoding Request:

```bash
curl http://localhost:8080/geocoder/v1?n=1&address=1%20Batterymarch%20Park%20Quincy%20MA
```

## Reverse Geocode API

Endpoint:

```bash
/reverse-geocoder/v1
```
Paramters:
- lat : latitude
- lon : longitude
- radius : radius in km to search for (lat, lon) pair
- n : number of results to return

Response:
```json
{
        "response" : "TO-DO"
}
```

Sample Reverse Geocoding Request:
```bash
http://localhost:8080/reverse-geocoder/v1?lat=42.2303&lon=-71.0269&radius=0.01&n=2
```

