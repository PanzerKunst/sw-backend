package main

import scalikejdbc.config.DBs
import java.util.Timer

object Poller {
  def main(args: Array[String]) {
    DBs.setup()

    // Run the EmailsToSendTasker task after 0ms, repeating every 5 seconds
    new Timer().schedule(EmailsToPollTasker, 0, 5 * 1000)
  }
}
