package com.domain.presentation.model

case class ClanSummary(clanId: Int, tag: String, emblem: String, membersCount: Int, skirmishBattles: Int, skirmishBattlesWins: Int, clanDelta: Option[ClanDelta] = None) {
  def totalWinRatio = BigDecimal((skirmishBattlesWins.toDouble / skirmishBattles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
}

case class ClanDelta(membersCount: Int, skirmishBattles: Int, skirmishBattlesWins: Int) {
  def winRatio = {
    if(skirmishBattles > 0) BigDecimal((skirmishBattlesWins.toDouble / skirmishBattles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
    else 0.toDouble
  }
}