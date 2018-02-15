package controllers

import javax.inject.Inject

import models.{DegreesOfSeparation, MarvelRepository}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

class SparkMarvelController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  lazy val marvelRepository = MarvelRepository
  lazy val dos = DegreesOfSeparation

  def mostPopularSuperHero() = Action { implicit request: Request[AnyContent] =>
    Ok(marvelRepository.getMostPopularSuperHero())
  }

  def mostPopularSuperHeroList(num: Int, sort: Boolean) = Action { implicit request: Request[AnyContent] =>
    Ok(marvelRepository.getMostPopularSuperHero(num, sort))
  }

  def degreesOfSeparation(startID: Int, targetID: Int) = Action { implicit request: Request[AnyContent] =>
    Ok(dos.getDegreesOfSeparation(startID, targetID))
  }
}
