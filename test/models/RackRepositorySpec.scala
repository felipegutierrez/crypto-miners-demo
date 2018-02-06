package models

import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplicationLoader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class RackRepositorySpec extends Specification {

  "RackRepository" should {

    val timeToRetrieve = 3 seconds

    "delete and insert Rack's" in new WithApplicationLoader {
      val app2dao = Application.instanceCache[RackRepository]
      val rackRepository: RackRepository = app2dao(app)

      Await.result(rackRepository.delete("r-1"), timeToRetrieve)
      Await.result(rackRepository.delete("r-2"), timeToRetrieve)
      Await.result(rackRepository.delete("r-3"), timeToRetrieve)

      val testRacks = Set(
        RackRow("r-1", 0.2F, System.currentTimeMillis()),
        RackRow("r-2", 0.5F, System.currentTimeMillis()),
        RackRow("r-3", 0.8F, System.currentTimeMillis())
      )

      Await.result(Future.sequence(testRacks.map(rackRepository.insert)), timeToRetrieve)
      val storedRacks = Await.result(rackRepository.list(), timeToRetrieve)

      storedRacks.toSet must contain(testRacks)
    }
  }
}
