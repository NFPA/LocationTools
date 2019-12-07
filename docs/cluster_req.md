---
id: cluster_req
title: Setting up Spark Cluster
sidebar_label: Setting up Spark Cluster
---

This section will get you up and running on a distributed cluster (like Cloudera CDH)

## Spark Cluster Config

Versions on which `batch-geocode` has been tested on
> - Spark 2.3.3
> - Scala 2.11
> - Hive [TO DO]
> - Hadoop 2.7.7

We use [Spark on YARN (Cloudera)](https://docs.cloudera.com/documentation/enterprise/5-13-x/topics/cdh_ig_running_spark_on_yarn.html)


## Lucene Index

In order to run the Spark Application, all the nodes in the cluster needs to have the previously built lucene index at the same path. This path is then set for `lucene.index.dir` key in the `[batch-geocode]` section of `driver.ini`

## libpostal and jpostal

In addition to lucene index, all the nodes in the cluster need to have libpostal installed and must have the jpostal java bindings from `jpostal/src/main/jniLibs` after compilation. The `jniLibs` path is passed as config to `spark2-submit` as `--conf spark.driver.extraLibraryPath=/path/to/jniLibs`

> For working with all the nodes in the cluster simulaneously you may want to use [cluster ssh](https://github.com/duncs/clusterssh).
