package org.nfpa.spatial;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.serializer.KryoSerializer;
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

    void initSpark(){
        SparkConf conf = new SparkConf()
                .setAppName("BatchGeocoder");
//                .setAppName("BatchGeocoder")
//                .setMaster("local");
        conf.set("spark.serializer", KryoSerializer.class.getName());
        conf.set("spark.kryo.registrator", GeoSparkKryoRegistrator.class.getName());

        spark = SparkSession.builder().config(conf).getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        GeoSparkSQLRegistrator.registerAll(spark.sqlContext());
        Logger.getLogger("org.apache.spark").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
    }

    void initHadoop(){
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

    void batchGeocode(String csvPath, String indexDir, String outputPath,
                      int nPartitions, int numResults, float fraction) throws IOException {

        Dataset<Row> inputDataFrame = spark.read().format("csv")
                .option("sep", "\t")
                .option("inferSchema", true)
                .option("quote", "\u0000")
                .option("header", true)
                .load(csvPath)
                .sample(fraction)
                .repartition(nPartitions);

        inputDataFrame.createOrReplaceTempView("input_data");

        int addressIndex = 0, joinKeyIndex=-1;
        try{
            addressIndex = inputDataFrame.schema().fieldIndex("address");
            joinKeyIndex = inputDataFrame.schema().fieldIndex("join_key");
        } catch (IllegalArgumentException e){
            logger.error("address and join_key must be in header");
            throw e;
        }

        int finalJoinKeyIndex = joinKeyIndex;
        int finalAddressIndex = addressIndex;

        JavaRDD<Row> rawRDD = inputDataFrame.toJavaRDD();
        JavaRDD<Row> newRDD = rawRDD.mapPartitions((FlatMapFunction<Iterator<Row>, Row>) iterator -> {
            GeocodeWrapper geocodeWrapper = new GeocodeWrapper(indexDir);
            List<Row> outputRows = new ArrayList<Row>();
            Map[] result; String joinKey, address; Row currentRow;
            while(iterator.hasNext()){
                currentRow = iterator.next();
                address = currentRow.getString(finalAddressIndex);
                joinKey = finalJoinKeyIndex > -1 ?
                        currentRow.getString(finalJoinKeyIndex) : "" + address.hashCode();
                result = geocodeWrapper.getSearchMap(address, numResults);

                outputRows.add(
                        RowFactory.create(joinKey, address, result)
                );
            }
            return outputRows.iterator();
        });

        ArrayType searchResultsType = DataTypes.createArrayType(
                DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType)
        );

        StructField[] fields = new StructField[]{
                DataTypes.createStructField("geocoder_join_key", DataTypes.StringType, false),
                DataTypes.createStructField("geocoder_address", DataTypes.StringType, true),
                DataTypes.createStructField("geocoder_address_output", searchResultsType, false)
        };

        Dataset outputFrame = spark.createDataFrame(newRDD, DataTypes.createStructType(fields));

        outputFrame.createOrReplaceTempView("geocoded_output");

        String saveToTableQuery = readResource("saveToHive.sql");

        logger.info("Executing: " + saveToTableQuery);

        spark.sql(saveToTableQuery);

        logger.info("Successfully written to hive table");

        jsc.stop();
        spark.stop();
        spark.close();
    }

    public static void main(String[] args) throws IOException {
        String inputCSVPath = args[0];
        String indexPath = args[1];
        String outputPath = args[2];
        int partitions = Integer.parseInt(args[3]);
        int numResults = Integer.parseInt(args[4]);
        float fraction = Float.parseFloat(args[5]);
        BatchGeocoder bg = new BatchGeocoder();
        bg.initSpark();
        bg.initHadoop();
        bg.batchGeocode(inputCSVPath, indexPath, outputPath, partitions, numResults, fraction);
    }
}
