package com.domain.db.schema


import java.sql.Timestamp

import slick.jdbc.JdbcProfile
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType
import com.domain.db.DB.executionContext

import scala.concurrent.Future

case class Tanker(accountId: Int, battles: Int, wn8: Double, updated: Option[Timestamp] = None)

class TankerTable(tag: Tag) extends Table[Tanker](tag, Tankers.tableName){

  def accountId = column[Int]("account_id", O.PrimaryKey)

  def battles = column[Int]("battles")

  def wn8 = column[Double]("wn8")

  def updated = column[Option[Timestamp]]("updated", SqlType("timestamp not null default NOW() on update NOW()"))

  def * = (accountId, battles, wn8, updated) <> (Tanker.tupled, Tanker.unapply)

}

object Tankers {
  val table = TableQuery[TankerTable]

  val tableName = "Tankers"

  trait TankersDao {
    def add(tanker: Tanker): Future[Int]
    def addOrUpdate(tanker: Tanker): Future[Int]
    def findByAccountId(accountId: Int): Future[Seq[Tanker]]
    def getAll: Future[Seq[Int]]
    def addOrUpdate(tankers: Seq[Tanker]): Future[Seq[Int]]
  }

  class TankersDaoImpl(implicit val db: JdbcProfile#Backend#Database) extends TankersDao {

    override def findByAccountId(accountId: Int): Future[Seq[Tanker]] = {
      db.run(table.filter(_.accountId === accountId).result)
    }

    override def add(tanker: Tanker): Future[Int] = {
      db.run(table += tanker)
    }

    override def addOrUpdate(tanker: Tanker): Future[Int] = {
      db.run(table.insertOrUpdate(tanker))
    }

    override def addOrUpdate(tankers: Seq[Tanker]): Future[Seq[Int]] = {
      val query = (for {
        res <- DBIO.sequence(tankers.map(tanker => table.filter(_.accountId === tanker.accountId).update(tanker)))
      } yield res).transactionally
      db.run(query)
    }

    override def getAll: Future[Seq[Int]] = {
      db.run(table.map(_.accountId).result)
    }

  }


}