package com.domain.clans

import com.domain.Constants
import com.domain.presentation.model.{ClanDelta, ClanSummary}
import play.libs.Json

import scala.collection.JavaConverters._
import scala.concurrent.Future

object ClanList {

  private def url = s"https://api.worldoftanks.eu/wot/clanratings/top/?application_id=${Constants.APPLICATION_ID}&rank_field=efficiency&fields=clan_id&limit=100"
  private def urlClanSkirmish(clanIds: String) = s"https://api.worldoftanks.eu/wot/stronghold/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanIds"
  private def urlClanDetails(clanIds: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&fields=clan_id%2Cmembers_count%2Cemblems.x24&clan_id=$clanIds"

  def topClansCurrentStats: Seq[ClanSummary] = {

    val clansResponse = scala.io.Source.fromURL(url).mkString
    val clansJson = Json.parse(clansResponse)
    val clansIds = clansJson.findPath("data").findValues("clan_id").asScala.mkString(",")

    val clanDetailsResponse = scala.io.Source.fromURL(urlClanDetails(clansIds)).mkString
    val clanDetailsJson = Json.parse(clanDetailsResponse)
    val clanMembersData: Map[Int, Int] = clanDetailsJson.findPath("data").asScala.map(clan => (clan.findPath("clan_id").asInt(), clan.findPath("members_count").asInt())).toMap
    val clanEmblemsData: Map[Int, String] = clanDetailsJson.findPath("data").asScala.map(clan => {
      (clan.findPath("clan_id").asInt(), clan.findPath("emblems").findPath("x24").findPath("portal").asText())
    }).toMap

    val clanSkirmishResponse = scala.io.Source.fromURL(urlClanSkirmish(clansIds)).mkString
    val clanSkirmishJson = Json.parse(clanSkirmishResponse)

    val data = clanSkirmishJson.findPath("data")
    data.elements().asScala.map(clan => {
      val clanId = clan.findPath("clan_id").asInt()

      val tag = clan.findPath("clan_tag").asText()
      val skirmish = clan.findPath("skirmish")

      val battles = skirmish.findPath("battles_count").asInt()
      val wins = skirmish.findPath("battles_wins").asInt()

      ClanSummary(clanId, tag, clanEmblemsData.getOrElse(clanId, ""), clanMembersData.getOrElse(clanId, 0), battles, wins)
    }).toSeq

  }

  import scala.concurrent.ExecutionContext.Implicits.global
  def topClansCurrentStatsFuture = Future { topClansCurrentStats }

  def previousStats: Seq[ClanSummary] = {

    val file = scala.io.Source.fromFile(ClanUtils.FILE_WITH_LAST_CLAN_STATS)
    val clanStats = file.getLines

    val result = clanStats.map(line => {
      val values = line.split(",")
      ClanSummary(values(0).toInt, "", "", values(1).toInt, values(2).toInt, values(3).toInt)
    }).toSeq

    result.size

    file.close()

    result
  }

  def previousStats2: Seq[ClanSummary] = {

    val filename = "E:\\Project\\29122016.txt"
    val clanStats = scala.io.Source.fromFile(filename).getLines

    clanStats.map(line => {
      val values = line.split(",")
      ClanSummary(values(0).toInt, "", "", values(1).toInt, values(2).toInt, values(3).toInt)
    }).toSeq
  }

  def main(args: Array[String]): Unit = {

    val previous = previousStats
    val current = previousStats2

    current.map(cur => {
      val prevOpt = previous.find(_.clanId == cur.clanId)
      val clanDelta = prevOpt match {
        case Some(prev) => Some(ClanDelta(cur.membersCount - prev.membersCount, cur.skirmishBattles - prev.skirmishBattles, cur.skirmishBattlesWins - prev.skirmishBattlesWins))
        case _ => None
      }
      ClanSummary(cur.clanId, cur.tag, cur.emblem, cur.membersCount, cur.skirmishBattles, cur.skirmishBattles, clanDelta)
    })


  }

}
