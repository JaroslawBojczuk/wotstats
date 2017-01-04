package com.domain.presentation.model

case class ClanDetails(tag: String, name: String, averageWn8: Double, members: Seq[ClanMemberDetails]) {}
case class ClanMemberDetails(name: String, accountId: Int) {}
