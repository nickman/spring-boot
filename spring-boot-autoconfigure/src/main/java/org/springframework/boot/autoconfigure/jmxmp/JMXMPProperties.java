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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for a JMXMP connector server.
 * @author nwhitehead TODO: Add support for authentication and authorization. See:
 * https://blogs.oracle.com/lmalventosa/entry/jmx_authentication_authorization
 */
@ConfigurationProperties(prefix = "spring.jmxmp")
public class JMXMPProperties {
	/**
	 * Default listening port used when the configured port is {@code null}.
	 */
	public static final int DEFAULT_PORT = 1892;

	/**
	 * Default binding interface used when the configured interface is {@code null}.
	 */
	public static final String DEFAULT_BIND = "127.0.0.1";

	/**
	 * Default MBeanServer default domain name when the configured domain is {@code null}.
	 */
	public static final String DEFAULT_DOMAIN = "DefaultDomain";

	/**
	 * JMXServiceURL template used.
	 */
	public static final String JMX_SERVICE_URL = "service:jmx:jmxmp://%s:%s";

	/**
	 * The default connector server MBean's ObjectName.
	 */
	public static final String DEFAULT_OBJECT_NAME = "javax.management.remote.jmxmp:service=JMXMPConnectorServer";

	/**
	 * The default mbean registration disablement.
	 */
	public static final boolean DEFAULT_DISABLE_MBEAN = false;

	/**
	 * The environment constant for the JAAS login module name.
	 */
	public static final String JMX_JAAS_MODULE = "jmx.remote.x.login.config";

	/**
	 * The environment constant for the authorization rights file or URL or resource name.
	 */
	public static final String JMX_AUTH_FILE = "jmx.remote.x.access.file";

	/**
	 * The environment constant for the authentication password file or URL or resource
	 * name.
	 */
	public static final String JMX_PASSWD_FILE = "jmx.remote.x.password.file";

	/**
	 * The environment constant for the JMXAuthenticator class name.
	 */
	public static final String JMX_AUTHENTICATOR = "jmx.remote.authenticator";

	/**
	 * The default security provider class name.
	 */
	public static final String PROVIDER = "com.sun.security.sasl.Provider";

	/**
	 * The default security profile.
	 */
	public static final String PROFILE = "TLS SASL/PLAIN";

	private static final Log logger = LogFactory.getLog(JMXMPProperties.class);

	/**
	 * The JMXMP connector server binding interface.
	 */
	private String bindInterface;

	/**
	 * The JMXMP connector server listening port.
	 */
	private Integer port = null;

	/**
	 * The MBeanServer's default domain name.
	 */
	private String domain;

	/**
	 * Indicates if the JMXMP connector server's management interface should NOT be
	 * registered.
	 */
	private boolean disableMBean = DEFAULT_DISABLE_MBEAN;

	/**
	 * The MBeanServer's management interface JMX ObjectName.
	 */
	private String objectName;

	/**
	 * Returns the interface to bind to.
	 * @return the bindInterface
	 */
	public String getBindInterface() {
		return this.bindInterface;
	}

	/**
	 * Sets the interface to bind to.
	 * @param bindInterface the bindInterface to set
	 */
	public void setBindInterface(String bindInterface) {
		this.bindInterface = bindInterface;
	}

	/**
	 * Returns the MBeanServer's default domain.
	 * @return the default domain
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Sets the MBeanServer's default domain.
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Returns the listening port.
	 * @return the port
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the listening port.
	 * @param port the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Returns the configured JMXServiceURL.
	 * @return the configured JMXServiceURL
	 */
	public String determineServiceURL() {
		return String.format(JMX_SERVICE_URL,
				(this.bindInterface == null ? DEFAULT_BIND : this.bindInterface),
				(this.port == null ? DEFAULT_PORT : this.port));
	}

	/**
	 * Returns the configured MBeanServer default domain name.
	 * @return the default domain name
	 */
	public String determineDefaultDomain() {
		return this.domain == null ? DEFAULT_DOMAIN : this.domain;
	}

	/**
	 * Returns the configured or default management interface ObjectName.
	 * @return the ObjectName
	 */
	public ObjectName determineObjectName() {
		if (this.disableMBean) {
			return null;
		}
		final String name = this.objectName == null ? DEFAULT_OBJECT_NAME
				: this.objectName.trim();
		try {
			return new ObjectName(name);
		}
		catch (Exception ex) {
			logger.warn("Invalid ObjectName for JMXMPConnectorServer: [" + name
					+ "]. Disabling management interface. Error was:" + ex);
			return null;
		}
	}

