package org.nfpa.spatial;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator;
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BatchGeocoder {


    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static SQLContext sqlContext;
    private static Configuration hConf;
    private static Logger logger = Logger.getLogger("BatchGeocoder");

    private void initSpark(){
        SparkConf conf = new SparkConf()
                .setAppName("BatchGeocoder");
//                .setAppName("BatchGeocoder")
//                .setMaster("local");
        conf.set("spark.serializer", org.apache.spark.serializer.KryoSerializer.class.getName());
        conf.set("spark.kryo.registrator", GeoSparkKryoRegistrator.class.getName());

        spark = SparkSession.builder().config(conf).getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        GeoSparkSQLRegistrator.registerAll(spark.sqlContext());
        Logger.getLogger("org.apache.spark.api.java.JavaSparkContext").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
    }

    private void registerGeocoderUDF(String indexDir) throws IOException {
        GeocodeWrapper geocodeWrapper = new GeocodeWrapper(indexDir);
        sqlContext = new SQLContext(jsc);
        sqlContext.udf().register(
                "FN_GEOCODE",
                (String address) -> {
                    return geocodeWrapper.search(address, 1);
                },
                DataTypes.StringType);
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

    private String readResource(String resName)  throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resName);
        return IOUtils.toString(in);
    }

    private void batchGeocode(String csvPath, String indexDir, String outputPath) throws IOException {
        Dataset<Row> dataFrame = spark.read().format("csv")
                .option("sep", "\t")
                .option("inferSchema", true)
                .option("quote", "\u0000")
                .option("header", true)
                .load(csvPath);
        final int addressIndex = dataFrame.schema().fieldIndex("address");

        JavaRDD<Row> check = dataFrame.toJavaRDD();

        JavaRDD<String> newRDD= check.mapPartitions((FlatMapFunction<Iterator<Row>, String>) iterator -> {
            GeocodeWrapper geocodeWrapper = new GeocodeWrapper(indexDir);
            List<String> addresses = new ArrayList<String>();
            while(iterator.hasNext()){
                Row row = iterator.next();
                addresses.add(
                        geocodeWrapper.search(row.getString(addressIndex), 1)
                );
            }
            return addresses.iterator();
        });
        newRDD.saveAsTextFile(outputPath);
//        logger.info(Arrays.asList(newRDD.collect().toArray()));

//        dataFrame.show(20);
//        dataFrame.createOrReplaceTempView("address_data");
//
//        String query = readResource("geocode.sql");
//
//        logger.info("Executing: " + query);
//
//        Dataset resultDF = spark.sql(query);
//        resultDF.createOrReplaceTempView("result");
//
//        logger.info("Writing results to disk ...");
//        resultDF.show(20);
//        resultDF.write()
//                .option("header", true)
//                .option("delimiter", "\t")
//                .option("escape", "\"")
//                .option("quote", "\"")
//                .csv(outputPath + "output/");
//
////        sqlContext.sql("create table temp.mytable as select * from result");
//
//        logger.info("Successfully written to disk");

        jsc.stop();
        spark.stop();
        spark.close();
    }

    public static void main(String[] args) throws IOException {
        String inputCSVPath = args[0];
        String indexPath = args[1];
        String outputPath = args[2];

        BatchGeocoder bg = new BatchGeocoder();
        bg.initSpark();
        bg.initHadoop();
//        bg.registerGeocoderUDF(indexPath);
        bg.batchGeocode(inputCSVPath, indexPath, outputPath);
    }
}
