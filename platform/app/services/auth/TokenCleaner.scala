package services.auth

import java.util.TimerTask
import db.AccountDto
import services.GlobalServices

object TokenCleaner extends TimerTask {
  var isRunning = false

  def run() {
    if (!isRunning) {
      isRunning = true

      val allExistingUsernames = AccountDto.getAllUsernames
      val usernamesToKeep = GlobalServices.authHelper.liveAccessTokens.keys.filter(clientIdentifier => allExistingUsernames.contains(clientIdentifier))

      for ((clientIdentifier, tokenPair) <- GlobalServices.authHelper.liveAccessTokens)
        if (!usernamesToKeep.toList.contains(clientIdentifier))
          GlobalServices.authHelper.liveAccessTokens -= clientIdentifier

      isRunning = false
    }
  }
}
