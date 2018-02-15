package models

import org.apache.spark.rdd.RDD
import org.apache.spark.{Accumulator, SparkContext}

import scala.collection.mutable.ArrayBuffer

class DegreesOfSeparation {

  // some custom data type
  // BFSData contains an array of hero ID connections, the distance, and color.
  type BFSData = (Array[Int], Int, String)
  // a BFSData has a heroID and the BFSData associated with it.
  type BFSNode = (Int, BFSData)

  // the characters we want to find the separation between
  val startCharacterID = 5306 // Spiderman
  val targetCharacterID = 14 // ADAM 3, 031 (who?)

  // we make our accumulator a "global" option so we can reference it in a mapper later.
  var hitCounter: Option[Accumulator[Int]] = None

  // create "iteration 0" of our RDD of BFSNodes
  def createStartingRdd(sc: SparkContext): RDD[BFSNode] = {
    val inputFile = sc.textFile("resource/marvel/Marvel-graph.txt")
    inputFile.map(convertToBFS)
  }

  // converts the line of raw input into a BFSNode
  def convertToBFS(line: String): BFSNode = {
    // split up the lines into fields
    val fields = line.split("\\s+")

    // extract the hero ID from the first fields
    val heroID = fields(0).toInt

    // extract the sequence hero ID's into the connections array
    var connections: ArrayBuffer[Int] = ArrayBuffer()
    for (conn <- 1 to (fields.length - 1)) {
      connections += fields(conn).toInt
    }

    // Default distance and color is 9999 (infinite) and while
    var color: String = "WHITE"
    var distance: Int = 9999

    // unless this is the character we are starting from
    if (heroID == startCharacterID) {
      color = "GRAY"
      distance = 0
    }
    (heroID, (connections.toArray, distance, color))
  }

  // expands a BFSNode into this node and its children
//  def bfsMap(node: BFSNode): Array[BFSNode] ={
//    // extract data from BFSNode
//    val characterID: Int = node._1
//    val data: BFSData = node._2
//
//    val connections: Array[Int] = data._1
//    val distance: Int = data._2
//    val color: String = data._3
//
//    // this is called from flatMap, so we return an array of potentially many BFSNodes to add to our new RDD
//    var results:ArrayBuffer[BFSNode] = ArrayBuffer()
//
//    // Gray node are flagged for expansion, and create new gray nodes for each connection
//    if (color == "GRAY") {
//      for (conn <- connections) {
//        val newCharacterID = conn
//        val newDistance = distance + 1
//        val newColor = "GRAY"
//
//        // have we stumble across the character that we are looking for?
//        // if so increment our accumulator so the driver scripts knows
//        if (targetCharacterID == conn) {
//          if(hitCounter.isDefined) hitCounter.get.add(1)
//        }
//
//        // create our new gray node for this connection and add it to the results
//        val newEntry: BFSNode = (newCharacterID, (Array(), newDistance, newColor))
//        results += newEntry
//      }
//
//      // color this node as black, indicating it has been processed already
//      // color = "BLACK"
//    }
//
//    // Add the
//  }
}
