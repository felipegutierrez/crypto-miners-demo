package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{gpuReads, gpuWrites}
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class GpuController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def addGpu() = Action(parse.json).async { request =>
    val either = request.body.validate[Gpu]
    either.fold(
      errors => Future.successful(BadRequest("invalid json Gpu.\n")),
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
          futureResult.map {
            case (Some(rackRow), seqGpuRow) =>
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
              val gpuRow = GpuRow(rackRow.id + "-gpu-" + seqGpuRow.size, rackRow.id, produced, Util.toTime(gpu.installedAt))
              gpuRepository.insert(gpuRow)
              // update produced from Rack as a sum of all gpu produced
              val total = seqGpuRow.map(_.produced).sum + produced
              rackRepository.update(rackRow.id, Some(total), None)
              Ok
            case (None, seqGpuRow) => BadRequest("Rack not found")
          }
        } catch {
          case pe: ParseException => Future.successful(BadRequest(s"Could not parse the date: ${pe.getMessage}\n"))
          case re: RackException => Future.successful(BadRequest(s"Rack Exception: ${re.getMessage}\n"))
          case ge: GpuException => Future.successful(BadRequest(s"Gpu Exception: ${ge.getMessage}\n"))
          case nsee: NoSuchElementException => Future.successful(BadRequest(s"Element not found.\n"))
          case e: Exception => Future.successful(BadRequest(s"Exception found: ${e.getMessage}\n"))
        }
      }
    )
  }

  def allGpu = Action.async {
    implicit request: Request[AnyContent] =>
      var gpuSeq: Seq[Gpu] = Seq.empty
      val futureList = gpuRepository.list()
      futureList.map { result =>
        result.foreach { gpuRow =>
          gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
        }
        Ok(Json.toJson(gpuSeq)).as(JSON)
      }
  }
}
