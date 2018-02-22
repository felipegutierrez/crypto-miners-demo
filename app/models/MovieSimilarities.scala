package models

import java.nio.charset.CodingErrorAction

import bootstrap.SparkCommons

import scala.io.{Codec, Source}

object MovieSimilarities {

  type MovieRating = (Int, Double)
  type UserRatingPair = (Int, (MovieRating, MovieRating))
  type RatingPair = (Double, Double)
  type RatingPairs = Iterable[RatingPair]

  def main(args: Array[String]): Unit = {
    lazy val sparkContext = SparkCommons.sparkSession.sparkContext // got sparkContext

    // loading movie names
    val movieDic = loadMovieNames()

    val data = sparkContext.textFile("downloads/ml-100k/u.data")

    // map ratings to key / value pairs: user ID => movieID, rating
    val ratings = data.map(l => l.split("\t")).map(l => (l(0).toInt, (l(1).toInt, l(2).toDouble)))

    // Emit every movie rated together by the same user. Self-join to find every combination
    val joinedRatings = ratings.join(ratings)

    // at this point our RDD consist of userID => ((movieID, rating), (movieID, rating))

    // filter out duplicate pairs
    val uniqueJoinedRatings = joinedRatings.filter(filterDuplicates)

    // now key by (movie1, movie2) pairs
    val moviePairs = uniqueJoinedRatings.map(makePairs)

    // we now have (movie1, movie2) => (rating1, rating2)
    // now collect all ratings for each movie pair and compute similarity
    val moviePairRatings = moviePairs.groupByKey()

    // we now have (movie1, movie2) => (rating1, rating2), (rating1, rating2), ...
    // can now compute similarities
    val moviePairSimilarities = moviePairRatings.mapValues(computeCosineSimilarity).cache()

    // save the result if desired
    val sorted = moviePairSimilarities.sortByKey()
    sorted.saveAsTextFile("downloads/movie-sims.out")

    // extract similarities for the movie we care about that are "good".
    val scoreThreshold = 0.97
    val coOccurenceThreshold = 50.0
    val movieID: Int = 50

    // Filter for movies with this similarity that are "good" as defined by our quality threshold above
    val filteredResults = moviePairSimilarities.filter(x => {
      val pair = x._1
      val sim = x._2
      (pair._1 == movieID || pair._2 == movieID) && sim._1 > scoreThreshold && sim._2 > coOccurenceThreshold
    })

    // sort by quality score
    val results = filteredResults.map(x => (x._2, x._1)).sortByKey(false).take(10)
    println("\nTop 10 similar movies for " + movieDic(movieID))
    for (result <- results) {
      val sim = result._1
      val pair = result._2
      // display the similarity result that isn't the movie we are looking at
      var similarMovieID = pair._1
      if (similarMovieID == movieID) similarMovieID = pair._2
      println(movieDic(similarMovieID) + "\t score: " + sim._1 + "\t strength: " + sim._2)
    }
  }

  def loadMovieNames(): Map[Int, String] = {
    // handle character encoding issue
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    // create a map of ints to String, and populate it from u.item
    var movieNames: Map[Int, String] = Map()
    val lines = Source.fromFile("downloads/ml-100k/u.item").getLines()
    for (line <- lines) {
      val fields = line.split('|')
      if (fields.length > 1) movieNames += (fields(0).toInt -> fields(1))
    }
    return movieNames
  }

  def makePairs(userRatings: UserRatingPair) = {
    val movieRating1 = userRatings._2._1
    val movieRating2 = userRatings._2._2

    val movie1 = movieRating1._1
    val rating1 = movieRating1._2
    val movie2 = movieRating2._1
    val rating2 = movieRating2._2

    ((movie1, movie2), (rating1, rating2))
  }

  def filterDuplicates(userRatings: UserRatingPair): Boolean = {
    val movieRating1 = userRatings._2._1
    val movieRating2 = userRatings._2._2

    val movie1 = movieRating1._1
    val movie2 = movieRating2._1

    return movie1 < movie2
  }

  def computeCosineSimilarity(ratingPairs: RatingPairs): (Double, Int) = {
    var numPairs: Int = 0
    var sum_xx: Double = 0.0
    var sum_yy: Double = 0.0
    var sum_xy: Double = 0.0

    for (pair <- ratingPairs) {
      val ratingX = pair._1
      val ratingY = pair._2

      sum_xx += ratingX * ratingX
      sum_yy += ratingY * ratingY
      sum_xy += ratingX * ratingY
      numPairs += 1
    }

    val numerator: Double = sum_xy
    val denominator = math.sqrt(sum_xx) * math.sqrt(sum_yy)

    var score: Double = 0.0
    if (denominator != 0) score = numerator / denominator

    return (score, numPairs)
  }
}
