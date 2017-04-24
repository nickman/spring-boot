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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * As yet unused authentication callback handler.
 * @author nwhitehead
 */
public class PropertiesFileCallbackHandler implements CallbackHandler {

	protected final Map<String, String> userPasswords;

	/**
	 * Creates a new PropertiesFileCallbackHandler.
	 * @param authSource The authentication source. Can be a file, a URL or a classpath
	 * resource containing java properties.
	 */
	public PropertiesFileCallbackHandler(final String authSource) {
		if (authSource == null || authSource.trim().isEmpty()) {
			throw new IllegalArgumentException(
					"The authentication source was null or empty");
		}
		this.userPasswords = load(authSource);
		if (this.userPasswords == null) {
			throw new IllegalArgumentException(
					"Failed to load auth source [" + authSource + "]");
		}
	}

	protected static Map<String, String> load(final String authSource) {
		final String src = authSource.trim();
		final File file = new File(src);
		if (file.exists()) {
			return load(file);
		}
		else {
			URL url = toURL(src);
			if (url != null) {
				return load(url);
			}
			url = PropertiesFileCallbackHandler.class.getClassLoader().getResource(src);
			return load(url);
		}
	}

	protected static URL toURL(final String src) {
		try {
			return new URL(src);
		}
		catch (Exception ex) {
			return null;
		}
	}

	protected static Map<String, String> load(final URL url) {
		final Properties p = new Properties();
		InputStream is = null;
		try {
			is = url.openStream();
			p.load(is);
			final Map<String, String> map = new HashMap<String, String>(p.size());
			for (String key : p.stringPropertyNames()) {
				final String value = p.getProperty(key);
				if (value == null) {
					continue;
				}
				map.put(key.trim(), value.trim());
			}
			return map;
		}
		catch (Exception ex) {
			return null;
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception x) {
					/* No Op */ }
			}
		}
	}

	protected static Map<String, String> load(final File file) {
		final Properties p = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			p.load(is);
			final Map<String, String> map = new HashMap<String, String>(p.size());
			for (String key : p.stringPropertyNames()) {
				final String value = p.getProperty(key);
				if (value == null) {
					continue;
				}
				map.put(key.trim(), value.trim());
			}
			return map;
		}
		catch (Exception ex) {
			return null;
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception x) {
					/* No Op */ }
			}
		}

	}

	/**
	 * The callback handler for authentication. Currently only handles authentication, not
	 * authorization.
	 * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.
	 * Callback[])
	 */
	@Override
	public void handle(final Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {
		NameCallback nameCallback = null;
		PasswordCallback passCallback = null;
		for (Callback c : callbacks) {
			if (c == null) {
				continue;
			}
			if (c instanceof NameCallback) {
				nameCallback = (NameCallback) c;
			}
			else if (c instanceof PasswordCallback) {
				passCallback = (PasswordCallback) c;
			}
		}
		String name = nameCallback != null ? nameCallback.getName() : null;
		char[] pass = passCallback != null ? passCallback.getPassword() : null;

	}

}
