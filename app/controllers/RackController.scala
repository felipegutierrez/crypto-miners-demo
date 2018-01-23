package controllers

import javax.inject._

import models._
import models.helper.ReadsWrites.{rackReads, rackWrites, setupWrites}
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RackController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def all = Action.async { implicit request: Request[AnyContent] =>
    val futureList = rackRepository.list()
    var rackSeq: Seq[Rack] = Seq.empty
    futureList.map { resultRack =>
      resultRack.foreach { r =>
        var gpuSeq: Seq[Gpu] = Seq.empty
        val gpuSeqFuture = gpuRepository.getByRack(r.id)
        gpuSeqFuture.map { result =>
          result.foreach { gpuRow =>
            gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
          }
          val rack = Rack(r.id, r.produced, Util.toDate(r.currentHour), gpuSeq)
          rackSeq = rackSeq :+ rack
        }
      }
      val setup: Setup = Setup(rackRepository.getProfitPerGpu, rackSeq)
      Ok(Json.toJson(setup)).as(JSON)
    }
  }

  def addRack = Action(parse.json) { request =>
    val either = request.body.validate[Rack]
    either.fold(
      errors => BadRequest("invalid json Rack.\n"),
      rack => {
        val f: Future[Option[RackRow]] = rackRepository.getById(rack.id)
        val result = Await.result(f, Duration.Inf)
        result match {
          case Some(r) =>
            // If the Rack already exists we update the produced and currentTime properties
            val fGpu: Future[Seq[GpuRow]] = gpuRepository.getByRack(r.id)
            // val total = fGpu.map(_.map(_.produced).sum)
            val resultGpu = Await.result(fGpu, Duration.Inf)
            val total = resultGpu.map(_.produced).sum
            rackRepository.update(r.id, Some(total), Some(System.currentTimeMillis))
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

    var rackSeq: Seq[Rack] = Seq.empty
    var gpuSeq: Seq[Gpu] = Seq.empty
    val futureList = rackRepository.get(Util.toTime(at))

    futureList.map { resultRack =>
      if (resultRack.isEmpty) BadRequest(Json.toJson("Rack not found."))
      else {
        resultRack.foreach { r =>
          val gpuSeqFuture = gpuRepository.getByRack(r.id)
          val listGpu = for {
            listGpu <- gpuSeqFuture
          } yield listGpu
          val result = Await.result(listGpu, Duration.Inf)
          result.foreach { gpuRow =>
            gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
          }
          val rack = Rack(r.id, r.produced, Util.toDate(r.currentHour), gpuSeq)
          rackSeq = rackSeq :+ rack
        }
        Ok(Json.toJson(rackSeq)).as(JSON)
      }
    }
    //    try {
    //    } catch {
    //      case pe: ParseException => BadRequest(Json.toJson("Error on parse String to time."))
    //    }
  }
}
