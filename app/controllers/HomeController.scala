package controllers

import javax.inject._

import com.domain.presentation.model.TankStats
import com.domain.wn8.Wn8Veh
import play.api._
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

    val res = Wn8Veh.calculate(accountId).map(tank => TankStats(tank._1, tank._3, tank._2.toInt))

    Ok(views.html.tanks(res))
  }
}