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
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator;
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator;
import scala.collection.immutable.Map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
        Dataset<Row> inputDataFrame = spark.read().format("csv")
                .option("sep", "\t")
                .option("inferSchema", true)
                .option("quote", "\u0000")
                .option("header", true)
                .load(csvPath).repartition(1);
        final int addressIndex = inputDataFrame.schema().fieldIndex("address");
        final int joinKeyIndex = inputDataFrame.schema().fieldIndex("join_key");

        JavaRDD<Row> rawRDD = inputDataFrame.toJavaRDD();

        JavaRDD<Row> newRDD = rawRDD.mapPartitions((FlatMapFunction<Iterator<Row>, Row>) iterator -> {
            GeocodeWrapper geocodeWrapper = new GeocodeWrapper(indexDir);
            List<Row> addresses = new ArrayList<Row>();
            Map[] result; String joinKey;
            while(iterator.hasNext()){
                Row row = iterator.next();
                joinKey = row.getString(joinKeyIndex);
                result = geocodeWrapper.search(row.getString(addressIndex), 2);

                addresses.add(
                        RowFactory.create(joinKey, result)
                );
            }
            return addresses.iterator();
        });

        ArrayType searchResultsType = DataTypes.createArrayType(
                DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType)
        );

        StructField[] fields = new StructField[]{
                DataTypes.createStructField("join_key", DataTypes.StringType, false),
                DataTypes.createStructField("address_output", searchResultsType, false)
        };

        Dataset outputFrame = spark.createDataFrame(newRDD, DataTypes.createStructType(fields));

        outputFrame.show(20);
        outputFrame.createOrReplaceTempView("geocoded_output");

        String saveToTableQuery = readResource("saveToHive.sql");

        logger.info("Executing: " + saveToTableQuery);

        spark.sql(saveToTableQuery);

        logger.info("Successfully written to disk");

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
        bg.batchGeocode(inputCSVPath, indexPath, outputPath);
    }
}
