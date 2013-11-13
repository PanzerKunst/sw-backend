package services.auth

case class AuthenticationResult(httpReturnCode: Int,
                   errorMessage: Option[String] = None)