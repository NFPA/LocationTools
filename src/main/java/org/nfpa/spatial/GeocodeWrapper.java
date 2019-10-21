package org.nfpa.spatial;

import org.apache.wink.json4j.JSONException;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public class GeocodeWrapper implements Serializable {
    private TigerGeocoder tigerGeocoder;

    GeocodeWrapper(String indexDir) throws IOException {
        this.tigerGeocoder = new TigerGeocoder();
        this.tigerGeocoder.setIndexDirectory(indexDir);
        this.tigerGeocoder.init();
    }

    GeocodeWrapper(TigerGeocoder tigerGeocoder) {
        this.tigerGeocoder = tigerGeocoder;
    }

    String search(String address, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        return tigerGeocoder.search(address, numRes).toString();
    }
}