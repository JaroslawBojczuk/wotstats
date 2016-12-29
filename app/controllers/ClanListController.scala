package controllers

import javax.inject._

import com.domain.clans.ClanList
import play.api.mvc._

@Singleton
class ClanListController @Inject() extends Controller {

  def list = Action { implicit request =>
    val res = ClanList.topClans.sortBy(-_.skirmishBattlesWins)
    Ok(views.html.clans(res))
  }
}
