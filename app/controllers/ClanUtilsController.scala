package controllers

import javax.inject._
import com.domain.wn8.ClanWn8
import play.api.mvc._

@Singleton
class ClanUtilsController @Inject() extends Controller {

  def refresh = Action { implicit request =>

    ClanWn8.saveCurrentClansInFile
    Ok(views.html.success())

  }
}
