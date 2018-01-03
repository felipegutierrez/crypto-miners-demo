package controllers

import models._
import play.api.i18n.I18nSupport
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

class HomeController(cc: ControllerComponents, rackRepository: RackRepository, gpuRepository: GpuRepository)
  extends AbstractController(cc) with I18nSupport {

  implicit lazy val ec = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  implicit val gpuWrites: Writes[Gpu] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "rackId").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "installedAt").write[Long]
    ) (unlift(Gpu.unapply))

  implicit val gpuReads: Reads[Gpu] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "rackId").read[String] and
      (JsPath \ "produced").read[Float] and
      ((JsPath \ "installedAt").read[Long] or Reads.pure(System.currentTimeMillis))
    ) (Gpu.apply _)

  implicit val rackWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "currentHour").write[Long] and
      (JsPath \ "gpuList").write[Seq[Gpu]]
    ) (unlift(Rack.unapply))

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      (JsPath \ "produced").read[Float] and
      ((JsPath \ "currentHour").read[Long] or Reads.pure(System.currentTimeMillis)) and
      ((JsPath \ "gpuList").read[Seq[Gpu]] or Reads.pure(Seq.empty[Gpu]))
    ) (Rack.apply _)

  def all = Action { implicit request: Request[AnyContent] =>
    val futureList = rackRepository.list()
    var rackSeq: Seq[Rack] = Seq.empty

    val resultRack = Await.result(futureList, 20 seconds)
    resultRack.foreach { r =>
      val gpuSeqFuture = gpuRepository.getByRack(r.id)
      val listGpu = for {
        listGpu <- gpuSeqFuture
      } yield listGpu
      val result = Await.result(listGpu, 20 seconds)
      val rack = Rack(r.id, r.produced, r.currentHour, result)
      println(rack)
      rackSeq = rackSeq :+ rack
    }
    println(rackSeq)
    Ok(Json.toJson(rackSeq)).as(JSON)
  }

  def addRack = Action(parse.json) {
    request =>
      val either = request.body.validate[Rack]
      either.fold(
        errors => BadRequest("invalid json Rack"),
        rack => {
          //        if (rackRepository.filter { r: Rack => r.id == rack.id }.size > 0) {
          //          BadRequest("Rack id already exists")
          //        } else {
          val rackRow = RackRow(rack.id, rack.produced, System.currentTimeMillis)
          rackRepository.insert(rackRow)
          Ok
          //        }
        }
      )
  }

  def getRacks(at: Long) = Action.async {
    implicit request: Request[AnyContent] =>
      println(at)
      val futureList = rackRepository.get(at)
      val rackSeq: Seq[Rack] = Seq.empty
      futureList.map {
        list =>
          list.foreach {
            r =>
              val gpuSeq = gpuRepository.getByRack(r.id)
              gpuSeq.map {
                gpuList =>
                  rackSeq :+ Rack(r.id, r.produced, r.currentHour, gpuList)
              }
          }
          Ok(Json.toJson(rackSeq)).as(JSON)
      }
  }

  def addGpu() = Action(parse.json) {
    request =>
      println("addGpu")
      val either = request.body.validate[Rack]
      either.fold(
        errors => BadRequest("invalid json Rack"),
        rack => {
          val f: Future[Option[RackRow]] = rackRepository.getById(rack.id)
          f.onComplete {
            case Success(value) =>
              value match {
                case Some(r) =>
                  val fGpu: Future[Seq[Gpu]] = gpuRepository.getByRack(r.id)
                  fGpu.onComplete {
                    case Success(seq) => seq.size
                      val gpu = Gpu(r.id + "-gpu-" + seq.size, r.id, 0, System.currentTimeMillis)
                      println(gpu)
                      gpuRepository.insert(gpu)
                      Ok
                    case Failure(e) => BadRequest("Failure")
                  }
                case None => BadRequest("Rack not found")
              }
            case Failure(e) => BadRequest("Failure")
          }
          Ok
        }
      )
  }

  def allGpu = Action.async {
    implicit request: Request[AnyContent] =>
      val futureList = gpuRepository.list()
      futureList.map {
        list =>
          Ok(Json.toJson(list)).as(JSON)
      }
  }
}
