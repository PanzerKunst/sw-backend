package models

import play.api.libs.json.{Writes, JsValue, Json}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

case class Account(@JsonDeserialize(contentAs = classOf[java.lang.Long])
                   id: Option[Long],

                   firstName: String,
                   lastName: String,
                   username: String,
                   password: String,
                   publicKey: String,
                   privateKey: Option[String])

object Account {
  implicit val writes = new Writes[Account] {
    def writes(user: Account): JsValue = {
      Json.obj(
        "id" -> user.id,
        "firstName" -> user.firstName,
        "lastName" -> user.lastName,
        "username" -> user.username,
        "publicKey" -> user.publicKey
      )
    }
  }
}
