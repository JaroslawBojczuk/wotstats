package com.domain

import java.io.File

object Constants {

  val APPLICATION_ID = "c0a88d6d3b5657d6750bd219d55fb550"

  val CLAN_LIMIT = 100

  val DATA_FOLDER_PATH: String = Config.getString("data_folder_path") + File.separator

  val FILE_WITH_LAST_CLAN_STATS = s"${DATA_FOLDER_PATH}last.txt"

  val FOLDER_WITH_CLAN_AVG_WN8 = s"${DATA_FOLDER_PATH}clans"

}
