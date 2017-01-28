package modules

import java.io.File

import com.google.inject.AbstractModule
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.oidc.client.OidcClient
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile
import org.pac4j.saml.client.{ SAML2Client, SAML2ClientConfiguration }
import org.pac4j.play.{ ApplicationLogoutController, CallbackController }
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{ PlayCacheStore, PlaySessionStore }
import play.api.{ Configuration, Environment }

import authorizers.EmailAuthorizer

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheStore])
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId("610296441200-l64orrbu9rdu2rjap2qsfsqtdm3ab65o.apps.googleusercontent.com")
    oidcConfiguration.setSecret("l9K-LG7ICePIukLE9o2e2bml")
    oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration")
    val oidcClient = new OidcClient[OidcProfile](oidcConfiguration)

    val samlConfiguration = new SAML2ClientConfiguration(
      "/Users/twong/samlKeystore.jks",
      "pac4j-demo-passwd",
      "pac4j-demo-passwd",
      "https://dev-880987.oktapreview.com/app/exk9eva61kEtE31bH0h7/sso/saml/metadata");
    samlConfiguration.setMaximumAuthenticationLifetime(3600)
    samlConfiguration.setServiceProviderEntityId("urn:mace:saml:pac4j.org")
    val samlClient = new SAML2Client(samlConfiguration)

    val clients = new Clients("http://localhost:9000/callback", oidcClient, samlClient, new AnonymousClient())

    val config = new Config(clients)
    config.addAuthorizer("email", new EmailAuthorizer())
    config.setHttpActionAdapter(new DefaultHttpActionAdapter())
    bind(classOf[Config]).toInstance(config)

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheStore])

    // callback
    val callbackController = new CallbackController()
    bind(classOf[CallbackController]).toInstance(callbackController)

    // Logout controller
    val logoutController = new ApplicationLogoutController()
    logoutController.setDefaultUrl("/logout-done")
    bind(classOf[ApplicationLogoutController]).toInstance(logoutController)
  }
}
