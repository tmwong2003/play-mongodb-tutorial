package controllers

import javax.inject.Inject

import scala.collection.JavaConversions._

import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.mvc.{ Action, Controller, RequestHeader }
import play.libs.concurrent.HttpExecutionContext

class Authentication @Inject() (val config: Config, val playSessionStore: PlaySessionStore, override val ec: HttpExecutionContext) extends Controller
    with Security[CommonProfile] {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def login() = Secure("OidcClient", "isAuthenticated") { profiles =>
    Action { implicit request =>
      Ok("Logged in: " + profiles)
    }
  }

  def logout() = Action { implicit request => Ok("Logged out")
  }
}
