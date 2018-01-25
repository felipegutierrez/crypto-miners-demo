package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{rackReads, rackWrites, setupWrites}
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class RackController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def all = Action.async { implicit request: Request[AnyContent] =>
    rackRepository.listAllRacksWithSetup().map { setup: Setup =>
      Ok(Json.toJson(setup)).as(JSON)
    }.recover {
      case re: RackException => BadRequest(Json.toJson(re.message))
      case e: Exception => BadRequest(Json.toJson("Error to get racks: " + e.getMessage))
      case _ => BadRequest(Json.toJson("Unknown error to get racks."))
    }
  }

  def addRack = Action(parse.json).async { request =>
    val either = request.body.validate[Rack]
    either.fold(
      errors => Future.successful(BadRequest("invalid json Rack [" + errors.toString() + "].\n")),
      rack => {
        rackRepository.getById(rack.id).flatMap {
          case Some(rackRow) =>
            // If the Rack already exists we update the produced and currentTime properties
            rackRepository.updateRackProduced(rackRow.id).map { _ =>
              Ok("Rack already exists! Updated produced and currentTime.\n")
            }.recover {
              case e: Exception => BadRequest(Json.toJson("Unknown error when try to update rack current time.\n"))
            }
          case None =>
            // If the Rack does not exist we create it.
            rackRepository.insert(RackRow(rack.id, rack.produced, System.currentTimeMillis)).map { _ =>
              Ok("Rack inserted successfully.\n")
            }.recover {
              case e: Exception => BadRequest(Json.toJson("Unknown error when try to update rack current time.\n"))
            }
        }.recover {
          case re: RackException => BadRequest(Json.toJson(re.message))
          case e: Exception => BadRequest(Json.toJson("Error to get racks: " + e.getMessage))
          case _ => BadRequest(Json.toJson("Unknown error to get racks."))
        }
      }
    )
  }

  def getRacks(at: String) = Action.async { implicit request: Request[AnyContent] =>
    rackRepository.getRacksByCurrentHour(at).map { seqRack: Seq[Rack] =>
      Ok(Json.toJson(seqRack)).as(JSON)
    }.recover {
      case pe: ParseException => BadRequest(Json.toJson("Error on parsing String [" + at + "] to Time."))
      case re: RackException => BadRequest(Json.toJson(re.message))
      case e: Exception => BadRequest(Json.toJson("Error to get racks: " + e.getMessage))
      case _ => BadRequest(Json.toJson("Unknown error to get racks."))
    }
  }
}
