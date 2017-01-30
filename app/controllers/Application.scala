package controllers

import javax.inject.{ Inject, Singleton }

import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, Controller }
import play.libs.concurrent.HttpExecutionContext

@Singleton
class Application @Inject() (val config: Config, val playSessionStore: PlaySessionStore, override val ec: HttpExecutionContext) extends Controller
    with Security[CommonProfile] {

  def index = Secure("AnonymousClient, OidcClient, SAML2Client") { profiles =>
    /* For this application, we take the first matching authentication
     * client.
     */
    profiles.lift(0) match {
      case Some(profile) => Action { request =>
        /* Check the user profile. If the user is anonymous (i.e.,
         * unauthenticated), redirect to the authentication page.
         * Otherwise, greet the user.
         */
        if (profile.getClientName() == "AnonymousClient") {
          Redirect(routes.Authentication.login)
        } else {
          Logger.info(request.session.toString())
          Ok(views.html.index(profile))
        }
      }
      /* If there's no authentication client, something is broken in the
       * authentication system, since at the very least we should see
       * 'AnonymousClient'.
       */
      case _ => Action { request =>
        InternalServerError("Got an authentication system error")
      }
    }
  }
}
