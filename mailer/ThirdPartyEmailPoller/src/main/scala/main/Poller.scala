package main

import akka.actor.ActorSystem
import com.typesafe.scalalogging.slf4j.Logging
import scalikejdbc.config.DBs
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory

object Poller extends Logging {
  val system = ActorSystem("PollerSystem")
  val conf = ConfigFactory.load()

  def main(args: Array[String]) {
    DBs.setup()

    //This will schedule to send a msg to the EmailsToPollTasker.actor after 0ms repeating every second
    val cancellable = system.scheduler.schedule(
      0 milliseconds,
      5 seconds,
      EmailsToPollTasker.actor,
      None
    )
  }
}
