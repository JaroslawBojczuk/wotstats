package com.domain.db.schema

import java.sql.Timestamp

import com.domain.db.schema.Tankers.table
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.Future

case class Clan(clanId: Int, wn8: Double, updated: Option[Timestamp] = None)

class ClanTable(tag: Tag) extends Table[Clan](tag, Clans.tableName){

  def clanId = column[Int]("clan_id", O.PrimaryKey)

  def wn8 = column[Double]("wn8")

  def updated = column[Option[Timestamp]]("updated", SqlType("timestamp not null default NOW() on update NOW()"))

  def * = (clanId, wn8, updated) <> (Clan.tupled, Clan.unapply)

}

object Clans {
  val table = TableQuery[ClanTable]

  val tableName = "Clans"

  trait ClansDao {
    def add(clan: Clan): Future[Int]
    def addOrUpdate(clan: Clan): Future[Int]
    def findByClanId(clanId: Int): Future[Seq[Clan]]
    def getAll: Future[Seq[Int]]
  }

  class ClansDaoImpl(implicit val db: JdbcProfile#Backend#Database) extends ClansDao {

    override def findByClanId(clanId: Int): Future[Seq[Clan]] = {
      db.run(table.filter(_.clanId === clanId).result)
    }

    override def add(clan: Clan): Future[Int] = {
      db.run(table += clan)
    }

    override def addOrUpdate(clan: Clan): Future[Int] = {
      db.run(table.insertOrUpdate(clan))
    }

    override def getAll: Future[Seq[Int]] = {
      db.run(table.map(_.clanId).result)
    }
  }


}