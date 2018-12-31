package com.domain.clans

import java.io.File

import com.domain.Constants
import com.domain.Constants.FILE_WITH_LAST_CLAN_STATS
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.Clan
import com.domain.presentation.model.ClanMemberDetails
import com.domain.user.UserWn8
import com.domain.user.UserWn8.UserWn8WithBattles
import com.fasterxml.jackson.databind.JsonNode
import io.FileOps
import play.libs.Json

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Codec

object ClanWn8 {

  def clanDetailsUrl(clanId: Int) = s"https://api.worldoftanks.eu/wgn/clans/info/?application_id=${Constants.APPLICATION_ID}&clan_id=$clanId"

  def refreshClanCachedWn8(clanId: Int): Future[Double] = {
    val average = calculateAverageWn8(ClanWn8.getClanMembersDetails(clanId))
    DB.ClansDao.addOrUpdate(Clan(clanId.toInt, average)).map(_ => average)
  }

  def getClanCachedWn8(clanId: Int): Double = {
    val result = DB.ClansDao.findByClanId(clanId).map(_.headOption).flatMap {
      case Some(clan) => Future(clan.wn8)
      case None => refreshClanCachedWn8(clanId)
    }
    Await.result(result, 1.minute)
  }

  def getClanMembersDetails(clanId: Int): Seq[ClanMemberDetails] = {
    val clanResponse = scala.io.Source.fromURL(ClanWn8.clanDetailsUrl(clanId))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)
    val data: JsonNode = clanJson.findPath("data").findPath(clanId.toString)
    val membersList: mutable.Buffer[ClanMemberDetails] = Seq.empty.toBuffer
    val members = data.findValue("members").elements()
    while (members.hasNext) {
      val member: JsonNode = members.next()
      val accountId = member.findPath("account_id").asInt
      membersList += ClanMemberDetails(
        member.findPath("account_name").asText(),
        accountId,
        member.findPath("role_i18n").asText(), 0, 0)
    }
    membersList.par.map(member => {
      val wn8AndBattles: UserWn8WithBattles = Await.result(UserWn8.getAccountCachedWn8(member.accountId.toString), 1.minute)
      member.copy(wn8 = wn8AndBattles.wn8, battles = wn8AndBattles.battles)
    }).seq
  }

  private def calculateAverageWn8(membersList: Seq[ClanMemberDetails]): Double = {
    val totalBattles = membersList.map(_.battles).sum
    val weightedWn8s = membersList.map(v => (v.wn8 * v.battles) / totalBattles)
    BigDecimal(weightedWn8s.sum).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def saveCurrentClansInFile() = Future {
    val file = new File(FILE_WITH_LAST_CLAN_STATS)
    file.createNewFile()
    FileOps.printToFile(file) {
      p =>
        ClanSkirmishUtils.getCurrentClanSkirmishes.foreach(clan => {
          p.println(s"${
            clan.clanId
          },${
            clan.membersCount
          },${
            clan.skirmish.battles6
          },${
            clan.skirmish.wins6
          },${
            clan.skirmish.battles8
          },${
            clan.skirmish.wins8
          },${
            clan.skirmish.battles10
          },${
            clan.skirmish.wins10
          }")
        })
    }
  }

}
