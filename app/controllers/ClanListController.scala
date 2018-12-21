package controllers

import com.domain.Constants._
import javax.inject._
import com.domain.clans.ClanList
import com.domain.presentation.model.{ClanDelta, ClanSummary}
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class ClanListController @Inject() extends Controller {

  private var previous = ClanList.previousStats

  private var clanDeltaTimestamp: Long = 0

  private var clanDeltaCached = Seq.empty[ClanSummary]

  private def getClanSkirmishesStats = {
    if(System.currentTimeMillis() - clanDeltaTimestamp > CLAN_SKIRMISH_PROBING_THRESHOLD)  {
      Logger.debug("Refreshing clan skirmishes")
      clanDeltaTimestamp = System.currentTimeMillis()
      calculateClanDelta.onComplete(r => {
        previous = ClanList.previousStats
        clanDeltaCached = r.get
      })
    }
    clanDeltaCached
  }

  private def calculateClanDelta = Future { ClanList.clanSkirmishesStats.map(cur => {
    val prevOpt = previous.find(_.clanId == cur.clanId)
    val clanDelta = prevOpt match {
      case Some(prev) => Some(ClanDelta(cur.membersCount - prev.membersCount, prev.skirmish, cur.skirmish))
      case _ => None
    }
    ClanSummary(cur.clanId, cur.tag, cur.emblem, cur.membersCount, cur.skirmish, clanDelta)
  }).sortBy(clan => {
    val delta = clan.clanDelta
    if (delta.isDefined) {
      if (delta.get.totalBattles > 0) {
        -(delta.get.totalBattles * 100) // clan with battles go on top
      } else {
        -clan.totalWinRatio
      }
    } else {
      0
    }
  })}

  def list: Action[AnyContent] = Action.async { implicit request =>
    Future(Ok(views.html.clans(getClanSkirmishesStats)))
  }
}
