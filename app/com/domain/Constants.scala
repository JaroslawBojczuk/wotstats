package com.domain

import java.io.File
import java.nio.file.{Path, Paths}

object Constants {

  val APPLICATION_ID = "c0a88d6d3b5657d6750bd219d55fb550"

  val CLAN_LIMIT = 100

  val DATA_FOLDER_PATH: String = Config.getString("data_folder_path") + File.separator

  val FILE_WITH_LAST_CLAN_STATS: String = s"${DATA_FOLDER_PATH}last.txt"

  val FOLDER_WITH_CLAN_AVG_WN8: String = s"${DATA_FOLDER_PATH}clans" + File.separator

  val FOLDER_WITH_USERS_WN8: String = s"${DATA_FOLDER_PATH}users" + File.separator

  val EXPECTED_TANKS_VALUES_CSV_PATH: Path = Paths.get(s"${DATA_FOLDER_PATH}wn8exp.csv")

  val WG_REDIRECT_TO: String = Config.getString("wg_redirect")

}
