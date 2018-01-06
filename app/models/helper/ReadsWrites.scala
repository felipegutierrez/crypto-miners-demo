package models.helper

import models.{Gpu, Rack, Setup}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

object ReadsWrites {
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

  implicit val rackReads: Reads[Rack] = (

    (JsPath \ "id").read[String] and
      ((JsPath \ "produced").read[Float] or Reads.pure(0.toFloat)) and
      ((JsPath \ "currentHour").read[String] or Reads.pure(Util.toDate(System.currentTimeMillis))) and
      ((JsPath \ "gpuList").read[Seq[Gpu]] or Reads.pure(Seq.empty[Gpu]))
    ) (Rack.apply _)

  implicit val rackWrites: Writes[Rack] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "produced").write[Float] and
      (JsPath \ "currentHour").write[String] and
      ((JsPath \ "gpuList").write[Seq[Gpu]])
    ) (unlift(Rack.unapply))

  implicit val setupWrites: Writes[Setup] = (
    (JsPath \ "profitPerGpu").write[Float] and
      ((JsPath \ "rackList").write[Seq[Rack]])
    ) (unlift(Setup.unapply))

}
