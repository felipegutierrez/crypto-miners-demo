package bootstrap

import org.apache.spark.sql.SparkSession

object SparkCommons {

  lazy val sparkSession: SparkSession = SparkSession.builder
    .master("local[*]")
    .appName("ApplicationController")
    .config("spark.driver.allowMultipleContexts", true)
    .getOrCreate()
}
