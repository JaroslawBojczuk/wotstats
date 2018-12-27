package com.domain.db

import java.util.concurrent.Executors

import com.domain.db.schema.Clans.ClansDaoImpl
import com.domain.db.schema.{Clans, Tanker, Tankers}
import com.domain.db.schema.Tankers.TankersDaoImpl
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration._

object DB {

  implicit val db: MySQLProfile.backend.Database = Database.forConfig("db")

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  val TankersDao = new TankersDaoImpl

  val ClansDao = new ClansDaoImpl

  def main(args: Array[String]): Unit = {
    println("Creating tables")

    Await.result(db.run(Tankers.tankers.schema.drop), 10.seconds)
    Await.result(db.run(Clans.clans.schema.drop), 10.seconds)

    Await.result(db.run(Tankers.tankers.schema.create), 10.seconds)
    Await.result(db.run(Clans.clans.schema.create), 10.seconds)

    Await.result(TankersDao.add(Tanker(22, "", 1, 1)), 10.seconds)

    println(Await.result(TankersDao.findByAccountId(22), 10.seconds))


    db.close()
  }

}
