package controllers

import com.domain.clans.ClanWn8
import com.domain.db.DB
import com.domain.presentation.model.TankStats
import com.domain.user.UserWn8
import javax.inject._
import com.domain.db.DB.executionContext
import play.api.mvc._

import scala.concurrent.Await


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() extends Controller {
  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def tanks(accountId: String) = Action { implicit request =>

    val res = List.empty[TankStats]

    Ok(views.html.tanks(res))
  }

  def refreshUsers: Action[AnyContent] = Action.async { implicit request =>
    DB.TankersDao.getAll.map(users => {
      users.par.foreach(user => UserWn8.refreshAccountCachedWn8(user.toString))
    }).map(_ => {
      Ok(views.html.success())
    })
  }

  def refreshClansWn8: Action[AnyContent] = Action.async { implicit request =>
    DB.ClansDao.getAll.map(clans => {
      clans.par.foreach(clan => ClanWn8.refreshClanCachedWn8(clan))
    }).map(_ => {
      Ok(views.html.success())
    })
  }


}
