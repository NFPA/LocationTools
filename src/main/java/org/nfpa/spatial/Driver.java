package org.nfpa.spatial;

import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Driver {
    public static void main(String... args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, org.json.simple.parser.ParseException {
//        new TigerIndexer().main(args);
        new TigerGeocoder().main(args);
    }
}
