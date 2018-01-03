package controllers

import javax.inject.Inject

import models.{Gpu, Rack}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val rackRepository: mutable.MutableList[Rack] = new mutable.MutableList[Rack]()

  val gpuRepository: mutable.MutableList[Gpu] = new mutable.MutableList[Gpu]()

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val rackWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "currentHour").write[Long]
    ) (unlift(Rack.unapply))

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "produced").read[Float] and
      ((JsPath \ "currentHour").read[Long] or Reads.pure(System.currentTimeMillis))
    ) (Rack.apply _)

  def all = Action {
    Ok(Json.toJson(rackRepository)).as(JSON)
  }

  def addRack = Action(parse.json) { request =>
    val either = request.body.validate[Rack]
    either.fold(
      errors => BadRequest("invalid json Rack"),
      rack => {
        rack.currentHour = System.currentTimeMillis
        rackRepository.+=(rack)
        Ok
      }
    )
  }

  def getRacks(at: Long) = Action { request =>
    println(at)
    val list = rackRepository.filter { r: Rack => r.currentHour == at }
    Ok(Json.toJson(list)).as(JSON)
  }

  def addGpu() = Action(parse.json) { request =>
    println("addGpu: ")
    val either = request.body.validate[Rack]
    either.fold(
      errors => BadRequest("invalid json Rack"),
      rack => {
        val listRack = rackRepository.filter { r: Rack => r.id == rack.id }
        if (listRack.size == 0) {
          NotFound
        } else {
          val listGpu = gpuRepository.filter { g: Gpu => g.rackId == rack.id }
          var size = listGpu.size
          listRack.foreach { r =>
            val gpu = Gpu(r.id + "-gpu-" + size, r.id, 0, System.currentTimeMillis)
            gpuRepository.+=(gpu)
            println("added: " + gpu)
            size += 1
          }
          Ok
        }
      }
    )
  }
}
