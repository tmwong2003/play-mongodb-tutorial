package modules

import com.google.inject.AbstractModule
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile
import org.pac4j.play.{ ApplicationLogoutController, CallbackController }
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{ PlayCacheStore, PlaySessionStore }
import play.api.{ Configuration, Environment }

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheStore])

    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId("610296441200-l64orrbu9rdu2rjap2qsfsqtdm3ab65o.apps.googleusercontent.com")
    oidcConfiguration.setSecret("l9K-LG7ICePIukLE9o2e2bml")
    oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration")
    val oidcClient = new OidcClient[OidcProfile](oidcConfiguration)

    val clients = new Clients("http://localhost:9000/callback", oidcClient)

    val config = new Config(clients)
    config.setHttpActionAdapter(new DefaultHttpActionAdapter())
    bind(classOf[Config]).toInstance(config)

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheStore])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    bind(classOf[CallbackController]).toInstance(callbackController)

    // Logout controller
    val logoutController = new ApplicationLogoutController()
    logoutController.setDefaultUrl("/logout-done")
    bind(classOf[ApplicationLogoutController]).toInstance(logoutController)
  }
}
