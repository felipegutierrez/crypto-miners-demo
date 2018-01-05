package models.helper

import org.scalatestplus.play.PlaySpec

class UtilSpec extends PlaySpec {

  "Time in miliseconds converted to date" must {
    "return the same time in miliseconds when converted back" in {
      val currentTime: Long = 1515012397212L
      val time: String = Util.toDate(currentTime)
      Util.toTime(time) mustBe currentTime
    }
  }
}
