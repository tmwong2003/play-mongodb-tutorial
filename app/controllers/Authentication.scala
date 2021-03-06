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

import views.html._

class Authentication @Inject() (val config: Config, val playSessionStore: PlaySessionStore, override val ec: HttpExecutionContext) extends Controller
    with Security[CommonProfile] {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def login = Secure("AnonymousClient") { profiles =>
    Action { request =>
      Ok(views.html.login())
    }
  }

  def loginOidc = Secure("OidcClient", "isAuthenticated") { _ =>
    Action { implicit request =>
      Redirect(routes.Application.index)
    }
  }

  def loginSAML2 = Secure("SAML2Client", "isAuthenticated") { _ =>
    Action { implicit request =>
      Redirect(routes.Application.index)
    }
  }

  def logout = Action { implicit request => Redirect(routes.Application.index)
  }
}
