package com.domain.wn8

import com.domain.Constants
import com.domain.presentation.model.TankStats
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.Try


object Wn8Veh {

  def calculate(account_id: String) = {

    val tanks: Map[Int, Vehicle] = UserWn8.tanksExpectedValues()

    val tanksDetailsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/encyclopedia/tanks/?application_id=${Constants.APPLICATION_ID}").mkString
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$account_id").mkString

    val parsedTanksDetails: JValue = parse(tanksDetailsResponse)
    val parsedTanksStats: JValue = render((parse(tanksStatsResponse) \ "data" \ s"$account_id"))
    val tanksStatsAsList = parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]

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

        val humanWN8: Double = BigDecimal(WN8).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

        Some(TankStats(tankName, battles.toInt, Try(tankLevel.toInt).getOrElse(0), humanWN8))

      } else {
        None
      }
    })
    res
  }

}