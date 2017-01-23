package com.domain.clans

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Date

import com.domain.Constants
import com.domain.presentation.model.{ClanDetails, ClanMemberDetails, StrongholdBattle}
import com.domain.wn8.UserWn8
import com.fasterxml.jackson.databind.JsonNode
import org.joda.time.LocalTime
import play.libs.Json
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable
import scala.io.{Codec, Source}

object ClanUtils {

  private def clanDetailsUrl(clanId: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  private def clanShBattlesUrl(clanId: String) = s"https://api.worldoftanks.eu/wot/stronghold/plannedbattles/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  //val FILE_WITH_LAST_CLAN_STATS = new File(s"E:\\Project\\last.txt")
  val FILE_WITH_LAST_CLAN_STATS = new File(s"C:\\Projects\\last.txt")
  val FOLDER_WITH_CLAN_AVG_WN8 = "C:\\Projects\\clans"

  private def clanFilePath(clanTag: String): String = FOLDER_WITH_CLAN_AVG_WN8 + "\\" + clanTag

  def getClanDetails(clanId: String): ClanDetails = {
    val clanResponse = scala.io.Source.fromURL(clanDetailsUrl(clanId))(Codec.UTF8).mkString
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

    val avg: Double = getClanAverageWn8(clanTag, membersList)

    ClanDetails(clanTag, clanName, avg, membersList, getClanStrongholdPlannedBattles(clanId))
  }

  private def getClanAverageWn8(clanTag: String, membersList: Seq[ClanMemberDetails]): Double = {
    getClanCachedWn8(clanTag) match {
      case Some(average) => average
      case _ => {
        val average = calculateAverageWn8(membersList)
        printToFile(new File(clanFilePath(clanTag)))(p => p.print(average))
        average
      }
    }
  }

  def getClanCachedWn8(clanTag: String): Option[Double] = {
    if (Files.exists(Paths.get(clanFilePath(clanTag)))) Some(Source.fromFile(clanFilePath(clanTag)).mkString.toDouble)
    else None
  }

  private def calculateAverageWn8(membersList: Seq[ClanMemberDetails]): Double = {
    val wn8sAndBattles = membersList.par.map(member => UserWn8.accountWn8(member.accountId.toString))
    val totalBattles = wn8sAndBattles.map(_.battles).sum
    val weightedWn8s = wn8sAndBattles.map(v => (v.wn8 * v.battles) / totalBattles)
    val avg = BigDecimal(weightedWn8s.sum).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    avg
  }

  def getClanStrongholdPlannedBattles(clanId: String): Seq[StrongholdBattle] = {
    val clanResponse = scala.io.Source.fromURL(clanShBattlesUrl(clanId)).mkString
    val parsedBattles: JValue = render(parse(clanResponse) \ "data" \ s"$clanId")

    if (parsedBattles.toSome.isDefined) {
      parsedBattles.values.asInstanceOf[List[Map[String, Any]]].map(battle => {
        StrongholdBattle(
          battle.get("attacker_clan_id").get.toString,
          battle.get("attacker_clan_tag").get.toString,
          battle.get("defender_clan_id").get.toString,
          battle.get("defender_clan_tag").get.toString,
          battle.get("battle_planned_date").get.toString.toLong)
      }).sortBy(_.date)
    } else Seq.empty
  }

  def main(args: Array[String]): Unit = {

    val det = getClanDetails("500000013")

    println(det)


    val date: Long = getClanStrongholdPlannedBattles("500034335").head.date

    val vs: String = getClanStrongholdPlannedBattles("500023625").head.defenderClanTag

    val joda = LocalTime.fromDateFields(new Date(date * 1000))

    println(vs + ": " + joda.toString("HH:mm"))
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
    printToFile(FILE_WITH_LAST_CLAN_STATS) {
      p =>
        current.foreach(clan => {
          p.println(s"${
            clan.clanId
          },${
            clan.membersCount
          },${
            clan.skirmishBattles
          },${
            clan.skirmishBattlesWins
          }")
        })
    }
  }

}
