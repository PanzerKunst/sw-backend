package models

import play.api.libs.json.{Writes, JsValue, Json}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

case class User(
                 @JsonDeserialize(contentAs = classOf[java.lang.Long])
                 id: Option[Long] = None,

                 firstName: String,
                 lastName: String,
                 username: String,
                 publicKey: String)

object User {
  implicit val writes = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(
        "id" -> user.id,
        "firstName" -> user.firstName,
        "lastName" -> user.lastName,
        "publicKey" -> user.publicKey
      )
    }
  }
}