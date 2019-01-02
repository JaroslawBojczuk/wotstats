package com.domain.user

import com.domain.Constants
import com.domain.Tanks._
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.TankerTank
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.{parse, render}

import scala.concurrent.Future

object UserTanksWn8 {

  def refreshTankerTanks(accountId: Int): Future[Seq[TankerTank]] = {
    val day = WGTankerDetails.getDayOfLastBattle(accountId)
    DB.TankerTanksDao.addOrReplaceCurrentDay(accountId, day, calculate(accountId, day))
  }

  def getTankerLatestTanks(accountId: Int): Future[Seq[TankerTank]] = for {
    tanksFromDb: Seq[TankerTank] <- DB.TankerTanksDao.findLatestForAccountId(accountId)
    tanks <- if (tanksFromDb.nonEmpty) Future(tanksFromDb) else refreshTankerTanks(accountId)
  } yield tanks

  private def calculate(accountId: Int, day: Long): List[TankerTank] = {

    val tanks: Map[Int, Vehicle] = tanksExpectedValues

    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")

    val tanksStatsAsList = parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]

    tanksStatsAsList.flatMap(elem => {
      val currentTankId = elem("tank_id").toString
      val currentTankStats = elem.get("all")

      val tankStatsMap = currentTankStats.get.asInstanceOf[Map[String, BigInt]]
      if (tanks.get(Integer.valueOf(currentTankId.toString)).isDefined && tankStatsMap("battles").toDouble != 0) {
        val damage: Double = tankStatsMap("damage_dealt").toDouble
        val spot: Double = tankStatsMap("spotted").toDouble
        val frags: Double = tankStatsMap("frags").toDouble
        val defence: Double = tankStatsMap("dropped_capture_points").toDouble
        val wins: Double = tankStatsMap("wins").toDouble
        val battles: Double = tankStatsMap("battles").toDouble
        val averageXp: Double = tankStatsMap("battle_avg_xp").toDouble

        val avg_damage: Double = damage / battles
        val avg_spot: Double = spot / battles
        val avg_frags: Double = frags / battles
        val avg_defence: Double = defence / battles
        val avg_wins: Double = BigDecimal((wins / battles) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble

        val expectedValues = tanks(Integer.valueOf(currentTankId.toString))

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

        Some(TankerTank(accountId, currentTankId.toInt, frags, damage, spot, defence, battles.toInt, wins.toInt, averageXp, humanWN8, day))
      } else {
        None
      }
    })
  }
}
