package controllers

import javax.inject._

import com.domain.clans.ClanList
import com.domain.presentation.model.{ClanDelta, ClanSummary}
import play.api.mvc._

@Singleton
class ClanListController @Inject() extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def list = Action.async { implicit request =>

    val previous = ClanList.previousStats

    ClanList.clanSkirmishesStats.map(summary => {
      summary.map(cur => {
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
    }).map { res => Ok(views.html.clans(res)) }

  }
}
