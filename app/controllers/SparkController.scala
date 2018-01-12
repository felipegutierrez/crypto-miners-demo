package controllers

import bootstrap.SparkCommons
import models.helper.Util
import org.apache.spark.sql.DataFrame
import play.api.mvc._
import play.api.i18n.I18nSupport
import javax.inject._

class SparkController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  val rdd = SparkCommons.sparkSession.read.json("downloads/tweet-json")

  def list = Action {
    Ok(toJsonString(rdd))
    // Ok("SparkController.list")
  }

  def toJsonString(rdd: DataFrame): String =
    "[" + rdd.toJSON.collect.toList.mkString(",\n") + "]"

  def count() = Action { implicit request: Request[AnyContent] =>

    val urlToDownload = "http://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
    val fileToDownload = "downloads/ml-latest-small.zip"

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    println("Spark count application")
    Ok("Spark count application")
  }
}
