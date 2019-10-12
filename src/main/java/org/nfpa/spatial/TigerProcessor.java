package org.nfpa.spatial;

import org.apache.commons.io.FilenameUtils;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TigerProcessor {

    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static Configuration hConf;

    private void initSpark(){
        SparkConf conf = new SparkConf()
                .setAppName("TigerProcessor")
                .setMaster("local[*]");
        conf.set("spark.serializer", org.apache.spark.serializer.KryoSerializer.class.getName());
        conf.set("spark.kryo.registrator", GeoSparkKryoRegistrator.class.getName());
        spark = SparkSession.builder().config(conf).getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        GeoSparkSQLRegistrator.registerAll(spark.sqlContext());
        Logger.getLogger("org").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
    }

    private void initHadoop(){
        hConf = new Configuration();
        hConf.set("fs.hdfs.impl",
                org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
        );
        hConf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );
    }

    static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static String readFile(String path)  throws IOException {
        Charset encoding = StandardCharsets.UTF_8;
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
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

        List<String> sub_directories = listDirectories(base_dir + directory, true);

        if (directory.equals("edges") || directory.equals("faces")){
            sub_directories = filterStateDirs(sub_directories, state);
        }

        System.out.println(directory + " : " + state + " : " + sub_directories.size());

        ListIterator<String> it = sub_directories.listIterator();

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

    public static void main(String[] args) throws IOException {
        String TIGER_BASE = args[0];

        TigerProcessor fr = new TigerProcessor();
        fr.initSpark();
        fr.initHadoop();

        HashSet<String> availableStates = getUniqueStates(listDirectories(TIGER_BASE + "edges", false));

        Dataset countyDF = fr.readDF(TIGER_BASE, "county", "ALL");
        Dataset placeDF = fr.readDF(TIGER_BASE, "place", "ALL");
        Dataset stateDF = fr.readDF(TIGER_BASE, "state", "ALL");

        countyDF.createOrReplaceTempView("county");
        placeDF.createOrReplaceTempView("place");
        stateDF.createOrReplaceTempView("state");

        String query = readFile("./resources/join.sql");
        System.out.println("Join query:\n" + query);

        Dataset edgesDF, facesDF, joinedData;

        for (String state :availableStates){
            System.out.println("State: " + state);
            facesDF = fr.readDF(TIGER_BASE, "faces", state);
            edgesDF = fr.readDF(TIGER_BASE, "edges", state);

            facesDF.createOrReplaceTempView("faces");
            edgesDF.createOrReplaceTempView("edges");

            joinedData = spark.sql(query);

            joinedData.show(10);

            joinedData
                    .write()
                    .option("header", true)
                    .option("delimiter", "\t")
                    .option("quote", "\u0000")
                    .csv(TIGER_BASE + "processed/" + state);

        }

        jsc.stop();
    }
}
