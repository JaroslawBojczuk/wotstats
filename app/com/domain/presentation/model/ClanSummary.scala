package com.domain.presentation.model

case class ClanSummary(clanId: Int, tag: String, emblem: String, membersCount: Int, skirmishBattles: Int, skirmishBattlesWins: Int) {
  def totalWinRatio = BigDecimal((skirmishBattlesWins.toDouble / skirmishBattles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
}