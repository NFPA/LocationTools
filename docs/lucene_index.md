---
id: lucene_index
title: Lucene Index from TIGER Census Data
sidebar_label: Building Lucene Index
---

## Download TIGER data

We use TIGER2018 data pulished by United States Census Bureau. TIGER data is in shape file format. We first download the zip files from census website.
You can specify which states to download in the `driver.ini` file by changing the `states` key in `[download]` section. You can specify a comma separated list of US state codes.

> Make sure you have about ~30G for complete US build

```bash
java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --download driver.ini
```

## Preprocess the data for indexing

For lucene to consume the downloaded data, we first convert the downloaded shape files to csv and combine information from multiple file types like `FACES`, `EDGES`, `STATE`, `COUNTY`. For this, we use [GeoSpark](https://github.com/DataSystemsLab/GeoSpark).

```bash
java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --process driver.ini
```

## Build Lucene Indexes
We can now index all the csv files into lucene.

```bash
java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --index driver.ini
```

> Make sure you `driver.ini` config has correct values

You should now have files in the `lucene.index.dir` directory.

It's always a good idea to check the index with Lucene Luke which you can find in [lucene binary releases](https://lucene.apache.org/core/downloads.html) (Lucene >= 8.1)

