package org.nfpa.spatial;

import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public class GeocodeWrapper implements Serializable {
    private TigerGeocoder tigerGeocoder;
    private String result;

    public GeocodeWrapper(TigerGeocoder tigerGeocoder) {
        this.tigerGeocoder = tigerGeocoder;
    }
    public String search(String address) throws InvocationTargetException, IllegalAccessException, ParseException, IOException {
        result =  tigerGeocoder.search(address).toJSONString();
        return result;
    }
}
