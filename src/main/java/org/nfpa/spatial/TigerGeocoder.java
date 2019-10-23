package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.OrderedJSONObject;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeReader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class TigerGeocoder implements Serializable {

    private static SpatialContext ctx;
    private static SpatialStrategy strategy;
    private static Directory directory;
    private static Interpolator interpolator;
    private static PostalQuery postalQuery;
    private static InterpolationMapper interpolationMapper;
    private static FileSystem hdfs;

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

    private void printResults(TopDocs results, IndexSearcher indexSearcher, CompositeQuery compositeQuery) throws IOException {
        Document doc1;

        if (results.totalHits.value > 0) {
            for (int i=0; i < 3 && i < results.scoreDocs.length; i++){
                logger.info("Result: " + i);

                doc1 = indexSearcher.doc(results.scoreDocs[i].doc);
                logger.info("Score: " + results.scoreDocs[i].score);
                for (IndexableField field : doc1) {
                    System.out.print(field.name() + ":" + field.stringValue() + "\t");
                }
            }
        }
        else {
            logger.info("No results");
        }
    }

    private void mapResults(TopDocs results, IndexSearcher indexSearcher, CompositeQuery compositeQuery) throws IOException, org.locationtech.jts.io.ParseException {

        if (results.totalHits.value < 1) return;

        Document resultDoc = indexSearcher.doc(results.scoreDocs[0].doc);
        int hno = Integer.parseInt(compositeQuery.get("ip_house_number"));

        interpolationMapper.mapWTKInterpolations(resultDoc, hno);
    }

    private JSONArray getResult(TopDocs topDocs, IndexSearcher indexSearcher, CompositeQuery compositeQuery) throws IOException, ParseException, JSONException {
        Document doc;
        LinkedHashMap resultHMap;
        JSONArray results = new JSONArray();

        if(topDocs.scoreDocs.length == 0) return results;

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);
            resultHMap = getJSONFromDoc(doc, compositeQuery);
            resultHMap.put("SCORE", topDocs.scoreDocs[i].score);
            resultHMap.putAll(compositeQuery.getHashMap());
            results.add(new OrderedJSONObject(resultHMap));
        }
        return results;
    }

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
                    resultHMap.put("LINT_LAT", pt.getY());
                    resultHMap.put("LINT_LONG", pt.getX());
                } catch(NumberFormatException nfe){
                    logger.info("Bad house number: " + houseNumber);
                }
            }
        return resultHMap;
    }

    JSONArray search(String address, int numRes) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, JSONException {
        CompositeQuery compositeQuery = postalQuery.makePostalQuery(address);
        Query searchQuery = compositeQuery.getQuery();
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