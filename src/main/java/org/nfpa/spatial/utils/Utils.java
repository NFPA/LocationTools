package org.nfpa.spatial.utils;

public class Utils {
    public static int parseToInt(String stringToParse, int defaultValue) {
        int ret;
        try
        {
            ret = Integer.parseInt(stringToParse);
        }
        catch(NumberFormatException ex)
        {
            ret = defaultValue; //Use default value if parsing failed
        }
        return ret;
    }
}
