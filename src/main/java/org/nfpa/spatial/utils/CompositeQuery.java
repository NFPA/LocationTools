package org.nfpa.spatial.utils;

import org.apache.lucene.search.Query;

import java.util.HashMap;
import java.util.LinkedHashMap;

/*
* This class holds all the information about the input address (fields and address score)
* , the lucene query and all the relevant information
* */

public class CompositeQuery {
    private Query query;
    private float addressScore = 0;
    private LinkedHashMap<String, String> inputFields;

    CompositeQuery(){
        this.inputFields  = new LinkedHashMap<>();
    }

    public void addToScore(float score){
        this.addressScore += score;
    }

    public float getAddressScore(){
        return this.addressScore;
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
