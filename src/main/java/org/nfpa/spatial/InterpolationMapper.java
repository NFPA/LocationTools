package org.nfpa.spatial;

import org.apache.lucene.document.Document;
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
import org.geotools.styling.*;
import org.geotools.styling.Font;
import org.geotools.swing.JMapFrame;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.nfpa.spatial.TigerIndexer.parseToInt;

public class InterpolationMapper {
    private static WKTReader wktReader;
    private static MapContent map;
    private static SimpleFeatureType EDGE_TYPE, POINT_TYPE;
    private static DefaultFeatureCollection lineCollection, pointCollection;
    private static Style lineStyle, pointStyle;
    private static int counter = 0;

    public InterpolationMapper() throws IOException {
        initFeatureTypes();
        initStyles();
        lineCollection = new DefaultFeatureCollection();
        pointCollection = new DefaultFeatureCollection();
        wktReader = new WKTReader();
        plotCanvas();
    }

    private static void initFeatureTypes(){
        SimpleFeatureTypeBuilder edgeTypeBuilder = new SimpleFeatureTypeBuilder();
        edgeTypeBuilder.setName("linestring");
        edgeTypeBuilder.add("GEOMETRY", LineString.class);
        edgeTypeBuilder.add("LEGEND", String.class);
        edgeTypeBuilder.setCRS( DefaultGeographicCRS.WGS84);

        EDGE_TYPE = edgeTypeBuilder.buildFeatureType();

        SimpleFeatureTypeBuilder pointTypeBuilder = new SimpleFeatureTypeBuilder();
        pointTypeBuilder.setName("point");
        pointTypeBuilder.add("GEOMETRY", Point.class);
        pointTypeBuilder.add("LEGEND", String.class);
        pointTypeBuilder.setCRS( DefaultGeographicCRS.WGS84);

        POINT_TYPE = pointTypeBuilder.buildFeatureType();
    }

    private static void initStyles(){
        lineStyle = SLD.createLineStyle(Color.RED, 2);
        StyleBuilder lineStyleBuilder = new StyleBuilder();
        String lineAttributeName = "LEGEND";
        Font lineFont = lineStyleBuilder.createFont("Ubuntu", 14.0);
        TextSymbolizer lineTextSymb = lineStyleBuilder.createTextSymbolizer(Color.black, lineFont, lineAttributeName);
        Rule lineRule = lineStyleBuilder.createRule(lineTextSymb);
        lineStyle.featureTypeStyles().get(0).rules().add(lineRule);

        pointStyle = SLD.createPointStyle("circle", Color.BLUE, Color.BLUE, 1, 5);
        StyleBuilder pointStyleBuilder = new StyleBuilder();
        String pointAttributeName = "LEGEND";
        Font pointFont = pointStyleBuilder.createFont("Ubuntu", 18.0);
        TextSymbolizer pointTextSymb = pointStyleBuilder.createTextSymbolizer(Color.black, pointFont, pointAttributeName);
        Rule pointRule = pointStyleBuilder.createRule(pointTextSymb);
        pointStyle.featureTypeStyles().get(0).rules().add(pointRule);

    }

    private static void  plotCanvas() throws IOException {
        map = new MapContent();
        map.setTitle("Interpolations");

        File file = new File("src/main/resources/tl_2018_us_state/tl_2018_us_state.shp");
        if (file == null) {
            System.out.println("No canvas Found");
            JMapFrame.showMap(map);
            return;
        }
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        final FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
        Style usStyle = SLD.createPolygonStyle(Color.LIGHT_GRAY,null, (float) 0.4);
        map.addLayer(new FeatureLayer(featureSource, usStyle));
        reader.close();
        JMapFrame.showMap(map);
    }

    private static void plotLine(LineString line, String legend) throws IOException {

        SimpleFeatureBuilder lineBuilder = new SimpleFeatureBuilder(EDGE_TYPE);
        lineBuilder.add(line);
        lineBuilder.add(legend == null?"":legend);

        SimpleFeature lineFeature = lineBuilder.buildFeature("" + counter++);
        lineCollection.add(lineFeature);

        map.addLayer(new FeatureLayer(lineCollection, lineStyle));
    }

    private static void plotPoint(Point point, String legend) {
        SimpleFeatureBuilder pointBuilder = new SimpleFeatureBuilder(POINT_TYPE);
        pointBuilder.add(point);
        pointBuilder.add(legend == null?"":legend);

        SimpleFeature pointFeature = pointBuilder.buildFeature("" + counter++);
        pointCollection.add(pointFeature);
        map.addLayer(new FeatureLayer(pointCollection, pointStyle));
    }

    private static int[] getAddressRange(Document doc){
        int fromAdd[] = {
                parseToInt(doc.get("LFROMADD"), -1),
                parseToInt(doc.get("RFROMADD"), -1)
        };

        int toAdd[] = {
                parseToInt(doc.get("LTOADD"), -1),
                parseToInt(doc.get("RTOADD"), -1)
        };
        Arrays.sort(fromAdd); Arrays.sort(toAdd);


        return new int[] {fromAdd[0], toAdd[toAdd.length -1]};
    }

    public static void mapWTKInterpolations(Document doc, int hNo) throws ParseException, IOException {

        String wktString = doc.get("GEOMETRY");
        int[] addRange = getAddressRange(doc);

        LineString line = (LineString) wktReader.read(wktString);
        plotLine(line, doc.get("FULLNAME"));

        LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(line);

        float multiplier = (float) (Math.abs(1.0 *(hNo - addRange[0])/(addRange[0] - addRange[1])));

        GeometryFactory gFactory = new GeometryFactory();

        for (int i=0;i<2; i++){
            Coordinate interCoord = lengthIndexedLine.extractPoint( i * lengthIndexedLine.getEndIndex());
            Point interPoint = gFactory.createPoint(interCoord);
            plotPoint(interPoint, "" + addRange[i]);
        }

        Coordinate interCoord = lengthIndexedLine.extractPoint( multiplier * lengthIndexedLine.getEndIndex());
        Point interPoint = gFactory.createPoint(interCoord);
        System.out.println("\n Interpolated Point: " + interPoint.toString());
        plotPoint(interPoint, "" + hNo);
    }
}


//120 Abrams hill rd duxbury ma 02332
//250 Crooked Lane duxbury ma 02332

//45 John St Worcester MA 01609
//220 Green Hill Parkway Worcester ma 01605
