package com.domain.user

import com.domain.Constants
import com.domain.presentation.model.TankerDetails
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json

object WGTankerDetails {

  private def url(accountId: String) = s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constants.APPLICATION_ID}&account_id=$accountId"

  private def findAccountId(name: String) = {
    val userResponse = scala.io.Source.fromURL(s"https://api.worldoftanks.eu/wot/account/list/?application_id=${Constants.APPLICATION_ID}&search=$name&type=exact&fields=account_id").mkString
    val userJson = Json.parse(userResponse)
    val count = userJson.findPath("meta").findPath("count").asInt()

    if (count == 1) Some(userJson.findPath("data").findPath("account_id").toString)
    else None
  }

  def getDetails(accountName: String) = {
    findAccountId(accountName) match {
      case Some(accountId) =>
        val userResponse = scala.io.Source.fromURL(url(accountId)).mkString
        val userJson = Json.parse(userResponse)
        val data: JsonNode = userJson.findPath("data")
        val name = data.findPath(accountId).findPath("nickname").asText()

        val statisticsAll: JsonNode = data.findPath("statistics").findPath("all")
        val battles = statisticsAll.findPath("battles").asInt()
        val wins = statisticsAll.findPath("wins").asInt()

        val statisticsSkirmish: JsonNode = data.findPath("statistics").findPath("stronghold_skirmish")
        val battlesSkirmish = statisticsSkirmish.findPath("battles").asInt()

        Some(TankerDetails(name, battles, wins, battlesSkirmish, com.domain.wn8.UserWn8.accountWn8(accountId).wn8))
      case _ => None
    }
  }

  def main(args: Array[String]) {
    println(getDetails("12"))
  }

}
