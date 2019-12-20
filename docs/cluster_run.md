---
id: cluster_run
title: Running Spark Application
sidebar_label: Running Spark Application
---

## Driver Configuration
As mentioned in the [Configuration](config.md) section, the `Driver` class picks up parameters from `driver.ini` configuration file. For batch geocoding you need to change parameters of the `[batch-geocode]` sections.

- The format of input files should be `tsv` (tab separated values), which must have `address` and `join_key` columns (headers).
- If the input files do not have header the first column is considered as join_key and the second column is considered as address
- This application outputs data directly to hive. You need to change the `hive.output.table` for each run or else the spark application will fail with `AnalysisException: Table already exists`

## Submitting Spark Application

```bash
spark2-submit --master yarn \
--deploy-mode cluster \
--class "org.nfpa.spatial.Driver" \
--num-executors 80 \
--executor-memory 18G \
--executor-cores 2 \
--files=driver.ini \
--conf "spark.storage.memoryFraction=1" \
--conf "spark.yarn.executor.memoryOverhead=2048" \
--conf spark.executor.extraLibraryPath=/path/to/jniLibs \
--conf spark.driver.extraLibraryPath=/path/to/jniLibs \
path/to/location-tools-1.0-SNAPSHOT.jar --batch-geocode driver.ini
```

You should run `spark2-submit` in either headless mode or in `tmux` session, since batch jobs may take several hours to execute.

## `spark2-submit` Arguments

- --master : URL where Application master for this job will be instantiated. If you are running Spark on YARN, then choose `yarn`. Default is `local`

- --deploy-mode : Choose `cluster` if you want to spin it on one of the node machines on the cluster. Otherwise, choose `client` for local.

- --class : Application driver class name

- --num-executors : Number of executors to spawn
 
- --executor-memory : Memory to be allocated to each executor

- --executor-cores : Number of cpu cores allocated to each executor

- --conf : Arbitary Spark Configuration as `Key=Value` format. For values that contain spaces wrap “key=value” in quotes
  
- `path/to/location-tools-1.0-SNAPSHOT.jar` : Location of the JAR file generated after [building the project](build.md)
  
- --batch-geocode : Direct the `Driver` class to run batch geocoding application
- driver.ini : Path to `driver.ini` configuration file

## Tuning Spark Paramters

Please refer to [this stackoverflow thread](https://stackoverflow.com/questions/37871194/) to estimate spark paramters
