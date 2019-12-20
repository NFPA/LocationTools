package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.wink.json4j.JSONException;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.nfpa.spatial.utils.CompositeQuery;
import org.nfpa.spatial.utils.Interpolator;
import org.nfpa.spatial.utils.PostalQuery;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;

public class TigerGeocoder implements Serializable {
    private static Directory directory;
    private static Interpolator interpolator;
    private static Query searchQuery;
    private static FileSystem hdfs;
    private PostalQuery postalQuery;

    private static final String IP_HOUSE_FIELD = "ip_postal_house_number";

    private static Logger logger = Logger.getLogger(TigerGeocoder.class);


    private static String INDEX_DIRECTORY;
    private static Configuration hConf;
    private static IndexSearcher indexSearcher;

    void init() throws IOException {
        this.initHadoop();
        this.initLucene();
        this.postalQuery = new PostalQuery();
        interpolator = new Interpolator();
    }

    private void initLucene() throws IOException {
        directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexReader indexReader = DirectoryReader.open(directory);
        indexSearcher = new IndexSearcher(indexReader);
        BooleanSimilarity similarity = new BooleanSimilarity();
        indexSearcher.setSimilarity(similarity);
    }

    private void initHadoop(){
        hConf = new Configuration();
        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    void setIndexDirectory(String dir) {
        INDEX_DIRECTORY = dir;
    }

    /*
    * Get results from lucene TopDocs after lucene query and put the results into a list of hashmaps.
    * List is for multiple search results. Each HashMap is a result from lucene index.
    * It also adds all input fields from CompositeQuery.
    * Composite Query holds Lucene Query and all input fields from libpostal output.
    * */
    private List<LinkedHashMap> getResult(TopDocs topDocs, IndexSearcher indexSearcher, CompositeQuery compositeQuery) throws IOException, ParseException, JSONException {
        Document doc;
        LinkedHashMap resultHMap;
        List<LinkedHashMap> results = new ArrayList<LinkedHashMap>();

        if(topDocs.scoreDocs.length == 0) return results;

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);
            resultHMap = getJSONFromDoc(doc, compositeQuery);
            resultHMap.put("SEARCH_SCORE", "" + topDocs.scoreDocs[i].score);
            resultHMap.put("ADDRESS_SCORE", "" + compositeQuery.getAddressScore());
            resultHMap.putAll(compositeQuery.getHashMap());
            results.add(resultHMap);
        }
        return results;
    }

    /*
    * Convert Lucene search result document to LinkedHashMap (to preserve order).
    * If house number is given in input address, it tries to interpolate on house number.
    * The interpolated lat long from TIGER is always preserved in the output.
    * LineString interpolation output is added as LINT_* out fields in the output HashMap.
    * */
    private LinkedHashMap getJSONFromDoc(Document doc, CompositeQuery compositeQuery) throws IOException, ParseException, JSONException {
        LinkedHashMap resultHMap = new LinkedHashMap();
            for (IndexableField field : doc) {
                resultHMap.put(field.name(), field.stringValue());
            }
            if(compositeQuery.containsInputField(IP_HOUSE_FIELD)){
                String houseNumber = compositeQuery.get(IP_HOUSE_FIELD);
                try{
                    int hNo = Integer.parseInt(houseNumber);
                    Point pt = interpolator.getInterpolation(
                            doc,
                            hNo,
                            "GEOMETRY"
                    );
                    resultHMap.put("LINT_LAT", "" + pt.getY());
                    resultHMap.put("LINT_LON", "" + pt.getX());
                } catch(NumberFormatException nfe){
                    logger.info("Bad house number: " + houseNumber);
                }
            }
        return resultHMap;
    }

    /*
    * 1. Compose a query (CompositeQuery which includes lucene query and input fields parsed by libpostal)
    * 2. Search Lucene Index with this query
    * 3. Format the results and return
    * */
    List<LinkedHashMap> search(String address, int numRes) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, JSONException {
        CompositeQuery compositeQuery = postalQuery.makePostalQuery(address);
        searchQuery = compositeQuery.getQuery();
        TopDocs topDocs = indexSearcher.search(searchQuery, numRes);
        return getResult(topDocs, indexSearcher, compositeQuery);
    }

    public static void main (String[] args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, JSONException {
        TigerGeocoder tigerGeocoder = new TigerGeocoder();
        tigerGeocoder.setIndexDirectory(args[0]);
        tigerGeocoder.init();
        String queryAddress = args[1];
        int numRes = Integer.parseInt(args[2]);
        tigerGeocoder.logger.info(tigerGeocoder.search(queryAddress, numRes));
    }
}