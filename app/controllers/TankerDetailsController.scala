package controllers

import javax.inject._
import com.domain.presentation.model.TankStats
import com.domain.user.WGTankerDetails
import com.domain.wn8.Wn8Veh
import play.api.mvc._

@Singleton
class TankerDetailsController @Inject() extends Controller {

  def details(accountName: String): Action[AnyContent] = Action { implicit request =>
    val res = WGTankerDetails.getDetails(accountName)
    val tier10Battles = res.get.tanks.filter(_.tier == 10).foldLeft(0d)((sum, b) => sum + b.battles)
    val tier10Wn8: Double = res.get.tanks.filter(_.tier == 10).foldLeft(0d)((sum, b) => sum + (b.battles / tier10Battles) * b.wn8)
    Ok(views.html.tanker_details(res, tier10Wn8))
  }
}
