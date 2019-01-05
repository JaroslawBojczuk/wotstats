package com.domain.user

import java.util.concurrent.TimeUnit

import com.domain.{Constants, WN8}
import com.domain.db.schema.TankerTank
import com.domain.presentation.model.{TankStats, TankerDetails, TankerSession}
import com.fasterxml.jackson.databind.JsonNode
import org.joda.time.LocalDate
import play.libs.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object WGTankerDetails {

  private def url(accountId: String) = s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId"

  private val tankDetailsUrl = s"https://api.worldoftanks.eu/wot/encyclopedia/vehicles/?application_id=${Constants.APPLICATION_ID}&fields=tank_id,name,tier,images.contour_icon"
  private val tankDetails = ujson.read(requests.get(tankDetailsUrl).text).obj.get("data")

  private def findAccountId(name: String) = {
    val userResponse = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/account/list/?application_id=${Constants.APPLICATION_ID}&search=$name&type=exact&fields=account_id").mkString
    val userJson = Json.parse(userResponse)
    val count = userJson.findPath("meta").findPath("count").asInt()

    if (count == 1) Some(userJson.findPath("data").findPath("account_id").toString.toInt)
    else None
  }

  def getDayOfLastBattle(accountId: Int): Long = {
    val url = s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=last_battle_time"
    val lastBattleSecondsSince1970 = Try(ujson.read(requests.get(url).text).obj("data")(accountId.toString)("last_battle_time").num.toLong).recover { case _ => 0L }.get
    TimeUnit.SECONDS.toDays(lastBattleSecondsSince1970)
  }

  private def convertTanksToUi: TankerTank => TankStats = {
    tank => {
      val tankResponse = tankDetails.get(tank.tankId.toString)
      val tankName = tankResponse("name").str
      val tankLevel = tankResponse("tier").num
      val imgPath = tankResponse("images")("contour_icon").str
      val avg_damage: Double = tank.damageDealt / tank.battles.toDouble
      val avg_spot: Double = tank.spotted / tank.battles.toDouble
      val avg_frags: Double = tank.frags / tank.battles.toDouble
      val avg_wins: Double = BigDecimal((tank.wins.toDouble / tank.battles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
      TankStats(tankName, imgPath, tank.battles, Try(tankLevel.toInt).getOrElse(0), tank.wn8,
        BigDecimal(avg_damage).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        BigDecimal(0).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        BigDecimal(avg_frags).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        BigDecimal(avg_spot).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        avg_wins, tank.battleAvgXp)
    }
  }

  def getDetails(accountId: Int): Some[TankerDetails] = {
    val userResponse = scala.io.Source.fromURL(url(accountId.toString)).mkString
    val userJson = Json.parse(userResponse)
    val data: JsonNode = userJson.findPath("data")
    val name = data.findPath(accountId.toString).findPath("nickname").asText()
    val clanId = data.findPath(accountId.toString).findPath("clan_id").asText()
    val wn8WithBattles = Await.result(UserWn8.getAccountCachedWn8(accountId.toString), 1.minute)

    val tanks = Await.result(UserTanksWn8.getTankerLatestTanks(accountId), 1.minute).sortBy(-_.wn8)
    val dayOfLastBattle = TimeUnit.SECONDS.toDays(Try(data.findPath(accountId.toString).findPath("last_battle_time").asText().toLong).recover { case _ => 0L }.get)

    val accountWn8 = wn8WithBattles.wn8
    val battles = wn8WithBattles.battles
    val wins = tanks.map(_.wins).sum
    val spotted = tanks.map(_.spotted).sum
    val frags = tanks.map(_.frags).sum

    val tanksUi = tanks.map(convertTanksToUi)
    val avgTier = tanksUi.map(t => t.tier * t.battles).sum.toDouble / tanksUi.map(t => if (t.tier > 0) t.battles else 0).sum.toDouble
    val avgSpot = spotted.toDouble / battles.toDouble
    val avgFrags = frags.toDouble / battles.toDouble

    val previousDayTanks = Await.result(UserTanksWn8.getTankerTanksForDay(accountId, dayOfLastBattle - 1), 1.minute).sortBy(-_.wn8)
    val lastDaySession = if(previousDayTanks.nonEmpty) {
      val sessionTanks = WN8.calculateWn8PerTank(tanks, previousDayTanks).map(convertTanksToUi)
      val (wn8, battles) = WN8.calculateTotalWn8AndBattles(tanks, previousDayTanks)
      Some(TankerSession(battles, wn8, sessionTanks))
    } else None

    val lastWeekTanks = Await.result(UserTanksWn8.getTankerTanksForDay(accountId, dayOfLastBattle - 7), 1.minute).sortBy(-_.wn8)
    val lastWeekSession = if(lastWeekTanks.nonEmpty) {
      val sessionTanks = WN8.calculateWn8PerTank(tanks, lastWeekTanks).map(convertTanksToUi)
      val (wn8, battles) = WN8.calculateTotalWn8AndBattles(tanks, lastWeekTanks)
      Some(TankerSession(battles, wn8, sessionTanks))
    } else None

    Some(TankerDetails(name, accountId, clanId, battles, wins, avgTier, avgSpot, avgFrags, accountWn8, new LocalDate(0).plusDays(dayOfLastBattle.toInt), tanksUi, lastDaySession, lastWeekSession))
  }

  def getDetails(accountName: String): Option[TankerDetails] = {
    findAccountId(accountName) match {
      case Some(accountId) => getDetails(accountId)
      case _ => None
    }
  }

}
