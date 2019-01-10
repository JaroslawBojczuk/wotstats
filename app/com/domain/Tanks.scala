package com.domain

import java.nio.file.Files

import com.domain.Constants.EXPECTED_TANKS_VALUES_CSV_PATH

import scala.collection.JavaConverters._

object Tanks {

  case class VehicleAverages(IDNum: Int, frag: Double, dmg: Double, spot: Double, defence: Double, win: Double) {
    def multiplyBy(battles: Double): VehicleAverages = this.copy(
      frag = this.frag * battles,
      dmg = this.dmg * battles,
      spot = this.spot * battles,
      defence = this.defence * battles,
      win = this.win * battles * 0.01)

    def +(that: VehicleAverages): VehicleAverages = this.copy(
      frag = that.frag + this.frag,
      dmg = that.dmg + this.dmg,
      spot = that.spot + this.spot,
      defence = that.defence + this.defence,
      win = that.win + this.win)
  }

  case class VehicleWn8(IDNum: Int, wn8: Double)

  val tanksExpectedValues: Map[Int, VehicleAverages] = Files.readAllLines(EXPECTED_TANKS_VALUES_CSV_PATH).asScala.map(line => {
    val tank = line.split(",")
    tank(0) match {
      case "tank_id" => (0, VehicleAverages(0, 1, 1, 1, 1, 1))
      case _ => (tank(0).toInt, VehicleAverages(tank(0).toInt, tank(2).toDouble, tank(1).toDouble, tank(3).toDouble, tank(4).toDouble, tank(5).toDouble))
    }
  }).toMap

}
