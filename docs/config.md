---
id: config
title: Driver Configuration File
sidebar_label: Configuration
---

The built jar has a `Driver` class which takes the following parameters
  - function to perform [ `download`, `process`, `index`, `goecode`, `batch-geocode`, `reverse-geocode` ]
  - configuration file path

You can find a sample config file `driver.ini` in the root folder of the project.

You need to change the configuration file in order to execute different functions. This file is made up of different sections, each corresponding to a function (except [runtime] section)

```ini
[runtime]

libpostal.so.path = /home/username/..../jpostal/src/main/jniLibs

[process]
uncompressed.dir = TIGER_RAW/uncompressed/
processed.dir = TIGER_RAW/uncompressed/processed/

[download]
ftp = ftp2.census.gov
base.dir = /geo/tiger/TIGER2018/
types = EDGES,FACES,COUNTY,STATE,PLACE
filter.types = EDGES,FACES,PLACE
download.dir = TIGER_RAW
states = DE, CT

[index]
processed.dir = TIGER_RAW/uncompressed/processed/
index.output.dir = TIGER_RAW/index/

[batch-geocode]
input.dir = *.txt
lucene.index.dir = TIGER_RAW/index
hive.output.table = temp.batch_geocoder_output
num.partitions = 2
num.results = 1
input.fraction = 0.9

[geocode]
input.address = 36 Lee Street Worcester MA
lucene.index.dir = TIGER_RAW/index
num.results = 1

[reverse-geocode]
lucene.index.dir = TIGER_RAW/index
ip.lat = 41.6994613
ip.lon = -72.6968585
radius.km = 1.5
num.results = 3
```

> Make sure that `*.dir` paths end in `/` except `input.dir` which can be regular expression

- If you need to include `,` in `input.address` you'll have to escape it like `Batterymarch Park \, Quincy`. However spaces work fine.
