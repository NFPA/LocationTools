package org.nfpa.spatial;

import org.apache.lucene.search.Query;

import java.util.HashMap;

public class ModQuery {
    private Query query;
    private HashMap<String, String> inputFields;

    public ModQuery(){
        this.inputFields  = new HashMap();
    }

    public void addInputField(String key, String value){
        this.inputFields.put(key, value);
    }

    public boolean containsInputField(String key){
        return this.inputFields.containsKey(key);
    }

    public String get(String key){
        return this.inputFields.get(key);
    }

    public void setQuery(Query query){
        this.query = query;
    }

    public Query getQuery(){
        return this.query;
    }

}
