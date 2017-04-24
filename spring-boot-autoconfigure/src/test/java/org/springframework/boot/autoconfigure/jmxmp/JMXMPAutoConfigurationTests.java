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

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author nwhitehead
 */
public class JMXMPAutoConfigurationTests {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
			if (this.context.getParent() != null) {
				((ConfigurableApplicationContext) this.context.getParent()).close();
			}
		}
	}

	@Before
	public void setupConfig() {
		System.clearProperty("spring.jmxmp.port");
		System.clearProperty("spring.jmxmp.bindInterface");
		System.clearProperty("spring.jmxmp.disableMBean");
		System.clearProperty("spring.jmxmp.objectName");
	}

	@Test
	public void testDefaultConnectorServer() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JMXMPAutoConfiguration.class);
		this.context.refresh();
		JMXMPConnectorServer connector = this.context.getBean(JMXMPConnectorServer.class);
		MBeanServer mbeanServer = this.context.getBean(MBeanServer.class);
		assertThat(connector).isNotNull();
		assertThat(mbeanServer).isNotNull();
		JMXServiceURL serviceURL = connector.getAddress();
		assertThat(serviceURL.getPort()).isEqualTo(JMXMPProperties.DEFAULT_PORT);
		assertThat(serviceURL.getHost()).isEqualTo(JMXMPProperties.DEFAULT_BIND);
		assertThat(serviceURL.getProtocol()).isEqualTo("jmxmp");
		assertThat(mbeanServer == connector.getMBeanServer()).isTrue();
		assertThat(connector.getMBeanServer().getDefaultDomain())
				.isEqualTo(JMXMPProperties.DEFAULT_DOMAIN);
		assertThat(isMBeanRegistered(connector.getMBeanServer(),
				JMXMPProperties.DEFAULT_OBJECT_NAME)).isTrue();
		// try {
		// Thread.currentThread().join();
		// }
		// catch (Exception x) {
		// }
	}

	@Test
	public void testConfiguredConnectorServer() {
		final int port = SocketUtils.findAvailableTcpPort(1025);
		final String objectName = JMXMPProperties.DEFAULT_OBJECT_NAME + ",port=" + port;
		System.setProperty("spring.jmxmp.port", "" + port);
		System.setProperty("spring.jmxmp.bindInterface", "0.0.0.0");
		System.setProperty("spring.jmxmp.domain", "foo");
		System.setProperty("spring.jmxmp.objectName", objectName);
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JMXMPAutoConfiguration.class);
		this.context.refresh();
		JMXMPConnectorServer connector = this.context.getBean(JMXMPConnectorServer.class);
		MBeanServer mbeanServer = this.context.getBean(MBeanServer.class);
		assertThat(connector).isNotNull();
		assertThat(mbeanServer).isNotNull();
		JMXServiceURL serviceURL = connector.getAddress();
		assertThat(serviceURL.getPort()).isEqualTo(port);
		assertThat(serviceURL.getHost()).isEqualTo("0.0.0.0");
		assertThat(serviceURL.getProtocol()).isEqualTo("jmxmp");
		assertThat(mbeanServer == connector.getMBeanServer()).isTrue();
		assertThat(connector.getMBeanServer().getDefaultDomain()).isEqualTo("foo");
		assertThat(isMBeanRegistered(connector.getMBeanServer(), objectName)).isTrue();

	}

	@Test
	public void testNoMBeanConnectorServer() {
		System.setProperty("spring.jmxmp.disableMBean", "true");
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JMXMPAutoConfiguration.class);
		this.context.refresh();
		JMXMPConnectorServer connector = this.context.getBean(JMXMPConnectorServer.class);
		assertThat(connector).isNotNull();
		assertThat(isMBeanRegistered(connector.getMBeanServer(),
				JMXMPProperties.DEFAULT_OBJECT_NAME)).isFalse();
	}

	@Test
	public void testConnectorServerLifecycle() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JMXMPAutoConfiguration.class);

		this.context.refresh();
		JMXMPConnectorServer connector = this.context.getBean(JMXMPConnectorServer.class);
		assertThat(connector).isNotNull();
		assertThat(connector.isActive()).isTrue();
		try {
			this.context.close();
			assertThat(connector.isActive()).isFalse();
		}
		finally {
			try {
				this.context.close();
			}
			catch (Exception ex) {
				/* No Op */ }
			this.context = null;
		}
	}

	protected ObjectName objectName(final String name) {
		try {
			return new ObjectName(name);
		}
		catch (MalformedObjectNameException me) {
			throw new RuntimeException("Failed to build ObjectName from [" + name + "]",
					me);
		}
	}

	protected boolean isMBeanRegistered(final MBeanServer server, final String name) {
		return server.isRegistered(objectName(name));
	}

	@SuppressWarnings("unchecked")
	protected <T> T getAttribute(final MBeanServer server, final String objectName,
			final String attributeName) {
		try {
			return (T) server.getAttribute(objectName(objectName), attributeName);
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to get attribute [" + attributeName
					+ "] from MBean at [" + objectName + "]", ex);

		}
	}

}
