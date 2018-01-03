package controllers

import javax.inject.Inject

import models.Rack
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val rackRepository: mutable.MutableList[Rack] = new mutable.MutableList[Rack]()

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val personWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float]
    ) (unlift(Rack.unapply))

  implicit val personReads: Reads[Rack] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "produced").read[Float]
    ) (Rack.apply _)

  def allRacks = Action {
    Ok(Json.toJson(rackRepository)).as(JSON)
  }

  def addRack = Action(parse.json) { request =>
    val either = request.body.validate[Rack]
    either.fold(
      errors => BadRequest("invalid json Rack"),
      rack => {
        rackRepository.+=(rack)
        Ok
      }
    )
  }
}
