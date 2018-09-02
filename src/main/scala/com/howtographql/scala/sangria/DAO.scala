package com.howtographql.scala.sangria
import slick.jdbc.H2Profile.api._
import DBSchema._
import com.howtographql.scala.sangria.models.Link
import scala.concurrent.Future


class DAO(db: Database) {
  def allLinks = db.run(Links.result)
  def getLinks(ids: Seq[Int]) = db.run(
    Links.filter(_.id inSet ids).result
  )

  def allUsers = db.run(Users.result)
  def getUsers(ids: Seq[Int]) = db.run(
    Users.filter(_.id inSet ids).result
  )

  def allVotes = db.run(Votes.result)
  def getVotes(ids: Seq[Int]) = db.run(
    Votes.filter(_.id inSet ids).result
  )
}
