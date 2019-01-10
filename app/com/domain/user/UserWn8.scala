package com.domain.user

import com.domain.WN8
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.{Tanker, TankerHistory}

import scala.concurrent.Future

object UserWn8 {

  case class UserWn8WithBattles(accountId: Int, wn8: Double, battles: Int)

  def refreshAccountCachedWn8(accountId: String): Future[UserWn8WithBattles] = {
    val accountIdAsInt = accountId.toInt
    val day = WGTankerDetails.getDayOfLastBattle(accountIdAsInt)
    for {
      tanks <- UserTanksWn8.refreshTankerTanks(accountIdAsInt)
      (wn8, battles) = WN8.calculateTotalWn8AndBattles(tanks)
      data <- Future(UserWn8WithBattles(accountIdAsInt, wn8, battles))
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

}
