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

Sample Geocoding Request:

```bash
curl http://localhost:8080/geocoder/v1?n=1&address=1%20Batterymarch%20Park%20Quincy%20MA
```

Response:
```json
{
    "version": "1.0",
    "input": "1 Batterymarch Park Quincy MA",
    "results": [
        {
            "ADDRESS_SCORE": "60.0",
            "BLKGRPCE": "1",
            "BLKGRPCE10": "1",
            "BLOCKCE10": "1022",
            "COUNTY": "Norfolk",
            "COUNTYFP": "021",
            "FULLNAME": "Batterymarch Park",
            "GEOMETRY": "LINESTRING (-71.02700999999999 42.23033999999999, -71.02670999999998 42.230299999999986, -71.02660999999999 42.23028)",
            "INTPTLAT": "42.23122024536133",
            "INTPTLON": "-71.02692413330078",
            "LFROMADD": "1",
            "LINT_LAT": "42.23033893043786",
            "LINT_LON": "-71.027001978284",
            "LTOADD": "99",
            "NAME": "Massachusetts",
            "PLACE": "Quincy",
            "RFROMADD": "",
            "RTOADD": "",
            "SEARCH_SCORE": "57.5",
            "STATEFP": "25",
            "STUSPS": "MA",
            "SUFFIX1CE": "",
            "TRACTCE": "418003",
            "UACE10": "09271",
            "ZCTA5CE10": "02169",
            "ZIPL": "02169",
            "ZIPR": "",
            "ip_postal_city": "quincy",
            "ip_postal_house_number": "1",
            "ip_postal_road": "batterymarch park",
            "ip_postal_state": "ma"
        }
    ]
}
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


Sample Reverse Geocoding Request:
```bash
curl http://localhost:8080/reverse-geocoder/v1?lat=42.2303&lon=-71.0269&radius=0.01&n=1
```

Response:
```json
{
    "version": "1.0",
    "input": "42.2303, -71.0269",
    "results": [
        {
            "BLKGRPCE": "1",
            "BLKGRPCE10": "1",
            "BLOCKCE10": "1022",
            "COUNTY": "Norfolk",
            "COUNTYFP": "021",
            "FULLNAME": "Batterymarch Park",
            "GEOMETRY": "LINESTRING (-71.02700999999999 42.23033999999999, -71.02670999999998 42.230299999999986, -71.02660999999999 42.23028)",
            "INTPTLAT": "42.23122024536133",
            "INTPTLON": "-71.02692413330078",
            "LFROMADD": "1",
            "LTOADD": "99",
            "NAME": "Massachusetts",
            "PLACE": "Quincy",
            "RFROMADD": "",
            "RTOADD": "",
            "STATEFP": "25",
            "STUSPS": "MA",
            "SUFFIX1CE": "",
            "TRACTCE": "418003",
            "UACE10": "09271",
            "ZCTA5CE10": "02169",
            "ZIPL": "02169",
            "ZIPR": ""
        }
    ]
}
```
