package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator;
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class BatchGeocoder {


    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static SQLContext sqlContext;
    private static Configuration hConf;
    private static TigerGeocoder geocoder;
    private static Logger logger = Logger.getLogger("BatchGeocoder");

    private void initSpark(){
        SparkConf conf = new SparkConf()
                .setAppName("TigerProcessor");
//                .setMaster("local[*]");
        conf.set("spark.serializer", org.apache.spark.serializer.KryoSerializer.class.getName());
        conf.set("spark.kryo.registrator", GeoSparkKryoRegistrator.class.getName());



        spark = SparkSession.builder().config(conf).getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        GeoSparkSQLRegistrator.registerAll(spark.sqlContext());
        Logger.getLogger("org").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);


    }

    private void registerGeocoderUDF(String indexDir) throws IOException, ParseException {
        geocoder = new TigerGeocoder();
        geocoder.init();
        geocoder.setIndexDirectory(indexDir);

        GeocodeWrapper geocodeWrapper = new GeocodeWrapper(geocoder);

        sqlContext = new SQLContext(jsc);
        sqlContext.udf().register("FN_GEOCODE", (String address) -> geocodeWrapper.search(address)
                , DataTypes.StringType);
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

    private void batchGeocode(String csvPath, String outputPath){
        Dataset<Row> dataFrame = spark.read().format("csv")
                .option("sep", ",")
                .option("inferSchema", true)
                .option("quote", "\u0000")
                .option("header", true)
                .load(csvPath);
        dataFrame.show(20);
        dataFrame.createOrReplaceTempView("address_data");

        String query = "select *, FN_GEOCODE(address) as geocoded_address from address_data";

        logger.info("Executing: " + query);

        Dataset resultDF = spark.sql(query);

        logger.info("Writing results to disk");
        resultDF.write()
                .option("header", true)
                .option("delimiter", "\t")
                .option("quote", "\u0000")
                .csv(outputPath + "geocoded/");

        logger.info("Successfully written to disk");
    }

    public static void main(String[] args) throws IOException, ParseException {

        String inputCSVPath = args[0];
        String indexPath = args[1];
        String outputPath = args[2];

        BatchGeocoder bg = new BatchGeocoder();
        bg.initSpark();
        bg.initHadoop();
        bg.registerGeocoderUDF(indexPath);

        bg.batchGeocode(inputCSVPath, outputPath);

        jsc.stop();
    }
}