package com.domain.user

import com.domain.Constants
import com.domain.Tanks._
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.{Tanker, TankerHistory}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future

object UserWn8 {

  case class UserWn8WithBattles(wn8: Double, battles: Int)

  def refreshAccountCachedWn8(accountId: String): Future[UserWn8WithBattles] = {
    val accountIdAsInt = accountId.toInt
    val day = WGTankerDetails.getDayOfLastBattle(accountIdAsInt)
    val data = calculateWn8(accountId)
    for {
      _ <- DB.TankersDao.addOrUpdate(Tanker(accountIdAsInt, data.battles, data.wn8))
      res <- DB.TankerHistoryDao.addOrReplaceCurrentDay(accountIdAsInt, day, TankerHistory(accountIdAsInt, data.battles, data.wn8, day)).map(_ => data)
    } yield res
  }

  def getAccountCachedWn8(accountId: String): Future[UserWn8WithBattles] = {
    DB.TankersDao.findByAccountId(accountId.toInt).map(_.headOption).flatMap {
      case Some(tanker) => Future(UserWn8WithBattles(tanker.wn8, tanker.battles))
      case None => refreshAccountCachedWn8(accountId)
    }
  }

  private def calculateWn8ForTank(actualValues: Vehicle, expectedValues: Vehicle): Double = {
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

    if (WN8 > 0) BigDecimal(WN8).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble else 0
  }

  private def calculateWn8(accountId: String): UserWn8WithBattles = {

    val tanks = accountTanks(accountId)

    if (tanks == null) return UserWn8WithBattles(0, 0)

    var totalUserBattles: Int = 0

    val totalExpected = tanks.par.flatMap(currentTank => {
      val currentTankId = currentTank("tank_id").toString.toInt
      val tankBattles = currentTank("all").asInstanceOf[Map[String, BigInt]]("battles").toDouble
      totalUserBattles += tankBattles.toInt
      tanksExpectedValues.get(currentTankId) match {
        case Some(expVal) => Some(Vehicle(expVal.IDNum, expVal.frag * tankBattles, expVal.dmg * tankBattles, expVal.spot * tankBattles, expVal.defence * tankBattles, 0.01 * expVal.win * tankBattles))
        case _ => None
      }
    }).reduce((a, b) => {
      Vehicle(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win)
    })

    val totalAccount = tanks.par.flatMap(currentTank => {
      val currentTankId = currentTank("tank_id").toString.toInt
      val currentTankStatsMap = currentTank("all").asInstanceOf[Map[String, BigInt]]
      tanksExpectedValues.get(currentTankId) match {
        case Some(_) =>
          val damage: Double = currentTankStatsMap("damage_dealt").toDouble
          val spot: Double = currentTankStatsMap("spotted").toDouble
          val frags: Double = currentTankStatsMap("frags").toDouble
          val defence: Double = currentTankStatsMap("dropped_capture_points").toDouble
          val wins: Double = currentTankStatsMap("wins").toDouble
          Some(Vehicle(currentTankId, frags, damage, spot, defence, wins))
        case _ => None
      }

    }).reduce((a, b) => {
      Vehicle(0, a.frag + b.frag, a.dmg + b.dmg, a.spot + b.spot, a.defence + b.defence, a.win + b.win)
    })

    UserWn8WithBattles(calculateWn8ForTank(totalAccount, totalExpected), totalUserBattles)

  }

  private def accountTanks(accountId: String): List[Map[String, Any]] = {
    val tanksStatsResponse: String = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/tanks/stats/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId&fields=tank_id%2Call").mkString
    val parsedTanksStats: JValue = render(parse(tanksStatsResponse) \ "data" \ s"$accountId")
    parsedTanksStats.values.asInstanceOf[List[Map[String, Any]]]
  }

}
