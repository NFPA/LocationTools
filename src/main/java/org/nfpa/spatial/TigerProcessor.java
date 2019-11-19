package org.nfpa.spatial;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.datasyslab.geospark.formatMapper.shapefileParser.ShapefileReader;
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator;
import org.datasyslab.geospark.spatialRDD.SpatialRDD;
import org.datasyslab.geosparksql.utils.Adapter;
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TigerProcessor {

    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static Configuration hConf;
    private static Logger logger = Logger.getLogger("TigerProcessor");

    TigerProcessor(){
        initHadoop();
    }

    private void initSpark(){
        SparkConf conf = new SparkConf()
                .setAppName("TigerProcessor")
                .setMaster("local[*]");
        conf.set("spark.serializer", org.apache.spark.serializer.KryoSerializer.class.getName());
        conf.set("spark.kryo.registrator", GeoSparkKryoRegistrator.class.getName());
        spark = SparkSession.builder().config(conf).getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        GeoSparkSQLRegistrator.registerAll(spark.sqlContext());
        Logger.getLogger("org.apache.spark.api.java.JavaSparkContext").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
    }

    private static void initHadoop(){
        hConf = new Configuration();
        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    String readResource(String resName)  throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resName);
        return IOUtils.toString(in);
    }

    private static List<String> listDirectories(String path, boolean ABS) throws IOException {
        Path dirPath = new Path(path);
        FileSystem fSystem = dirPath.getFileSystem(hConf);
        FileStatus[] dirFileStatuses = fSystem.listStatus(dirPath);

        List<String> directories = new ArrayList<>();

        for (FileStatus fs : dirFileStatuses) {
            if (ABS){
                directories.add(path + "/" + fs.getPath().getName());
            }
            else {
                directories.add(fs.getPath().getName());
            }
        }
        return directories;
    }

    private Dataset readDF(String base_dir, String directory, String state) throws IOException {

        List<String> subDirectories = listDirectories(base_dir + directory, true);

        if (!state.equals("ALL")){
            subDirectories = filterStateDirs(subDirectories, state);
        }

        logger.info(directory + " : " + state + " : " + subDirectories.size());

        ListIterator<String> it = subDirectories.listIterator();

        Dataset returnDF = Adapter.toDf(
                ShapefileReader.readToGeometryRDD(jsc, it.next()),
                spark);

        while (it.hasNext()){
            try{
                SpatialRDD tmpRDD = ShapefileReader.readToGeometryRDD(jsc,
                        it.next());
                Dataset tmpDF = Adapter.toDf(tmpRDD, spark);
                returnDF = returnDF.unionAll(tmpDF);
            } catch (Exception e){

            }
        }

        return  returnDF;
    }

    private static HashSet<String> getUniqueStates(List<String> directories){
        HashSet<String> states = new HashSet();
        for (String dir : directories) {
            states.add(dir.split("_")[2].substring(0, 2));
        }
        return states;
    }

    private static List<String> filterStateDirs(List<String> directories, String state){
        List<String> filteredDirectories = new ArrayList<>();
        for (String dir : directories){
            if (FilenameUtils.getBaseName(dir).startsWith("tl_2018_" + state)){
                filteredDirectories.add(dir);
            }
        }
        return filteredDirectories;
    }

    public static void process(String TIGER_BASE, String TIGER_PROCESSED) throws IOException {

        HashSet<String> availableStates = getUniqueStates(listDirectories(TIGER_BASE + "edges", false));

        Dataset countyDF, placeDF, stateDF,  edgesDF, facesDF, joinedData;

        for (String state :availableStates){
            TigerProcessor processor = new TigerProcessor();
            processor.initSpark();
            String query = processor.readResource("tigerJoin.sql");

            countyDF = processor.readDF(TIGER_BASE, "county", "ALL");
            placeDF = processor.readDF(TIGER_BASE, "place", "ALL");
            stateDF = processor.readDF(TIGER_BASE, "state", "ALL");

            countyDF.createOrReplaceTempView("county");
            placeDF.createOrReplaceTempView("place");
            stateDF.createOrReplaceTempView("state");

            logger.info("STATE FIPS: " + state);

            facesDF = processor.readDF(TIGER_BASE, "faces", state);
            edgesDF = processor.readDF(TIGER_BASE, "edges", state);

            facesDF.createOrReplaceTempView("faces");
            edgesDF.createOrReplaceTempView("edges");

            logger.info("Executing join query:\n" + query);
            joinedData = spark.sql(query);
            logger.info("Query executed successfully");

            joinedData.head();

            joinedData
                    .write()
                    .option("header", true)
                    .option("delimiter", "\t")
                    .option("quote", "\u0000")
                    .csv(TIGER_PROCESSED + state);
            spark.stop();
            spark.close();
            jsc.stop();
            System.gc();
        }
    }
}
