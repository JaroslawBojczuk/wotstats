package controllers

import com.domain.clans.ClanWn8
import javax.inject._
import play.api.mvc._

@Singleton
class ClanUtilsController @Inject() extends Controller {

  def refresh = Action { implicit request =>

    ClanWn8.saveCurrentClansInFile
    Ok(views.html.success())

  }
}
