package controllers

import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

class GpuControllerSpec extends PlaySpecification {
  "GpuController" should {
    "send 404 on a bad request" in new WithApplication {
      val result = route(app, FakeRequest(GET, "/boum")).get
      status(result) mustEqual NOT_FOUND
    }
  }
}