	/**
	 * Returns the configured management interface JMX ObjectName.
	 * @return the objectName
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Sets the management interface JMX ObjectName.
	 * @param objectName the objectName to set
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * Indicates if the JMXMP connector server's management interface should NOT be
	 * registered.
	 * @return true if the management interface should NOT be registered, false otherwise.
	 */
	public boolean isDisableMBean() {
		return this.disableMBean;
	}

	/**
	 * Specifies if the JMXMP connector server's management interface should NOT be
	 * registered.
	 * @param disableMBean true to disable, false otherise
	 */
	public void setDisableMBean(boolean disableMBean) {
		this.disableMBean = disableMBean;
	}

	/**
	 * Enumerates the possible outcomes of determining the source type for an arbitrary
	 * string intended to reference a properties source.
	 */
	public static enum ResourceType {
		FILE, URL, RESOURCE, UNKNOWN;
	}

	public static class PropertiesResource {
		final ResourceType resourceType;
		final String resourceName;
		final File file;
		final URL url;

		public static final String PID = ManagementFactory.getRuntimeMXBean().getName()
				.split("@")[0];
		public static final String TMP_DIR = System.getProperty("java.io.tmpdir");

		private PropertiesResource(final String name) {
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("The passed name was null or empty");
			}
			this.resourceName = name.trim();
			if (isURL(this.resourceName)) {
				this.resourceType = ResourceType.URL;
				this.url = toURL(this.resourceName);
				this.file = null;
			}
			else if (isFile(this.resourceName)) {
				this.resourceType = ResourceType.FILE;
				this.url = null;
				this.file = new File(this.resourceName);
			}
			else if (isResource(this.resourceName)) {
				this.resourceType = ResourceType.RESOURCE;
				this.url = JMXMPProperties.class.getResource(this.resourceName);
				this.file = null;
			}
			else {
				this.resourceType = ResourceType.UNKNOWN;
				this.url = null;
				this.file = null;
			}
		}

		public String toFile(final String prefix, final String suffix) {
			if (this.resourceType == ResourceType.FILE) {
				return this.file.getAbsolutePath();
			}
			if (this.resourceType == ResourceType.UNKNOWN) {
				throw new IllegalStateException(
						"Unrecognized resource type [" + this.resourceName + "]");
			}
			FileOutputStream fos = null;
			InputStream is = null;
			final byte[] buff = new byte[2048];
			int bytesRead = -1;
			try {
				final File copiedFile = File.createTempFile(PID + "-" + prefix, suffix,
						new File(TMP_DIR));
				copiedFile.deleteOnExit();
				fos = new FileOutputStream(copiedFile, false);
				is = this.url.openStream();
				while ((bytesRead = is.read(buff)) != -1) {
					fos.write(buff, 0, bytesRead);
				}
				fos.flush();
				fos.close();
				fos = null;
				is.close();
				is = null;
				return copiedFile.getAbsolutePath();
			}
			catch (IOException e) {
				throw new RuntimeException(
						"Failed to create temp file [" + prefix + "/" + suffix + "]", e);
			}
			finally {
				if (fos != null) {
					try {
						fos.close();
					}
					catch (Exception x) {
						/* No Op */ }
				}
				if (is != null) {
					try {
						is.close();
					}
					catch (Exception x) {
						/* No Op */ }
				}
			}
		}

		public InputStream inputStream() {
			switch (this.resourceType) {
			case FILE:
				try {
					return new FileInputStream(this.file);
				}
				catch (FileNotFoundException e) {
					throw new IllegalStateException(
							"Failed to read file [" + this.file + "] (Not found)", e);
				}
			case RESOURCE:
			case URL:
				try {
					return this.url.openStream();
				}
				catch (IOException e) {
					throw new IllegalStateException(
							"Failed to read URL [" + this.url + "]", e);
				}
			case UNKNOWN:
				throw new IllegalStateException(
						"Unrecognized resource type [" + this.resourceName + "]");
			default:
				throw new IllegalStateException(
						"Programmer Error. Unrecognized resource type ["
								+ this.resourceName + "] assigned type ["
								+ this.resourceType + "]");
			}
		}

		public static boolean isFile(final String name) {
			File f = new File(name);
			return f.exists() && f.isFile() && f.canRead();
		}

		public static boolean isResource(final String name) {
			URL url = JMXMPProperties.class.getResource(name);
			return url != null;
		}

		public static boolean isURL(final String name) {
			try {
				new URL(name);
				return true;
			}
			catch (MalformedURLException uex) {
				return false;
			}
		}

		public static URL toURL(final String name) {
			try {
				return new URL(name);
			}
			catch (MalformedURLException uex) {
				throw new RuntimeException("Invalid URL [" + name + "]", uex);

			}
		}

	}

}
