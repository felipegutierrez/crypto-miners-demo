package models

import java.nio.charset.CodingErrorAction

import bootstrap.SparkCommons
import models.helper.Util
import org.apache.spark.sql.DataFrame

import scala.io.{Codec, Source}

object MovieRepository {

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

  def listByGenre(genres: String): DataFrame = {

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
    results
  }

  def listAll(): DataFrame = {
    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)
    movies
  }

  def countMovies(): Long = {
    Util.downloadSourceFile(fileToDownload, urlToDownload)
    Util.unzip(fileToDownload)
    movies.count()
  }

  def rateMovies(): DataFrame = {

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

    results
  }

  def getPopularMovies() = {
    Util.downloadSourceFile("downloads/ml-100k.zip", "http://files.grouplens.org/datasets/movielens/ml-100k.zip")
    Util.unzip("downloads/ml-100k.zip")

    val sparkContext = SparkCommons.sparkSession.sparkContext // got sparkContext

    // Create a broadcast variable of our ID -> movie name map to our entire cluster
    val nameDict = sparkContext.broadcast(loadMovieNames)

    val popularMovies = sparkContext
      .textFile("downloads/ml-100k/u.data") // popularMovies
      .map(x => (x.split("\t")(1).toInt, 1)) // Map to (movieID , 1) tuples
      .reduceByKey((x, y) => x + y) // Count up all the 1's for each movie
      .map(x => (x._2, x._1)) // Flip (movieId, count) to (count, movieId)
      .sortByKey(false) // Sort and invert the order to show the most popular movies first
      .map(x => (nameDict.value(x._2), x._1)) // Fold in the movie names from the broadcast variable

    // collect and print the result
    popularMovies.collect()
  }

  // Load up a Map of movies IDs to movie names
  def loadMovieNames(): Map[Int, String] = {
    // handle character encoding issue
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    // create a map of Ints to Strings and populate it from u.item
    var movieNames: Map[Int, String] = Map()
    val lines = Source.fromFile("downloads/ml-100k/u.item").getLines()
    for (line <- lines) {
      val fields = line.split('|')
      if (fields.length > 1) movieNames += (fields(0).toInt -> fields(1))
    }
    movieNames
  }
}
