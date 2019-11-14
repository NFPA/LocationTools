package org.nfpa.spatial;

import org.apache.commons.net.ftp.*;
import org.apache.log4j.Logger;
import org.nfpa.spatial.utils.StateFIPS;
import org.nfpa.spatial.utils.ZipUtils;

import java.io.*;
import java.util.*;

public class TigerDownloader {
    private FTPClient ftp;
    private FTPClientConfig ftpConfig;
    private Properties properties;
    private HashSet<String> statesFIPS = new HashSet<String>();
    private static Logger logger = Logger.getLogger(TigerDownloader.class);

    private static String TIGER_DOWNLOAD_DIR;
    private static String TIGER_BASE_DIR;
    private static String TIGER_FTP;
    private static String TIGER_STATES;
    private static String TIGER_TYPES;
    private static String TIGER_FILTER_TYPES;

    private void readProperties() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("tiger.properties");
        properties = new Properties();
        properties.load(in);

        TIGER_DOWNLOAD_DIR = properties.getProperty("tiger.download.dir");
        TIGER_BASE_DIR = properties.getProperty("tiger.base.dir");
        TIGER_FTP = properties.getProperty("tiger.ftp");
        TIGER_STATES = properties.getProperty("tiger.states");
        TIGER_TYPES = properties.getProperty("tiger.types");
        TIGER_FILTER_TYPES = properties.getProperty("tiger.filter.types");

        for (String state : TIGER_STATES.split(",")){
            statesFIPS.add(StateFIPS.getFIPS(state.trim()));
        }
        logger.info(statesFIPS);
    }

    private void logResponse(FTPClient ftp) throws IOException {
        int reply;
        logger.info(ftp.getReplyString());
        reply = ftp.getReplyCode();

        if(!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            logger.error("FTP server refused connection.");
            System.exit(1);
        }
    }

    private void initFTPClient() throws IOException {
        ftp = new FTPClient();
        ftp.configure(ftpConfig);
        ftp.connect(TIGER_FTP);
        ftp.setControlKeepAliveTimeout(300);
        ftp.enterLocalPassiveMode();
        ftp.login("anonymous", null);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);

        logResponse(ftp);
    }

    private void initFTPConfig(){
        ftpConfig = new FTPClientConfig();
    }

    private void downloadData() throws IOException {
        new File(TIGER_DOWNLOAD_DIR).mkdirs();

        HashSet<String> subDirectories = getDownloadDirectories();
        HashSet<String> filterDirectories = getFilterDirectories();

        logger.info("All files directories: " + subDirectories);
        logger.info("Filter files directories: " +filterDirectories);

        for (String directory: subDirectories){
            ftp.changeWorkingDirectory(TIGER_BASE_DIR);
            ftp.changeWorkingDirectory(directory);
            FTPFile[] files = ftp.listFiles();

            for (FTPFile file: files){
                boolean toDownload = false;
                String fileName = file.getName();
                if (filterDirectories.contains(directory)){
                    if (isValidStateFile(fileName)) toDownload = true;
                }
                else toDownload = true;
                if (toDownload){
                    logger.info("DOWNLOADING: " + fileName + " SIZE: " + file.getSize()/1024 + "KB");
                    downloadRemoteFile(fileName, directory);
                    logger.info("Unzipping: " + fileName);
                    unzipFile(fileName, directory);
                }
            }
        }
    }

    private boolean isValidStateFile(String remoteFileName){
        String fileFIPS = remoteFileName.split("_")[2].substring(0,2);
        return statesFIPS.contains(fileFIPS);
    }

    private void downloadRemoteFile(String remoteName, String subDirectory) throws IOException {
        String typeDir = subDirectory.toLowerCase();
        new File(TIGER_DOWNLOAD_DIR + "/" + typeDir).mkdirs();
        String localFile = TIGER_DOWNLOAD_DIR + "/" + typeDir + "/" + remoteName;

        FileOutputStream fos = new FileOutputStream(localFile);
        ftp.retrieveFile(remoteName, fos);
        fos.close();
        logger.info("COMPLETE: " + remoteName);
    }

    private void unzipFile(String remoteName, String subDirectory) {
        String typeDir = subDirectory.toLowerCase();
        String localFile = TIGER_DOWNLOAD_DIR +
                "/" + typeDir + "/" + remoteName;
        ZipUtils.unzip(localFile,
                TIGER_DOWNLOAD_DIR + "/" + "uncompressed" + "/" +
                        typeDir + "/" + remoteName.split(".zip")[0]
        );
    }

    private HashSet<String> getDownloadDirectories(){
        HashSet<String> subDirectories = new HashSet<String>();
        for (String directory : TIGER_TYPES.split(",")){
            subDirectories.add(directory.trim());
        };
        return subDirectories;
    }

    private HashSet<String> getFilterDirectories(){
        HashSet<String> filterDirectories = new HashSet<String>();
        for (String directory : TIGER_FILTER_TYPES.split(",")){
            filterDirectories.add(directory.trim());
        };
        return filterDirectories;
    }

    public static void main(String[] args) throws IOException {
        TigerDownloader downloader = new TigerDownloader();
        downloader.readProperties();
        downloader.initFTPConfig();
        downloader.initFTPClient();
        downloader.downloadData();
    }
}
