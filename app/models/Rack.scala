package models

import scala.concurrent.{ ExecutionContext, Future }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.{ JdbcBackend, JdbcProfile }

final case class RackRow(id: String, gpuId: String)

class Rack(protected val dbConfigProvider: DatabaseConfigProvider)
            (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def getProfile: JdbcProfile = profile

  def database: JdbcBackend#DatabaseDef = db

  class RackTable(tag: Tag) extends Table[RackRow](tag, "foobar") {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, gpuId) <> (RackRow.tupled, RackRow.unapply)

    def id = column[String]("id")

    def gpuId = column[String]("gpuId")

  }

  lazy val RackTable = new TableQuery(tag => new RackTable(tag))

  def create(row: List[RackRow]): Future[Option[Int]] =
    db.run(RackTable ++= row)

  def insert(row: RackRow): Future[Unit] =
    db.run(RackTable += row).map(_ => ())

  def list(): Future[Seq[RackRow]] =
    db.run(RackTable.result)

  // def get(id: String): Future[RackRow] = db.run(RackTable. )
}

