package com.domain.presentation.model

case class TankerDetails(name: String, clanId: String, battles: Int, wins: Int, avgTier: Double, avgSpot: Double, avgFrags: Double, wn8: Double, tanks: Seq[TankStats]) {
  def winratio = if (battles > 0) BigDecimal((wins.toDouble / battles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble else 0.0

}
