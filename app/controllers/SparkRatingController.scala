package controllers

import javax.inject._

import bootstrap.SparkCommons
import models.helper.Util
import org.apache.spark.sql.DataFrame
import play.api.i18n.I18nSupport
import play.api.mvc._

class SparkRatingController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  lazy val movies: DataFrame = SparkCommons.sparkSession.read
    .format("csv")
    .option("delimiter", ",")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load("downloads/ml-latest-small/movies.csv")
  lazy val ratings: DataFrame = SparkCommons.sparkSession.read
    .format("csv")
    .option("delimiter", ",")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load("downloads/ml-latest-small/ratings.csv")
  val urlToDownload = "http://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
  val fileToDownload = "downloads/ml-latest-small.zip"

  def listAll() = Action { implicit request: Request[AnyContent] =>

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)
    Ok(toJsonString(movies))
  }

  def count() = Action { implicit request: Request[AnyContent] =>

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)
    Ok(movies.count().toString)
  }

  def rate() = Action { implicit request: Request[AnyContent] =>

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    println("movies")
    movies.createOrReplaceTempView("movies")
    movies.cache()
    movies.printSchema()


    println("ratings")
    ratings.createOrReplaceTempView("ratings")
    ratings.cache()
    ratings.printSchema()

    val results = SparkCommons.sparkSession.sqlContext
      .sql("SELECT count(*) count_ratings, m.genres, m.title, m.movieId " +
        "FROM movies m JOIN ratings r ON (m.movieId = r.movieId) " +
        "GROUP BY m.movieId, m.title, m.genres " +
        "ORDER BY count_ratings desc")
    println("results")
    results.printSchema()

    Ok(toJsonString(results))
  }

  def toJsonString(rdd: DataFrame): String =
    "[" + rdd.toJSON.collect.toList.mkString(",\n") + "]"
}
