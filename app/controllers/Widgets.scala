package controllers

import javax.inject.Inject

import scala.concurrent.Future

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, BodyParsers, Controller }
import play.modules.reactivemongo.{ MongoController, ReactiveMongoApi, ReactiveMongoComponents }
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.bson.BSONCountCommand.{ Count, CountResult }
import reactivemongo.api.commands.bson.BSONCountCommandImplicits._
import reactivemongo.bson.{ BSONObjectID, BSONDocument }
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection._

import repos.WidgetRepoImpl

object WidgetFields {
  val Id = "_id"
  val Name = "name"
  val Description = "description"
  val Author = "author"
}

class Widgets @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends Controller
    with MongoController with ReactiveMongoComponents {

  import controllers.WidgetFields._

  def jsonCollection = reactiveMongoApi.db.collection[JSONCollection]("widgets");
  def bsonCollection = reactiveMongoApi.db.collection[BSONCollection]("widgets");

  def widgetRepo = new WidgetRepoImpl(reactiveMongoApi)

  def index = Action.async { implicit request =>
    widgetRepo.find.map(widgets => Ok(Json.toJson(widgets)))
  }

  def create = Action {
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
        jsonCollection.bulkInsert(posts.toStream, ordered = true).foreach(i => Logger.info("Inserted records into table"))
      }
    }

    Redirect(routes.Widgets.index)
  }

  def destroy = Action {
    jsonCollection.drop.onComplete {
      case _ => Logger.info("Dropped table")
    }
    Redirect(routes.Widgets.index)
  }

  def insert = Action.async(BodyParsers.parse.json) { implicit request =>
    val name = (request.body \ Name).as[String]
    val description = (request.body \ Description).as[String]
    val author = (request.body \ Author).as[String]
    widgetRepo.save(BSONDocument(
      Name -> name,
      Description -> description,
      Author -> author)).map(result => Created)
  }

  def read(id: String) = Action.async { implicit request =>
    widgetRepo.select(BSONDocument(Id -> BSONObjectID(id))).map(widget => Ok(Json.toJson(widget)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    val name = (request.body \ Name).as[String]
    val description = (request.body \ Description).as[String]
    val author = (request.body \ Author).as[String]
    widgetRepo.update(BSONDocument(
      Id -> BSONObjectID(id)),
      BSONDocument("$set" -> BSONDocument(Name -> name, Description -> description, Author -> author))).map(result => Accepted)
  }

  def delete(id: String) = Action.async {
    widgetRepo.remove(BSONDocument(Id -> BSONObjectID(id))).map(result => Accepted)
  }
}
