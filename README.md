# LocationTools: Geocoding for Huge Datasets 

Geocoding millions of address points is painful, time consuming, and often a very expensive process. That, however, is the problem NFPA encountered with geocoding fire department response information from NFIRS ( [National Fire Incident Reporting System](https://www.usfa.fema.gov/data/nfirs/index.html) ), a data set dissemenated by annually the United States Fire Adminstration (USFA) containing information 20,000+ fire departments across the United States. A single year of  NFIRS data contains records of 20-25 million fire department. While individaul fire departments often collect and store geolocation data for the incidents that they respond to, the public data provdided by USFA only contains street addresses for those incidents.  Complicating matters, those street addresses are often only partially filled out, leaving us with a very large amount of very messy data.

We quickly discovered that standard approaches to geocoding, either through commercial services or existing open source platforms, couldn't produce results that were accurate, fast, and cost-effective for the NFIRS data,  So we launched this project, codenamed 'Wandering Moose', to solve some aspects of spatial analysis on huge datasets for downstream spatial analysis.  Trade-offs, though, are inevitable, and focused on speed and cost at the cost of some of the accuracy.  We made this tradeoff since our use cases didn't require roof-top accuracy, and being "close enough" was "good enough" for what we wanted.   

Whlie this tool is initially geared towards NFIRS type dataset, we presume that it can be extended to any dataset with address fields. We provide you with sufficient documentation related to how it can be setup in different ways (Server or Docker) with code, examples, and sample result outputs. See Documentaion Section.

## Built With

* [IntelliJ Idea](https://www.jetbrains.com/idea/) - JAVA IDE for code development
* [Maven](https://maven.apache.org/) - Dependency Management
* [Lucene](https://lucene.apache.org/core/8_2_0/) - For Indexing and searching geo-spatial data
* [libpostal](https://github.com/openvenues/libpostal) - NLP based address processing/normalization engine
* [US Census Shapefiles](https://www.census.gov/programs-surveys/geography/technical-documentation/complete-technical-documentation/tiger-geo-line.2018.html) - Shapefiles (2018) used for indexing
* [GeoSpark](https://github.com/DataSystemsLab/GeoSpark) - for Spark based geo-processing
* [Vert.x](https://vertx.io/) - For Simple API Deployment
* [CDH](https://docs.cloudera.com/documentation/enterprise/6/6.3/topics/installation.html) - For Cluster Deployment
* [Docker](https://www.docker.com/) - For container based Deployment

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.
You can run the project either by spinning up an API service (locahost:8080) which will geocode addresses sequentially or run in batch mode using Spark-on-YARN service using CDH cluster. Both of these runtimes require you to have: 
1) JAR file built from the project codebase
2) Location to Lucene Indexes
3) Address Parsing libraries 
4) Configuration files to control geocoding parameters (`driver.ini` for batch mode and `vertx-conf.json` for API mode)

The setup is tried and tested only on Linux/Ubuntu 16.04, for Windows the preferred way would be to use the docker.

#### Prerequisites - 

  - Install libpostal from [here](https://github.com/openvenues/libpostal#installation-maclinux)
  - Install Java bindings(jpostal) to libpostal from [here](https://github.com/openvenues/jpostal#building-jpostal)
    Shared libraries will be available at `jpostal/src/main/jniLibs`. This location will be used in the driver configuration later.
  - Intall git(>=2.17) & maven(>=3.6.0)

  ```bash
  sudo apt-get update
  sudo apt-get install git maven
  ```

#### Building the JAR - 

  ```
  git clone https://github.com/NFPA/LocationTools.git
  mvn clean install
  ```

  This generates JAR named `location-tools-1.0-SNAPSHOT.jar` in your `target` directory.

#### Create Configurations File - 

  See sample configuration files `drivier.ini` and `vertx-conf.json` in project root folder.

  The `driver.ini` configuration file serves multiple purposes
  1) Params for Downloading, indexing TIGER Census data and creating lucene indexes (see section `download`, `process` and `index`)
  2) Params for geocoding single address and multiple files (see section `geocode`, `batch-geocode`)
  3) Params for simple reverse geocoding. (see section `reverse-geocode`)

  > [ **Note: Make sure that \*.dir paths end in / except input.dir which can be regular expression** ]

  The `vertx-conf.json` contains params for the webserver. 


#### Build Lucene Index - 

  [ **Note: Make sure you have about ~30G for complete US build** ]

    # Download the TIGER data:
    # We use TIGER2018 data pulished by United States Census Bureau. TIGER data is in shape file format. We first download the zip files from census website. You can specify which states to download in the `driver.ini` file by changing the states key in [download] section. You can specify a comma separated list of US state codes.

    ```java
    java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --download driver.ini
    ```

    # Preprocess the data for indexing:
    # For lucene to consume the downloaded data, we first convert the downloaded shape files to csv and combine information from multiple file types like FACES, EDGES, STATE, COUNTY using GeoSpark functionality.
    
    ```java
    java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --process driver.ini
    ```

    # Build Lucene Indexes:
    # We can now index all the csv files into lucene.
    ```java
    java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --index driver.ini
    ```
    
    # You should now have files in the `lucene.index.dir` directory.
    # It's always a good idea to check the index with Lucene Luke which you can find in [lucene binary releases](https://lucene.apache.org/core/downloads.html) (Lucene >= 8.1)

    # Test Single address:
    ```java
    java -cp target/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --geocode driver.ini
    ```

    # Test Reverse geocode single address:
    ```java
    java -cp target/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --reverse-geocode driver.ini
    ```

## Deployment
Now that you have the JAR file, location for address parsers(jpostal) and location for generated lucene indexes. The app can be deployed using a simple JAVA API webserver using vertx or batch mode using a cluster setup.

#### API WebServer - 
  
  Starting the web server with below command creates two endpoints 
  `/geocoder/v1` - geocoding which can take two arguments `address` and `n` number of results
  `/reverse-geocoder/v1` - geocoding which takes arguments `lat`,`lon`,`n` and `radius`
  
  ```java
  java -Dvertx.options.blockedThreadCheckInterval=9999 \
      -Djava.library.path="/path/to/jniLibs" \
      -jar /path/to/location-tools-1.0-SNAPSHOT.jar \
      -conf vertx-conf.json
  ```
  
  Edit `vertx-conf.json` file to run on different host and port.
  
  **Testing geocoding endpoint:**
  
  ```curl
  curl http://localhost:8080/geocoder/v1?n=1&address=1%20Batterymarch%20Park%20Quincy%20MA
  ```
  Respose:
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
  
  **Testing Reverse geocoding endpoint:** 
  
  ```curl
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

#### Spark Cluster mode - 

 Our assumption is you have already setup a CDH cluster with SparkOnYARN enabled. Please see Cloudera documentation how to [Run Spark application on YARN](https://docs.cloudera.com/documentation/enterprise/5-13-x/topics/cdh_ig_running_spark_on_yarn.html) 
  
  > Note: In order to run the Spark Application, all the nodes in the cluster needs to have the previously built lucene index at the same path. This path is then set for lucene.index.dir key in the [batch-geocode] section of driver.ini
  
  > In addition to lucene index, all the nodes in the cluster need to have libpostal installed and must have the jpostal java bindings from `jpostal/src/main/jniLibs` after compilation. The jniLibs path is passed as config to `spark2-submit` as `--conf spark.driver.extraLibraryPath=/path/to/jniLibs`.
  
  The format of input files should be tsv (tab separated values), first column is taken as `address` and second as `join_key`.
  
  This application outputs data directly to hive. You need to change the `hive.output.table` for each run or else the spark application will fail with `AnalysisException: Table already exists`
  
  ```java
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
  
  > You should run spark2-submit in either headless mode or in tmux session, since batch jobs may take several hours to execute.

#### Tuning Spark Application

  [Tuning Spark Applications](https://docs.cloudera.com/documentation/enterprise/5-13-x/topics/admin_spark_tuning1.html)

## Additional Documentation

  For additional documentation please check out https://nfpa.github.io/LocationTools

## Authors

  * **Jason Yates** - *Initial work* - [Clairvoyant](https://clairvoyantsoft.com/)
  * **Rahul Pande** - [WPI](https://www.linkedin.com/in/pande-rahul/)

## Guidance
  * **Joe Gochal** - [NFPA](https://nfpa.org/)
  * **Mohammed Ayub** - [NFPA](https://nfpa.org/)

  See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

  This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

  * [Clairvoyant](https://clairvoyantsoft.com/) - Shekhar Vemuri, Jason Yates, and team for their help in framing initial versions of the project.
  * [Cloudera](https://www.cloudera.com/) Team for their support.
  * [NFPA Data Analytics](https://www.nfpa.org/News-and-Research/Data-research-and-tools/Data-solutions) Team for testing and providing feedback
