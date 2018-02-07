package models

import bootstrap.SparkCommons

object MarvelRepository {

  def getMostPopularSuperHero(): String = {
    lazy val sparkContext = SparkCommons.sparkSession.sparkContext // got sparkContext

    val namesRdd = sparkContext
      .textFile("resource/marvel/Marvel-names.txt") // build up a hero ID - name RDD
      .flatMap(parseNames)

    val mostPopularHero = sparkContext
      .textFile("resource/marvel/Marvel-graph.txt") // build up superhero co-apperance data
      .map(countCoOccurrences) // convert to (hero ID, number of connections) RDD
      .reduceByKey((x, y) => x + y) // combine entries that span more than one line
      .map(x => (x._2, x._1)) // flip it to (number of connections, hero ID)
      .max // find the max connections

    // Look up the name (lookup returns an array of results, so we need to access the first result with (0))
    val mostPopularHeroName = namesRdd.lookup(mostPopularHero._2)(0)

    return s"The most popular superhero is [$mostPopularHeroName] ID [${mostPopularHero._2}] with [${mostPopularHero._1}] co-appearances."
  }

  // Function to extract the hero ID and number of connections from each line
  def countCoOccurrences(line: String) = {
    // regex expression to split using any type of space occurrence in the line
    val elements = line.split("\\s+")
    (elements(0).toInt, elements.length - 1)
  }

  // function to extract hero ID -> hero name tuples (or None in case of Failure)
  def parseNames(line: String): Option[(Int, String)] = {
    val fields = line.split('\"')
    if (fields.length > 1) return Some(fields(0).trim.toInt, fields(1))
    else return None
  }
}
