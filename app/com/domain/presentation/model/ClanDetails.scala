package com.domain.presentation.model

case class ClanDetails(tag: String, name: String, members: Seq[ClanMemberDetails]) {}
case class ClanMemberDetails(name: String, accountId: Int) {}
