package com.domain.clans

import java.io.File

/**
  * Created by Jaro on 2016-12-31.
  */
object ClanUtils {

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def saveCurrentClansInFile = {
    val current = ClanList.topClansCurrentStats
    val file = new File(s"E:\\Project\\last.txt")
    //val file = new File(s"E:\\Project\\${System.currentTimeMillis()}.txt")
    file.createNewFile()
    printToFile(file) { p =>
      current.foreach(clan => {
        p.println(s"${clan.clanId},${clan.membersCount},${clan.skirmishBattles},${clan.skirmishBattlesWins}")
      })
    }
  }

}
