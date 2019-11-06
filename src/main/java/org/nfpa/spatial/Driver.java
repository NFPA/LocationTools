package org.nfpa.spatial;

import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Driver {
    private static Logger logger = Logger.getLogger("Driver");
    public static void main(String... params) throws IOException, JSONException, InvocationTargetException, IllegalAccessException, org.locationtech.jts.io.ParseException {
        String option = params[0];
        switch (option){
            case "--process":{
                logger.info("Processing raw TIGER data.");
                TigerProcessor.main(new String[] {params[1]});
            }
            break;
            case "--index":{
                logger.info("Indexing.");
                TigerIndexer.main(new String[] {params[1]});
            }
            break;
            case "--search":{
                logger.info("Searching.");
                TigerGeocoder.main(new String[]{params[1], params[2], params[3]});
            }
            break;
            case "--batch":{
                logger.info("Batch Geocoding");
                BatchGeocoder.main(new String[]{params[1], params[2], params[3], params[4], params[5]});
            }
            break;
        }
    }
}
