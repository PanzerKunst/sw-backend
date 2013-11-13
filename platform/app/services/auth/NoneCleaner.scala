package services.auth

import java.util.{Calendar, GregorianCalendar, TimerTask}
import play.api.Logger
import services.GlobalServices
import play.Play

object NoneCleaner extends TimerTask {
  val authRetainedTimeframeInSeconds = Play.application().configuration().getString("auth.retainedTimeframeInSeconds").toInt

  var isRunning = false

  def run() {
    Logger.info("isNonesCleanerRunning: " + isRunning)

    if (!isRunning) {
      isRunning = true

      val lastRetainedTime = new GregorianCalendar()
      lastRetainedTime.add(Calendar.SECOND, -authRetainedTimeframeInSeconds)

      val timestampOfLastRetainedTimeInSeconds = lastRetainedTime.getTimeInMillis / 1000

      Logger.info("timestampOfLastRetainedTimeInSeconds: " + timestampOfLastRetainedTimeInSeconds)

      for ((nonce, timestamp) <- GlobalServices.authHelper.usedNonces)
        if (timestamp < timestampOfLastRetainedTimeInSeconds)
          GlobalServices.authHelper.usedNonces -= nonce

      isRunning = false
    }
  }
}
