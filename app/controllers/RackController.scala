package controllers

import models._
import models.helper.Util
import play.api.i18n.I18nSupport
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import javax.inject._

class RackController @Inject()(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  val profitPerGpu: Float = 0.1235567.toFloat

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

  implicit val rackWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "currentHour").write[String] and
      ((JsPath \ "gpuList").write[Seq[Gpu]])
    ) (unlift(Rack.unapply))

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      ((JsPath \ "produced").read[Float] or Reads.pure(0.toFloat)) and
      ((JsPath \ "currentHour").read[String] or Reads.pure(Util.toDate(System.currentTimeMillis))) and
      ((JsPath \ "gpuList").read[Seq[Gpu]] or Reads.pure(Seq.empty[Gpu]))
    ) (Rack.apply _)

  implicit val setupWrites: Writes[Setup] = (
    (JsPath \ "profitPerGpu").write[Float] and
      ((JsPath \ "rackList").write[Seq[Rack]])
    ) (unlift(Setup.unapply))

  def all = Action { implicit request: Request[AnyContent] =>
    val futureList = rackRepository.list()
    var rackSeq: Seq[Rack] = Seq.empty

    val resultRack = Await.result(futureList, 20 seconds)
    resultRack.foreach { r =>
      var gpuSeq: Seq[Gpu] = Seq.empty
      val gpuSeqFuture = gpuRepository.getByRack(r.id)
      val listGpu = for {
        listGpu <- gpuSeqFuture
      } yield listGpu
      val result = Await.result(listGpu, 20 seconds)
      result.foreach { gpuRow =>
        gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
      }
      val rack = Rack(r.id, r.produced, Util.toDate(r.currentHour), gpuSeq)
      rackSeq = rackSeq :+ rack
    }
    val setup: Setup = Setup(profitPerGpu, rackSeq)
    Ok(Json.toJson(setup)).as(JSON)
  }

  def addRack = Action(parse.json) {
    request =>
      val either = request.body.validate[Rack]
      either.fold(
        errors => BadRequest("invalid json Rack"),
        rack => {
          val f: Future[Option[RackRow]] = rackRepository.getById(rack.id)
          val result = Await.result(f, 20 seconds)
          result match {
            case Some(r) =>
              // If the Rack already exists we update the produced and currentTime properties
              val fGpu: Future[Seq[GpuRow]] = gpuRepository.getByRack(r.id)
              val resultGpu = Await.result(fGpu, 20 seconds)
              val total = resultGpu.map(_.produced).sum
              rackRepository.update(r.id, Some(total), Some(System.currentTimeMillis))
              Ok("Rack already exists! Updated produced and currentTime.")
            case None =>
              // If the Rack does not exist we create it.
              val rackRow = RackRow(rack.id, rack.produced, System.currentTimeMillis)
              rackRepository.insert(rackRow)
              Ok
          }
        }
      )
  }

  def getRacks(at: String) = Action { implicit request: Request[AnyContent] =>
    val time: Long = Util.toTime(at)
    val futureList = rackRepository.get(time)
    val resultRack = Await.result(futureList, 20 seconds)
    var rackSeq: Seq[Rack] = Seq.empty
    var gpuSeq: Seq[Gpu] = Seq.empty

    if (resultRack.isEmpty) BadRequest("Rack not found")
    else {
      resultRack.foreach { r =>
        val gpuSeqFuture = gpuRepository.getByRack(r.id)
        val listGpu = for {
          listGpu <- gpuSeqFuture
        } yield listGpu
        val result = Await.result(listGpu, 20 seconds)
        result.foreach { gpuRow =>
          gpuSeq = gpuSeq :+ Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
        }
        val rack = Rack(r.id, r.produced, Util.toDate(r.currentHour), gpuSeq)
        rackSeq = rackSeq :+ rack
      }
    }
    Ok(Json.toJson(rackSeq)).as(JSON)
  }
}
