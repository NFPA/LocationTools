package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
    public static final String INDEX_DIRECTORY = "index";
    private static Configuration hConf;

    protected void initGeoStuff(){
        this.ctx = JtsSpatialContext.GEO;

        this.shapeReader = this.ctx.getFormats().getReader(ShapeIO.WKT);

        int maxLevels = 5; //precision for geohash

        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);

        this.strategy = new RecursivePrefixTreeStrategy(grid, "GEOMETRY");
    }

    private void initHadoop(){
        hConf = new Configuration();
        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/core-site.xml"));
        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/hdfs-site.xml"));

        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    protected void initIndexer() throws IOException{

        this.directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        IndexWriterConfig iwConfig = new IndexWriterConfig(new StandardAnalyzer());
        iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexWriter = new IndexWriter(directory, iwConfig);
        indexWriter.commit();
    }

    protected void finishIndexer() throws IOException {
        indexWriter.commit();
        indexWriter.close();
    }

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

    private void indexFile(String filePath, IndexWriter indexWriter) throws InvalidShapeException, IOException, ParseException {

        Path csvPath = new Path(filePath);
        hdfs = csvPath.getFileSystem(hConf);

        FSDataInputStream inputStream = hdfs.open(csvPath);

//        System.out.println(IOUtils.toString(inputStream, "UTF-8"));

        Reader in = new InputStreamReader(inputStream);
        Iterable<CSVRecord> records = CSVFormat.RFC4180
                .withDelimiter('\t')
                .withTrim()
                .withFirstRecordAsHeader()
                .parse(in);

        for (CSVRecord record : records) {
            indexWriter.addDocument(newDocument(record));
        }
        indexWriter.commit();
        System.out.println("Index Successful: " + filePath);
    }

    private Document newDocument(CSVRecord record) throws IOException, ParseException {

        Document doc = new Document();

        int lAdd[] = {
                parseToInt(record.get("LFROMADD"), -1),
                parseToInt(record.get("LTOADD"), -1)
        };

        int rAdd[] = {
                parseToInt(record.get("RFROMADD"), -1),
                parseToInt(record.get("RTOADD"), -1)
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
                    doc.add(new TextField(name, record.get(name).toString(), Field.Store.YES));
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

    public static void main (String[] args) throws IOException {
        TigerIndexer tigerIndexer = new TigerIndexer();
        tigerIndexer.initGeoStuff();
        tigerIndexer.initHadoop();

        String TIGER_PROCESSED = args[0];

        List<String> csvFiles = getCSVFiles(TIGER_PROCESSED);

        try {
            tigerIndexer.initIndexer();
            for (String csvFile : csvFiles ){
                tigerIndexer.indexFile(csvFile, indexWriter);
            }
            tigerIndexer.finishIndexer();
        }
        catch (Exception e){
            System.out.println(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        finally {
        }
    }
}