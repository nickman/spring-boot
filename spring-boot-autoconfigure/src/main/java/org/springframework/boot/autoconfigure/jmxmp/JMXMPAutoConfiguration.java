/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jmxmp;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.MBeanExportConfiguration.SpecificPlatform;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.support.MBeanServerFactoryBean;

/**
 * Autoconfiguration support for JMXMP.
 * @author nwhitehead TODO: Add support for authentication and authorization
 */
@Configuration
@ConditionalOnClass({ JMXMPConnectorServer.class })
@ConditionalOnProperty(prefix = "spring.jmxmp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JMXMPProperties.class)
public class JMXMPAutoConfiguration
		implements ApplicationListener<ApplicationContextEvent> {
	private JMXMPConnectorServer connectorServer;
	private ObjectName objectName = null;
	private volatile MBeanServer server = null;
	@Autowired
	private JMXMPProperties jmxmpProperties;

	private static final Log logger = LogFactory.getLog(JMXMPAutoConfiguration.class);

	/**
	 * Starts the connector server on app ctx refresh and stops it on close.
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(final ApplicationContextEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			// Start the connector server in a daemon thread
			final CountDownLatch latch = new CountDownLatch(1);
			final Thread starter = new Thread("JMXMPAutoConfiguration-Starter") {
				@Override
				public void run() {
					logger.info("Starting JMXMPConnectorServer...");
					try {
						JMXMPAutoConfiguration.this.connectorServer.start();
						logger.info("Started JMXMPConnectorServer on ["
								+ JMXMPAutoConfiguration.this.connectorServer.getAddress()
								+ "]");
					}
					catch (IOException iex) {
						logger.error("Failed to start JMXMPConnectorServer on ["
								+ JMXMPAutoConfiguration.this.connectorServer.getAddress()
								+ "]", iex);
					}
					finally {
						latch.countDown();
					}
				}
			};
			starter.setDaemon(true);
			starter.start();
			try {
				if (!latch.await(5, TimeUnit.SECONDS)) {
					logger.warn("JMXMPConnectorServer not started after 5 seconds");
				}
			}
			catch (InterruptedException ie) {
				logger.warn("JMXMPConnectorServer starter thread interrupted", ie);
			}
		}
		else if (event instanceof ContextClosedEvent) {
			if (this.connectorServer != null) {
				try {
					this.connectorServer.stop();
					logger.info("Stopped JMXMPConnectorServer");
				}
				catch (IOException e) {
					/* No Op */}
			}
			if (this.objectName != null && this.server != null) {
				try {
					this.server.unregisterMBean(this.objectName);
				}
				catch (Exception e) {
					/* No Op */}
			}

		}
	}

	@Bean
	@ConditionalOnMissingBean(MBeanServer.class)
	public MBeanServer mbeanServer() {
		if (this.server != null) {
			return this.server;
		}
		SpecificPlatform platform = SpecificPlatform.get();
		if (platform != null) {
			return platform.getMBeanServer();
		}
		MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
		String configuredDomain = this.jmxmpProperties.getDomain();
		final boolean locateExisting = configuredDomain == null
				|| JMXMPProperties.DEFAULT_DOMAIN.equals(configuredDomain);
		configuredDomain = this.jmxmpProperties.determineDefaultDomain();
		factory.setDefaultDomain(configuredDomain);
		factory.setLocateExistingServerIfPossible(locateExisting);
		factory.afterPropertiesSet();

		this.server = factory.getObject();
		return this.server;
	}

	@Bean
	@ConditionalOnMissingBean
	public JMXMPConnectorServer jmxmpConnector(final MBeanServer mbeanServer)
			throws IOException {
		String serviceURL = this.jmxmpProperties.determineServiceURL();
		JMXServiceURL jmxServiceURL = new JMXServiceURL(serviceURL);
		this.connectorServer = new JMXMPConnectorServer(jmxServiceURL, null, mbeanServer);
		this.objectName = this.jmxmpProperties.determineObjectName();
		if (this.objectName != null) {
			try {
				mbeanServer.registerMBean(this.connectorServer, this.objectName);
			}
			catch (Exception ex) {
				logger.error(
						"Failed to register JMXMPConnectorServer management interface at ["
								+ this.objectName + "]",
						ex);
			}
		}
		return this.connectorServer;
	}

}
