package controllers

import javax.inject._

import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.mvc._

class SparkController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def count() = Action { implicit request: Request[AnyContent] =>

    val urlToDownload = "http://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
    val fileToDownload = "downloads/ml-latest-small.zip"

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    // val sparkContext = new SparkContext("local[*]", "SparkController")

    // val lines = sparkContext.textFile("")

    println("Spark count application")
    Ok("Spark count application")
  }
}
