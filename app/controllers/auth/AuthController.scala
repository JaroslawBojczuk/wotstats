package controllers.auth

import javax.inject._

import play.api.Logger
import play.api.mvc._
import storage.{UserToken, UserTokens}


@Singleton
class AuthController @Inject() extends Controller {

  def authorize(status: String = "", access_token: String = "", nickname: String = "", account_id: String = "", expires_at: String = "") = Action { implicit request =>
    UserTokens.tokens.put(account_id, UserToken(access_token, nickname, account_id, expires_at))
    Logger.logger.debug(s"Saved token and session for user: $nickname")
    Redirect("/").withSession(request2session.+("user", account_id))
  }
}
