package com.domain.db.schema


import java.sql.Timestamp

import slick.jdbc.JdbcProfile
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.Future

case class Tanker(accountId: Int, name: String, battles: Int, wn8: Double, updated: Option[Timestamp] = None)

class TankerTable(tag: Tag) extends Table[Tanker](tag, Tankers.tableName){

  def accountId = column[Int]("account_id", O.PrimaryKey)

  def name = column[String]("name")

  def battles = column[Int]("battles")

  def wn8 = column[Double]("wn8")

  def updated = column[Option[Timestamp]]("updated", SqlType("timestamp not null default NOW() on update NOW()"))

  def * = (accountId, name, battles, wn8, updated) <> (Tanker.tupled, Tanker.unapply)

}

object Tankers {
  val tankers = TableQuery[TankerTable]

  val tableName = "Tankers"

  trait TankersDao {
    def add(tanker: Tanker): Future[Int]
    def addOrUpdate(tanker: Tanker): Future[Int]
    def findByAccountId(accountId: Int): Future[Seq[Tanker]]
  }

  class TankersDaoImpl(implicit val db: JdbcProfile#Backend#Database) extends TankersDao {

    override def findByAccountId(accountId: Int): Future[Seq[Tanker]] = {
      db.run(tankers.filter(_.accountId === accountId).result)
    }

    override def add(tanker: Tanker): Future[Int] = {
      db.run(tankers += tanker)
    }

    override def addOrUpdate(tanker: Tanker): Future[Int] = {
      db.run(tankers.insertOrUpdate(tanker))
    }
  }


}