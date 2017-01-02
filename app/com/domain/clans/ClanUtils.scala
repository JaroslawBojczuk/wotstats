package com.domain.clans

import java.io.File

import com.domain.Constants
import com.domain.presentation.model.{ClanDetails, ClanMemberDetails}
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json

import scala.collection.mutable

/**
 * Created by Jaro on 2016-12-31.
 */
object ClanUtils {

  private def clanDetailsUrl(clanId: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

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

    ClanDetails(clanTag, clanName, membersList)
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
    val file = new File(s"C:\\Projects\\last.txt")
    //val file = new File(s"E:\\Project\\${System.currentTimeMillis()}.txt")
    file.createNewFile()
    printToFile(file) { p =>
      current.foreach(clan => {
        p.println(s"${clan.clanId},${clan.membersCount},${clan.skirmishBattles},${clan.skirmishBattlesWins}")
      })
    }
  }

}
