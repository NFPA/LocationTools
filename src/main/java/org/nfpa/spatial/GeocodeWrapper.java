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

/*
* This class wraps output of TigerGeocoder.search function into different
* formats for consumption like JSONArray, String and Scala Map for hive.
* Any future format for consumption should be added here.
* */

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

    JSONArray getSearchJSONArray(String address, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        List<LinkedHashMap> interRes = tigerGeocoder.search(address, numRes);
        JSONArray result = new JSONArray();
        for (LinkedHashMap elem: interRes){
            result.add(new JSONObject(elem));
        }
        return result;
    }

    Map[] getSearchMap(String address, int numRes) throws InvocationTargetException, IllegalAccessException, ParseException, IOException, NoSuchFieldException, JSONException {
        List<LinkedHashMap> interRes = tigerGeocoder.search(address, numRes);
        Map[] result = new Map[interRes.size()];
        for (int i=0; i < result.length; i++){
            result[i] = ScalaMapper.toScalaMap(interRes.get(i));
        }
        return result;
    }
}