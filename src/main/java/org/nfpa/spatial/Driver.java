package org.nfpa.spatial;

import org.apache.log4j.Logger;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Driver {
    private static Logger logger = Logger.getLogger("Driver");
    public static void main(String... params) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, org.json.simple.parser.ParseException {
        String option = params[0];
        switch (option){
            case "--process":{
                logger.info("Processing raw TIGER data.");
                new TigerProcessor().main(new String[] {params[1]});
            }
            break;
            case "--index":{
                logger.info("Indexing.");
                new TigerIndexer().main(new String[] {params[1]});
            }
            break;
            case "--search":{
                logger.info("Searching.");
                TigerGeocoder tigerGeocoder = new TigerGeocoder();
                tigerGeocoder.setIndexDirectory(params[1]);
                tigerGeocoder.init();
                String queryAddress = params[2];
                logger.info(tigerGeocoder.search(queryAddress));
            }
            break;
            case "--batch":{
                logger.info("Batch Geocoding");
                BatchGeocoder.main(new String[]{params[1], params[2], params[3]});
            }
            break;
        }
    }
}
