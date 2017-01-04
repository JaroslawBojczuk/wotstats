package com.domain.clans

import java.io.File

import com.domain.Constants
import com.domain.presentation.model.{ClanDetails, ClanMemberDetails}
import com.domain.wn8.UserWn8
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json

import scala.collection.mutable

object ClanUtils {

  private def clanDetailsUrl(clanId: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"
  private def clanShBattlesUrl(clanId: String) = s"https://api.worldoftanks.eu/wot/stronghold/plannedbattles/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"
  val FILE_WITH_LAST_CLAN_STATS = new File(s"E:\\Project\\last.txt")

  def getClanDetails(clanId: String): ClanDetails = {
    val clanResponse = scala.io.Source.fromURL(clanDetailsUrl(clanId)).mkString
    val clanJson = Json.parse(clanResponse)

    val data: JsonNode = clanJson.findPath("data").findPath(clanId)

    val clanName = data.findPath("name").asText()
    val clanTag = data.findPath("tag").asText()
    val members = data.findValue("members").elements()

    val membersList: mutable.Buffer[ClanMemberDetails] = Seq.empty.toBuffer

    while (members.hasNext) {
      val member: JsonNode = members.next()
      membersList += ClanMemberDetails(member.findPath("account_name").asText(), member.findPath("account_id").asInt)
    }

    //val wn8sAndBattles = membersList.par.map(member => UserWn8.accountWn8(member.accountId.toString))
    //val totalBattles = wn8sAndBattles.map(_.battles).sum
    //val weightedWn8s = wn8sAndBattles.map(v => (v.wn8 * v.battles) / totalBattles)
    //val avg = BigDecimal(weightedWn8s.sum).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    val avg = 0

    ClanDetails(clanTag, clanName, avg, membersList)
  }

  def getClanStrongholdPlannedBattles(clanId: String) = {
    val clanResponse = scala.io.Source.fromURL(clanShBattlesUrl(clanId)).mkString
    val clanJson = Json.parse(clanResponse)
    clanJson.asText()
  }

  def main(args: Array[String]): Unit = {
    getClanStrongholdPlannedBattles("500136070")
  }

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  def saveCurrentClansInFile = {
    val current = ClanList.topClansCurrentStats
    FILE_WITH_LAST_CLAN_STATS.createNewFile()
    printToFile(FILE_WITH_LAST_CLAN_STATS) { p =>
      current.foreach(clan => {
        p.println(s"${clan.clanId},${clan.membersCount},${clan.skirmishBattles},${clan.skirmishBattlesWins}")
      })
    }
  }

}
