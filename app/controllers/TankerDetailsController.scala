package controllers

import javax.inject._
import com.domain.presentation.model.TankStats
import com.domain.user.WGTankerDetails
import com.domain.wn8.Wn8Veh
import play.api.mvc._

@Singleton
class TankerDetailsController @Inject() extends Controller {

  def details(accountName: String) = Action { implicit request =>
    val res = WGTankerDetails.getDetails(accountName)
    Ok(views.html.tanker_details(res))
  }
}
