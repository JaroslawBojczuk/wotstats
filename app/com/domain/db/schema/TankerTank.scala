package com.domain.db.schema

import slick.jdbc.JdbcProfile
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import com.domain.db.DB.executionContext

import scala.concurrent.Future

case class TankerTank(accountId: Int,
                      tankId: Int,
                      frags: Double,
                      damageDealt: Double,
                      spotted: Double,
                      droppedCapturePoints: Double,
                      battles: Int,
                      wins: Int,
                      battleAvgXp: Double,
                      wn8: Double,
                      day: Long)

class TankerTankTable(tag: Tag) extends Table[TankerTank](tag, TankerTanks.tableName){

  def accountId = column[Int]("account_id")

  def tankId = column[Int]("tank_id")

  def frags = column[Double]("frags")

  def damageDealt = column[Double]("damage_dealt")

  def spotted = column[Double]("spotted")

  def droppedCapturePoints = column[Double]("dropped_capture_points")

  def battles = column[Int]("battles")

  def wins = column[Int]("wins")

  def battleAvgXp = column[Double]("battle_avg_xp")

  def wn8 = column[Double]("wn8")

  def day = column[Long]("day")

  def idx1 = index("TANKER_TANKS_ACCOUNT_ID_INDEX", accountId, unique = false)

  def idx2 = index("TANKER_TANKS_ACCOUNT_ID_DAY_INDEX", (accountId, day), unique = false)

  //def accountIdFKey = foreignKey("TANKER_TANKS_ACCOUNT_ID_FK", accountId, Tankers.table)(_.accountId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (accountId, tankId, frags, damageDealt, spotted, droppedCapturePoints, battles, wins, battleAvgXp, wn8, day) <> (TankerTank.tupled, TankerTank.unapply)

}

object TankerTanks {
  val table = TableQuery[TankerTankTable]

  val tableName = "TankerTanks"

  trait TankerTanksDao {
    def add(tankerTanks: Seq[TankerTank]): Future[Option[Int]]
    def addOrReplaceGivenDay(accountId: Int, day: Long, tankerTanks: Seq[TankerTank]): Future[Seq[TankerTank]]
    def addOrReplaceInBatch(tankerTanks: Seq[TankerTank]): Future[Seq[TankerTank]]
    def findByAccountId(accountId: Int): Future[Seq[TankerTank]]
    def findLatestForAccountId(accountId: Int): Future[Seq[TankerTank]]
    def findForAccountIdAndLastDayBattle(accountId: Int, day: Long): Future[Seq[TankerTank]]
    def findPreviousDayForAccountId(accountId: Int, referenceDay: Long): Future[Option[Long]]
  }

  class TankerTanksDaoImpl(implicit val db: JdbcProfile#Backend#Database) extends TankerTanksDao {

    override def add(tankerTanks: Seq[TankerTank]): Future[Option[Int]] = {
      db.run(table ++= tankerTanks)
    }

    override def findByAccountId(accountId: Int): Future[Seq[TankerTank]] = {
      db.run(table.filter(_.accountId === accountId).result)
    }

    override def addOrReplaceGivenDay(accountId: Int, day: Long, tankerTanks: Seq[TankerTank]): Future[Seq[TankerTank]] = {
      val q = (for {
        _ <- table.filter(tank => tank.accountId === accountId && tank.day === day).delete
        _ <- table ++= tankerTanks
      } yield()).transactionally
      db.run(q).map(_ => tankerTanks)
    }

    override def addOrReplaceInBatch(tankerTanks: Seq[TankerTank]): Future[Seq[TankerTank]] = {
      val delete = (for {
        res <- DBIO.sequence(tankerTanks.map(t => (t.accountId, t.day)).distinct.map {
          tanker => table.filter(tanks => tanks.accountId === tanker._1 && tanks.day === tanker._2).delete
        })
      } yield res).transactionally
      val add = (for {
        _ <- table ++= tankerTanks
      } yield()).transactionally
      db.run(delete).flatMap(_ => db.run(add).map(_ => tankerTanks))
    }

    override def findLatestForAccountId(accountId: Int): Future[Seq[TankerTank]] = {
      val q = (for {
         latestDay <- table.filter(_.accountId === accountId).sortBy(_.day.desc).map(_.day).take(1)
         latestTankEntries <- table.filter(_.accountId === accountId).filter(_.day === latestDay)
      } yield latestTankEntries).result.transactionally
      db.run(q)
    }

    override def findPreviousDayForAccountId(accountId: Int, referenceDay: Long): Future[Option[Long]] = {
      val q = (for {
        previousDay <- table.filter(_.accountId === accountId).map(_.day).distinct.filter(_ <= referenceDay).sortBy(_.desc).take(2)
      } yield previousDay).result.transactionally
      db.run(q).map(_.lastOption)
    }

    override def findForAccountIdAndLastDayBattle(accountId: Int, day: Long): Future[Seq[TankerTank]] = {
      val q = (for {
        latestTankEntries <- table.filter(_.accountId === accountId).filter(_.day === day)
      } yield latestTankEntries).result.transactionally
      db.run(q)
    }
  }

}