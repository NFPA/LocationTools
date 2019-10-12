package org.nfpa.spatial;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class TigerProcessor {

    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static Configuration hConf;

    private void initSpark(){
        SparkConf conf = new SparkConf().setAppName("Test").setMaster("local[*]");
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
//        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/core-site.xml"));
//        hConf.addResource(new Path("/home/hduser/hadoop/etc/hadoop/hdfs-site.xml"));

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

    private Dataset readDF(String base_dir, String directory) throws IOException {

        Path dirPath = new Path(base_dir + directory);
        FileSystem fSystem = dirPath.getFileSystem(hConf);
        FileStatus[] dirFileStatuses = fSystem.listStatus(dirPath);

        List<String> directories = new ArrayList<>();

        for (FileStatus fs : dirFileStatuses) {
            directories.add(base_dir + directory + "/" + fs.getPath().getName());
        }
        System.out.println(Arrays.asList(directories));
        ListIterator<String> it = directories.listIterator();

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

    public static void main(String[] args) throws IOException {

        String TIGER_BASE = "hdfs://localhost:9000/user/hduser/TIGER2018/";

        TigerProcessor fr = new TigerProcessor();
        fr.initSpark();
        fr.initHadoop();

        Dataset edgesDF = fr.readDF(TIGER_BASE, "edges");
        Dataset countyDF = fr.readDF(TIGER_BASE, "county");
        Dataset facesDF = fr.readDF(TIGER_BASE, "faces");
        Dataset placeDF = fr.readDF(TIGER_BASE, "place");
        Dataset stateDF = fr.readDF(TIGER_BASE, "state");

        edgesDF.createOrReplaceTempView("edges");
        countyDF.createOrReplaceTempView("county");
        facesDF.createOrReplaceTempView("faces");
        placeDF.createOrReplaceTempView("place");
        stateDF.createOrReplaceTempView("state");

        String query = readFile("./resources/join.sql");

        System.out.println("Join query:\n" + query);

        Dataset joinedData = spark.sql(query);

        joinedData.show(10);

        joinedData
                .write()
                .option("header", true)
                .option("delimiter", "\t")
                .option("quote", "\u0000")
                .csv("hdfs://localhost:9000/user/hduser/TIGER2018/processed");
        jsc.stop();
    }
}
