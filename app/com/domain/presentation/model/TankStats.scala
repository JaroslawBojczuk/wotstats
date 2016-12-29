package com.domain.presentation.model

case class TankStats(name: String, battles: Int, wn8: Int) {
  def color = wn8 match {
    case x if 0 <= x && x < 300 => "very_bad"
    case x if 300 <= x && x < 450 => "bad"
    case x if 450 <= x && x < 650 => "below_average"
    case x if 650 <= x && x < 900 => "average"
    case x if 900 <= x && x < 1200 => "above_average"
    case x if 1200 <= x && x < 1600 => "good"
    case x if 1600 <= x && x < 2000 => "very_good"
    case x if 2000 <= x && x < 2450 => "great"
    case x if 2450 <= x && x < 2900 => "unicum"
    case _ => "super_unicum"
  }
}
