package org.nfpa.spatial;

import org.apache.log4j.Logger;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Driver {
    private static Logger logger = Logger.getLogger("Driver");
    public static void main(String... args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, org.json.simple.parser.ParseException {
//        System.setProperty("java.library.path", "/home/ubuntu/jpostal/src/main/jniLibs");
        String option = args[0];
        switch (option){
            case "--process":{
                logger.info("Processing raw TIGER data.");
                new TigerProcessor().main(new String[] {args[1]});
            }
            break;
            case "--index":{
                logger.info("Indexing.");
                new TigerIndexer().main(new String[] {args[1]});
            }
            break;
            case "--search":{
                logger.info("Searching.");
                TigerGeocoder tigerGeocoder = new TigerGeocoder();
                tigerGeocoder.init();
                tigerGeocoder.setIndexDirectory(args[1]);
                String queryAddress = args[2];
                logger.info(tigerGeocoder.search(queryAddress));
            }
            break;
            case "--batch":{
                logger.info("Batch Geocoding");
                BatchGeocoder.main(new String[]{args[1], args[2], args[3]});
            }
            break;
        }
    }
}
