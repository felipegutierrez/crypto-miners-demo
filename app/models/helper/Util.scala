package models.helper

import java.io._
import java.net.URL
import java.nio.file.{Files, Path, Paths}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date
import java.util.zip.ZipInputStream

import scala.sys.process._

object Util {

  val formatLocaleGMT: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

  def downloadSourceFile(localFile: String, urlToDownload: String): Unit = {
    if (Files.notExists(new File(localFile).toPath())) {
      println(s"[${localFile}] does not exist, downloading... [${urlToDownload}]")
      try {
        // download file
        new URL(urlToDownload) #> new File(localFile) !!

        println("Download complete")
      } catch {
        case e: IOException => println(s"Error occurred on downloading file [${urlToDownload}]: [${e}]")
        case e: Exception => println(s"Error occurred on downloading file [${urlToDownload}]: [${e}]")
      }
    } else {
      println("Path exists, no need to download.")
    }
  }

  def unzip(source: String): Unit = {

    try {
      val dir: Path = new File(source.replace(".zip", "")).toPath
      if (Files.notExists(dir) && !Files.isDirectory(dir)) {
        val zipFile: InputStream = new FileInputStream(new File(source))
        val zis = new ZipInputStream(zipFile)

        Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach { file =>
          if (!file.isDirectory) {
            val outPath = Paths.get("downloads").resolve(file.getName)
            val outPathParent = outPath.getParent
            if (!outPathParent.toFile.exists()) {
              outPathParent.toFile.mkdirs()
            }

            val outFile = outPath.toFile
            val out = new FileOutputStream(outFile)
            val buffer = new Array[Byte](4096)
            Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(out.write(buffer, 0, _))
          }
        }
        println("Decompress complete")
      } else {
        println("Dir is already decompressed.")
      }
    } catch {
      case e: Exception => println(s"Error occurred on decompressing the file [${source}]: [${e}]")
    }
  }

  def main(args: Array[String]): Unit = {
    val currentTime: Long = 1515012397212L
    val time: String = Util.toDate(currentTime)
    println(currentTime)
    println(time)
    println(Util.toTime(time))
  }

  def toDate(time: Long): String = {
    formatLocaleGMT.format(new Date(time))
  }

  def toTime(date: String): Long = {
    formatLocaleGMT.parse(date).getTime
  }
}
