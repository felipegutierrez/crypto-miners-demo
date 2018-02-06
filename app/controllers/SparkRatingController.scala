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

  def toJsonString(rdd: DataFrame): String =
    "[" + rdd.toJSON.collect.toList.mkString(",\n") + "]"

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

  def popularMovies() = Action { implicit request: Request[AnyContent] =>
    Util.downloadSourceFile("downloads/ml-100k.zip", "http://files.grouplens.org/datasets/movielens/ml-100k.zip")
    Util.unzip("downloads/ml-100k.zip")

    val sparkContext = SparkCommons.sparkSession.sparkContext // got sparkContext

    val popularMovies = sparkContext
      .textFile("downloads/ml-100k/u.data") // popularMovies
      .map(x => (x.split("\t")(1).toInt, 1)) // Map to (movieID , 1) tuples
      .reduceByKey((x, y) => x + y) // Count up all the 1's for each movie
      .map(x => (x._2, x._1)) // Flip (movieId, count) to (count, movieId)
      .sortByKey(false) // Sort and invert the order to show the most popular movies first

    // collect and print the result
    val results = popularMovies.collect().toList.mkString(",\n")

    Ok("[" + results + "]")
  }

  def listByGenre(genres: String) = Action { implicit request: Request[AnyContent] =>

    val genreArray = genres.split(",")
    val genreCriteria = genreArray.mkString("WHERE m.genres LIKE (\'%", "%\') OR m.genres LIKE (\'%", "%\') ")

    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)

    val results: DataFrame = SparkCommons.sparkSession.sqlContext
      .sql("SELECT m.title, m.genres, m.movieId, SUM(r.rating) rating " +
        "FROM movies m JOIN ratings r ON (m.movieId = r.movieId) " +
        genreCriteria +
        "GROUP BY m.title, m.genres, m.movieId " +
        "ORDER BY rating desc, m.genres asc"
      )
    println("results")
    results.printSchema()
    Ok(toJsonString(results))
  }
}
