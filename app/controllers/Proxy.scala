package controllers

import javax.inject.Inject

import scala.concurrent.Future

import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.apache.commons.validator.routines.UrlValidator
import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.{ Configuration, Logger }
import play.api.http.HttpEntity
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.mvc.{ Action, Controller }
import play.libs.concurrent.HttpExecutionContext

object Proxy {
  private val _proxied_server_default = "INVALID_SERVER"
  private val _url_validator = UrlValidator.getInstance
}

class Proxy @Inject() (
    val config: Config,
    val applicationConfig: Configuration,
    override val ec: HttpExecutionContext,
    implicit val mat: Materializer,
    val playSessionStore: PlaySessionStore,
    val ws: WSClient) extends Controller with Security[CommonProfile] {
  import Proxy._

  private val _config = applicationConfig.getConfig("proxy").getOrElse(Configuration())
  /*
   * TODO: I think this should throw an exception about a missing config
   * variable.
   */
  private val _proxied_server = _config.getString("backend_server").getOrElse(_proxied_server_default)

  def proxy(proxied_path: String = "") = Secure("OidcClient, SAML2Client") { _ =>
    Action.async {
      val proxied_url = "%s/%s".format(_proxied_server, proxied_path)
      Logger.info("Got request to proxy through to %s".format(proxied_url))
      if (!_url_validator.isValid(proxied_url)) {
        Logger.warn("Got invalid path: %s".format(proxied_path))
        Future.apply(BadRequest)
      } else {
        ws.url(proxied_url).withMethod("GET").stream().map {
          case StreamedResponse(response, body) =>
            if (response.status == 200) {
              /*
          	 * If the response from the backend has no content type, assume
          	 * a generic stream.
           	 */
              val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/octet-stream")
              /*
             * If we have a content length, we can stream the response;
             * otherwise, we chunk the response.
             */
              response.headers.get("Content-Length") match {
                case Some(Seq(length)) =>
                  Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
                case _ =>
                  Ok.chunked(body).as(contentType)
              }
            } else {
              BadGateway
            }
        }
      }
    }
  }
}
