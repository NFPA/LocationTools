---
id: requirements
title: Requirements
sidebar_label: Installing Prerequisites
---

## libpostal

Install [libpostal](https://github.com/openvenues/libpostal)

## jpostal

Install [jpostal](https://github.com/openvenues/jpostal)

### Building the Project
- Install GIT and Maven on your laptop 
  
  [Github Desktop](https://desktop.github.com/) or [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) should work. 
  
  [Maven 3.6.0 or higher](https://maven.apache.org/install.html) should work.
- Clone the Location Tools Repo
  
  ```git clone https://github.com/NFPA/LocationTools.git```
- Install local version  
  ```mvn clean install``` 
  > This should produce Locationtools-1.0-snapshot.jar or similar .jar file that will be used for running the application.

### Geo-coder Runtime Requirements
- Installation of Address Pasrsing/Normalization Module  
  - Install Libpostal (Please refer [How to Install Libpostal](https://github.com/openvenues/libpostal)) 
  - Our project uses the JAVA Bindings to Libpostal. (Please refer [How to build Jpostal Bindings](https://github.com/openvenues/jpostal))
  > Note down the path for shared LibPostal Libraries (this will be used later in running the project)

## Batch Mode -
This section will get you up and running on a Distributed Cluster (like Cloudera Distribution Hadoop). In addition to the above steps please run the below commands. 
- Please check if your hadoop services are up and running.
  ```jps```
  > Hadoop version 2.7 and above should be work.
- Check Spark version and Spark Submit version [TO DO add more details here]
  > Spark version 2.0 and above should work. 
- We use [Spark on YARN](https://docs.cloudera.com/documentation/enterprise/5-13-x/topics/cdh_ig_running_spark_on_yarn.html) service. 
