package main

import akka.actor.ActorSystem
import com.typesafe.scalalogging.slf4j.Logging
import scalikejdbc.config.DBs
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

object Poller extends Logging {
  val system = ActorSystem("PollerSystem")

  def main(args: Array[String]) {
    DBs.setup()

    //This will schedule to send a msg to the EmailsToPollTasker.actor after 0ms repeating every second
    val cancellable = system.scheduler.schedule(
      0 milliseconds,
      1 hour,
      EmailsToPollTasker.actor,
      None
    )
  }
}
