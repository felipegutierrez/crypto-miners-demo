package models

import javax.inject._

import models.helper.Util
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}

case class Gpu(id: String, rackId: String, produced: Float, installedAt: String)

case class GpuRow(id: String, rackId: String, produced: Float, installedAt: Long)

case class GpuException(message: String) extends Exception(message)

class GpuRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                             (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  lazy val GpuTable = new TableQuery(tag => new GpuTable(tag))
  lazy val rackRepository = new RackRepository(dbConfigProvider)

  def getProfile: JdbcProfile = profile

  def database: JdbcBackend#DatabaseDef = db

  def create(row: List[GpuRow]): Future[Option[Int]] =
    db.run(GpuTable ++= row)

  def listAllGpu(): Future[Seq[Gpu]] = {
    list().map { seqGpuRow: Seq[GpuRow] =>
      seqGpuRow.map(gpuRowToGpu) // return Seq[Gpu]
    }
  }

  def list(): Future[Seq[GpuRow]] =
    db.run(GpuTable.result)

  def gpuRowToGpu(gpuRow: GpuRow): Gpu = {
    Gpu(gpuRow.id, gpuRow.rackId, gpuRow.produced, Util.toDate(gpuRow.installedAt))
  }

  def addGpuAtRack(gpu: Gpu): Future[Unit] = {
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
        getByRack(rackRow.id).map { seqGpuRow: Seq[GpuRow] =>
          // Insert Gpu in the Rack
          insert(GpuRow(rackRow.id + "-gpu-" + seqGpuRow.size, rackRow.id, produced, Util.toTime(gpu.installedAt)))
          // update produced from Rack as a sum of all gpu produced
          val total = seqGpuRow.map(_.produced).sum + produced
          rackRepository.update(rackRow.id, Some(total), None)
        }
      case None =>
        // If the Rack does not exist throw an Exception
        throw new RackException("Rack [" + gpu.rackId + "] does not exist.")
    }
  }

  def insert(row: GpuRow): Future[Unit] =
    db.run(GpuTable += row).map(_ => ())

  def getByRack(rackId: String): Future[Seq[GpuRow]] =
    db.run(GpuTable.filter(_.rackId === rackId).result)

  class GpuTable(tag: Tag) extends Table[GpuRow](tag, "gpu") {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, rackId, produced, installedAt) <> (GpuRow.tupled, GpuRow.unapply)

    def id = column[String]("id")

    def rackId = column[String]("rackId")

    def produced = column[Float]("produced")

    def installedAt = column[Long]("installedAt")
  }

}
