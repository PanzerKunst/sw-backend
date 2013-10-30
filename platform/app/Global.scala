import play.api.Application
import play.api.GlobalSettings
import services.AuthTasker
import scala.concurrent.duration._
import concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    runAuthNonesCleaner()
    runAuthTokenCleaner()
  }

  private def runAuthNonesCleaner() {
    //This will schedule to send a msg to the AuthTasker.nonesCleanerActor after 0ms repeating every second
    val cancellable = AuthTasker.system.scheduler.schedule(
      0 milliseconds,
      1 second,
      AuthTasker.nonesCleanerActor,
      None
    )
  }

  private def runAuthTokenCleaner() {
    //This will schedule to send a msg to the AuthTasker.tokenCleanerActor after 0ms repeating every 10 minutes
    val cancellable = AuthTasker.system.scheduler.schedule(
      0 milliseconds,
      10 minutes,
      AuthTasker.tokenCleanerActor,
      None
    )
  }
}
