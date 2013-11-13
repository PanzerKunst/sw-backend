package services

import auth.helper.{RealAuthHelper, MockAuthHelper, AuthHelper}
import play.Play

object GlobalServices {
  val authHelper: AuthHelper = if (Play.application().configuration().getBoolean("auth.isMock")) new MockAuthHelper() else new RealAuthHelper()
}
