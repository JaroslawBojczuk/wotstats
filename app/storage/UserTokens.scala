package storage

import scala.collection.mutable

case class UserToken(accessToken: String = "", nickname: String = "", accountId: String = "", expiresAt: String = "")

object UserTokens {

  val tokens = new mutable.HashMap[String, UserToken]()

  def apply(accountId: String) = {
    tokens.get(accountId)
  }

}
