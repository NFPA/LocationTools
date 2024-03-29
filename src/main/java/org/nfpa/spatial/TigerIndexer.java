package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.shape.Shape;
import org.nfpa.spatial.utils.Utils;

import java.io.*;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class TigerIndexer {

    private SpatialContext ctx;
    private SpatialStrategy strategy;
    private Directory directory;
    private ShapeReader shapeReader;
    private static IndexWriter indexWriter;
    private static FileSystem hdfs;
    private String INDEX_DIRECTORY = "index";
    private static Configuration hConf;

    private static Logger logger = Logger.getLogger(TigerIndexer.class);

    TigerIndexer(){
        initGeoStuff();
        initHadoop();
    }

    /*Initialize spatial strategy to process geometric information*/
    void initGeoStuff(){
        this.ctx = JtsSpatialContext.GEO;
        this.shapeReader = this.ctx.getFormats().getReader(ShapeIO.WKT);
        int maxLevels = 8; //precision for geohash
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        this.strategy = new RecursivePrefixTreeStrategy(grid, "GEOMETRY");
    }

    void setIndexDirectory(String indexDir){
        this.INDEX_DIRECTORY = indexDir;
    }

    /*
    * Needed when you use both local and hdfs file systems
    * */
    void initHadoop(){
        hConf = new Configuration();
        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    private void initIndexer() throws IOException{
        this.directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig iwConfig = new IndexWriterConfig(new StandardAnalyzer());
        iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexWriter = new IndexWriter(directory, iwConfig);
        indexWriter.commit();
    }

    private void finishIndexer() throws IOException {
        indexWriter.commit();
        indexWriter.close();
    }

    private void indexFile(String filePath, IndexWriter indexWriter) throws InvalidShapeException, IOException, ParseException {

        Path csvPath = new Path(filePath);
        hdfs = csvPath.getFileSystem(hConf);

        FSDataInputStream inputStream = hdfs.open(csvPath);

        Reader in = new InputStreamReader(inputStream);
        Iterable<CSVRecord> records = CSVFormat.RFC4180
                .withDelimiter('\t')
                .withTrim()
                .withFirstRecordAsHeader()
                .parse(in);

        for (CSVRecord record : records) {
            try{
                indexWriter.addDocument(newDocument(record));
            }
            catch (Exception e){
                logger.info("Bad Record: " + e.toString());
                logger.info(filePath);
            }

        }
        indexWriter.commit();
        logger.info("Index Successful: " + filePath);
    }

    /*
    * This converts a csv line record into a lucene document to index
    * */
    private Document newDocument(CSVRecord record) throws IOException, ParseException {

        Document doc = new Document();

        int[] lAdd = {
                Utils.parseToInt(record.get("LFROMADD"), -1),
                Utils.parseToInt(record.get("LTOADD"), -1)
        };

        int[] rAdd = {
                Utils.parseToInt(record.get("RFROMADD"), -1),
                Utils.parseToInt(record.get("RTOADD"), -1)
        };
        Arrays.sort(lAdd); Arrays.sort(rAdd);
        doc.add(new IntRange("LADDRANGE", new int[] {lAdd[0]}, new int[] {lAdd[1]}));
        doc.add(new IntRange("RADDRANGE", new int[] {rAdd[0]}, new int[] {rAdd[1]}));

        Set<String> headers = record.toMap().keySet();
        for (String name: headers){
            if (name.contains("GEOMETRY")){
                Shape shape = this.shapeReader.read(record.get(name));
                for (Field f : strategy.createIndexableFields(shape)) {
                    doc.add(f);
                }
                doc.add(new StoredField(strategy.getFieldName(), shape.toString()));
            }
            else {
                if (record.get(name) != null){
                    doc.add(new TextField(name, record.get(name), Field.Store.YES));
                }
            }
        }
        return doc;
    }

    private static List<String> getCSVFiles(String baseDir) throws IOException {
        Path tigerProcessedPath = new Path(baseDir);
        hdfs = tigerProcessedPath.getFileSystem(hConf);
        FileStatus[] dirFileStatuses = hdfs.listStatus(tigerProcessedPath);


        List<String> csvFiles = new ArrayList<>();

        for (FileStatus fs : dirFileStatuses) {
            if(fs.getPath().getName().endsWith(".csv")){
                csvFiles.add(baseDir + fs.getPath().getName());
            }
        }
        return csvFiles;
    }

    private static List<String> getAllFilePath(String baseDir) throws IOException {
        Path tigerProcessedPath = new Path(baseDir);
        hdfs = tigerProcessedPath.getFileSystem(hConf);
        List<String> csvFiles = new ArrayList<String>();
        FileStatus[] fileStatuses = hdfs.listStatus(tigerProcessedPath);
        for (FileStatus fileStat : fileStatuses) {
            if (fileStat.isDirectory()) {
                csvFiles.addAll(getAllFilePath(baseDir + fileStat.getPath().getName() + "/"));
            } else {
                if(fileStat.getPath().getName().endsWith(".csv")){
                    csvFiles.add(baseDir + fileStat.getPath().getName());
                }
            }
        }
        return csvFiles;
    }

    void startIndexing(String tigerProcessedDir) throws IOException {
        List<String> csvFiles = getAllFilePath(tigerProcessedDir);

        try {
            initIndexer();
            for (String csvFile : csvFiles ){
                indexFile(csvFile, indexWriter);
            }
            finishIndexer();
        }
        catch (Exception e){
            logger.info(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public static void main (String[] args) throws IOException {
        String TIGER_PROCESSED = args[0];
        String TIGER_INDEX = args[1];

        TigerIndexer tigerIndexer = new TigerIndexer();
        tigerIndexer.initGeoStuff();
        tigerIndexer.initHadoop();
        tigerIndexer.setIndexDirectory(TIGER_INDEX);
        tigerIndexer.startIndexing(TIGER_PROCESSED);
    }
}