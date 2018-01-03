package models

case class Rack(id: String, produced: Float, var currentHour: Long, var gpuList: Seq[Gpu])
