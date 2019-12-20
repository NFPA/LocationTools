package org.nfpa.spatial.utils;

import java.util.HashMap;

/*
* Mapping for 2 char state code to state FIPS code.
* */

public class StateFIPS {
    private static final HashMap<String, String> stateCDMap = new HashMap();
    static {
        stateCDMap.put("AL", "01");
        stateCDMap.put("AK", "02");
        stateCDMap.put("AZ", "04");
        stateCDMap.put("AR", "05");
        stateCDMap.put("CA", "06");
        stateCDMap.put("CO", "08");
        stateCDMap.put("CT", "09");
        stateCDMap.put("DE", "10");
        stateCDMap.put("DC", "11");
        stateCDMap.put("FL", "12");
        stateCDMap.put("GA", "13");
        stateCDMap.put("HI", "15");
        stateCDMap.put("ID", "16");
        stateCDMap.put("IL", "17");
        stateCDMap.put("IN", "18");
        stateCDMap.put("IA", "19");
        stateCDMap.put("KS", "20");
        stateCDMap.put("KY", "21");
        stateCDMap.put("LA", "22");
        stateCDMap.put("ME", "23");
        stateCDMap.put("MD", "24");
        stateCDMap.put("MA", "25");
        stateCDMap.put("MI", "26");
        stateCDMap.put("MN", "27");
        stateCDMap.put("MS", "28");
        stateCDMap.put("MO", "29");
        stateCDMap.put("MT", "30");
        stateCDMap.put("NE", "31");
        stateCDMap.put("NV", "32");
        stateCDMap.put("NH", "33");
        stateCDMap.put("NJ", "34");
        stateCDMap.put("NM", "35");
        stateCDMap.put("NY", "36");
        stateCDMap.put("NC", "37");
        stateCDMap.put("ND", "38");
        stateCDMap.put("OH", "39");
        stateCDMap.put("OK", "40");
        stateCDMap.put("OR", "41");
        stateCDMap.put("PA", "42");
        stateCDMap.put("RI", "44");
        stateCDMap.put("SC", "45");
        stateCDMap.put("SD", "46");
        stateCDMap.put("TN", "47");
        stateCDMap.put("TX", "48");
        stateCDMap.put("UT", "49");
        stateCDMap.put("VT", "50");
        stateCDMap.put("VA", "51");
        stateCDMap.put("WA", "53");
        stateCDMap.put("WV", "54");
        stateCDMap.put("WI", "55");
        stateCDMap.put("WY", "56");
        stateCDMap.put("AS", "60");
        stateCDMap.put("FM", "64");
        stateCDMap.put("GU", "66");
        stateCDMap.put("MH", "68");
        stateCDMap.put("MP", "69");
        stateCDMap.put("PW", "70");
        stateCDMap.put("PR", "72");
        stateCDMap.put("UM", "74");
        stateCDMap.put("VI", "78");
    }

    public static String getFIPS(String stateCode){
        return stateCDMap.get(stateCode);
    }
}
