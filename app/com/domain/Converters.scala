package com.domain

import com.domain.Tanks.VehicleAverages
import com.domain.db.schema.TankerTank

object Converters {

  def toVehicleAveragesFromTank: TankerTank => VehicleAverages = tank => {
    val battles: Double = tank.battles
    val avg_damage: Double = tank.damageDealt / battles
    val avg_spot: Double = tank.spotted / battles
    val avg_frags: Double = tank.frags / battles
    val avg_defence: Double = tank.droppedCapturePoints / battles
    val avg_wins: Double = BigDecimal((tank.wins / battles) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
    VehicleAverages(tank.tankId, avg_frags, avg_damage, avg_spot, avg_defence, avg_wins)
  }

  def toVehicleFromTank: TankerTank => VehicleAverages = tank =>
    VehicleAverages(tank.tankId, tank.frags, tank.damageDealt, tank.spotted, tank.droppedCapturePoints, tank.wins)

  val toVehicleFromMap: Map[String, Any] => VehicleAverages = {
    elem => {
      val tankStatsMap = elem("all").asInstanceOf[Map[String, BigInt]]
      val battles: Double = tankStatsMap("battles").toDouble
      val avg_damage: Double = tankStatsMap("damage_dealt").toDouble / battles
      val avg_spot: Double = tankStatsMap("spotted").toDouble / battles
      val avg_frags: Double = tankStatsMap("frags").toDouble / battles
      val avg_defence: Double = tankStatsMap("dropped_capture_points").toDouble / battles
      val avg_wins: Double = BigDecimal((tankStatsMap("wins").toDouble / battles) * 100).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble
      VehicleAverages(elem("tank_id").toString.toInt, avg_frags, avg_damage, avg_spot, avg_defence, avg_wins)
    }
  }

}
