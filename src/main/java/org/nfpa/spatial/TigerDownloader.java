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
    private HashSet<String> statesFIPS = new HashSet<String>();
    private static Logger logger = Logger.getLogger(TigerDownloader.class);

    private static String TIGER_DOWNLOAD_DIR;
    private static String TIGER_BASE_DIR;
    private static String TIGER_FTP;
    private static List<String> TIGER_STATES;
    private static List<String> TIGER_TYPES;
    private static List<String> TIGER_FILTER_TYPES;

    private void readProperties() throws IOException {
        for (String state : TIGER_STATES){
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

    private void downloadTIGERData() throws IOException {
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
        for (String directory : TIGER_TYPES){
            subDirectories.add(directory.trim());
        };
        return subDirectories;
    }

    private HashSet<String> getFilterDirectories(){
        HashSet<String> filterDirectories = new HashSet<String>();
        for (String directory : TIGER_FILTER_TYPES){
            filterDirectories.add(directory.trim());
        };
        return filterDirectories;
    }

    public void download(String tigerDownloadDir, String tigerBaseDir, String tigerFTP,
                         List<String> tigerStates, List<String> tigerTypes, List<String> tigerFilterTypes) throws IOException {
        TIGER_DOWNLOAD_DIR = tigerDownloadDir;
        TIGER_BASE_DIR = tigerBaseDir;
        TIGER_FTP = tigerFTP;
        TIGER_STATES = tigerStates;
        TIGER_TYPES = tigerTypes;
        TIGER_FILTER_TYPES = tigerFilterTypes;
        readProperties();
        initFTPConfig();
        initFTPClient();
        downloadTIGERData();
    }

    public static void main(String[] args) {
        logger.info("Use download method with config instead of main");
    }
}
