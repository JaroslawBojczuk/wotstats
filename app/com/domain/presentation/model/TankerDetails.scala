package com.domain.presentation.model

case class TankerDetails(name: String, battles: Int, wins: Int, battlesSkirmish: Int, wn8: Int) {
  def winratio = BigDecimal((wins.toDouble / battles.toDouble) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
}
