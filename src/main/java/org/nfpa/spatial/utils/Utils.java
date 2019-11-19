package org.nfpa.spatial.utils;

import java.lang.reflect.Field;

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

    public static void loadLibPostal(String libpostalPath) throws NoSuchFieldException, IllegalAccessException {
        System.setProperty("java.library.path", libpostalPath);
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }
}
