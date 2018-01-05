package controllers

import javax.inject._

import models._
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class GpuController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val gpuWrites: Writes[Gpu] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "rackId").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "installedAt").write[String]
    ) (unlift(Gpu.unapply))

  implicit val gpuReads: Reads[Gpu] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "rackId").read[String] and
      (JsPath \ "produced").read[Float] and
      (JsPath \ "installedAt").read[String]
    ) (Gpu.apply _)

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      ((JsPath \ "produced").read[Float] or Reads.pure(0.toFloat)) and
      ((JsPath \ "currentHour").read[String] or Reads.pure(Util.toDate(System.currentTimeMillis))) and
      ((JsPath \ "gpuList").read[Seq[Gpu]] or Reads.pure(Seq.empty[Gpu]))
    ) (Rack.apply _)

  def addGpu() = Action(parse.json) {
    request =>
      val either = request.body.validate[Rack]
      either.fold(
        errors => BadRequest("invalid json Rack"),
        rack => {
          val f: Future[Option[RackRow]] = rackRepository.getById(rack.id)
          f.onComplete {
            case Success(value) =>
              value match {
                case Some(r) =>
                  val fGpu: Future[Seq[GpuRow]] = gpuRepository.getByRack(r.id)
                  fGpu.onComplete {
                    case Success(seq) =>
                      val gpuRow = GpuRow(r.id + "-gpu-" + seq.size, r.id, rack.produced, Util.toTime(rack.currentHour))
                      gpuRepository.insert(gpuRow)

                      // update produced from Rack as a sum of all gpu produced
                      val total = seq.map(_.produced).sum + rack.produced
                      rackRepository.update(r.id, Some(total), None)
                    // Ok
                    case Failure(e) => BadRequest("Failure")
                  }
                case None => BadRequest("Rack not found")
              }
            case Failure(e) => BadRequest("Failure")
          }
          Ok
        }
      )
  }

  def allGpu = Action {
    implicit request: Request[AnyContent] =>
      var gpuSeq: Seq[Gpu] = Seq.empty
      val futureList = gpuRepository.list()
      val result = Await.result(futureList, 20 seconds)
      result.foreach { gpuRow =>
        gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
      }
      Ok(Json.toJson(gpuSeq)).as(JSON)
  }
}
