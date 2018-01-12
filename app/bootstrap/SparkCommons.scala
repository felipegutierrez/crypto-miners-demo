package bootstrap

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkContext, SparkConf}

object SparkCommons {

  lazy val sparkSession: SparkSession = SparkSession.builder
      .master("local")
      .appName("ApplicationController")
      .getOrCreate()
}
