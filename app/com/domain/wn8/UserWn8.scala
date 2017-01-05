package com.domain.wn8

import java.nio.file.{Files, Path, Paths}

import com.domain.Constants
import com.domain.clans.ClanUtils
import org.json4s._
import org.json4s.jackson.JsonMethods._
import play.api.Logger

import scala.collection.JavaConverters._
import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}
import scala.concurrent.forkjoin.ForkJoinPool

object UserWn8 {

  val expectedTanksValuesCsvPath: Path = Paths.get("C:\\Projects\\expected_tank_values_28.csv")

  def tanksExpectedValues(path: Path = expectedTanksValuesCsvPath): Map[Int, Vehicle] = Files.readAllLines(path).asScala.map(line => {
    val tank = line.split(",")
    tank(0) match {
      case "tankid" => (0, Vehicle(0, 1, 1, 1, 1, 1))
      case _ => (tank(0).toInt, Vehicle(tank(0).toInt, tank(1).toDouble, tank(2).toDouble, tank(3).toDouble, tank(4).toDouble, tank(5).toDouble))
    }
  }).toMap

  private def accountTanks(accountId: String): List[Map[String, Any]] = {
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=tank_id%2Call").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")
    parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]
  }

  private def accountTanksBattles(accountId: String): List[Map[String, Any]] = {
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=tank_id%2Call.battles").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")
    parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]
  }

  private def accountStatisticsAll(accountId: String) = {
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=statistics.all").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId" \ "statistics" \ "all")
    parsedTanksStats.values.asInstanceOf[Map[String, Any]]
  }

  private def currentTankAvgValues(tankId: Int, values: Map[String, BigInt]): Option[Vehicle] = {
    val damage: Double = values.get("damage_dealt").get.toDouble
    val spot: Double = values.get("spotted").get.toDouble
    val frags: Double = values.get("frags").get.toDouble
    val defence: Double = values.get("dropped_capture_points").get.toDouble
    val wins: Double = values.get("wins").get.toDouble
    val battles: Double = values.get("battles").get.toDouble

    if (battles > 0) {
      val avg_damage: Double = damage / battles
      val avg_spot: Double = spot / battles
      val avg_frags: Double = frags / battles
      val avg_defence: Double = defence / battles
      val avg_wins: Double = BigDecimal((wins / battles) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
      Some(Vehicle(tankId, avg_frags, avg_damage, avg_spot, avg_defence, avg_wins))
    } else None
  }

  private def calculateWn8(actualValues: Vehicle, expectedValues: Vehicle): Double = {
    val rDAMAGE: Double = actualValues.dmg / expectedValues.dmg
    val rSPOT: Double = actualValues.spot / expectedValues.spot
    val rFRAG: Double = actualValues.frag / expectedValues.frag
    val rDEF: Double = actualValues.defence / expectedValues.defence
    val rWin: Double = actualValues.win / expectedValues.win

    val rWINc = Math.max(0, (rWin - 0.71) / (1 - 0.71))
    val rDAMAGEc = Math.max(0, (rDAMAGE - 0.22) / (1 - 0.22))
    val rFRAGc = Math.max(0, Math.min(rDAMAGEc + 0.2, (rFRAG - 0.12) / (1 - 0.12)))
    val rSPOTc = Math.max(0, Math.min(rDAMAGEc + 0.1, (rSPOT - 0.38) / (1 - 0.38)))
    val rDEFc = Math.max(0, Math.min(rDAMAGEc + 0.1, (rDEF - 0.10) / (1 - 0.10)))

    val WN8 = 980 * rDAMAGEc + 210 * rDAMAGEc * rFRAGc + 155 * rFRAGc * rSPOTc + 75 * rDEFc * rFRAGc + 145 * Math.min(1.8, rWINc)

    BigDecimal(WN8).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def accountTanksWn8s(accountId: String): List[VehicleWn8] = {
    val tanks = accountTanks(accountId)
    val expectedValues = tanksExpectedValues()

    tanks.flatMap(currentTank => {
      val currentTankId = currentTank.get("tank_id").get.toString.toInt
      val currentTankStatsMap = currentTank.get("all").get.asInstanceOf[Map[String, BigInt]]
      expectedValues.get(currentTankId) match {
        case Some(vehicleExpectedValues) => {
          val currentTankAverageValues: Option[Vehicle] = currentTankAvgValues(currentTankId, currentTankStatsMap)

          val result = currentTankAverageValues match {
            case Some(currentVeh) => Some(VehicleWn8(currentTankId, calculateWn8(currentVeh, vehicleExpectedValues)))
            case _ => None
          }
          result
        }
        case _ => None
      }
    })
  }

  case class UserWn8WithBattles(wn8: Double, battles: Int)

  def accountWn8(accountId: String) = {
    Logger.logger.debug(s"Calculating wn8 for user: $accountId")
    val tanks = accountTanks(accountId)
    val expectedValues = tanksExpectedValues()

    var totalUserBattles: Int = 0

    val totalExpected = tanks.par.flatMap(currentTank => {
      val currentTankId = currentTank.get("tank_id").get.toString.toInt
      val tankBattles = currentTank.get("all").get.asInstanceOf[Map[String, BigInt]].get("battles").get.toDouble
      totalUserBattles += tankBattles.toInt
      expectedValues.get(currentTankId) match {
        case Some(expVal) => Some(Vehicle(expVal.IDNum, expVal.frag * tankBattles, expVal.dmg * tankBattles, expVal.spot * tankBattles, expVal.defence * tankBattles, 0.01 * expVal.win * tankBattles))
        case _ => None
      }
    }).reduce((a, b) => { Vehicle(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win) })

    val totalAccount = tanks.par.flatMap(currentTank => {
      val currentTankId = currentTank.get("tank_id").get.toString.toInt
      val currentTankStatsMap = currentTank.get("all").get.asInstanceOf[Map[String, BigInt]]
      expectedValues.get(currentTankId) match {
        case Some(_) => {
          val damage: Double = currentTankStatsMap.get("damage_dealt").get.toDouble
          val spot: Double = currentTankStatsMap.get("spotted").get.toDouble
          val frags: Double = currentTankStatsMap.get("frags").get.toDouble
          val defence: Double = currentTankStatsMap.get("dropped_capture_points").get.toDouble
          val wins: Double = currentTankStatsMap.get("wins").get.toDouble
          Some(Vehicle(currentTankId, frags, damage, spot, defence, wins))
        }
        case _ => None
      }

    }).reduce((a, b) => { Vehicle(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win) })

    UserWn8WithBattles(calculateWn8(totalAccount, totalExpected), totalUserBattles)
  }

  def main(args: Array[String]) {

    //val res = accountWn8("529089908")
    //println(res)
    //val res = accountTanksWn8s("513663004")
    //res.sortBy(-_.wn8).foreach(println(_))

    val clanDetails = ClanUtils.getClanDetails("500034335")

    val wn8sAndBattles = clanDetails.members.par.map(member => UserWn8.accountWn8(member.accountId.toString))
    val totalBattles = wn8sAndBattles.map(_.battles).sum
    val weightedWn8s = wn8sAndBattles.map(v => (v.wn8 * v.battles) / totalBattles)

    val avg = weightedWn8s.sum

    //val avg: Double = clanDetails.members.par.map(member => UserWn8.accountWn8(member.accountId.toString))
    println(avg)

  }

}
