package controllers

import javax.inject._

import com.domain.clans.ClanUtils
import com.domain.wn8.UserWn8
import play.api.Logger
import play.api.mvc._

@Singleton
class ClanDetailsController @Inject() extends Controller {

  def details(clanId: String) = Action { implicit request =>

    Logger.logger.debug(s"Gathering details for clan id: $clanId")

    val clanDetails = ClanUtils.getClanDetails(clanId)

    Ok(views.html.clan_details(clanDetails))

  }
}
