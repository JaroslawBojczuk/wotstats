package com.domain.clans

import java.io.File
import java.nio.file.{Files, Paths}
import java.util
import java.util.Date

import com.domain.Constants
import com.domain.presentation.model.{ClanDetails, ClanMemberDetails, StrongholdBattle}
import com.domain.wn8.UserWn8
import com.domain.wn8.UserWn8.UserWn8WithBattles
import com.fasterxml.jackson.databind.JsonNode
import io.FileOps
import org.joda.time.LocalTime
import org.json4s._
import org.json4s.jackson.JsonMethods._
import play.libs.Json

import scala.collection.{immutable, mutable}
import scala.collection.parallel.ParSeq
import scala.io.{Codec, Source}

object ClanUtils {

  private def clanTag(clanId: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId&fields=clan_tag"

  private def clanDetailsUrl(clanId: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  private def clanShBattlesUrl(clanId: String) = s"https://api.worldoftanks.eu/wot/stronghold/plannedbattles/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  //val FILE_WITH_LAST_CLAN_STATS = new File(s"E:\\Project\\last.txt")
  val FILE_WITH_LAST_CLAN_STATS = new File(s"C:\\Projects\\last.txt")
  val FOLDER_WITH_CLAN_AVG_WN8 = "C:\\Projects\\clans"

  private def clanFilePath(clanTag: String): String = FOLDER_WITH_CLAN_AVG_WN8 + "\\" + clanTag

  def getClanTag(clanId: String): String = {
    val clanResponse = scala.io.Source.fromURL(clanDetailsUrl(clanId))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)
    val data: JsonNode = clanJson.findPath("data").findPath(clanId)
    data.findPath("tag").asText()
  }

  def getClanDetails(clanId: String): ClanDetails = {
    val clanResponse = scala.io.Source.fromURL(clanDetailsUrl(clanId))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)

    val data: JsonNode = clanJson.findPath("data").findPath(clanId)
    val clanName = data.findPath("name").asText()
    val clanTag = data.findPath("tag").asText()

    val membersWithWn8 = calculateWn8ForClanMembers(data)
    val avg = getClanAverageWn8(clanTag, membersWithWn8)

    ClanDetails(clanTag, clanName, avg, membersWithWn8.sortBy(-_.wn8), getClanStrongholdPlannedBattles(clanId))
  }

  private def calculateWn8ForClanMembers(data: JsonNode): Seq[ClanMemberDetails] = {
    val membersWithWn8 = extractMembers(data).par.map(member => {
      val wn8AndBattles: UserWn8WithBattles = UserWn8.getAccountCachedWn8(member.accountId.toString)
      ClanMemberDetails(member.name, member.accountId, member.role, wn8AndBattles.wn8, wn8AndBattles.battles)
    }).toList.toSeq
    membersWithWn8
  }

  private def extractMembers(data: JsonNode): Seq[ClanMemberDetails] = {
    val membersList: mutable.Buffer[ClanMemberDetails] = Seq.empty.toBuffer
    val members = data.findValue("members").elements()
    while (members.hasNext) {
      val member: JsonNode = members.next()
      membersList += ClanMemberDetails(member.findPath("account_name").asText(), member.findPath("account_id").asInt, member.findPath("role_i18n").asText(), 0, 0)
    }
    membersList.toSeq
  }

  private def getClanAverageWn8(clanTag: String, membersList: Seq[ClanMemberDetails]): Double = {
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

    println(getClanTag("500000013"))

  }


  def saveCurrentClansInFile = {
    val current = ClanList.topClansCurrentStats
    FILE_WITH_LAST_CLAN_STATS.createNewFile()
    FileOps.printToFile(FILE_WITH_LAST_CLAN_STATS) {
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
