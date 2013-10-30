package services

import akka.actor.{Actor, Props, ActorSystem}
import java.util.{Calendar, GregorianCalendar}
import play.Play
import db.AccountDto

object AuthTasker {
  val system = ActorSystem("AuthTaskerSystem")

  val nonesCleanerActor = system.actorOf(Props(new Actor {
    def receive = {
      case _ =>
        val lastRetainedTime = new GregorianCalendar()
        lastRetainedTime.add(Calendar.SECOND, -Play.application().configuration().getString("auth.retainedTimeframeInSeconds").toInt)

        val timestampOfLastRetainedTimeInSeconds = lastRetainedTime.getTimeInMillis / 1000

        for ((nonce, timestamp) <- AuthHelper.usedNonces)
          if (timestamp < timestampOfLastRetainedTimeInSeconds)
            AuthHelper.usedNonces -= nonce
    }
  }))

  val tokenCleanerActor = system.actorOf(Props(new Actor {
    def receive = {
      case _ =>
        val allExistingUsernames = AccountDto.getAllUsernames
        val usernamesToKeep = AuthHelper.liveAccessTokens.keys.filter(clientIdentifier => allExistingUsernames.contains(clientIdentifier))

        for ((clientIdentifier, tokenPair) <- AuthHelper.liveAccessTokens)
          if (!usernamesToKeep.toList.contains(clientIdentifier))
            AuthHelper.liveAccessTokens -= clientIdentifier
    }
  }))
}
