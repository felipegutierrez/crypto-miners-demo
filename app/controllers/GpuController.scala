package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{gpuWrites, rackReads}
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class GpuController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def addGpu() = Action(parse.json) {
    request =>
      val either = request.body.validate[Rack]
      either.fold(
        errors => BadRequest("invalid json Rack"),
        rack => {
          try {
            val futureResult = for {
              futureRackRow <- rackRepository.getById(rack.id) recoverWith {
                case e: Exception => throw RackException(s"Error on select Rack: ${e.getMessage}")
              }
              futureSeqGpuRow <- gpuRepository.getByRack(futureRackRow.get.id) recoverWith {
                case e: Exception => throw GpuException(s"Error on select Gpu's from Rack: ${e.getMessage}")
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
            case pe: ParseException => BadRequest(s"Could not parse the date: ${pe.getMessage}\n")
            case re: RackException => BadRequest(s"Rack Exception: ${re.getMessage}\n")
            case ge: GpuException => BadRequest(s"Gpu Exception: ${ge.getMessage}\n")
            case nsee: NoSuchElementException => BadRequest(s"Element not found.\n")
            case e: Exception => BadRequest(s"Exception found: ${e.getMessage}\n")
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
