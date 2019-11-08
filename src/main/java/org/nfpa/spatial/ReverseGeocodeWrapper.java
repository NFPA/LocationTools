package org.nfpa.spatial;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.locationtech.jts.io.ParseException;
import org.nfpa.spatial.utils.ScalaMapper;
import scala.collection.immutable.Map;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;

public class ReverseGeocodeWrapper implements Serializable {
    private TigerReverseGeocoder reverseGeocoder;

    ReverseGeocodeWrapper(String indexDir) throws IOException {
        this.reverseGeocoder = new TigerReverseGeocoder();
        this.reverseGeocoder.setIndexDirectory(indexDir);
        this.reverseGeocoder.init();
    }

    ReverseGeocodeWrapper(TigerReverseGeocoder reverseGeocoder) {
        this.reverseGeocoder = reverseGeocoder;
    }

    String searchString(double ipLat, double ipLon, float radiusKM, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        return reverseGeocoder.spatialSearch(ipLat, ipLon, radiusKM, numRes).toString();
    }

    JSONArray getSearchJSONArray(double ipLat, double ipLon, float radiusKM, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        List<LinkedHashMap> interRes = reverseGeocoder.spatialSearch(ipLat, ipLon, radiusKM, numRes);
        JSONArray result = new JSONArray();
        for (LinkedHashMap elem: interRes){
            result.add(new JSONObject(elem));
        }
        return result;
    }

    Map[] getSearchMap(double ipLat, double ipLon, float radiusKM, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        List<LinkedHashMap> interRes = reverseGeocoder.spatialSearch(ipLat, ipLon, radiusKM, numRes);
        Map[] result = new Map[interRes.size()];
        for (int i=0; i < result.length; i++){
            result[i] = ScalaMapper.toScalaMap(interRes.get(i));
        }
        return result;
    }
}