package models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

case class User(
                 @JsonDeserialize(contentAs = classOf[java.lang.Long])
                 id: Option[Long] = None,

                 firstName: Option[String] = None,
                 lastName: Option[String] = None,
                 username: Option[String] = None,
                 publicKey: Option[String] = None)