package models.helper

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date


object Util {

  val formatLocaleGMT: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

  def toDate(time: Long): String = {
    formatLocaleGMT.format(new Date(time))
  }

  def toTime(date: String): Long = {
    formatLocaleGMT.parse(date).getTime
  }

  def main(args: Array[String]): Unit = {
    val currentTime: Long = 1515012397212L
    val time: String = Util.toDate(currentTime)
    println(currentTime)
    println(time)
    println(Util.toTime(time))
  }
}
