package com.domain.user

import com.domain.Constants
import com.domain.presentation.model.TankerDetails
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json

import scala.concurrent.Await
import scala.concurrent.duration._

object WGTankerDetails {

  private def url(accountId: String) = s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId"

  private def findAccountId(name: String) = {
    val userResponse = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/account/list/?application_id=${Constants.APPLICATION_ID}&search=$name&type=exact&fields=account_id").mkString
    val userJson = Json.parse(userResponse)
    val count = userJson.findPath("meta").findPath("count").asInt()

    if (count == 1) Some(userJson.findPath("data").findPath("account_id").toString)
    else None
  }

  def getDetails(accountId: Int): Some[TankerDetails] = {
    val userResponse = scala.io.Source.fromURL(url(accountId.toString)).mkString
    val userJson = Json.parse(userResponse)
    val data: JsonNode = userJson.findPath("data")
    val name = data.findPath(accountId.toString).findPath("nickname").asText()
    val clanId = data.findPath(accountId.toString).findPath("clan_id").asText()

    val statisticsAll: JsonNode = data.findPath("statistics").findPath("all")
    val battles = statisticsAll.findPath("battles").asInt()
    val wins = statisticsAll.findPath("wins").asInt()
    val spotted = statisticsAll.findPath("spotted").asInt()
    val frags = statisticsAll.findPath("frags").asInt()

    val tanks = Await.result(Wn8Veh.getTankerLatestTanks(accountId), 1.minute).sortBy(-_.wn8)

    val avgTier = tanks.map(t => t.tier * t.battles).sum.toDouble / tanks.map(t => if(t.tier > 0) t.battles else 0).sum.toDouble
    val avgSpot = spotted.toDouble / battles.toDouble
    val avgFrags = frags.toDouble / battles.toDouble

    Some(TankerDetails(name, accountId, clanId, battles, wins, avgTier, avgSpot, avgFrags, UserWn8.getAccountCachedWn8(accountId.toString).wn8, tanks))
  }

  def getDetails(accountName: String): Option[TankerDetails] = {
    findAccountId(accountName) match {
      case Some(accountId) => getDetails(accountId.toInt)
      case _ => None
    }
  }

}
