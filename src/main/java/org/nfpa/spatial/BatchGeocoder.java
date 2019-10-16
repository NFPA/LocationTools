package org.nfpa.spatial;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator;
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BatchGeocoder {

    private static JavaSparkContext jsc;
    private static SparkSession spark;
    private static SQLContext sqlContext;
    private static Configuration hConf;
    TigerGeocoder geocoder;

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

    private void registerGeoUDF(String indexDir) throws IOException, ParseException {
        geocoder = new TigerGeocoder();
        geocoder.init();
        geocoder.setIndexDirectory(indexDir);

        sqlContext = new SQLContext(jsc);
        sqlContext.udf().register("FN_GEOCODE", (String address) -> geocoder.search(address).size()
                , DataTypes.IntegerType);
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

    private void callBG(String csvPath){
        Dataset<Row> dataFrame = spark.read().format("csv")
                .option("sep", ",")
                .option("inferSchema", "true")
                .option("header", "true")
                .load(csvPath);
        dataFrame.show(20);
        dataFrame.createOrReplaceTempView("address_data");
        spark.sql("select FN_GEOCODE(address) from address_data").show();

    }

    public static void main(String[] args) throws IOException, ParseException {

        BatchGeocoder bg = new BatchGeocoder();
        bg.initSpark();
        bg.initHadoop();
        bg.registerGeoUDF("index");
        bg.callBG("address_sample.txt");

        jsc.stop();
    }


}
