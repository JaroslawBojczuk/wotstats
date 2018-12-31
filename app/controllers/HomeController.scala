package controllers

import com.domain.clans.ClanWn8
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.{Tanker, TankerHistory}
import com.domain.presentation.model.TankStats
import com.domain.user.{UserWn8, WGTankerDetails}
import javax.inject._
import play.api.mvc._


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
      val tankersHistory = users.par.map(accountId => {
        val day = WGTankerDetails.getDayOfLastBattle(accountId)
        val userWn8 = UserWn8.calculateWn8(accountId)
        TankerHistory(userWn8.accountId, userWn8.battles, userWn8.wn8, day)
      }).seq
      DB.TankerHistoryDao.addOrReplaceCurrentDayBatch(tankersHistory)
      DB.TankersDao.addOrUpdate(tankersHistory.map(tanker => Tanker(tanker.accountId, tanker.battles, tanker.wn8)))
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
