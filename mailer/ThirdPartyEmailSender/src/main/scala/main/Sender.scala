package main

import scalikejdbc.config.DBs
import java.util.Timer
import akka.actor.ActorSystem

object Sender {
  val system = ActorSystem("SenderSystem")

  def main(args: Array[String]) {
    DBs.setup()

    // Run the EmailsToSendTasker task after 0ms, repeating every 5 seconds
    new Timer().schedule(EmailsToSendTasker, 0, 5 * 1000)
  }
}
