package com.howtographql.scala.sangria
import slick.jdbc.H2Profile.api._
import DBSchema._
import com.howtographql.scala.sangria.models._
import scala.concurrent.Future
import sangria.execution.deferred.{RelationIds, SimpleRelation}



class DAO(db: Database) {
  def allLinks = db.run(Links.result)
  def getLinks(ids: Seq[Int]) = db.run(
    Links.filter(_.id inSet ids).result
  )
  def getLinksByUserIds(ids: Seq[Int]): Future[Seq[Link]] = {
    db.run {
      Links.filter(_.postedBy inSet ids).result
    }
  }
  def getVotesByRelationIds(rel: RelationIds[Vote]): Future[Seq[Vote]] =
    db.run(
      Votes.filter { vote =>
        rel.rawIds.collect({
          case (SimpleRelation("byUser"), ids: Seq[Int]) => vote.userId inSet ids
          case (SimpleRelation("byLink"), ids: Seq[Int]) => vote.linkId inSet ids
        }).foldLeft(true: Rep[Boolean])(_ || _)

      }.result
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
