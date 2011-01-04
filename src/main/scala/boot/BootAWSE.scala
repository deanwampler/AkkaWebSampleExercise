package org.chicagoscala.awse.boot
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.finance._
import org.chicagoscala.awse.server.rest._
import akka.actor._
import akka.actor.Actor._
import akka.config.Config._
import akka.config.Supervision._
import akka.util.Logging

class BootAWSE {

  // Some global initialization:
  InstrumentAnalysisServerSupervisor.init
  
  val factory = Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Throwable]), 5, 5000),
      Supervise(
        actorOf(new InstrumentAnalysisServerSupervisor).start,
        Permanent) :: 
      Nil))
}
