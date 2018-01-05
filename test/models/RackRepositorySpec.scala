package models

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.db.evolutions._
import play.api.db.{Database, Databases}
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Injecting

class RackRepositorySpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val database = Databases(
    driver = "org.sqlite.JDBC",
    url = "jdbc:sqlite:development.db",
    name = "default",
    config = Map(
      "username" -> "",
      "password" -> ""
    )
  )
  val guice = new GuiceInjectorBuilder()
    .overrides(bind[Database].toInstance(database))
    .injector()
  // val defaultDbProvider = guice.instanceOf[DatabaseConfigProvider]

  def beforeAll() = Evolutions.applyEvolutions(database)

  def afterAll() = {
    // Evolutions.cleanupEvolutions(database)
    database.shutdown()
  }

  Evolution(
    1,
    "create table test (id bigint not null, name varchar(255));",
    "drop table test;"
  )

  //  "RackController GET" should {
  //    "render the rack page from a new instance of controller" in {
  //      val controller = new RackController(stubControllerComponents(), rack, gpu)
  //      val result = controller.all().apply(FakeRequest(GET, "/api/all"))
  //
  //      status(result) mustBe OK
  //      contentType(result) mustBe Some("application/json")
  //      contentAsString(result) must include("{ \"profitPerGpu\" }")
  //    }
  //  }
}
