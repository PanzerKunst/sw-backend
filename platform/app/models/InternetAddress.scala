package models

import play.api.libs.json.{Json, JsValue, Writes}

case class InternetAddress(email: String,
                           name: Option[String])

object InternetAddress {
  implicit val writes = new Writes[InternetAddress] {
    def writes(internetAddress: InternetAddress): JsValue = {
      Json.obj(
        "email" -> internetAddress.email,
        "name" -> internetAddress.name
      )
    }
  }
}