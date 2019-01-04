package com.domain.user

import com.domain.{Constants, WN8}
import com.domain.Tanks._
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.{Tanker, TankerHistory}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import play.api.Logger

import scala.concurrent.Future
import scala.util.Try

object UserWn8 {

  case class UserWn8WithBattles(accountId: Int, wn8: Double, battles: Int)

  def refreshAccountCachedWn8(accountId: String): Future[UserWn8WithBattles] = {
    val accountIdAsInt = accountId.toInt
    val day = WGTankerDetails.getDayOfLastBattle(accountIdAsInt)
    val data = calculateWn8(accountIdAsInt)
    for {
      _ <- DB.TankersDao.addOrUpdate(Tanker(accountIdAsInt, data.battles, data.wn8))
      res <- DB.TankerHistoryDao.addOrReplaceCurrentDay(accountIdAsInt, TankerHistory(accountIdAsInt, data.battles, data.wn8, day)).map(_ => data)
    } yield res
  }

  def getAccountCachedWn8(accountId: String): Future[UserWn8WithBattles] = {
    DB.TankersDao.findByAccountId(accountId.toInt).map(_.headOption).flatMap {
      case Some(tanker) => Future(UserWn8WithBattles(accountId.toInt, tanker.wn8, tanker.battles))
      case None => refreshAccountCachedWn8(accountId)
    }
  }

  def calculateWn8(accountId: Int): UserWn8WithBattles = {
    Logger.debug(s"Calculating account wn8 for user: $accountId")
    val tanks: Seq[Map[String, Any]] = accountTanks(accountId)

    if (tanks == null || tanks.isEmpty) return UserWn8WithBattles(0, 0, 0)

    var totalUserBattles: Int = 0

    val totalExpected = tanks.par.flatMap(currentTank => {
      val currentTankId = currentTank("tank_id").toString.toInt
      val tankBattles = currentTank("all").asInstanceOf[Map[String, BigInt]]("battles").toDouble
      totalUserBattles += tankBattles.toInt
      tanksExpectedValues.get(currentTankId) match {
        case Some(expVal) => Some(VehicleAverages(expVal.IDNum, expVal.frag * tankBattles, expVal.dmg * tankBattles, expVal.spot * tankBattles, expVal.defence * tankBattles, 0.01 * expVal.win * tankBattles))
        case _ => None
      }
    }).reduce((a, b) => {
      VehicleAverages(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win)
    })

    val totalAccount = tanks.par.flatMap((currentTank: Map[String, Any]) => {
      val currentTankId = currentTank("tank_id").toString.toInt
      val currentTankStatsMap = currentTank("all").asInstanceOf[Map[String, BigInt]]
      tanksExpectedValues.get(currentTankId) match {
        case Some(_) =>
          val damage: Double = currentTankStatsMap("damage_dealt").toDouble
          val spot: Double = currentTankStatsMap("spotted").toDouble
          val frags: Double = currentTankStatsMap("frags").toDouble
          val defence: Double = currentTankStatsMap("dropped_capture_points").toDouble
          val wins: Double = currentTankStatsMap("wins").toDouble
          Some(VehicleAverages(currentTankId, frags, damage, spot, defence, wins))
        case _ => None
      }

    }).reduce((a, b) => {
      VehicleAverages(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win)
    })

    val wn8 = WN8.calculateWn8Value(totalAccount, totalExpected)
    Logger.debug(s"Calculated wn8 for user: $accountId")

    UserWn8WithBattles(accountId.toInt, wn8, totalUserBattles)

  }

  private def accountTanks(accountId: Int): List[Map[String, Any]] = {
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=tank_id%2Call").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")
    Try(parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]).recover { case _ => List.empty}.get
  }


}
