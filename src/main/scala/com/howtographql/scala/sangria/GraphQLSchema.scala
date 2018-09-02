package com.howtographql.scala.sangria
import sangria.schema.{Field, ListType, ObjectType}
import models._
import sangria.execution.deferred.Fetcher
import sangria.execution.deferred.DeferredResolver
import sangria.execution.deferred.HasId

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

  val LinkType = ObjectType[Unit, Link](
    "Link",
    interfaces[Unit, Link](IdentifiableType),
    fields[Unit, Link](
      Field("id", IntType, resolve = _.value.id),
      Field("url", StringType, resolve = _.value.url),
      Field("description", StringType, resolve = _.value.description),
      Field("createdAt", GraphQLDateTime, resolve= _.value.createdAt)
    )
  )
//  implicit val LinkType =  deriveObjectType[Unit, Link](
//    Interfaces(IdentifiableType),
//    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
//  )
  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  val UserType =  deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )
  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val VoteType =  deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )
  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
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
