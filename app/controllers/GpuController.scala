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

  def addGpu() = Action(parse.json).async { request =>
    val either = request.body.validate[Gpu]
    either.fold(
      errors => Future.successful(BadRequest("invalid json Gpu.\n")),
      gpu => {
        rackRepository.getById(gpu.rackId).flatMap {
          case Some(rackRow) =>
            // If the Rack exists we add a Gpu
            // given time (should be after currentHour, which was set during setup)
            if (Util.toTime(gpu.installedAt) < rackRow.currentHour) {
              throw new GpuException("Given time [" + gpu.installedAt + "] is not after the Rack currentHour [" + Util.toDate(rackRow.currentHour) + "].")
            }
            // Calculate the Gpu produced based on the profitPerGpu
            val produced = if (gpu.produced == 0.toFloat) {
              val times = ((Util.toTime(gpu.installedAt) - rackRow.currentHour) / 3600000) + 1
              rackRepository.getProfitPerGpu * times
            } else gpu.produced

            // Get all the Gpu of the Rack to create the GpuId and update the Rack.produced
            gpuRepository.getByRack(rackRow.id).map { seqGpuRow: Seq[GpuRow] =>
              // Insert Gpu in the Rack
              gpuRepository.insert(GpuRow(rackRow.id + "-gpu-" + seqGpuRow.size, rackRow.id, produced, Util.toTime(gpu.installedAt)))
              // update produced from Rack as a sum of all gpu produced
              val total = seqGpuRow.map(_.produced).sum + produced
              rackRepository.update(rackRow.id, Some(total), None)
            }
            Future.successful(Ok("Gpu added to the Rack [" + gpu.rackId + "].\n"))
          case None =>
            // If the Rack does not exist throw an Exception
            throw new RackException("Rack [" + gpu.rackId + "] does not exist.")
        }.recover {
          case ge: GpuException => BadRequest(s"Gpu Exception: ${ge.getMessage}\n")
          case re: RackException => BadRequest(s"Rack Exception: ${re.getMessage}\n")
          case pe: ParseException => BadRequest(s"Could not parse the date: ${pe.getMessage}\n")
          case e: Exception => BadRequest(Json.toJson("Error to add Gpu: " + e.getMessage))
          case _ => BadRequest(Json.toJson("Unknown error to add Gpu."))
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
