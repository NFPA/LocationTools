package org.nfpa.spatial;

import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Driver {
    public static void main(String... args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, org.json.simple.parser.ParseException {
        String option = args[0];
        switch (option){
            case "--process":{
                System.out.println("Processing");
                new TigerProcessor().main(new String[] {args[1]});
            }
            break;
            case "--index":{
                System.out.println("Indexing");
                new TigerIndexer().main(new String[] {args[1]});
            }
            break;
            case "--search":{
                System.out.println("Searching");
                TigerGeocoder tigerGeocoder = new TigerGeocoder();
                tigerGeocoder.init();
                tigerGeocoder.setIndexDirectory(args[1]);
                String queryAddress = args[2];
                System.out.println(tigerGeocoder.search(queryAddress));
            }
            break;
            case "--batch":{
                System.out.println("Batch Geocoding");
                BatchGeocoder.main(new String[]{args[1], args[2], args[3]});
            }
            break;
        }
    }
}
