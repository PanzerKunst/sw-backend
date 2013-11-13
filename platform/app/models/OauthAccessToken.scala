package models

import play.api.libs.json.{Json, JsValue, Writes}


case class OauthAccessToken(token: String,
                            sharedSecret: String)

object OauthAccessToken {
  implicit val writes = new Writes[OauthAccessToken] {
    def writes(oauthAccessToken: OauthAccessToken): JsValue = {
      Json.obj(
        "token" -> oauthAccessToken.token,
        "sharedSecret" -> oauthAccessToken.sharedSecret
      )
    }
  }
}
