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
import org.geotools.styling.Font;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.nfpa.spatial.TigerIndexer.parseToInt;

public class Interpolator {
    private static WKTReader wktReader;
    private static GeometryFactory gFactory;
    private static MapContent map;
    private static SimpleFeatureType EDGE_TYPE, POINT_TYPE;
    private static DefaultFeatureCollection lineCollection, pointCollection;
    private static Style lineStyle, pointStyle;
    private static int counter = 0;

    public Interpolator() throws IOException {
        wktReader = new WKTReader();
        gFactory = new GeometryFactory();
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

    public Point getInterpolation(Document doc, String house_number) throws ParseException, IOException {

        String wktString = doc.get("GEOMETRY");
        int[] addRange = getAddressRange(doc);
        int hNo = Integer.parseInt(house_number);

        LineString lString = (LineString) wktReader.read(wktString);
        LengthIndexedLine lenIdxLine = new LengthIndexedLine(lString);

        float multiplier = (float) (Math.abs(1.0 *(hNo - addRange[0])/(addRange[0] - addRange[1])));

        Coordinate interCoord = lenIdxLine.extractPoint( multiplier * lenIdxLine.getEndIndex());
        Point interpolatedPoint = gFactory.createPoint(interCoord);

        return interpolatedPoint;
    }
}


//120 Abrams hill rd duxbury ma 02332
//250 Crooked Lane duxbury ma 02332

//45 John St Worcester MA 01609
//220 Green Hill Parkway Worcester ma 01605
