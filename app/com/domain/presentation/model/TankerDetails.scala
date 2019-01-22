package com.domain.presentation.model

import org.joda.time

case class TankerDetails(name: String,
                         accountId: Int,
                         clanId: String,
                         battles: Int,
                         wins: Int,
                         avgTier: Double,
                         avgSpots: Double,
                         avgFrags: Double,
                         avgDamage: Double,
                         wn8: Double,
                         lastBattle: time.LocalDate,
                         tanks: Seq[TankStats],
                         lastDaySession: TankerSession,
                         lastWeekSession: TankerSession,
                         lastMonthSession: TankerSession,
                         history: Seq[UserHistoryEntry]) {
  def winratio = if (battles > 0) BigDecimal((wins.toDouble / battles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble else 0.0

}

case class TankerSession(battles: Int, wn8: Double, avgSpots: Double, avgFrags: Double, avgDamage: Double, avgWins: Double, tanks: Seq[TankStats])

object TankerSession {
  def empty = TankerSession(0, 0, 0, 0, 0, 0, Seq.empty[TankStats])
}
