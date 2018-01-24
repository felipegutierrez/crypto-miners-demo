package controllers

import java.text.ParseException
import javax.inject._

import models._
import models.helper.ReadsWrites.{rackReads, rackWrites, setupWrites}
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class RackController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def all = Action.async { implicit request: Request[AnyContent] =>
    rackRepository.list().flatMap {
      seqRackRow: Seq[RackRow] =>
        val futureSeqRackRow: Seq[Future[Rack]] = seqRackRow.map {
          rackRow: RackRow =>
            gpuRepository.getByRack(rackRow.id).map {
              seqGpuRow: Seq[GpuRow] =>
                val seqGpu = seqGpuRow.map(gpuRepository.gpuRowToGpu) // return Seq[Gpu]
                Rack(rackRow.id, rackRow.produced, Util.toDate(rackRow.currentHour), seqGpu)
            } // return Future[Rack]
        }
        val futureSeqRack: Future[Seq[Rack]] = Future.sequence(futureSeqRackRow)
        futureSeqRack.map(racks => Ok(Json.toJson(Setup(rackRepository.getProfitPerGpu, racks))).as(JSON))
    }.recover {
      case re: RackException => BadRequest(Json.toJson(re.message))
      case e: Exception => BadRequest(Json.toJson("Error to get racks: " + e.getMessage))
      case _ => BadRequest(Json.toJson("Unknown error to get racks."))
    }
  }

  def addRack = Action(parse.json).async { request =>
    val either = request.body.validate[Rack]
    either.fold(
      errors => Future.successful(BadRequest("invalid json Rack.\n")),
      rack => {
        val f: Future[Option[RackRow]] = rackRepository.getById(rack.id)
        f.map {
          case Some(r) =>
            // If the Rack already exists we update the produced and currentTime properties
            val fGpu: Future[Seq[GpuRow]] = gpuRepository.getByRack(r.id)
            val total = fGpu.map(_.map(_.produced).sum)
            total.map { total =>
              rackRepository.update(r.id, Some(total), Some(System.currentTimeMillis))
            }
            Ok("Rack already exists! Updated produced and currentTime.\n")
          case None =>
            // If the Rack does not exist we create it.
            val rackRow = RackRow(rack.id, rack.produced, System.currentTimeMillis)
            rackRepository.insert(rackRow)
            Ok
        }
      }
    )
  }

  def getRacks(at: String) = Action.async { implicit request: Request[AnyContent] =>
    try {
      rackRepository.get(Util.toTime(at)).flatMap {
        seqRackRow: Seq[RackRow] =>
          if (seqRackRow.isEmpty) throw new RackException("There is no Rack with this time [" + at + "].")
          val seqFutureRack: Seq[Future[Rack]] = seqRackRow.map {
            rackRow: RackRow =>
              gpuRepository.getByRack(rackRow.id).map {
                seqGpuRow: Seq[GpuRow] =>
                  val seqGpu = seqGpuRow.map(gpuRepository.gpuRowToGpu) // return Seq[Gpu]
                  Rack(rackRow.id, rackRow.produced, Util.toDate(rackRow.currentHour), seqGpu)
              } // return Future[Rack]
          }
          val futureSeqRack: Future[Seq[Rack]] = Future.sequence(seqFutureRack)
          futureSeqRack.map(racks => Ok(Json.toJson(racks)).as(JSON))
      }.recover {
        case re: RackException => BadRequest(Json.toJson(re.message))
        case e: Exception => BadRequest(Json.toJson("Error to get racks: " + e.getMessage))
        case _ => BadRequest(Json.toJson("Unknown error to get racks."))
      }
    } catch {
      case pe: ParseException => Future.successful(BadRequest(Json.toJson("Error on parsing String [" + at + "] to Time.")))
      case e: Exception => Future.successful(BadRequest(Json.toJson("Error to get racks: " + e.getMessage)))
      case _ => Future.successful(BadRequest(Json.toJson("Unknown error to get racks.")))
    }
  }
}
