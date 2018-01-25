package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{gpuReads, gpuWrites}
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
        gpuRepository.addGpuAtRack(gpu).map { _ =>
          Ok("Gpu added to the Rack [" + gpu.rackId + "].\n")
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

  def allGpu = Action.async { implicit request: Request[AnyContent] =>
    gpuRepository.listAllGpu().map { seqGpu: Seq[Gpu] =>
      Ok(Json.toJson(seqGpu)).as(JSON)
    }.recover {
      case ge: GpuException => BadRequest(s"Gpu Exception: ${ge.getMessage}\n")
      case e: Exception => BadRequest(Json.toJson("Error to add Gpu: " + e.getMessage))
      case _ => BadRequest(Json.toJson("Unknown error to add Gpu."))
    }
  }
}
