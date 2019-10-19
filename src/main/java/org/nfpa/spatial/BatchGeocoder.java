package org.nfpa.spatial;

import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BatchGeocoder {


    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static SQLContext sqlContext;
    private static Configuration hConf;
    private static TigerGeocoder geocoder;
    private static Logger logger = Logger.getLogger("BatchGeocoder");

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

    private void registerGeocoderUDF(String indexDir) throws IOException, ParseException {
        geocoder = new TigerGeocoder();
        geocoder.setIndexDirectory(indexDir);
        geocoder.init();
        geocoder.getAbbreviations();
        
        GeocodeWrapper geocodeWrapper = new GeocodeWrapper(geocoder);

        sqlContext = new SQLContext(jsc);
        sqlContext.udf().register(
                "FN_GEOCODE",
                (String address) -> geocodeWrapper.search(address),
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

    private void batchGeocode(String csvPath, String outputPath) throws IOException {
        Dataset<Row> dataFrame = spark.read().format("csv")
                .option("sep", "\t")
                .option("inferSchema", true)
                .option("quote", "\u0000")
                .option("header", true)
                .load(csvPath);
        dataFrame.show(20);
        dataFrame.createOrReplaceTempView("address_data");

        String query = readResource("geocode.sql");

        logger.info("Executing: " + query);

        Dataset resultDF = spark.sql(query);

        logger.info("Writing results to disk");
        resultDF.show(20);
        resultDF.write()
                .option("header", true)
                .option("delimiter", "\t")
                .option("quote", "\u0000")
                .csv(outputPath + "output/");
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
