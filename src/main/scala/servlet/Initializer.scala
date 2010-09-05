package org.chicagoscala.awse.servlet

import se.scalablesolutions.akka.remote.BootableRemoteActorService
import se.scalablesolutions.akka.actor.BootableActorLoaderService
import se.scalablesolutions.akka.servlet.{Initializer => AkkaInitializer}
import javax.servlet.ServletContextEvent
 
 /**
  * This initializer overrides a method in Akka's se.scalablesolutions.akka.servlet.Initializer
  * to omit the initialization of Camel, which forces unnecessary dependent jars to be deployed if you 
  * aren't using Camel. So, the listener tag in your web.xml becomes:
  *
  *<web-app>
  * ...
  *  <listener>
  *    <listener-class>org.chicagoscala.awse.servlet.Initializer</listener-class>
  *  </listener>
  * ...
  *</web-app>
  * TODO: This was useful for Akka .8 and .9, but might be obsolete for 0.10.
  */ 
class Initializer extends AkkaInitializer {
     
  // Like Akka's Initializer.contextInitialized, but omits the CamelService mixin.
  override def contextInitialized(e: ServletContextEvent): Unit = 
    loader.boot(true, new BootableActorLoaderService with BootableRemoteActorService)
}