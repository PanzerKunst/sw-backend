import play.api.Application
import play.api.GlobalSettings
import services.AuthTasker
import scala.concurrent.duration._
import concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    runAuthNonesCleaner()
  }

  private def runAuthNonesCleaner() {
    //This will schedule to send a msg to the AuthTasker.actor after 0ms repeating every second
    val cancellable = AuthTasker.system.scheduler.schedule(
      0 milliseconds,
      1 second,
      AuthTasker.actor,
      None
    )
  }
}
