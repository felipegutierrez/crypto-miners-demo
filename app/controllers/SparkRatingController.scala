package controllers

import javax.inject._

import bootstrap.SparkCommons
import models.helper.Util
import org.apache.spark.sql.DataFrame
import play.api.i18n.I18nSupport
import play.api.mvc._

class SparkRatingController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  val rdd = SparkCommons.sparkSession.read.json("downloads/ml-latest-small")

  def listAll() = Action { implicit request: Request[AnyContent] =>

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

    Ok(dataFrame.count().toString)
  }

  def rate() = Action { implicit request: Request[AnyContent] =>

    val urlToDownload = "http://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
    val fileToDownload = "downloads/ml-latest-small.zip"

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    println("Spark count application")

    val movies: DataFrame = SparkCommons.sparkSession.read
      .format("csv")
      .option("delimiter", ",")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load("downloads/ml-latest-small/movies.csv")

    val ratings = SparkCommons.sparkSession.read
      .format("csv")
      .option("delimiter", ",")
      .option("header", "true") //reading the headers
      .option("mode", "DROPMALFORMED")
      .load("downloads/ml-latest-small/ratings.csv")

    ratings.registerTempTable("ratings")
    ratings.cache()

    movies.registerTempTable("movies")
    movies.cache()

    val usersWatched = SparkCommons.sparkSession.sqlContext
      .sql("Select r.movieId from ratings r, ratings x where r.userId = x.userId and x.movieId = 2273")

    usersWatched.registerTempTable("usersWatched")
    usersWatched.cache()

    val results = SparkCommons.sparkSession.sqlContext
      .sql("Select u.movieId, m.title, m.genres, count(*) cnt from usersWatched u, movies m where u.movieId = m.movieId group by u.movieId, m.title, m.genres Order by cnt desc")

    Ok(toJsonString(results))
  }
}
