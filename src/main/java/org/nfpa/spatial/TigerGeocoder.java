package org.nfpa.spatial;

import com.mapzen.jpostal.AddressParser;
import com.mapzen.jpostal.ParsedComponent;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Font;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.shape.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TigerGeocoder {

    private static SpatialContext ctx;
    private static SpatialStrategy strategy;
    private static Directory directory;
    private static ShapeReader shapeReader;
    private static FileSystem hdfs;
    private static AddressParser p;
    private static Map<String, Method> methodMap;
    private static JSONObject abbreviations;


    private static final String INDEX_DIRECTORY = "index";
    private static Configuration hConf;

    private static void initGeoStuff(){
        ctx = JtsSpatialContext.GEO;
        shapeReader = ctx.getFormats().getReader(ShapeIO.WKT);
        int maxLevels = 5; //precision for geohash
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        strategy = new RecursivePrefixTreeStrategy(grid, "GEOMETRY");
    }

    private static void init() {
        initGeoStuff();
        initHadoop();
        initLibPostal();
    }

    private static void initLibPostal() {
        System.out.println("Jpostal shared library: " + System.getProperty("java.library.path"));
        p = AddressParser.getInstance();
        methodMap = new HashMap<String, Method>();

        HashMap<String, String> libPostalMap = new HashMap<>();
        libPostalMap.put("house_number", "addHouseClause");
        libPostalMap.put("road", "addStreetClause");
        libPostalMap.put("city", "addCityClause");
        libPostalMap.put("postcode", "addZipClause");
        libPostalMap.put("state", "addStateClause");

        libPostalMap.forEach((k, v) -> {
            try {
                methodMap.put(k, TigerGeocoder.class.getDeclaredMethod(v, Query.class, String.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });

    }

    private static void initHadoop(){
        hConf = new Configuration();
        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/core-site.xml"));
        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/hdfs-site.xml"));

        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    private void spatialSearch() throws IOException {
        IndexReader indexReader = DirectoryReader.open(this.directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Point pt;

//        pt = ctx.makePoint(- 70.558924, 41.598509); //MASS
        pt = ctx.makePoint(- 78.764386, 39.680944); //MARYLAND

        System.out.println("Searching: " + pt);
        DoubleValuesSource valueSource = strategy.makeDistanceValueSource(pt, DistanceUtils.DEG_TO_KM);
        Sort distSort = new Sort(valueSource.getSortField(false)).rewrite(indexSearcher);


        SpatialArgs args = new SpatialArgs(
                SpatialOperation.Intersects,
                ctx.makeCircle(pt, DistanceUtils.dist2Degrees(0.05, DistanceUtils.EARTH_MEAN_RADIUS_KM))
        );

        Query query = this.strategy.makeQuery(args);
        TopDocs docs = indexSearcher.search(query, 10, distSort);

        System.out.println(docs.totalHits);

        Document doc1 = indexSearcher.doc(docs.scoreDocs[0].doc);

        for (IndexableField field : doc1){
            System.out.println(field.name() + ":" + field.stringValue());
        }
    }

    private static Query addHouseClause(Query q, String houseNumber){
        int hno = Integer.parseInt(houseNumber);
        Query rAddQuery = IntRange.newContainsQuery("RADDRANGE", new int[]{hno}, new int[]{hno});
        Query lAddQuery = IntRange.newContainsQuery("LADDRANGE", new int[]{hno}, new int[]{hno});
        BooleanQuery.Builder hnoQueryBuilder = new BooleanQuery.Builder();

        hnoQueryBuilder.add(q, BooleanClause.Occur.MUST);
        hnoQueryBuilder.add(rAddQuery, BooleanClause.Occur.SHOULD);
        hnoQueryBuilder.add(lAddQuery, BooleanClause.Occur.SHOULD);
        return hnoQueryBuilder.build();
    }

    private static Query addStreetClause(Query q, String street){
        BooleanQuery.Builder streetQueryBuilder = new BooleanQuery.Builder();
        streetQueryBuilder.add(q, BooleanClause.Occur.MUST);

        String[] elems = street.split("\\s+");
        if (elems.length > 1){
            SpanQuery[] clauses = new SpanQuery[elems.length];
            for (int i=0; i<clauses.length; i++){
                clauses[i] = new SpanMultiTermQueryWrapper(new FuzzyQuery(new Term("FULLNAME", elems[i]), 2));
            }
            SpanNearQuery query = new SpanNearQuery(clauses, 0
                    , true);
            streetQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
            return streetQueryBuilder.build();
        } else {
            streetQueryBuilder.add(new FuzzyQuery(new Term("FULLNAME", street), 2), BooleanClause.Occur.SHOULD);
            return streetQueryBuilder.build();
        }
    }

    private static Query addCityClause(Query q, String city){
        BooleanQuery.Builder cityQueryBuilder = new BooleanQuery.Builder();
        cityQueryBuilder.add(q, BooleanClause.Occur.MUST);

        String[] elems = city.split("\\s+");
        if (elems.length > 1){
            SpanQuery[] clauses = new SpanQuery[elems.length];
            for (int i=0; i<clauses.length; i++){
                clauses[i] = new SpanMultiTermQueryWrapper(new FuzzyQuery(new Term("PLACE", elems[i]), 2));
            }
            SpanNearQuery query = new SpanNearQuery(clauses, 0
                    , true);
            cityQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
            return cityQueryBuilder.build();
        } else {
            cityQueryBuilder.add(new FuzzyQuery(new Term("PLACE", city), 2), BooleanClause.Occur.MUST);
            return cityQueryBuilder.build();
        }
    }

    private static Query addZipClause(Query q, String zip){
        Query ziplQuery = new TermQuery(new Term("ZIPL", zip));
        Query ziprQuery = new TermQuery(new Term("ZIPR", zip));
        BooleanQuery.Builder zipQueryBuilder = new BooleanQuery.Builder();

        zipQueryBuilder.add(q, BooleanClause.Occur.MUST);
        zipQueryBuilder.add(ziplQuery, BooleanClause.Occur.SHOULD);
        zipQueryBuilder.add(ziprQuery, BooleanClause.Occur.SHOULD);
        return zipQueryBuilder.build();
    }

    private static Query addStateClause(Query q, String state){
        Query stuspsQuery = new TermQuery(new Term("STUSPS", state));
        Query stNameQuery = new TermQuery(new Term("NAME", state));
        BooleanQuery.Builder stateQueryBuilder = new BooleanQuery.Builder();

        stateQueryBuilder.add(q, BooleanClause.Occur.MUST);
        stateQueryBuilder.add(stuspsQuery, BooleanClause.Occur.SHOULD);
        stateQueryBuilder.add(stNameQuery, BooleanClause.Occur.SHOULD);
        return stateQueryBuilder.build();
    }

    private static String replaceWithAbbrev(String comp){
        String[] elems = comp.split("\\s+");
        String replacement;
        for (int i=0 ; i < elems.length; i++){
            replacement = (String) abbreviations.get(elems[i]);
            if (replacement != null){
                elems[i] = replacement;
            }
        }
        return String.join(" ", elems);
    }

    private static Query makePostalQuery(String address) throws InvocationTargetException, IllegalAccessException {

        ParsedComponent[] addComp = p.parseAddress(address);
        Query query = new MatchAllDocsQuery();

        for(ParsedComponent comp: addComp){
            if (methodMap.containsKey(comp.getLabel())) {
                System.out.println(comp.getLabel() + " : " + comp.getValue());
                System.out.println(comp.getValue() + " --> " + replaceWithAbbrev(comp.getValue()));
//                Object[] parameters = {query, comp.getValue()};
                Object[] parameters = {query, replaceWithAbbrev(comp.getValue())};
                query = (Query) methodMap.get(comp.getLabel()).invoke(null, parameters);
            }
        }
        return query;
    }

    private void printResults(TopDocs results, IndexSearcher indexSearcher, Query q) throws IOException {
        Document doc1;
        System.out.println(results.totalHits.value);

        if (results.totalHits.value > 0) {
            for (int i=0; i < 3 && i < results.scoreDocs.length; i++){
//          for (int i=0; i < results.scoreDocs.length; i++){
                System.out.println("Result: " + i);
//                Explanation exp = indexSearcher.explain(q, i);
//                for (Explanation nexp: exp.getDetails()){
//                    System.out.println(nexp.getDescription());
//                }
                doc1 = indexSearcher.doc(results.scoreDocs[i].doc);
                System.out.println("Score: " + results.scoreDocs[i].score);
                for (IndexableField field : doc1) {
                    System.out.print(field.name() + ":" + field.stringValue() + "\t");
                }
//                System.in.read();
            }
        }
        else {
            System.out.println("No results");
        }
    }

    private void search(String address) throws IOException, IllegalAccessException, InvocationTargetException, ParseException {

        directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Query searchQuery = makePostalQuery(address);
        TopDocs topDocs = indexSearcher.search(searchQuery, 20);
        printResults(topDocs, indexSearcher, searchQuery);
//        mapResults(topDocs, indexSearcher);
    }

    private void mapResults(TopDocs results, IndexSearcher indexSearcher) throws IOException, org.locationtech.jts.io.ParseException {

        if (results.totalHits.value < 1) return;

        MapContent map = new MapContent();
        map.setTitle("Search Results");

        WKTReader wktReader = new WKTReader();
        LineString line;


        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("edge");
        typeBuilder.add("the_geom", LineString.class);
        typeBuilder.add("COUNTY", String.class);
        typeBuilder.setCRS( DefaultGeographicCRS.WGS84);

        SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        Document doc1;
        DefaultFeatureCollection lineCollection = new DefaultFeatureCollection();;
        if (results.totalHits.value > 0) {
            for (int i = 0; i < results.scoreDocs.length; i++) {
                doc1 = indexSearcher.doc(results.scoreDocs[i].doc);
                line = (LineString) wktReader.read(doc1.get("GEOMETRY"));

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(TYPE);
                builder.add(line);
                builder.add(doc1.get("COUNTY") + " " + doc1.get("ZIPL"));

                SimpleFeature lineFeature = builder.buildFeature("" + i);
                lineCollection.add(lineFeature);
            }
        }

        Style style = SLD.createLineStyle(Color.RED, 2);
        StyleBuilder styleBuilder = new StyleBuilder();
        String attributeName = "COUNTY";
        Font font = styleBuilder.createFont("Ubuntu", 18.0);
        TextSymbolizer textSymb = styleBuilder.createTextSymbolizer(Color.black, font, attributeName);
        Rule rule = styleBuilder.createRule(textSymb);
        style.featureTypeStyles().get(0).rules().add(rule);

        map.addLayer(new FeatureLayer(lineCollection, style));

        File file = new File("src/main/resources/tl_2018_us_state/tl_2018_us_state.shp");
        if (file == null) {
            System.out.println("No canvas Found");
            JMapFrame.showMap(map);
            return;
        }
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        final FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
        Style usStyle = SLD.createPolygonStyle(Color.BLUE,null, (float) 0.4);
        map.addLayer(new FeatureLayer(featureSource, usStyle));
        reader.close();

        JMapFrame.showMap(map);

    }

    private void getAbbreviations() throws IOException, org.json.simple.parser.ParseException {
        FileReader reader = new FileReader("./src/main/resources/abbreviations.json");
        JSONParser jsonParser = new JSONParser();
        abbreviations = (JSONObject) jsonParser.parse(reader);
        System.out.println(abbreviations.toJSONString());
    }

    public static void main (String[] args) throws IOException, IllegalAccessException, InvocationTargetException, ParseException, org.json.simple.parser.ParseException {
        TigerGeocoder tigerGeocoder = new TigerGeocoder();
        tigerGeocoder.init();
        tigerGeocoder.getAbbreviations();
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("\nEnter Address:");
            tigerGeocoder.search(scanner.nextLine());
        }

    }
}