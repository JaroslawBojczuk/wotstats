package controllers

import java.util.concurrent.Executors

import com.domain.Constants._
import com.domain.clans.ClanSkirmishUtils
import com.domain.presentation.model.{ClanDelta, ClanSummary}
import javax.inject._
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClanListController @Inject() extends Controller {

  def list: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.clans(clanDeltaCached))
  }

  Future {
    while (true) {
      Logger.debug("Refreshing clan skirmishes")
      current = ClanSkirmishUtils.getCurrentClanSkirmishes
      refreshClanDelta()
      Logger.debug("Clan skirmishes refreshed")
      Thread.sleep(CLAN_SKIRMISH_PROBING_THRESHOLD)
    }
  }(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))

  Future {
    while (true) {
      Logger.debug("Refreshing clan skirmishes from file")
      previous = ClanSkirmishUtils.previousStats
      Logger.debug("Clan skirmishes from file refreshed")
      Thread.sleep(CLAN_SKIRMISH_FILE_PROBING_THRESHOLD)
    }
  }(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))

  private var previous = Seq.empty[ClanSummary]

  private var current = Seq.empty[ClanSummary]

  private var clanDeltaCached = Seq.empty[ClanSummary]

  private def refreshClanDelta(): Unit = clanDeltaCached = calculateClanDelta(previous, current)

  private def calculateClanDelta(previous: Seq[ClanSummary], current: Seq[ClanSummary]): Seq[ClanSummary] = {
    current.map(cur => {
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
    })
  }


}
