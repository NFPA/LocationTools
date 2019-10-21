package org.nfpa.spatial;

import org.apache.lucene.search.Query;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CompositeQuery {
    private Query query;
    private LinkedHashMap<String, String> inputFields;

    CompositeQuery(){
        this.inputFields  = new LinkedHashMap<>();
    }

    HashMap<String, String> getHashMap(){
        return this.inputFields;
    }

    void addInputField(String key, String value){
        this.inputFields.put(key, value);
    }

    boolean containsInputField(String key){
        return this.inputFields.containsKey(key);
    }

    String get(String key){
        return this.inputFields.get(key);
    }

    void setQuery(Query query){
        this.query = query;
    }

    Query getQuery(){
        return this.query;
    }

}
