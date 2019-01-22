package com.domain.user

import com.domain.Converters._
import com.domain.Tanks._
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.TankerTank
import com.domain.{Constants, WN8}
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.{parse, render}

import scala.concurrent.Future
import scala.util.Try

object UserTanksWn8 {

  def getTanksFromWG(accountId: Int, currentDay: Long): Option[Seq[TankerTank]] = {
    if (WGTankerDetails.getDayOfLastBattle(accountId) == currentDay) Some(fetchTanks(accountId, currentDay)) else None
  }

  def getTankerTanksForHisLastDay(accountId: Int): Seq[TankerTank] = fetchTanks(accountId, WGTankerDetails.getDayOfLastBattle(accountId))

  def refreshTankerTanks(accountId: Int): Future[Seq[TankerTank]] = {
    val day = WGTankerDetails.getDayOfLastBattle(accountId)
    DB.TankerTanksDao.addOrReplaceGivenDay(accountId, day, fetchTanks(accountId, day))
  }

  def getTankerLatestTanks(accountId: Int): Future[Seq[TankerTank]] = for {
    tanksFromDb: Seq[TankerTank] <- DB.TankerTanksDao.findLatestForAccountId(accountId)
    tanks <- if (tanksFromDb.nonEmpty) Future(tanksFromDb) else refreshTankerTanks(accountId)
  } yield tanks

  def getTankerTanksForDay(accountId: Int, day: Long): Future[Seq[TankerTank]] = for {
    tanksFromDb: Seq[TankerTank] <- DB.TankerTanksDao.findForAccountIdAndLastDayBattle(accountId, day)
    tanks <- if (tanksFromDb.nonEmpty) Future(tanksFromDb) else {
      getTanksFromWG(accountId, day).foreach(tanks => DB.TankerTanksDao.addOrReplaceGivenDay(accountId, day, tanks))
      DB.TankerTanksDao.findForAccountIdAndLastDayBattle(accountId, day)
    }
  } yield tanks

  def getTankerTanksForPreviousDay(accountId: Int, referenceDay: Long): Future[Seq[TankerTank]] = {
    for {
      prevDay <- DB.TankerTanksDao.findPreviousDayForAccountId(accountId, referenceDay)
      tanks <- getTankerTanksForDay(accountId, prevDay.getOrElse(0))
    } yield tanks
  }

  def getTankerTanksForCurrentOrNextDay(accountId: Int, referenceDay: Long): Future[Seq[TankerTank]] = {
    for {
      day <- DB.TankerTanksDao.findDayForAccountIdStartingFrom(accountId, referenceDay)
      tanks <- getTankerTanksForDay(accountId, day.getOrElse(0))
    } yield tanks
  }

  private def fetchTanks(accountId: Int, day: Long): List[TankerTank] = {

    val tanks: Map[Int, VehicleAverages] = tanksExpectedValues

    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")

    val tanksStatsAsList = Try(parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]).recover {
      case _ => return List.empty
    }.get

    if (tanksStatsAsList == null) return List.empty

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
        val battles: Int = tankStatsMap("battles").toInt
        val averageXp: Double = tankStatsMap("battle_avg_xp").toDouble

        val expectedForVehicle = tanks(Integer.valueOf(currentTankId.toString))
        val humanWN8: Double = WN8.calculateWn8Value(toVehicleFromMap(elem), expectedForVehicle)

        Some(TankerTank(accountId, currentTankId.toInt, frags, damage, spot, defence, battles, wins.toInt, averageXp, humanWN8, day))
      } else {
        None
      }
    })
  }
}
