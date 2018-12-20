package com.domain

import com.typesafe.config.ConfigFactory

object Config {

  private val configuration = ConfigFactory.load()

  def getString(key: String): String = configuration.getString(key)

  def getInt(key: String): Int = configuration.getInt(key)

}
