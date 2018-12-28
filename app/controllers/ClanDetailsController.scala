package controllers

import java.util.concurrent.Executors

import com.domain.clans.ClanWn8
import com.domain.presentation.model.ClanDetails
import com.fasterxml.jackson.databind.JsonNode
import javax.inject._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.libs.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Codec

@Singleton
class ClanDetailsController @Inject() extends Controller {

  private val execContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

  def refresh(clanId: String): Action[AnyContent] = Action { implicit request =>
    Await.result(ClanWn8.refreshClanCachedWn8(clanId.toInt), 1.minute)
    Ok(views.html.success())
  }

  def details(clanId: String): Action[AnyContent] = Action.async { implicit request =>
    Future(getClanDetails(clanId))(execContext).map(res => {
      Ok(views.html.clan_details(res))
    })
  }

  def getClanDetails(clanId: String): ClanDetails = {

    Logger.debug(s"Gathering details for clan id: $clanId")

    val clanResponse = scala.io.Source.fromURL(ClanWn8.clanDetailsUrl(clanId.toInt))(Codec.UTF8).mkString
    val clanJson = Json.parse(clanResponse)

    val data: JsonNode = clanJson.findPath("data").findPath(clanId)
    val clanName = data.findPath("name").asText()
    val clanTag = data.findPath("tag").asText()

    val membersWithWn8 = ClanWn8.getClanMembersDetails(clanId.toInt)
    val avg = ClanWn8.getClanCachedWn8(clanId.toInt)

    ClanDetails(clanId.toInt, clanTag, clanName, avg, membersWithWn8.sortBy(-_.wn8), Seq.empty)
  }

}
