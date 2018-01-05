package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

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
          try {
            val futureResult = for {
              futureRackRow <- rackRepository.getById(rack.id) recoverWith {
                case e: Exception => Future.failed(new Exception("Error on select Rack."))
              }
              futureSeqGpuRow <- gpuRepository.getByRack(futureRackRow.get.id) recoverWith {
                case e: Exception => Future.failed(new Exception("Error on select Gpu's from Rack."))
              }
            } yield (futureRackRow, futureSeqGpuRow)
            val result = Await.result(futureResult, 20 seconds)

            result._1 match {
              case Some(rackRow) =>
                // Insert Gpu in the Rack
                val gpuRow = GpuRow(rackRow.id + "-gpu-" + result._2.size, rackRow.id, rack.produced, Util.toTime(rack.currentHour))
                gpuRepository.insert(gpuRow)

                // update produced from Rack as a sum of all gpu produced
                val total = result._2.map(_.produced).sum + rack.produced
                rackRepository.update(rackRow.id, Some(total), None)
              case None => BadRequest("Rack not found")
            }
            Ok
          } catch {
            case pe: ParseException => BadRequest(s"Could not parse the date: ${pe.getMessage}")
            case e: Exception => BadRequest(s"Exception found: ${e.getMessage}")
          }
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
