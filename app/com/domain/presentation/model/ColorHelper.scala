package com.domain.presentation.model

object ColorHelper {

  def skirmishWinRatio(winRatio: Double) = winRatio match {
    case x if 0 <= x && x < 30 => "very_bad"
    case x if 30 <= x && x < 35 => "bad"
    case x if 35 <= x && x < 40 => "below_average"
    case x if 40 <= x && x < 45 => "average"
    case x if 45 <= x && x < 55 => "above_average"
    case x if 55 <= x && x < 65 => "good"
    case x if 65 <= x && x < 75 => "very_good"
    case x if 75 <= x && x < 85 => "great"
    case x if 85 <= x && x < 89 => "unicum"
    case _ => "super_unicum"
  }

  def wn8Color(wn8: Double): String = wn8 match {
    case x if 0 < x && x < 300 => "very_bad"
    case x if 300 <= x && x < 450 => "bad"
    case x if 450 <= x && x < 650 => "below_average"
    case x if 650 <= x && x < 900 => "average"
    case x if 900 <= x && x < 1200 => "above_average"
    case x if 1200 <= x && x < 1600 => "good"
    case x if 1600 <= x && x < 2000 => "very_good"
    case x if 2000 <= x && x < 2450 => "great"
    case x if 2450 <= x && x < 2900 => "unicum"
    case x if 0 == x  => ""
    case _ => "super_unicum"
  }

  def wn8Color(wn8: String): String = wn8Color(wn8.toDouble)

}
