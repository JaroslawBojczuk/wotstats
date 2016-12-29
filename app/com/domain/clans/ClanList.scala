package com.domain.clans

import java.io.File

import com.domain.Constans
import com.domain.presentation.model.ClanSummary
import play.libs.Json

import scala.collection.JavaConverters._

object ClanList {

  private def url = s"https://api.worldoftanks.eu/wot/clanratings/top/?application_id=${Constans.APPLICATION_ID}&rank_field=efficiency&fields=clan_id&limit=100"

  private def urlClanSkirmish(clanIds: String) = s"https://api.worldoftanks.eu/wot/stronghold/info/?application_id=${Constans.APPLICATION_ID}&clan_id=$clanIds"
  private def urlClanDetails(clanIds: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constans.APPLICATION_ID}&fields=clan_id%2Cmembers_count%2Cemblems.x24&clan_id=$clanIds"

  def topClans: Seq[ClanSummary] = {

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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def main(args: Array[String]): Unit = {

    printToFile(new File("E:\\Project\\29122016v2.txt")) { p =>
      topClans.sortBy(_.clanId).foreach(clan => p.println(s"${clan.clanId},${clan.membersCount},${clan.skirmishBattles},${clan.skirmishBattlesWins}"))
    }

  }

}
