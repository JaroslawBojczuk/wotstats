package controllers

import javax.inject._
import com.domain.user.WGTankerDetails
import play.api.mvc._

@Singleton
class TankerDetailsController @Inject() extends Controller {

  def details(accountId: String) = Action { implicit request =>
    val res = WGTankerDetails.getDetails(accountId)
    Ok(views.html.tanker_details(res))
  }
}
