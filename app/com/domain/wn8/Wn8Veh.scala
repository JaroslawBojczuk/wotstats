package com.domain.wn8

import java.nio.file.{Files, Paths}

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._


object Wn8Veh {

  case class Vehicle(IDNum: Int, frag: Double, dmg: Double, spot: Double, defence: Double, win: Double)

  def calculate(account_id: String): List[(String, Double, Int, Double)] = {
    println("Starting")

    val csvStart = System.currentTimeMillis()
    val path = Paths.get("E:\\Project\\expected_tank_values_29.csv")

    val tanks: Map[Int, Vehicle] = Files.readAllLines(path).asScala.map(line => {
      val tank = line.split(",")
      tank(0) match {
        case "tankid" => (0, Vehicle(0, 1, 1, 1, 1, 1))
        case _ => (tank(0).toInt, Vehicle(tank(0).toInt, tank(1).toDouble, tank(2).toDouble, tank(3).toDouble, tank(4).toDouble, tank(5).toDouble))
      }
    }).toMap
    val csvEnd = System.currentTimeMillis()

    val application_id: String = "c0a88d6d3b5657d6750bd219d55fb550"

    val tankDetailsStart = System.currentTimeMillis()
    val tanksDetailsResponse: String = scala.io.Source
      .fromURL(s"https://api.worldoftanks.eu/wot/encyclopedia/tanks/?application_id=$application_id").mkString
    val tankDetailsEnd = System.currentTimeMillis()

    val tankStatsStart = System.currentTimeMillis()
    val tanksStatsResponse: String = scala.io.Source
      .fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=$application_id&account_id=$account_id").mkString
    val tankStatsEnd = System.currentTimeMillis()

    val parsedTanksDetails: JValue = parse(tanksDetailsResponse)
    val parsedTanksStats: JValue = render((parse(tanksStatsResponse) \ "data" \ s"$account_id"))
    val tanksStatsAsList = parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]

    val calcsStart = System.currentTimeMillis()
    val res = tanksStatsAsList.flatMap(elem => {

      val currentTankId: Any = elem.get("tank_id").get
      val currentTankStats = elem.get("all")

      val tankName = render((parsedTanksDetails \ "data" \ s"$currentTankId" \ "name_i18n")).values.toString
      val tankLevel = render((parsedTanksDetails \ "data" \ s"$currentTankId" \ "level")).values.toString
      val tankStatsMap = currentTankStats.get.asInstanceOf[Map[String, BigInt]]

      if (tanks.get(Integer.valueOf(currentTankId.toString)).isDefined && tankStatsMap.get("battles").get.toDouble != 0) {
        //if (tankLevel.equals("10") && tanks.get(Integer.valueOf(currentTankId.toString)).isDefined) {

        val damage: Double = tankStatsMap.get("damage_dealt").get.toDouble
        val spot: Double = tankStatsMap.get("spotted").get.toDouble
        val frags: Double = tankStatsMap.get("frags").get.toDouble
        val defence: Double = tankStatsMap.get("dropped_capture_points").get.toDouble
        val wins: Double = tankStatsMap.get("wins").get.toDouble
        val battles: Double = tankStatsMap.get("battles").get.toDouble

        val avg_damage: Double = damage / battles
        val avg_spot: Double = spot / battles
        val avg_frags: Double = frags / battles
        val avg_defence: Double = defence / battles
        val avg_wins: Double = BigDecimal((wins / battles) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble

        val expectedValues = tanks.get(Integer.valueOf(currentTankId.toString)).get

        val expFrag: Double = expectedValues.frag
        val expDmg: Double = expectedValues.dmg
        val expSpot: Double = expectedValues.spot
        val expDef: Double = expectedValues.defence
        val expWin: Double = expectedValues.win

        val rDAMAGE: Double = avg_damage / expDmg
        val rSPOT: Double = avg_spot / expSpot
        val rFRAG: Double = avg_frags / expFrag
        val rDEF: Double = avg_defence / expDef
        val rWin: Double = avg_wins / expWin


        val rWINc = Math.max(0, (rWin - 0.71) / (1 - 0.71))
        val rDAMAGEc = Math.max(0, (rDAMAGE - 0.22) / (1 - 0.22))
        val rFRAGc = Math.max(0, Math.min(rDAMAGEc + 0.2, (rFRAG - 0.12) / (1 - 0.12)))
        val rSPOTc = Math.max(0, Math.min(rDAMAGEc + 0.1, (rSPOT - 0.38) / (1 - 0.38)))
        val rDEFc = Math.max(0, Math.min(rDAMAGEc + 0.1, (rDEF - 0.10) / (1 - 0.10)))

        val WN8 = 980 * rDAMAGEc + 210 * rDAMAGEc * rFRAGc + 155 * rFRAGc * rSPOTc + 75 * rDEFc * rFRAGc + 145 * Math.min(1.8, rWINc)

        val humanWN8 = BigDecimal(WN8).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

        Some((tankName, humanWN8, battles.toInt, avg_frags))
      } else {
        None
      }
    })

    res.sortBy(elem => -elem._2)
  }

  def main(args: Array[String]) {

    val calcsEnd = System.currentTimeMillis()

    calculate("500557563").sortBy(elem => -elem._2).filter(e => e._3 > 1)
      .foreach(elem => println(f"${elem._1.padTo(15, " ").mkString("")} \t ${elem._2}%2.0f \t ${elem._3} \t ${elem._4}%2.2f"))

  }

}