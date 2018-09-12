package com.howtographql.scala.sangria
import sangria.schema.{Field, ListType, ObjectType}
import models._
import sangria.execution.deferred.Fetcher
import sangria.execution.deferred.DeferredResolver
import sangria.execution.deferred.Relation
import sangria.execution.deferred.RelationIds

// #
import sangria.schema._
import sangria.macros.derive._
import sangria.ast.StringValue
import akka.http.scaladsl.model.DateTime

object GraphQLSchema {

  implicit val GraphQLDateTime = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  lazy val UserType : ObjectType[Unit, User] =  deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt",
      Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    AddFields(
      Field("links", ListType(LinkType), resolve = c =>  linksFetcher.deferRelSeq(linkByUserRel, c.value.id))),
    AddFields(
      Field("votes", ListType(VoteType), resolve = c =>  votesFetcher.deferRelSeq(voteByUserRel, c.value.id))),

  )
  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  lazy val LinkType : ObjectType[Unit, Link] =  deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt",
      Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("postedBy",
      Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))),
    AddFields(
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByLinkRel, c.value.id))
      )

  )
  val linkByUserRel = Relation[Link, Int]("byUser", l => Seq(l.postedBy))
  val voteByUserRel = Relation[Vote, Int]("byUser", v => Seq(v.userId))
  val voteByLinkRel = Relation[Vote, Int]("byLink", v => Seq(v.linkId))

  val linksFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: MyContext, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  val VoteType =  deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ExcludeFields("userId", "linkId"),
    AddFields(Field("user",  UserType, resolve = c => usersFetcher.defer(c.value.userId))),
    AddFields(Field("link",  LinkType, resolve = c => linksFetcher.defer(c.value.linkId)))
  )
  val votesFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids),
//    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByUserIds(ids(voteByUserRel))
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationIds(ids)
  )

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks",
        ListType(LinkType),
        resolve = c => c.ctx.dao.allLinks
      ),
      Field("link", //1
        OptionType(LinkType), //2
        arguments = Id :: Nil,
        resolve = c => linksFetcher.defer(c.arg(Id))
      ),
      Field("links", //1
        ListType(LinkType), //2
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids)),
      ),
      Field("users", //1
        ListType(UserType), //2
        arguments = Ids :: Nil,
        resolve = c => usersFetcher.deferSeq(c.arg(Ids)),
      ),
      Field("votes", //1
        ListType(VoteType), //2
        arguments = Ids :: Nil,
        resolve = c => votesFetcher.deferSeq(c.arg(Ids)),
      ),
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)

}
