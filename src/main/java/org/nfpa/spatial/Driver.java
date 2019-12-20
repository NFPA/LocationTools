package org.nfpa.spatial;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.INIConfiguration;
import org.apache.log4j.Logger;
import org.apache.spark.SparkFiles;
import org.apache.wink.json4j.JSONException;
import org.locationtech.jts.io.ParseException;
import org.nfpa.spatial.utils.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

public class Driver {
    private static Logger logger = Logger.getLogger(Driver.class);

    /*
    * The driver class accepts function to perform (string like --download) and config
    * file as explained in the documentation. Each section in the config file corresponds
    * to a function.
    * */
    public static void main(String... params) throws IOException, JSONException,
            InvocationTargetException, IllegalAccessException, ParseException,
            ConfigurationException, NoSuchFieldException {

        String option = params[0];
        String configPath = params[1];

        switch (option){
            case "--download":{
                logger.info("Processing raw TIGER data...");
                INIConfiguration config = new INIConfiguration(configPath);

                String TIGER_DOWNLOAD_DIR = config.getString("download.download.dir");
                String TIGER_BASE_DIR = config.getString("download.base.dir");
                String TIGER_FTP = config.getString("download.ftp");
                List TIGER_STATES = config.getList("download.states");
                List TIGER_TYPES = config.getList("download.types");
                List TIGER_FILTER_TYPES = config.getList("download.filter.types");

                logger.info("TIGER_DOWNLOAD_DIR: " + TIGER_DOWNLOAD_DIR);
                logger.info("TIGER_BASE_DIR: " + TIGER_BASE_DIR);
                logger.info("TIGER_FTP: " + TIGER_FTP);
                logger.info("TIGER_STATES: " + TIGER_STATES);
                logger.info("TIGER_TYPES: " + TIGER_TYPES);
                logger.info("TIGER_FILTER_TYPES: " + TIGER_FILTER_TYPES);


                new TigerDownloader().download(
                        TIGER_DOWNLOAD_DIR,
                        TIGER_BASE_DIR,
                        TIGER_FTP,
                        TIGER_STATES,
                        TIGER_TYPES,
                        TIGER_FILTER_TYPES
                );
            }
            break;
            case "--process":{
                logger.info("Processing raw TIGER data...");
                INIConfiguration config = new INIConfiguration(configPath);

                String TIGER_UNCOMPRESSED_DIR = config.getString("process.uncompressed.dir");
                String TIGER_PROCESSED_DIR = config.getString("process.processed.dir");

                logger.info("TIGER_UNCOMPRESSED_DIR: " + TIGER_UNCOMPRESSED_DIR);

                TigerProcessor processor = new TigerProcessor();
                processor.process(TIGER_UNCOMPRESSED_DIR, TIGER_PROCESSED_DIR);
            }
            break;
            case "--index":{
                logger.info("Indexing...");
                INIConfiguration config = new INIConfiguration(configPath);

                String TIGER_PROCESSED_DIR = config.getString("index.processed.dir");
                String INDEX_OUTPUT_DIR = config.getString("index.index.output.dir");

                logger.info("TIGER_PROCESSED_DIR: " + TIGER_PROCESSED_DIR);
                logger.info("INDEX_OUTPUT_DIR: " + INDEX_OUTPUT_DIR);

                TigerIndexer tigerIndexer = new TigerIndexer();
                tigerIndexer.setIndexDirectory(INDEX_OUTPUT_DIR);
                tigerIndexer.startIndexing(TIGER_PROCESSED_DIR);
            }
            break;
            case "--geocode":{
                logger.info("Searching...");
                INIConfiguration config = new INIConfiguration(configPath);

                String LIBPOSTAL_PATH = config.getString("runtime.libpostal.so.path");
                logger.info(LIBPOSTAL_PATH);
                Utils.loadLibPostal(LIBPOSTAL_PATH);

                String LUCENE_INDEX_DIR = config.getString("geocode.lucene.index.dir");
                String INPUT_ADDRESS = config.getString("geocode.input.address");
                Integer NUM_RESULTS = config.getInt("geocode.num.results", 1);

                logger.info("LUCENE_INDEX_DIR: " + LUCENE_INDEX_DIR);
                logger.info("INPUT_ADDRESS: " + INPUT_ADDRESS);
                logger.info("NUM_RESULTS: " + NUM_RESULTS);

                TigerGeocoder tigerGeocoder = new TigerGeocoder();
                tigerGeocoder.setIndexDirectory(LUCENE_INDEX_DIR);
                tigerGeocoder.init();
                logger.info(tigerGeocoder.search(INPUT_ADDRESS, NUM_RESULTS));
            }
            break;
            case "--batch-geocode":{
                logger.info("Batch Geocoding...");

                BatchGeocoder batchGeocoder = new BatchGeocoder();
                batchGeocoder.initSpark();
                batchGeocoder.addFileToContext(configPath);
                INIConfiguration config = batchGeocoder.getConfig(configPath);

                String INPUT_DIR = config.getString("batch-geocode.input.dir");
                String LUCENE_INDEX_DIR = config.getString("batch-geocode.lucene.index.dir");
                String HIVE_OUTPUT_TABLE = config.getString("batch-geocode.hive.output.table");
                Integer NUM_PARTITIONS = config.getInteger("batch-geocode.num.partitions", 100);
                Integer NUM_RESULTS = config.getInteger("batch-geocode.num.results", 1);
                Float INPUT_FRACTION = config.getFloat("batch-geocode.input.fraction");

                logger.info("INPUT_DIR: " + INPUT_DIR);
                logger.info("LUCENE_INDEX_DIR: " + LUCENE_INDEX_DIR);
                logger.info("HIVE_OUTPUT_TABLE: " + HIVE_OUTPUT_TABLE);
                logger.info("NUM_PARTITIONS: " + NUM_PARTITIONS);
                logger.info("NUM_RESULTS: " + NUM_RESULTS);
                logger.info("INPUT_FRACTION: " + INPUT_FRACTION);

                batchGeocoder.initHadoop();
                batchGeocoder.batchGeocode(
                        INPUT_DIR,
                        LUCENE_INDEX_DIR,
                        HIVE_OUTPUT_TABLE,
                        NUM_PARTITIONS,
                        NUM_RESULTS,
                        INPUT_FRACTION
                );
            }
            break;
            case "--reverse-geocode":{
                logger.info("Searching...");
                INIConfiguration config = new INIConfiguration(configPath);

                String LUCENE_INDEX_DIR = config.getString("reverse-geocode.lucene.index.dir");
                Double IP_LAT = config.getDouble("reverse-geocode.ip.lat");
                Double IP_LON = config.getDouble("reverse-geocode.ip.lon");
                Float RADIUS_KM = config.getFloat("reverse-geocode.radius.km", 1.0F);
                Integer NUM_RESULTS = config.getInteger("reverse-geocode.num.results", 1);

                logger.info("LUCENE_INDEX_DIR: " + LUCENE_INDEX_DIR);
                logger.info("IP_LAT: " + IP_LAT);
                logger.info("IP_LON: " + IP_LON);
                logger.info("RADIUS_KM: " + RADIUS_KM);
                logger.info("NUM_RESULTS: " + NUM_RESULTS);

                TigerReverseGeocoder reverseGeocoder = new TigerReverseGeocoder();
                reverseGeocoder.setIndexDirectory(LUCENE_INDEX_DIR);
                reverseGeocoder.init();
                logger.info(reverseGeocoder.spatialSearch(IP_LAT, IP_LON, RADIUS_KM, NUM_RESULTS));
            }
            break;
        }
    }
}
