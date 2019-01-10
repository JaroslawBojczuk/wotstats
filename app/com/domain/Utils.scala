package com.domain

import com.domain.db.schema.TankerTank

object Utils {

  def tankDiff(actual: Seq[TankerTank], previous: Seq[TankerTank]): Seq[TankerTank] = {
    actual.union(previous).groupBy(_.tankId).mapValues(tanks => tanks.reduce((a, b) => {
      TankerTank(a.accountId, a.tankId,
        Math.abs(a.frags - b.frags),
        Math.abs(a.damageDealt - b.damageDealt),
        Math.abs(a.spotted - b.spotted),
        Math.abs(a.droppedCapturePoints - b.droppedCapturePoints),
        Math.abs(a.battles - b.battles),
        Math.abs(a.wins - b.wins),
        if (a.battles > b.battles) a.battleAvgXp else b.battleAvgXp,
        0, Math.max(a.day, b.day))
    })).values.filter(_.battles > 0).toSeq
  }
}
