package org.chicagoscala.awse.servlet

import se.scalablesolutions.akka.remote.BootableRemoteActorService
import se.scalablesolutions.akka.actor.BootableActorLoaderService
import se.scalablesolutions.akka.camel.service.CamelService
import se.scalablesolutions.akka.config.Config
import se.scalablesolutions.akka.util.{Logging, Bootable}
import se.scalablesolutions.akka.servlet.{Initializer => AkkaInitializer}
import javax.servlet.{ServletContextListener, ServletContextEvent}
 
 /**
  * This initializer overrides a method in Akka's {@link se.scalablesolutions.akka.servlet.Initializer}
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
  */ 
class Initializer extends AkkaInitializer {
     
  // Like Akka's Initializer.contextInitialized, but omits the CamelService mixin.
  override def contextInitialized(e: ServletContextEvent): Unit = 
    loader.boot(true, new BootableActorLoaderService with BootableRemoteActorService)
}