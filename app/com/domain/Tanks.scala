package com.domain

import java.nio.file.Files

import com.domain.Constants.EXPECTED_TANKS_VALUES_CSV_PATH
import scala.collection.JavaConverters._

object Tanks {

  case class Vehicle(IDNum: Int, frag: Double, dmg: Double, spot: Double, defence: Double, win: Double)

  case class VehicleWn8(IDNum: Int, wn8: Double)

  val tanksExpectedValues: Map[Int, Vehicle] = Files.readAllLines(EXPECTED_TANKS_VALUES_CSV_PATH).asScala.map(line => {
    val tank = line.split(",")
    tank(0) match {
      case "tank_id" => (0, Vehicle(0, 1, 1, 1, 1, 1))
      case _ => (tank(0).toInt, Vehicle(tank(0).toInt, tank(2).toDouble, tank(1).toDouble, tank(3).toDouble, tank(4).toDouble, tank(5).toDouble))
    }
  }).toMap

}
