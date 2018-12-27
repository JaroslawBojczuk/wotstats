package com.domain.clans

import com.domain.Constants
import com.domain.presentation.model.{ClanDetails, StrongholdBattle}
import com.domain.wn8.ClanWn8
import com.fasterxml.jackson.databind.JsonNode
import org.json4s._
import org.json4s.jackson.JsonMethods._
import play.api.Logger
import play.libs.Json

import scala.io.Codec

object ClanUtils {

  private def clanShBattlesUrl(clanId: String) = s"https://api.worldoftanks.eu/wot/stronghold/plannedbattles/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  def getClanTag(clanId: String): String = {
    val clanResponse = scala.io.Source.fromURL(ClanWn8.clanDetailsUrl(clanId.toInt))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)
    val data: JsonNode = clanJson.findPath("data").findPath(clanId)
    data.findPath("tag").asText()
  }

  def getClanDetails(clanId: String): ClanDetails = {

    Logger.debug(s"Gathering details for clan id: $clanId")

    val clanResponse = scala.io.Source.fromURL(ClanWn8.clanDetailsUrl(clanId.toInt))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)

    val data: JsonNode = clanJson.findPath("data").findPath(clanId)
    val clanName = data.findPath("name").asText()
    val clanTag = data.findPath("tag").asText()

    val membersWithWn8 = ClanWn8.calculateWn8ForClanMembers(data)
    val avg = ClanWn8.getClanCachedWn8(clanId.toInt)

    ClanDetails(clanTag, clanName, avg, membersWithWn8.sortBy(-_.wn8), getClanStrongholdPlannedBattles(clanId))
  }


  def getClanStrongholdPlannedBattles(clanId: String): Seq[StrongholdBattle] = {
    val clanResponse = scala.io.Source.fromURL(clanShBattlesUrl(clanId))(Codec.UTF8).mkString
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

}
