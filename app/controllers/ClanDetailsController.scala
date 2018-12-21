package controllers

import java.util.concurrent.Executors

import com.domain.clans.ClanUtils
import javax.inject._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClanDetailsController @Inject() extends Controller {

  private val execContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

  def details(clanId: String): Action[AnyContent] = Action.async { implicit request =>
    Future(ClanUtils.getClanDetails(clanId))(execContext).map(res => {
      Ok(views.html.clan_details(res))
    })
  }
}
