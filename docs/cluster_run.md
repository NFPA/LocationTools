---
id: cluster_run
title: Building the project
sidebar_label: Building JAR
---

### Spark Submit Command

```
spark2-submit --master yarn \
 --deploy-mode cluster \
 --class "org.nfpa.spatial.Driver" \
 --num-executors 8 \
 --executor-memory 16G \
 --executor-cores 8 \
 --conf "spark.storage.memoryFraction=1" \
 --conf "spark.yarn.executor.memoryOverhead=4096" \
 --conf spark.executor.extraLibraryPath=/home/rpande/jniLibs \
 --conf spark.driver.extraLibraryPath=/home/rpande/jniLibs \
 ./LocationTools/target/location-tools-1.0-SNAPSHOT.jar \
 --batch-geocode hdfs://10.10.10.80:8020/user/rpande/comparison \
 /home/rpande/index/ \
 hdfs://10.10.10.80:8020/user/rpande/comparison/output \
 50 \
 1.0 \
 2>&1 | tee batch.log
```

  #### Arguments:
  **Note** : Run `spark2-submit --help` for more configuration options.
  > **Master URL (--master)** - URL where Application master for this job will be instantiated. If you have yarn installed then choose `yarn`. Default is `local`.
  >
  > **Deploy Mode (--deploy-mode)**- Place to launch the driver program. Choose `cluster` if you want ot spin it on one of the worker machines on the cluster. Otherwise, choose `client` for local.
  > **Java Class (--class)** - Application Driver Class Name
  >
  > **Executor (--executor-memory)** -
  >
  > **Executor Cores (--executor-cores)** -
  >
  > **Spark Configuration (--conf)** - Arbitary Spark Configuration as `Prop=Value` format
  >
  > **Jar File Location** - Location of your JAR file generated from Getting Started step.
  >
  > **Functionality Tag (--batch-geocode)** - Function to Run. Refer Scripts [TODO Add Driver Script Explanation]
  >
  > **Input File Location** - Location of your input files.
  >
  > **Index location** - Location of Index Files generated from Build Indexes step.
  >
  > **No of Partitions** - Number of output file partitions to create.
  >
  > **No of Results** - Number of results to be returned in output.

