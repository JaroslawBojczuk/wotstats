package com.domain.presentation.model

import org.joda.time.LocalDate

case class UserHistoryEntry(day: LocalDate, wn8: Double) {
  override def toString: String = {
    s"[new Date(${day.getYear}, ${day.getMonthOfYear - 1}, ${day.getDayOfMonth}), $wn8]" }
}
