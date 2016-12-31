package controllers

import javax.inject._

import com.domain.clans.ClanUtils
import play.api.mvc._

@Singleton
class ClanUtilsController @Inject() extends Controller {

  def refresh = Action { implicit request =>

    ClanUtils.saveCurrentClansInFile
    Ok(views.html.success())

  }
}
