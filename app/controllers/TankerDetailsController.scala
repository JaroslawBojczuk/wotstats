package controllers

import javax.inject._
import com.domain.user.{UserWn8, WGTankerDetails, Wn8Veh}
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class TankerDetailsController @Inject() extends Controller {

  def refresh(accountId: String): Action[AnyContent] = Action { implicit request =>
    Await.result(Wn8Veh.refreshTankerTanksForCurrentDay(accountId.toInt), 1.minute)
    Await.result(UserWn8.refreshAccountCachedWn8(accountId), 1.minute)
    Ok(views.html.success())
  }

  def details(accountName: String): Action[AnyContent] = Action { implicit request =>
    val res = WGTankerDetails.getDetails(accountName)
    val tier10Battles = res.get.tanks.filter(_.tier == 10).foldLeft(0d)((sum, b) => sum + b.battles)
    val tier10Wn8: Double = res.get.tanks.filter(_.tier == 10).foldLeft(0d)((sum, b) => sum + (b.battles / tier10Battles) * b.wn8)
    Ok(views.html.tanker_details(res, tier10Wn8))
  }
}
