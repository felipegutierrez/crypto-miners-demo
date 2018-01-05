package controllers

import models.{GpuRepository, RackRepository}
import org.h2.engine.Mode
import org.scalatest.prop.Configuration
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test._

class RackControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

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
  //  val rack = guice.instanceOf[RackRepository]
  //  val gpu = guice.instanceOf[GpuRepository]
  //
  //  def beforeAll() = Evolutions.applyEvolutions(database)
  //  def afterAll() = {
  //    // Evolutions.cleanupEvolutions(database)
  //    database.shutdown()
  //  }
  //
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
