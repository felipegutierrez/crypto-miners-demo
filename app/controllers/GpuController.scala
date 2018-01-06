package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{gpuReads, gpuWrites}
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
      val either = request.body.validate[Gpu]
      either.fold(
        errors => BadRequest("invalid json Gpu"),
        gpu => {
          try {
            val futureResult = for {
              futureRackRow <- rackRepository.getById(gpu.rackId) recoverWith {
                case e: Exception => throw RackException(s"Error on select Rack: ${e.getMessage}")
              }
              futureSeqGpuRow <- gpuRepository.getByRack(futureRackRow.get.id) recoverWith {
                case e: Exception => throw GpuException(s"Error on select Gpu's from Rack: ${e.getMessage}")
              }
            } yield (futureRackRow, futureSeqGpuRow)
            val result = Await.result(futureResult, 20 seconds)
            result._1 match {
              case Some(rackRow) =>
                // given time (should be after currentHour, which was set during setup)
                if (Util.toTime(gpu.installedAt) < rackRow.currentHour) {
                  throw GpuException("Given time is not after the currentHour.")
                }
                // Calculate the Gpu produced based on the profitPerGpu
                val produced = if (gpu.produced == 0.toFloat) {
                  val times = ((Util.toTime(gpu.installedAt) - rackRow.currentHour) / 3600000) + 1
                  rackRepository.getProfitPerGpu * times
                } else gpu.produced
                // Insert Gpu in the Rack
                val gpuRow = GpuRow(rackRow.id + "-gpu-" + result._2.size, rackRow.id, produced, Util.toTime(gpu.installedAt))
                gpuRepository.insert(gpuRow)

                // update produced from Rack as a sum of all gpu produced
                val total = result._2.map(_.produced).sum + produced
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
