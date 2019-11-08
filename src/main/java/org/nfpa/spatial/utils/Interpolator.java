package org.nfpa.spatial.utils;

import org.apache.lucene.document.Document;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.linearref.LengthIndexedLine;
import java.util.Arrays;

public class Interpolator {
    private static WKTReader wktReader;
    private static GeometryFactory gFactory;

    public Interpolator(){
        wktReader = new WKTReader();
        gFactory = new GeometryFactory();
    }



    private static int[] getAddressRange(Document doc){
        int fromAdd[] = {
                Utils.parseToInt(doc.get("LFROMADD"), -1),
                Utils.parseToInt(doc.get("RFROMADD"), -1)
        };
        int toAdd[] = {
                Utils.parseToInt(doc.get("LTOADD"), -1),
                Utils.parseToInt(doc.get("RTOADD"), -1)
        };
        Arrays.sort(fromAdd); Arrays.sort(toAdd);
        return new int[] {fromAdd[0], toAdd[toAdd.length -1]};
    }

    public Point getInterpolation(Document doc, int hNo, String geometryField) throws ParseException {

        String wktString = doc.get(geometryField);
        int[] addRange = getAddressRange(doc);

        LineString lString = (LineString) wktReader.read(wktString);
        LengthIndexedLine lenIdxLine = new LengthIndexedLine(lString);

        float multiplier = (float) (Math.abs(1.0 *(hNo - addRange[0])/(addRange[0] - addRange[1])));

        Coordinate interCoord = lenIdxLine.extractPoint( multiplier * lenIdxLine.getEndIndex());
        Point interpolatedPoint = gFactory.createPoint(interCoord);

        return interpolatedPoint;
    }
}