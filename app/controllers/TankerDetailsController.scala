package controllers

import javax.inject._
import com.domain.user.{UserWn8, WGTankerDetails, UserTanksWn8}
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class TankerDetailsController @Inject() extends Controller {

  def refresh(accountId: String): Action[AnyContent] = Action { implicit request =>
    Await.result(UserWn8.refreshAccountCachedWn8(accountId), 1.minute)
    Await.result(UserTanksWn8.refreshTankerTanks(accountId.toInt), 1.minute)
    Ok(views.html.success())
  }

  def details(accountName: String): Action[AnyContent] = Action { implicit request =>
    val res = WGTankerDetails.getDetails(accountName)
    Ok(views.html.tanker_details(res))
  }
}
