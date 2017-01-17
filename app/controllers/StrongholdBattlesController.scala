package controllers

import javax.inject._

import com.domain.Constants
import com.domain.clans.ClanUtils
import play.api.mvc.{Action, _}
import play.libs.Json

import scala.collection.JavaConverters._

@Singleton
class StrongholdBattlesController @Inject() extends Controller {

  private def url = s"https://api.worldoftanks.eu/wot/clanratings/top/?application_id=${Constants.APPLICATION_ID}&rank_field=efficiency&fields=clan_id&limit=15"

  def list() = Action { implicit request =>

    val clansResponse = scala.io.Source.fromURL(url).mkString
    val clansJson = Json.parse(clansResponse)
    val clansIds = clansJson.findPath("data").findValues("clan_id").asScala

    val battles = clansIds.flatMap(node => ClanUtils.getClanStrongholdPlannedBattles(node.asText())).toSeq.distinct.sortBy(_.date)

    //val attackers = battles.groupBy(_.attackerClanTag).map(entry => (entry._1, entry._2.size)).toSeq.sortBy(-_._2)
    //val defenders = battles.groupBy(_.defenderClanTag).map(entry => (entry._1, entry._2.size)).toSeq.sortBy(-_._2)

    Ok(views.html.stronghold_battles(battles))
  }

}

