package controllers

import javax.inject._

import models.MovieRepository
import org.apache.spark.sql.DataFrame
import play.api.i18n.I18nSupport
import play.api.mvc._

class SparkRatingController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  lazy val movieRepository = MovieRepository

  def listAll() = Action { implicit request: Request[AnyContent] =>
    Ok(toJsonString(movieRepository.listAll()))
  }

  def toJsonString(rdd: DataFrame): String =
    "[" + rdd.toJSON.collect.toList.mkString(",\n") + "]"

  def count() = Action { implicit request: Request[AnyContent] =>
    Ok(movieRepository.countMovies().toString)
  }

  def rate() = Action { implicit request: Request[AnyContent] =>
    Ok(toJsonString(movieRepository.rateMovies()))
  }

  def popularMovies() = Action { implicit request: Request[AnyContent] =>
    val results = movieRepository.getPopularMovies().toList.mkString(",\n")
    Ok("[" + results + "]")
  }

  def listByGenre(genres: String) = Action { implicit request: Request[AnyContent] =>
    val results: DataFrame = movieRepository.listByGenre(genres)
    Ok(toJsonString(results))
  }
}
