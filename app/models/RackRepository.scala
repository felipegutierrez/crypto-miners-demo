package models

import scala.concurrent.{ ExecutionContext, Future }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.{ JdbcBackend, JdbcProfile }

case class Rack(id: String, produced: Float, currentHour: String, gpuList: Seq[Gpu])

case class RackRow(id: String, produced: Float, currentHour: Long)

class RackRepository (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def getProfile: JdbcProfile = profile

  def database: JdbcBackend#DatabaseDef = db

  class RackTable(tag: Tag) extends Table[RackRow](tag, "rack") {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, produced, currentHour) <> (RackRow.tupled, RackRow.unapply)

    def id = column[String]("id")
    def produced = column[Float]("produced")
    def currentHour = column[Long]("currentHour")
    // def gpuList = column[Seq[Gpu]]("gpuList")
  }

  lazy val RackTable = new TableQuery(tag => new RackTable(tag))

  def create(row: List[RackRow]): Future[Option[Int]] =
    db.run(RackTable ++= row)

  def insert(row: RackRow): Future[Unit] =
    db.run(RackTable += row).map(_ => ())

  def list(): Future[Seq[RackRow]] =
    db.run(RackTable.result)

  def get(at: Long): Future[Seq[RackRow]] =
    db.run(RackTable.filter(_.currentHour === at).result)

  def getById(id: String): Future[Option[RackRow]] =
    db.run(RackTable.filter(_.id === id).result).map(_.headOption)
}