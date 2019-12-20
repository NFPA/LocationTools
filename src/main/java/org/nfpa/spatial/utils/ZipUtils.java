package org.nfpa.spatial.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.log4j.Logger;

/*
* Helper class for unzipping files
* */

public class ZipUtils {
    private static Logger logger = Logger.getLogger(ZipUtils.class);
    public static boolean unzip(String source, String destination){

        try {
            ZipFile zipFile = new ZipFile(source);
            if (zipFile.isEncrypted()) {
                logger.info("File is encrypted");
                return false;
            }
            zipFile.extractAll(destination);
            return true;
        } catch (ZipException e) {
            e.printStackTrace();
            return false;
        }
    }
}
