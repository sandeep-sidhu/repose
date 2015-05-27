package org.openrepose.filters.Keystonev2

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.keystonev2.config.KeystoneV2Config

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                  akkaServiceClient: AkkaServiceClient,
                                  datastoreService: DatastoreService)
  extends Filter
  with UpdateListener[KeystoneV2Config]
  with HttpDelegationManager
  with LazyLogging {

  private val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  var configurationFile: String = DEFAULT_CONFIG
  var configuration: KeystoneV2Config = _
  var initialized = false

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing Keystone V2 Filter using config $configurationFile")
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[KeystoneV2Config]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = ???

  override def configurationUpdated(configurationObject: KeystoneV2Config): Unit = {
    configuration = configurationObject
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}