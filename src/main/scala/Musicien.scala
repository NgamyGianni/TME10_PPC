package upmc.akka.leader

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Map
import DataBaseActor._

case class FromProvider (measure : Measure, id : Int)
case class FromChief (measure : Measure)
case class Start (sys : ActorSystem)
case class Play ()

object Musicien {
     case class Alive (id : Int, count : Int)
     case class Ping ()
     case class WarmUp ()

     var states : Map[Int, Boolean] = Map(0 -> false, 1 -> false, 2 -> false, 3 -> false)
     var pings : Map[Int, Int] = Map(0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0)
}

class Musicien (val id:Int, val terminaux:List[Terminal]) extends Actor {
     import Musicien._
     import Projet._

     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val provider = context.actorOf(Props[ProviderActor], name = "ProviderActor")
     val player = context.actorOf(Props[PlayerActor], name = "PlayerActor")
     val db = context.actorOf(Props[DataBaseActor], name = "DataBaseActor")

     val TIME_BASE = 1800 milliseconds // Ping < WarmUp : 100 < 1800
     val TIME_PING = 100 milliseconds
     val scheduler = context.system.scheduler
     val rand = new scala.util.Random

     var timeout = 30
     var count = 0
     var system = ActorSystem("Tmp")

     val musiciens = terminaux.map(
                         e => e match {
                              case Terminal (id:Int, ip:String, port:Int) => context.actorSelection("akka.tcp://MozartSystem"+id+"@127.0.0.1:"+port+"/user/Musicien"+id)
                         }
                    )

     def receive = {

          // Initialisation
          case Start(sys) => {
               displayActor ! Message ("Musicien " + this.id + " is created")
               system = sys
               self ! Ping
               self ! WarmUp
          }

          case Alive(id, count) => {
               states(id) = true
               pings(id) = count
          }

          case Ping => {
               count += 1

               states = Map(0 -> false, 1 -> false, 2 -> false, 3 -> false)

               musiciens.foreach(
                    musicien => musicien ! Alive(this.id, count)
               )

               scheduler.scheduleOnce(TIME_PING, self, Ping)
          }

          case Play => {
               val de1 = rand.nextInt(5) + 1
               val de2 = rand.nextInt(5) + 1

               provider ! ResultatDe(de1 + de2, this.id)
          }

          case WarmUp => {
               if(states.filter(x => x._2 == false).size >= 3) timeout -= 1
               else {
                    timeout = 10
                    val filteredPings = pings.filter(x => states(x._1))
                    val maxCount = filteredPings.valuesIterator.max
                    val chef = filteredPings.find(_._2 == maxCount) match {case Some((k, v)) => k}

                    if(chef == this.id) {
                         println("Le chef d'orchestre est Musicien "+this.id)
                         self ! Play
                    }
               }

               if(timeout == 0)   {
                    println("KO : System terminate !")
                    system.terminate()
               }
               scheduler.scheduleOnce(TIME_BASE, self, WarmUp)
          }

          case FromChief(measure) => {
               println("Musicien "+this.id + " joue la mesure")
               player ! measure
          }

          case FromProvider(measure, chef) => {
               val lStates = states.filter(x => x._2 && x._1 != chef).toList
               val de = findRandom(0, lStates.size)

               lStates(de) match {
                    case (id : Int, state : Boolean) => musiciens(id) ! FromChief(measure)
               }
          }
     }

     def findRandom(a : Int, b : Int) : Int = {
          val res = rand.nextInt(b)
          if(res >= a) res else findRandom(a, b)
     }
}
