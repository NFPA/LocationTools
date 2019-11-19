package org.nfpa.spatial;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.INIConfiguration;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Driver {
    private static Logger logger = Logger.getLogger(Driver.class);
    public static void main(String... params) throws IOException, JSONException, InvocationTargetException, IllegalAccessException, ParseException, ConfigurationException {
        String option = params[0];
        String configPath = params[1];

        INIConfiguration config = new INIConfiguration(configPath);

        logger.info(config.getString("global.AppName"));

        switch (option){
            case "--download":{
                logger.info("Processing raw TIGER data...");

                String TIGER_DOWNLOAD_DIR = config.getString("download.download.dir");
                String TIGER_BASE_DIR = config.getString("download.base.dir");
                String TIGER_FTP = config.getString("download.ftp");
                List<String> TIGER_STATES = config.getList("download.states");
                List<String> TIGER_TYPES = config.getList("download.types");
                List<String> TIGER_FILTER_TYPES = config.getList("download.filter.types");

                logger.info("TIGER_DOWNLOAD_DIR: " + TIGER_DOWNLOAD_DIR);
                logger.info("TIGER_BASE_DIR: " + TIGER_BASE_DIR);
                logger.info("TIGER_FTP: " + TIGER_FTP);
                logger.info("TIGER_STATES: " + TIGER_STATES);
                logger.info("TIGER_TYPES: " + TIGER_TYPES);
                logger.info("TIGER_FILTER_TYPES: " + TIGER_FILTER_TYPES);


                new TigerDownloader().download(TIGER_DOWNLOAD_DIR, TIGER_BASE_DIR, TIGER_FTP,
                        TIGER_STATES, TIGER_TYPES, TIGER_FILTER_TYPES);
            }
            break;
            case "--process":{
                logger.info("Processing raw TIGER data...");

                String TIGER_UNCOMPRESSED_DIR = config.getString("process.uncompressed.dir");

                logger.info("TIGER_UNCOMPRESSED_DIR: " + TIGER_UNCOMPRESSED_DIR);

                TigerProcessor.main(new String[] {TIGER_UNCOMPRESSED_DIR});
            }
            break;
            case "--index":{
                logger.info("Indexing...");

                String TIGER_PROCESSED_DIR = config.getString("index.processed.dir");
                String INDEX_OUTPUT_DIR = config.getString("index.index.output.dir");

                logger.info("TIGER_PROCESSED_DIR: " + TIGER_PROCESSED_DIR);
                logger.info("INDEX_OUTPUT_DIR: " + INDEX_OUTPUT_DIR);

                TigerIndexer.main(new String[] {TIGER_PROCESSED_DIR, INDEX_OUTPUT_DIR});
            }
            break;
            case "--geocode":{
                logger.info("Searching...");
                TigerGeocoder.main(new String[]{params[1], params[2], params[3]});
            }
            break;
            case "--batch-geocode":{
                logger.info("Batch Geocoding...");

                String INPUT_DIR = config.getString("batch-geocode.input.dir");
                String LUCENE_INDEX_DIR = config.getString("batch-geocode.lucene.index.dir");
                String HIVE_OUTPUT_TABLE = config.getString("batch-geocode.hive.output.table");
                String NUM_PARTITIONS = config.getString("batch-geocode.num.partitions");
                String NUM_RESULTS = config.getString("batch-geocode.num.results");
                String INPUT_FRACTION = config.getString("batch-geocode.input.fraction");

                logger.info("INPUT_DIR: " + INPUT_DIR);
                logger.info("LUCENE_INDEX_DIR: " + LUCENE_INDEX_DIR);
                logger.info("HIVE_OUTPUT_TABLE: " + HIVE_OUTPUT_TABLE);
                logger.info("NUM_PARTITIONS: " + NUM_PARTITIONS);
                logger.info("NUM_RESULTS: " + NUM_RESULTS);
                logger.info("INPUT_FRACTION: " + INPUT_FRACTION);

                BatchGeocoder.main(new String[]{INPUT_DIR, LUCENE_INDEX_DIR, HIVE_OUTPUT_TABLE, NUM_PARTITIONS, INPUT_FRACTION});
            }
            break;
            case "--reverse-geocode":{
                logger.info("Searching...");

                String LUCENE_INDEX_DIR = config.getString("reverse-geocode.lucene.index.dir");
                String IP_LAT = config.getString("reverse-geocode.ip.lat");
                String IP_LON = config.getString("reverse-geocode.ip.lon");
                String RADIUS_KM = config.getString("reverse-geocode.radius.km");
                String NUM_RESULTS = config.getString("reverse-geocode.num.results");

                logger.info("LUCENE_INDEX_DIR: " + LUCENE_INDEX_DIR);
                logger.info("IP_LAT: " + IP_LAT);
                logger.info("IP_LON: " + IP_LON);
                logger.info("RADIUS_KM: " + RADIUS_KM);
                logger.info("NUM_RESULTS: " + NUM_RESULTS);

                TigerReverseGeocoder.main(new String[]{LUCENE_INDEX_DIR, IP_LAT, IP_LON, RADIUS_KM, NUM_RESULTS});
            }
            break;
        }
    }
}
