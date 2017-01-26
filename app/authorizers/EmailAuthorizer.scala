package authorizers

import org.pac4j.core.authorization.authorizer.ProfileAuthorizer
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile
import play.api.Logger

class EmailAuthorizer extends ProfileAuthorizer[CommonProfile] {

  def isAuthorized(context: WebContext, profiles: java.util.List[CommonProfile]): Boolean = {
    return isAnyAuthorized(context, profiles)
  }

  def isProfileAuthorized(context: WebContext, profile: CommonProfile): Boolean = {
    if (profile == null) {
      Logger.warn("Got null user profile during profile authorization")
      false
    } else {
      Logger.warn("Got " + profile.getEmail() + " user profile during authorizaton")
      profile.getEmail() == "tmw@tmwong.org"
    }
  }
}
