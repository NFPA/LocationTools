package org.nfpa.spatial.utils;

import org.apache.lucene.search.Query;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class CompositeQuery {
    private Query query;
    private LinkedHashMap<String, String> inputFields;

    CompositeQuery(){
        this.inputFields  = new LinkedHashMap<>();
    }

    public HashMap<String, String> getHashMap(){
        return this.inputFields;
    }

    void addInputField(String key, String value){
        this.inputFields.put(key, value);
    }

    public boolean containsInputField(String key){
        return this.inputFields.containsKey(key);
    }

    public String get(String key){
        return this.inputFields.get(key);
    }

    void setQuery(Query query){
        this.query = query;
    }

    public Query getQuery(){
        return this.query;
    }

}
