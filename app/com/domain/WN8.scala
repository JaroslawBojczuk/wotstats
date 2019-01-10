package com.domain

import com.domain.Converters._
import com.domain.Tanks.{VehicleAverages, tanksExpectedValues}
import com.domain.Utils._
import com.domain.db.schema.TankerTank

object WN8 {

  def calculateWn8Value(actualValues: VehicleAverages, expectedValues: VehicleAverages): Double = {
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

  def calculateWn8PerTank(actual: Seq[TankerTank], previous: Seq[TankerTank]): Seq[TankerTank] = {
    tankDiff(actual, previous).map(currentTank => {
      val expected = tanksExpectedValues(currentTank.tankId)
      currentTank.copy(wn8 = calculateWn8Value(toVehicleAveragesFromTank(currentTank), expected))
    })
  }

  def calculateTotalWn8AndBattles(tanks: Seq[TankerTank]): (Double, Int) = {
    val diffs: Seq[(VehicleAverages, VehicleAverages, Int)] = tanks.map(currentTank => {
      val tankTotal = toVehicleFromTank(currentTank)
      val expected = tanksExpectedValues(currentTank.tankId).multiplyBy(currentTank.battles)
      val battles = currentTank.battles
      (tankTotal, expected, battles)
    })
    if (diffs.isEmpty) return (0, 0)
    val (total, expected, battles) = diffs.reduce((a, b) => (a._1 + b._1, a._2 + b._2, a._3 + b._3))
    (calculateWn8Value(total, expected), battles)
  }

  def calculateTotalWn8AndBattles(actual: Seq[TankerTank], previous: Seq[TankerTank]): (Double, Int) = {
    calculateTotalWn8AndBattles(tankDiff(actual, previous))
  }
}
