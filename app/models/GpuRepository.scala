package models


import scala.concurrent.{ ExecutionContext, Future }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.{ JdbcBackend, JdbcProfile }

case class Gpu(id: String, rackId: String, produced: Float, installedAt: String)

case class GpuRow(id: String, rackId: String, produced: Float, installedAt: Long)

class GpuRepository (protected val dbConfigProvider: DatabaseConfigProvider)
                    (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def getProfile: JdbcProfile = profile

  def database: JdbcBackend#DatabaseDef = db

  class GpuTable(tag: Tag) extends Table[GpuRow](tag, "gpu") {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, rackId, produced, installedAt) <> (GpuRow.tupled, GpuRow.unapply)

    def id = column[String]("id")
    def rackId = column[String]("rackId")
    def produced = column[Float]("produced")
    def installedAt = column[Long]("installedAt")
  }

  lazy val GpuTable = new TableQuery(tag => new GpuTable(tag))

  def create(row: List[GpuRow]): Future[Option[Int]] =
    db.run(GpuTable ++= row)

  def insert(row: GpuRow): Future[Unit] =
    db.run(GpuTable += row).map(_ => ())

  def list(): Future[Seq[GpuRow]] =
    db.run(GpuTable.result)

  def getByRack(rackId: String): Future[Seq[GpuRow]] =
    db.run(GpuTable.filter(_.rackId === rackId).result)

}
