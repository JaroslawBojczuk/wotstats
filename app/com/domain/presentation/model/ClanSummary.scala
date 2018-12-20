package com.domain.presentation.model

case class ClanSummary(clanId: Int,
                       tag: String,
                       emblem: String,
                       membersCount: Int,
                       skirmish: ClanSkirmish,
                       clanDelta: Option[ClanDelta] = None) {
  def totalWinRatio: Double =
    BigDecimal((skirmish.totalWins.toDouble / (if(skirmish.totalBattles == 0) 1 else skirmish.totalBattles).toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
}

case class ClanSkirmish(battles6: Int, wins6: Int, battles8: Int, wins8: Int, battles10: Int, wins10: Int) {
  def totalBattles: Int = battles6 + battles8 + battles10
  def totalWins: Int = wins6 + wins8 + wins10
}

case class ClanDelta(membersCount: Int, private val previousSkirmish: ClanSkirmish, private val currentSkirmish: ClanSkirmish) {
  def winRatio: Double = {
    if(totalBattles > 0) BigDecimal((totalWins.toDouble / totalBattles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
    else 0.toDouble
  }

  def totalBattles: Int = currentSkirmish.totalBattles - previousSkirmish.totalBattles
  def totalWins: Int = currentSkirmish.totalWins - previousSkirmish.totalWins
  def battles6: Int = currentSkirmish.battles6 - previousSkirmish.battles6
  def wins6: Int = currentSkirmish.wins6 - previousSkirmish.wins6
  def battles8: Int = currentSkirmish.battles8 - previousSkirmish.battles8
  def wins8: Int = currentSkirmish.wins8 - previousSkirmish.wins8
  def battles10: Int = currentSkirmish.battles10 - previousSkirmish.battles10
  def wins10: Int = currentSkirmish.wins10 - previousSkirmish.wins10

}