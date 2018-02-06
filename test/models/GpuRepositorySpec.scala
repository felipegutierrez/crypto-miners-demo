package models

import models.helper.Util
import org.specs2.mutable.Specification
import play.api.Application
import play.api.test.WithApplicationLoader

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class GpuRepositorySpec extends Specification {
  "GpuRepository" should {

    val timeToRetrieve = 3 seconds

    "delete and insert Gpu's" in new WithApplicationLoader {
      val app2dao = Application.instanceCache[GpuRepository]
      val gpuRepository: GpuRepository = app2dao(app)

      Await.result(gpuRepository.delete("g-1"), timeToRetrieve)
      Await.result(gpuRepository.delete("g-2"), timeToRetrieve)
      Await.result(gpuRepository.delete("g-3"), timeToRetrieve)

      val testGpus = Set(
        GpuRow("g-1", "r-1", 0.2F, System.currentTimeMillis()),
        GpuRow("g-2", "r-1", 0.5F, System.currentTimeMillis()),
        GpuRow("g-3", "r-1", 0.9F, System.currentTimeMillis())
      )

      Await.result(Future.sequence(testGpus.map(gpuRepository.insert)), timeToRetrieve)
      val storedGpus = Await.result(gpuRepository.list(), timeToRetrieve)

      storedGpus.toSet must contain(testGpus)
    }

    "get Gpu by Rack ID" in new WithApplicationLoader {
      val app2dao = Application.instanceCache[GpuRepository]
      val gpuRepository: GpuRepository = app2dao(app)

      Await.result(gpuRepository.delete("g-1"), timeToRetrieve)
      Await.result(gpuRepository.delete("g-2"), timeToRetrieve)
      Await.result(gpuRepository.delete("g-3"), timeToRetrieve)

      val testGpus = Set(
        GpuRow("g-1", "r-1", 0.2F, System.currentTimeMillis()),
        GpuRow("g-2", "r-1", 0.5F, System.currentTimeMillis()),
        GpuRow("g-3", "r-1", 0.9F, System.currentTimeMillis())
      )

      Await.result(Future.sequence(testGpus.map(gpuRepository.insert)), timeToRetrieve)

      val storedGpus = Await.result(gpuRepository.getByRack("r-1"), timeToRetrieve)

      storedGpus.toSet must contain(testGpus)
    }

    "convert GpuRow to Gpu" in new WithApplicationLoader {
      val app2dao = Application.instanceCache[GpuRepository]
      val gpuRepository: GpuRepository = app2dao(app)

      val gpuRow: GpuRow = GpuRow("g-1", "r-1", 0.2F, System.currentTimeMillis())
      val gpu: Gpu = gpuRepository.gpuRowToGpu(gpuRow)

      gpu.id must equalTo(gpuRow.id)
      gpu.rackId must equalTo(gpuRow.rackId)
      gpu.produced must equalTo(gpuRow.produced)
      Util.toTime(gpu.installedAt) must equalTo(gpuRow.installedAt)
    }

    "add Gpu at Rack" in new WithApplicationLoader {
      val rackDao = Application.instanceCache[RackRepository]
      val rackRepository: RackRepository = rackDao(app)
      val gpuDao = Application.instanceCache[GpuRepository]
      val gpuRepository: GpuRepository = gpuDao(app)
      val time: Long = System.currentTimeMillis()


      // Delete and Add Rack r-1
      Await.result(rackRepository.delete("r-1"), timeToRetrieve)
      val rackRow: RackRow = RackRow("r-1", 0.2F, time)
      val testRacks = Set(rackRow)
      Await.result(rackRepository.insert(rackRow), timeToRetrieve)

      // Delete and Add Gpu g-1 to Rack r-1
      Await.result(gpuRepository.deleteByRack("r-1"), timeToRetrieve)
      val gpuRow: GpuRow = GpuRow("g-1", "r-1", 0.2F, time)
      val gpu: Gpu = gpuRepository.gpuRowToGpu(gpuRow)
      Await.result(gpuRepository.addGpuAtRack(gpu), timeToRetrieve)

      // List Gpu's by Rack
      val storedGpus = Await.result(gpuRepository.getByRack(rackRow.id), timeToRetrieve)
      val gpuRowResult: GpuRow = GpuRow("r-1-gpu-0", "r-1", 0.2F, time)
      storedGpus.toSet must contain(gpuRowResult)
    }
  }
}
