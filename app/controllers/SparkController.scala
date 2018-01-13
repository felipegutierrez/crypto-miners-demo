package controllers

import javax.inject._

import bootstrap.SparkCommons
import models.helper.Util
import org.apache.spark.sql.DataFrame
import play.api.i18n.I18nSupport
import play.api.mvc._

class SparkController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  val rdd = SparkCommons.sparkSession.read.json("downloads/ml-latest-small")

  def list = Action {
    Ok(toJsonString(rdd))
  }

  def count() = Action { implicit request: Request[AnyContent] =>

    val urlToDownload = "http://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
    val fileToDownload = "downloads/ml-latest-small.zip"

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    println("Spark count application")

    val dataFrame: DataFrame = SparkCommons.sparkSession.read
      .format("csv")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load("downloads/ml-latest-small/movies.csv")

    Ok(toJsonString(dataFrame))
  }

  def toJsonString(rdd: DataFrame): String =
    "[" + rdd.toJSON.collect.toList.mkString(",\n") + "]"
}
