package models

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}
import javax.inject._

case class Rack(id: String, produced: Float, currentHour: String, gpuList: Seq[Gpu])

case class RackRow(id: String, produced: Float, currentHour: Long)

case class RackException(message: String) extends Exception(message)

class RackRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                    (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  lazy val RackTable = new TableQuery(tag => new RackTable(tag))

  def getProfile: JdbcProfile = profile

  def database: JdbcBackend#DatabaseDef = db

  def create(row: List[RackRow]): Future[Option[Int]] =
    db.run(RackTable ++= row)

  def insert(row: RackRow): Future[Unit] =
    db.run(RackTable += row).map(_ => ())

  def updateProduced(rackId: String, produced: Float): Future[Unit] =
    db.run(RackTable.filter(_.id === rackId).map(r => r.produced).update(produced)).map(_ => ())

  def updateRack(rackId: String, produced: Float, currentHour: Long): Future[Unit] =
    db.run(RackTable.filter(_.id === rackId).map(r => (r.produced, r.currentHour)).update(produced, currentHour)).map(_ => ())

  def update(rackId: String, produced: Option[Float], currentHour: Option[Long]): Future[Unit] = {
    (produced, currentHour) match {
      case (Some(produced), Some(currentHour)) =>
        db.run(RackTable.filter(_.id === rackId).map(r => (r.produced, r.currentHour)).update(produced, currentHour)).map(_ => ())
      case (Some(produced), None) =>
        db.run(RackTable.filter(_.id === rackId).map(r => r.produced).update(produced)).map(_ => ())
      case (None, Some(currentHour)) =>
        db.run(RackTable.filter(_.id === rackId).map(r => r.currentHour).update(currentHour)).map(_ => ())
      case (None, None) => Future("update not executed.")
    }
  }

  def list(): Future[Seq[RackRow]] =
    db.run(RackTable.result)

  def get(at: Long): Future[Seq[RackRow]] =
    db.run(RackTable.filter(_.currentHour === at).result)

  def getById(id: String): Future[Option[RackRow]] =
    db.run(RackTable.filter(_.id === id).result).map(_.headOption)

  class RackTable(tag: Tag) extends Table[RackRow](tag, "rack") {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, produced, currentHour) <> (RackRow.tupled, RackRow.unapply)

    def id = column[String]("id")

    def produced = column[Float]("produced")

    def currentHour = column[Long]("currentHour")

    // def gpuList = column[Seq[Gpu]]("gpuList")
  }

}
