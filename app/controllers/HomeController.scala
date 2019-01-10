package controllers

import com.domain.WN8
import com.domain.clans.ClanWn8
import com.domain.db.DB
import com.domain.db.DB.executionContext
import com.domain.db.schema.{Tanker, TankerHistory}
import com.domain.presentation.model.TankStats
import com.domain.user.UserTanksWn8
import javax.inject._
import play.api.Logger
import play.api.mvc._

import scala.collection.parallel.ForkJoinTaskSupport


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

  private val taskSupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(10))

  def refreshUsers: Action[AnyContent] = Action.async { implicit request =>
    DB.TankersDao.getAll.map((users: Seq[Int]) => {
      Logger.debug(s"Users to refresh: ${users.size}")
      users.grouped(100).foreach(u => {
        val par = u.par
        par.tasksupport = taskSupport
        val (tankersHistory, tanks) = par.map(accountId => {
          Logger.debug(s"[$accountId] Refreshing data for user")
          val tanks = UserTanksWn8.getTankerTanksForHisLastDay(accountId)
          val (wn8, battles) = WN8.calculateTotalWn8AndBattles(tanks)
          (TankerHistory(accountId, battles, wn8, tanks.map(_.day).headOption.getOrElse(0L)), tanks)
        }).seq.unzip
        Logger.debug(s"Inserting into database")
        DB.TankerHistoryDao.addOrReplaceCurrentDayBatch(tankersHistory)
        DB.TankersDao.addOrUpdate(tankersHistory.map(tanker => Tanker(tanker.accountId, tanker.battles, tanker.wn8)))
        DB.TankerTanksDao.addOrReplaceInBatch(tanks.flatten)
      })
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
