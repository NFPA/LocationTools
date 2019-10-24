package org.nfpa.spatial;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.OrderedJSONObject;
import org.locationtech.jts.io.ParseException;
import scala.collection.immutable.Map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;

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

    String searchString(String address, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        return tigerGeocoder.search(address, numRes).toString();
    }

    Map[] search(String address, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        List<LinkedHashMap> interRes = tigerGeocoder.search(address, numRes);
        Map[] result = new Map[interRes.size()];
        for (int i=0; i < result.length; i++){
            result[i] = ScalaMapper.toScalaMap(interRes.get(i));
        }
        return result;
    }
}