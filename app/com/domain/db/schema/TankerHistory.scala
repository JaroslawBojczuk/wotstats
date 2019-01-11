package com.domain.db.schema

import com.domain.db.DB
import com.domain.db.DB.executionContext
import play.api.Logger
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future

case class TankerHistory(accountId: Int, battles: Int, wn8: Double, day: Long)

class TankerHistoryTable(tag: Tag) extends Table[TankerHistory](tag, TankersHistory.tableName) {

  def accountId = column[Int]("account_id")

  def battles = column[Int]("battles")

  def wn8 = column[Double]("wn8")

  def day = column[Long]("day")

  def accountIdFKey = foreignKey("TANKER_HISTORY_ACCOUNT_ID_FK", accountId, Tankers.table)(_.accountId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (accountId, battles, wn8, day) <> (TankerHistory.tupled, TankerHistory.unapply)

}

object TankersHistory {
  val table = TableQuery[TankerHistoryTable]

  val tableName = "TankersHistory"

  trait TankerHistoryDao {
    def add(TankerHistory: TankerHistory): Future[Int]

    def findByAccountId(accountId: Int): Future[Seq[TankerHistory]]

    def addOrReplaceCurrentDay(accountId: Int, tankerHistory: TankerHistory): Future[TankerHistory]

    def addOrReplaceCurrentDayBatch(tankerHistory: Seq[TankerHistory]): Future[Seq[TankerHistory]]
  }

  class TankerHistoryDaoImpl(implicit val db: JdbcProfile#Backend#Database) extends TankerHistoryDao {

    override def findByAccountId(accountId: Int): Future[Seq[TankerHistory]] = {
      db.run(table.filter(_.accountId === accountId).sortBy(_.day).result)
    }

    override def add(tankerHistory: TankerHistory): Future[Int] = {
      db.run(table += tankerHistory)
    }

    override def addOrReplaceCurrentDay(accountId: Int, tankerHistory: TankerHistory): Future[TankerHistory] = {
      val q = (for {
        _ <- table.filter(tanker => tanker.accountId === accountId && tanker.day === tankerHistory.day).delete
        _ <- table += tankerHistory
      } yield()).transactionally
      db.run(q).map(_ => tankerHistory)
    }

    override def addOrReplaceCurrentDayBatch(tankerHistory: Seq[TankerHistory]): Future[Seq[TankerHistory]] = {
      Future.sequence(tankerHistory.groupBy(_.day).map(grouped => {
        val (dayOfBattle, tankersHistory) = grouped
        val q = (for {
          _ <- table.filter(tanker => tanker.accountId.inSet(tankersHistory.map(_.accountId)) && tanker.day === dayOfBattle).delete
          _ <- table ++= tankersHistory
        } yield()).transactionally
        db.run(q).map(_ => tankersHistory)
      }).toSeq).map(_.flatten)
    }
  }

}