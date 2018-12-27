package com.domain.wn8

import java.io.File
import java.nio.file.{Files, Paths}

import com.domain.Constants.{FILE_WITH_LAST_CLAN_STATS, FOLDER_WITH_CLAN_AVG_WN8}
import com.domain.clans.ClanList
import com.domain.presentation.model.ClanMemberDetails
import io.FileOps

import scala.concurrent.Future
import scala.io.Source

object ClanWn8 {

  private def clanFilePath(clanTag: String): String = FOLDER_WITH_CLAN_AVG_WN8 + clanTag

  def getClanAverageWn8(clanTag: String, membersList: Seq[ClanMemberDetails]): Double = {
    getClanCachedWn8(clanTag) match {
      case Some(average) => average
      case _ => {
        val average = calculateAverageWn8(membersList)
        FileOps.printToFile(new File(clanFilePath(clanTag)))(p => p.print(average))
        average
      }
    }
  }

  def getClanCachedWn8(clanTag: String): Option[Double] = {
    if (Files.exists(Paths.get(clanFilePath(clanTag)))) Some(Source.fromFile(clanFilePath(clanTag)).mkString.toDouble)
    else None
  }

  private def calculateAverageWn8(membersList: Seq[ClanMemberDetails]): Double = {
    val totalBattles = membersList.map(_.battles).sum
    val weightedWn8s = membersList.map(v => (v.wn8 * v.battles) / totalBattles)
    val avg = BigDecimal(weightedWn8s.sum).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    avg
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def saveCurrentClansInFile() = Future {
    val file = new File(FILE_WITH_LAST_CLAN_STATS)
    file.createNewFile()
    FileOps.printToFile(file) {
      p =>
        ClanList.clanSkirmishesStats.foreach(clan => {
          p.println(s"${
            clan.clanId
          },${
            clan.membersCount
          },${
            clan.skirmish.battles6
          },${
            clan.skirmish.wins6
          },${
            clan.skirmish.battles8
          },${
            clan.skirmish.wins8
          },${
            clan.skirmish.battles10
          },${
            clan.skirmish.wins10
          }")
        })
    }
  }

}
