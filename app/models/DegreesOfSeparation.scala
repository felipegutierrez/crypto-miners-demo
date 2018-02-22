package models

import bootstrap.SparkCommons
import org.apache.spark.rdd.RDD
import org.apache.spark.util.LongAccumulator

import scala.collection.mutable.ArrayBuffer

object DegreesOfSeparation {

  // some custom data type
  // BFSData contains an array of hero ID connections, the distance, and color.
  type BFSData = (Array[Long], Long, String)
  // a BFSData has a heroID and the BFSData associated with it.
  type BFSNode = (Long, BFSData)

  // the characters we want to find the separation between
  // 5306 "Spider-man"
  // 84 "ALFY"
  // 14 "ADAM 3,031"
  var startCharacterID = 84 // ALFY
  var targetCharacterID = 14 // ADAM 3,031 (who?)

  // we make our accumulator a "global" option so we can reference it in a mapper later.
  var hitCounter: Option[LongAccumulator] = None
  var result = ""

  def main(args: Array[String]) {
    println(DegreesOfSeparation.getDegreesOfSeparation(84, 14))
  }

  def getDegreesOfSeparation(startID: Int, targetID: Int): String = {
    startCharacterID = startID
    targetCharacterID = targetID
    // val sc = new SparkContext("local[*]", "DegreesOfSeparation")
    val sc = SparkCommons.sparkSession.sparkContext // got sparkContext

    val namesRdd = sc
      .textFile("resource/marvel/Marvel-names.txt") // build up a hero ID - name RDD
      .flatMap(MarvelRepository.parseNames)

    val startCharacterName: String = namesRdd.lookup(startID)(0)
    val targetCharacterName: String = namesRdd.lookup(targetID)(0)

    // our accumulator, used to signal when we find the target character in our BFS traversal
    hitCounter = Some(sc.longAccumulator("Accumulator of degrees between superheros"))
    hitCounter.get.reset()

    // create "iteration 0" of our RDD of BFSNodes
    var iterationRdd: RDD[BFSNode] = sc
      .textFile("resource/marvel/Marvel-graph.txt")
      .map(convertToBFS)

    for (iterarion <- 1 to 10) {
      result += "Running BFS iteration# " + iterarion + ".\n"

      // create new vertices as needed to darken or reduce distances in the reduce stage.
      // If we encounter the node that we are looking for as a GRAY node, increment our accumulator
      // to signal that we are done.
      val mapped = iterationRdd.flatMap(bfsMap)

      // note that mapped.count action here forces the RDD to evaluate and that is the only reason
      // that our accumulator is actually updated.
      result += "The superhero [" + startCharacterID + " - " + startCharacterName + "] coappears with " +
        mapped.count + " other superheroes characters.\n"

      if (hitCounter.isDefined) {
        val hitCount = hitCounter.get.value
        result += "HitCount: " + hitCount + ".\n"
        if (hitCount > 0) {
          result += "The degree of separation between [" + startCharacterID + " - " + startCharacterName +
            "] and [" + targetCharacterID + " - " + targetCharacterName + "] is [" + hitCount + "].\n"
          return result
        }
      }
      // reducer combines data for each character ID, preserving the darkest color and shortest path
      iterationRdd = mapped.reduceByKey(bfsReduce)
    }
    result += "Could not find the degree of separation between [" + startCharacterID + "] and [" + targetCharacterID + "].\n"
    return result
  }

  def bfsReduce(data1: BFSData, data2: BFSData): BFSData = {
    // extract data that we are combining
    val edges1: Array[Long] = data1._1
    val edges2: Array[Long] = data2._1
    val distance1: Long = data1._2
    val distance2: Long = data2._2
    val color1: String = data1._3
    val color2: String = data2._3

    // default node values
    var distance: Long = 9999
    var color: String = "WHITE"
    var edges: ArrayBuffer[Long] = ArrayBuffer()

    // See if one is the original node with its connections. If so preserve them
    if (edges1.length > 0) edges ++= edges1
    if (edges2.length > 0) edges ++= edges2

    // preserve minimum distance
    if (distance1 < distance) distance = distance1
    if (distance2 < distance) distance = distance2

    // preserve the darkest color
    if (color1 == "WHITE" && (color2 == "GRAY" || color2 == "BLACK")) color = color2
    if (color1 == "GRAY" && color2 == "BLACK") color = color2
    if (color2 == "WHITE" && (color1 == "GRAY" || color1 == "BLACK")) color = color1
    if (color2 == "GRAY" && color1 == "BLACK") color = color1

    return (edges.toArray, distance, color)
  }

  // converts the line of raw input into a BFSNode
  def convertToBFS(line: String): BFSNode = {
    // split up the lines into fields
    val fields = line.split("\\s+")

    // extract the hero ID from the first fields
    val heroID = fields(0).toLong

    // extract the sequence hero ID's into the connections array
    var connections: ArrayBuffer[Long] = ArrayBuffer()
    for (conn <- 1 to (fields.length - 1)) {
      connections += fields(conn).toLong
    }

    // Default distance and color is 9999 (infinite) and while
    var color: String = "WHITE"
    var distance: Int = 9999

    // unless this is the character we are starting from
    if (heroID == startCharacterID) {
      println("startCharacterID: " + startCharacterID)
      color = "GRAY"
      distance = 0
    }
    return (heroID, (connections.toArray, distance, color))
  }

  // expands a BFSNode into this node and its children
  def bfsMap(node: BFSNode): Array[BFSNode] = {
    // extract data from BFSNode
    val characterID: Long = node._1
    val data: BFSData = node._2

    val connections: Array[Long] = data._1
    val distance: Long = data._2
    var color: String = data._3

    // this is called from flatMap, so we return an array of potentially many BFSNodes to add to our new RDD
    var results: ArrayBuffer[BFSNode] = ArrayBuffer()

    // Gray node are flagged for expansion, and create new gray nodes for each connection
    if (color == "GRAY") {
      for (conn <- connections) {
        val newCharacterID = conn
        val newDistance = distance + 1
        val newColor = "GRAY"

        // have we stumble across the character that we are looking for?
        // if so increment our accumulator so the driver scripts knows
        if (targetCharacterID == conn) {
          if (hitCounter.isDefined) {
            result += "HitCounter.get.value: " + hitCounter.get.value + " and add 1.\n"
            hitCounter.get.add(1)
          }
        }

        // create our new gray node for this connection and add it to the results
        val newEntry: BFSNode = (newCharacterID, (Array(), newDistance, newColor))
        results += newEntry
      }

      // color this node as black, indicating it has been processed already
      color = "BLACK"
    }

    // Add the original node black, so its connections can get merged with the gray nodes in the reducer
    val thisEntry: BFSNode = (characterID, (connections, distance, color))
    results += thisEntry

    return results.toArray
  }
}
