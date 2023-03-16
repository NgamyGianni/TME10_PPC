package upmc.akka.leader

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Checker {
    var musiciens = List[Int]()
}

class Checker () extends Actor {
     import DataBaseActor._
     import Checker._

     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val provider = context.actorOf(Props[ProviderActor], name = "ProviderActor")
     val player = context.actorOf(Props[PlayerActor], name = "PlayerActor")
     val db = context.actorOf(Props[DataBaseActor], name = "DataBaseActor")

     val TIME_BASE = 1800 milliseconds
     val scheduler = context.system.scheduler
     val rand = new scala.util.Random

     def receive = {
        case id : Int => {
            musiciens = id :: musiciens

            println(musiciens)
        }
     }
}
