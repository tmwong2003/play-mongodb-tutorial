package controllers

import javax.inject.{ Inject, Singleton }

import scala.concurrent.Future

import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.mvc.{ Action, Controller }
import play.libs.concurrent.HttpExecutionContext

object Application {
  private val _defaultProfile = new AnonymousProfile
}

@Singleton
class Application @Inject() (val config: Config, val playSessionStore: PlaySessionStore, val proxy: controllers.Proxy, override val ec: HttpExecutionContext) extends Controller
    with Security[CommonProfile] {

  def index = Secure("AnonymousClient, OidcClient, SAML2Client") { profiles =>
    /*
     * For this application, we take the first matching authentication
     * client.
     */
    val defaultProfile = new AnonymousProfile()
    profiles.lift(0).getOrElse(Application._defaultProfile) match {
      /*
       * If the user is anonymous (i.e., unauthenticated), redirect to
       * the authentication page.
       */
      case _: AnonymousProfile => Action { request =>
        Redirect(routes.Authentication.login)
      }
      /*
       * If authenticated, serve up the proxied site.
       */
      case profile: CommonProfile => proxy.proxy()
      /*
       * If there's no authentication profile, something is broken in the
       * authentication system, since at the very least we should see
       * an anonymous profile.
       */
      case _ => Action { _ =>
        InternalServerError("Got an authentication system failure")
      }
    }
  }
}
