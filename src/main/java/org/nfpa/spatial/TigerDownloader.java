package org.nfpa.spatial;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TigerDownloader {
    private FTPClient ftp;
    private FTPClientConfig ftpConfig;
    private Properties properties;
    private static Logger logger = Logger.getLogger(TigerDownloader.class);

    private void readProperties() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("tiger.properties");
        properties = new Properties();
        properties.load(in);
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
        ftp.connect(properties.getProperty("tiger.ftp"));
        ftp.setControlKeepAliveTimeout(300);
        ftp.enterLocalPassiveMode();
        ftp.login("anonymous", null);

        logResponse(ftp);
    }

    private void initFTPConfig(){
        ftpConfig = new FTPClientConfig();
    }

    private void downloadData() throws IOException {
        new File(properties.getProperty("tiger.download.dir")).mkdirs();

        List<String> subDirectories = getDownloadDirectories();
        for (String directory: subDirectories){
            ftp.changeWorkingDirectory(properties.getProperty("tiger.base.dir"));
            ftp.changeWorkingDirectory(directory);
            List<String> files = Arrays.asList(ftp.listNames());
            logger.info(files);

            for (String file: files){
                downloadRemoteFile(file);
            }
        }
    }

    private void downloadRemoteFile(String remoteName) throws IOException {
        String localFile = properties.getProperty("tiger.download.dir") +
                "/" + remoteName;
        FileOutputStream fos = new FileOutputStream(localFile);
        ftp.retrieveFile(remoteName, fos);
        fos.close();
        logger.info("COMPLETE: " + remoteName);
    }

    private List<String> getDownloadDirectories(){
        String[] subdirectories = properties.getProperty("tiger.types").split(",");
        return Arrays.asList(subdirectories);
    }

    public static void main(String[] args) throws IOException {
        TigerDownloader downloader = new TigerDownloader();
        downloader.readProperties();
        downloader.initFTPConfig();
        downloader.initFTPClient();
        downloader.downloadData();
    }
}
