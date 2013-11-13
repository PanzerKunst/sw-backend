import java.util.Timer
import play.api.Application
import play.api.GlobalSettings
import services.auth.{TokenCleaner, NoneCleaner}
import scala.concurrent.duration._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    // Run the NoneCleaner task after 0ms, repeating every second
    new Timer().schedule(NoneCleaner, 0, 1000)

    // Run the TokenCleaner task after 0ms, repeating every 10 minutes
    new Timer().schedule(TokenCleaner, 0, 10 * 60 * 1000)
  }
}
