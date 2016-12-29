package com.domain.wn8

import com.domain.Constans
import com.domain.presentation.model.TankerDetails
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json

object WGTankerDetails {

  private def url(accountId: String) = s"https://api.worldoftanks.eu/wot/account/info/?application_id=${Constans.APPLICATION_ID}&account_id=$accountId"

  def getDetails(accountId: String): TankerDetails = {
    val userResponse = scala.io.Source.fromURL(url(accountId)).mkString
    val userJson = Json.parse(userResponse)
    val data: JsonNode = userJson.findPath("data")
    val name = data.findPath(accountId).findPath("nickname").asText()

    val statisticsAll: JsonNode = data.findPath("statistics").findPath("all")
    val battles = statisticsAll.findPath("battles").asInt()
    val wins = statisticsAll.findPath("wins").asInt()

    val statisticsSkirmish: JsonNode = data.findPath("statistics").findPath("stronghold_skirmish")
    val battlesSkirmish = statisticsSkirmish.findPath("battles").asInt()

    TankerDetails(name, battles, wins, battlesSkirmish, 44)
  }

  def main(args: Array[String]): Unit = {
    getDetails(url("500557563"))
  }

}
