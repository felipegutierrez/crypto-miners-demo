package controllers

import play.api.i18n.I18nSupport
import play.api.mvc._

class HomeController(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

}
