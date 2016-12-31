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

    val currentFuture = ClanList.topClansCurrentStatsFuture

    val futureRes = currentFuture.map(summary => {
      summary.map(cur => {
        val prevOpt = previous.find(_.clanId == cur.clanId)
        val clanDelta = prevOpt match {
          case Some(prev) => Some(ClanDelta(cur.membersCount - prev.membersCount, cur.skirmishBattles - prev.skirmishBattles, cur.skirmishBattlesWins - prev.skirmishBattlesWins))
          case _ => None
        }
        ClanSummary(cur.clanId, cur.tag, cur.emblem, cur.membersCount, cur.skirmishBattles, cur.skirmishBattlesWins, clanDelta)
      }).sortBy(clan => {
        val delta = clan.clanDelta
        if (delta.isDefined) {
          -delta.get.skirmishBattles
        } else {
          0
        }
      })
    })

    futureRes.map { res => Ok(views.html.clans(res)) }

  }
}
