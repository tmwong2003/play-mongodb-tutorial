package controllers

import javax.inject.{ Inject, Singleton }

import scala.collection.JavaConversions._
import scala.concurrent.Future

import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, RequestHeader }
import play.libs.concurrent.HttpExecutionContext
import play.modules.reactivemongo.{ MongoController, ReactiveMongoApi, ReactiveMongoComponents }
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.BSONCountCommand.{ Count, CountResult }
import reactivemongo.api.commands.bson.BSONCountCommandImplicits._
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection._

@Singleton
class Application @Inject() (val reactiveMongoApi: ReactiveMongoApi, val config: Config, val playSessionStore: PlaySessionStore, override val ec: HttpExecutionContext) extends Controller
    with MongoController with ReactiveMongoComponents with Security[CommonProfile] {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def jsonCollection = reactiveMongoApi.db.collection[JSONCollection]("widgets");
  def bsonCollection = reactiveMongoApi.db.collection[BSONCollection]("widgets");

  def login() = Secure("OidcClient", "isAuthenticated") { profiles =>
    Action { implicit request =>
      Ok("Logged in: " + profiles)
    }
  }

  def logout() = Action { implicit request => Ok("Logged out")
  }

  def index = Action {
    Logger.info("Application startup...")

    val posts = List(
      Json.obj(
        "name" -> "Widget One",
        "description" -> "My first widget",
        "author" -> "Justin"),
      Json.obj(
        "name" -> "Widget Two: The Return",
        "description" -> "My second widget",
        "author" -> "Justin"))

    val query = BSONDocument("name" -> BSONDocument("$exists" -> true))
    val command = Count(query)
    val result: Future[CountResult] = bsonCollection.runCommand(command)

    result.map { res =>
      val numberOfDocs: Int = res.value
      if (numberOfDocs < 1) {
        jsonCollection.bulkInsert(posts.toStream, ordered = true).foreach(i => Logger.info("Record added."))
      }
    }

    Ok("Your database is ready.")
  }

  def cleanup = Action {
    jsonCollection.drop().onComplete {
      case _ => Logger.info("Database collection dropped")
    }
    Ok("Your database is clean.")
  }
}
