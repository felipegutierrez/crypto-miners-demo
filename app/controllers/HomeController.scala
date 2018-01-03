package controllers

import javax.inject.Inject

import models.{Gpu, Rack}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import scala.collection.mutable

class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val rackRepository: mutable.MutableList[Rack] = new mutable.MutableList[Rack]()

  val gpuRepository: mutable.MutableList[Gpu] = new mutable.MutableList[Gpu]()

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val gpuWrites: Writes[Gpu] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "rackId").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "installedAt").write[Long]
    )(unlift(Gpu.unapply))

  implicit val gpuReads: Reads[Gpu] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "rackId").read[String] and
      (JsPath \ "produced").read[Float] and
      ((JsPath \ "installedAt").read[Long] or Reads.pure(System.currentTimeMillis))
    ) (Gpu.apply _)

  implicit val rackWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "currentHour").write[Long] and
      (JsPath \ "gpuList").write[Seq[Gpu]]
    ) (unlift(Rack.unapply))

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "produced").read[Float] and
      ((JsPath \ "currentHour").read[Long] or Reads.pure(System.currentTimeMillis)) and
      ((JsPath \ "gpuList").read[Seq[Gpu]] or Reads.pure(Seq.empty[Gpu]))
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
    println("addGpu")
    val either = request.body.validate[Rack]
    either.fold(
      errors => BadRequest("invalid json Rack"),
      rack => {
        val listRack = rackRepository.filter(_.id == rack.id)
          .map { r: Rack =>
            val size = r.gpuList.size
            val gpu = Gpu(r.id + "-gpu-" + size, r.id, 0, System.currentTimeMillis)
            println(gpu)
            r.gpuList = r.gpuList :+ gpu
          }
        if (listRack.size == 0) {
          NotFound
        } else {
          Ok
        }
      }
    )
  }
}
