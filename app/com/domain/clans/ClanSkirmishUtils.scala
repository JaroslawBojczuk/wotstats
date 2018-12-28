package com.domain.clans

import java.io.File

import com.domain.Constants._
import com.domain.presentation.model.{ClanSkirmish, ClanSummary}
import play.api.Logger
import play.libs.Json

import scala.collection.JavaConverters._

object ClanSkirmishUtils {

  private def url = s"https://api.worldoftanks.eu/wot/clanratings/top/?application_id=$APPLICATION_ID&rank_field=global_rating_weighted_avg&fields=clan_id&limit=$CLAN_LIMIT"

  private def urlClanSkirmish(clanIds: String) = s"https://api.worldoftanks.eu/wot/stronghold/claninfo/?application_id=$APPLICATION_ID&clan_id=$clanIds"

  private def urlClanDetails(clanIds: String) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=$APPLICATION_ID&fields=clan_id%2Cmembers_count%2Cemblems.x24&clan_id=$clanIds"

  def getCurrentClanSkirmishes: Seq[ClanSummary] = {

    Logger.debug("Getting clan skirmishes stats")

    val clansResponse = scala.io.Source.fromURL(url).mkString
    val clansJson = Json.parse(clansResponse)
    val clansIds = clansJson.findPath("data").findValues("clan_id").asScala.mkString(",")

    val clanDetailsResponse = scala.io.Source.fromURL(urlClanDetails(clansIds)).mkString
    val clanDetailsJson = Json.parse(clanDetailsResponse)
    val clanMembersData: Map[Int, Int] = clanDetailsJson.findPath("data").asScala.map(clan => (clan.findPath("clan_id").asInt(), clan.findPath("members_count").asInt())).toMap
    val clanEmblemsData: Map[Int, String] = clanDetailsJson.findPath("data").asScala.map(clan => {
      (clan.findPath("clan_id").asInt(), clan.findPath("emblems").findPath("x24").findPath("portal").asText())
    }).toMap

    clansIds.split(",").grouped(10).flatMap(clansIdsSplitted => {
      val clanSkirmishResponse = scala.io.Source.fromURL(urlClanSkirmish(clansIdsSplitted.mkString(","))).mkString
      val clanSkirmishJson = Json.parse(clanSkirmishResponse)

      val data = clanSkirmishJson.findPath("data")
      data.elements().asScala.map(clan => {
        val clanId = clan.findPath("clan_id").asInt()

        val tag = clan.findPath("clan_tag").asText()
        val skirmish = clan.findPath("skirmish_statistics")

        val battles6 = skirmish.findPath("total_6").asInt()
        val wins6 = skirmish.findPath("win_6").asInt()

        val battles8 = skirmish.findPath("total_8").asInt()
        val wins8 = skirmish.findPath("win_8").asInt()

        val battles10 = skirmish.findPath("total_10").asInt()
        val wins10 = skirmish.findPath("win_10").asInt()

        val clanSkirmish = ClanSkirmish(battles6, wins6, battles8, wins8, battles10, wins10)

        ClanSummary(clanId, tag, clanEmblemsData.getOrElse(clanId, ""), clanMembersData.getOrElse(clanId, 0), clanSkirmish)
      }).toSeq
    }).toSeq
  }

  def previousStats: Seq[ClanSummary] = {

    Logger.debug("Getting data from clans skirmishes file")

    val file = scala.io.Source.fromFile(new File(FILE_WITH_LAST_CLAN_STATS))
    val clanStats = file.getLines

    val result = clanStats.map(line => {
      val values = line.split(",")
      val skirmish = ClanSkirmish(values(2).toInt, values(3).toInt, values(4).toInt, values(5).toInt, values(6).toInt, values(7).toInt)
      ClanSummary(values(0).toInt, "", "", values(1).toInt, skirmish)
    }).toSeq

    result.size

    file.close()

    result
  }

}
