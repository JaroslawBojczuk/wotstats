package com.domain.presentation.model

case class TankStats(name: String,
                     imgPath: String,
                     battles: Int,
                     tier: Int,
                     wn8: Double,
                     avgDmg: Double,
                     expDmg: Double,
                     avgFrags: Double,
                     avgSpot: Double,
                     avgWins: Double,
                     avgXp: Double)
