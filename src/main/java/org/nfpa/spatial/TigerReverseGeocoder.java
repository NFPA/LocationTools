package org.nfpa.spatial;

    /*

*/

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.wink.json4j.JSONException;
import org.locationtech.jts.io.ParseException;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;

public class TigerReverseGeocoder implements Serializable {

    private static SpatialContext ctx;
    private static SpatialStrategy strategy;
    private static Directory directory;

    private static Logger logger = Logger.getLogger(TigerGeocoder.class);


    private static String INDEX_DIRECTORY;
    private static Configuration hConf;
    private static IndexSearcher indexSearcher;


    void init() throws IOException {
        this.initHadoop();
        this.initLucene();
        this.initGeoStuff();
    }

    private static void initGeoStuff() throws IOException {
        ctx = JtsSpatialContext.GEO;
        int maxLevels = 8; //precision for geohash
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        strategy = new RecursivePrefixTreeStrategy(grid, "GEOMETRY");
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

    private List<LinkedHashMap> getResult(TopDocs topDocs, IndexSearcher indexSearcher, Query searchQuery) throws IOException, ParseException, JSONException {
        Document doc;
        LinkedHashMap resultHMap;
        List<LinkedHashMap> results = new ArrayList<LinkedHashMap>();

        if(topDocs.scoreDocs.length == 0) return results;

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);
            resultHMap = getJSONFromDoc(doc);
            results.add(resultHMap);
        }
        return results;
    }

    private LinkedHashMap getJSONFromDoc(Document doc) throws IOException, ParseException, JSONException {
        LinkedHashMap resultHMap = new LinkedHashMap();
        for (IndexableField field : doc) {
            resultHMap.put(field.name(), field.stringValue());
        }
        return resultHMap;
    }

    /*
    * Make point with given lat long and a circle around it with given radius.
    * this will serve as a filter by distance for the search results.
    * then return all results sorted by distance
    * */
    public List<LinkedHashMap> spatialSearch(double ipLat, double ipLon, float radiusKM, int numRes) throws IOException, ParseException, JSONException {

        Point pt = ctx.makePoint(ipLon, ipLat);
        DoubleValuesSource valueSource = strategy.makeDistanceValueSource(pt, DistanceUtils.DEG_TO_KM);
        Sort distSort = new Sort(valueSource.getSortField(false)).rewrite(indexSearcher);

        SpatialArgs args = new SpatialArgs(
                SpatialOperation.Intersects,
                ctx.makeCircle(pt, DistanceUtils.dist2Degrees(radiusKM, DistanceUtils.EARTH_MEAN_RADIUS_KM))
        );

        Query searchQuery = this.strategy.makeQuery(args);
        TopDocs topDocs = indexSearcher.search(searchQuery, numRes, distSort);

        return getResult(topDocs, indexSearcher, searchQuery);
    }

    public static void main (String[] args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, JSONException {
        TigerReverseGeocoder reverseGeocoder = new TigerReverseGeocoder();
        reverseGeocoder.setIndexDirectory(args[0]);
        reverseGeocoder.init();
        float ipLat = Float.parseFloat(args[1]);
        float ipLon = Float.parseFloat(args[2]);
        float radiusKM = Float.parseFloat(args[3]);
        int numRes = Integer.parseInt(args[4]);
        reverseGeocoder.logger.info(reverseGeocoder.spatialSearch(ipLat, ipLon, radiusKM, numRes));
    }
}