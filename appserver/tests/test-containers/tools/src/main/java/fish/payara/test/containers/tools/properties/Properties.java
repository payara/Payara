/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tools.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Better alternative of the {@link java.util.Properties}. Does not allow duplicities.
 *
 * @author David Matějček
 */
public class Properties {

    private static final Logger LOG = LoggerFactory.getLogger(Properties.class);

    private final Map<String, String> properties;


    /**
     * @param propertyFile source *.properties
     */
    public Properties(final File propertyFile) {
        validate(propertyFile);
        this.properties = loadProperties(propertyFile);
        LOG.info("Configuration file {} has been loaded", propertyFile);
    }


    /**
     * @param classpathResource source *.properties to be obtained via the thread context
     *            classloader.
     */
    public Properties(final String classpathResource) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(classpathResource)) {
            Objects.requireNonNull(stream, classpathResource);
            this.properties = loadProperties(stream);
        } catch (final IOException e) {
            throw new PropertiesLoadingException("Cannot load test configuration file.", e);
        }
        LOG.info("Configuration has been loaded from input stream.");
    }


    private Properties() {
        this.properties = new HashMap<>();
    }


    /**
     * Check if the file is not null, exists and can be read.
     * Used by all constructors.
     *
     * @param file
     * @throws IllegalArgumentException if some check failed.
     */
    protected void validate(final File file) throws IllegalArgumentException {
        LOG.trace("validate(file={})", file);

        if (file == null) {
            throw new IllegalArgumentException("The file must not be null!");
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException(String.format("The file '%s' cannot be read!", file.getAbsolutePath()));
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException(
                String.format("The parameter '%s' is not a file!", file.getAbsolutePath()));
        }
    }


    /**
     * Loads all the properties. Must be called only from {@link #refresh(boolean)}.
     */
    private static Map<String, String> loadProperties(final File propertyFile) {
        LOG.debug("loadProperties(propertyFile={})", propertyFile);
        try (FileInputStream in = new FileInputStream(propertyFile)) {
            return loadProperties(in);
        } catch (final IOException | PropertiesLoadingException e) {
            throw new PropertiesLoadingException(propertyFile, e);
        }
    }


    /**
     * Loads all the properties. Must be called only from {@link #refresh(boolean)}.
     */
    private static Map<String, String> loadProperties(final InputStream source) {
        LOG.debug("loadProperties(source)");
        try {
            final java.util.Properties newprop = new java.util.Properties();
            newprop.load(source);
            return newprop.entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        } catch (final IOException e) {
            throw new PropertiesLoadingException("Could not load properties from the input stream.", e);
        }
    }


    /**
     * @return the size of the internal property storage
     */
    public int getSize() {
        return this.properties.size();
    }


    /**
     * @return a set of property names. Never null.
     */
    public Set<String> getPropertyNames() {
        return this.properties.keySet();
    }


    /**
     * @param key
     * @return a value for the property <code>key</code>.
     */
    public String getString(final String key) {
        return this.properties.get(key);
    }


    /**
     * @param key
     * @param defaultValue
     * @return a value of the property <code>key</code>. If this property is not found, then the
     *         <code>defaultValue</code> is returned.
     */
    public String getString(final String key, final String defaultValue) {
        final String value = this.properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    /**
     * @return true if properties were not loaded or the file was empty.
     */
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }


    /**
     * Uses the {@link #getPropertyNames()} and {@link #getString(String)} to construct
     * a new properties.
     *
     * @param keyPrefix - a part of the key to be selected
     * @param cutPrefix - if true, keyPrefix is cut
     * @return only properties starting with keyPrefix, never null.
     */
    public Properties getSubset(final String keyPrefix, final boolean cutPrefix) {
        final Properties subset = new Properties();
        for (final String key : getPropertyNames()) {
            if (key.startsWith(keyPrefix)) {
                final String value = getString(key);
                if (cutPrefix) {
                    subset.put(key.substring(keyPrefix.length()), value);
                } else {
                    subset.put(key, value);
                }
            }
        }

        return subset;
    }


    /**
     * @param propertyKey
     * @param regex regex parameter for {@link String#split(String)}
     * @return String array constructed from the string value under the key
     */
    public String[] getStringArray(final String propertyKey, final String regex) {
        final String reportTypes = getString(propertyKey);
        if (reportTypes == null) {
            return new String[0];
        }
        return reportTypes.split(regex);
    }


    /**
     * @param key
     * @param defaultValue
     * @return
     *         <ul>
     *         <li>true if the property is set and it's value is true/on/yes,
     *         <li>defaultValue if it is not set,
     *         <li>false is set to anything else (including empty string)
     */
    public boolean getBoolean(final String key, final boolean defaultValue) {
        final String value = getString(key);

        if (value == null) {
            return defaultValue;
        }

        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }


    /**
     * Note: May throw a NumberFormatException if the property is not set properly (cannot parse int
     * value)
     *
     * @param key
     * @param defaultValue
     * @return int value of the property or defaultValue if it is not set.
     * @throws PropertiesLoadingException if the property is not set properly (cannot parse int
     *             value)
     */
    public int getInt(final String key, final int defaultValue) throws PropertiesLoadingException {
        final String value = getString(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new PropertiesLoadingException("The property value is not an int number: " + key, e);
        }
    }


    /**
     * @param key
     * @param defaultValue
     * @return a long value for the property or defaultValue if it is not set.
     * @throws PropertiesLoadingException if the property is not set properly (cannot parse long
     *             value)
     */
    public long getLong(final String key, final long defaultValue) throws PropertiesLoadingException {
        final String value = getString(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException e) {
            throw new PropertiesLoadingException("The property value is not a long number: " + key, e);
        }
    }


    /**
     * @param key
     * @return a file for the property
     */
    public File getFile(final String key) {
        final String value = getString(key);

        if (value == null) {
            return null;
        }

        return new File(value);
    }


    /**
     * @param key
     * @param defaultValue
     * @return a file for the property or defaultValue if it is not set.
     */
    public File getFile(final String key, final File defaultValue) {
        final File value = getFile(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }


    /**
     * @param key
     * @return an URI for the property key
     */
    public URI getURI(final String key) {
        final String url = getString(key);
        if (url == null) {
            return null;
        }

        try {
            return new URI(url);
        } catch (final URISyntaxException e) {
            throw new PropertiesLoadingException("The property value is not an uri: " + key, e);
        }
    }


    /**
     * @param key
     * @param defaultValue
     * @return an URI for the property key of defaultValue if it is not set.
     * @throws PropertiesLoadingException - if the property has not a correct format
     */
    public URI getURI(final String key, final URI defaultValue) {
        final URI value = getURI(key);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }


    /**
     * @param key
     * @param value
     * @return an old value set under the same key
     */
    public String put(final String key, final String value) {
        return this.properties.put(key, value);
    }


    /**
     * Put all properties from source to this instance. Null-safe.
     *
     * @param source
     */
    public void putAll(final Properties source) {
        if (source != null) {
            this.properties.putAll(source.toMap());
        }
    }


    /**
     * @return a {@link Map} with all the keys and values.
     */
    public Map<String, String> toMap() {
        final Map<String, String> map = new HashMap<>(this.properties.size());
        map.putAll(this.properties);
        return map;
    }


    /**
     * Calls the internal properties toString.
     *
     * @return a {@link HashMap}-like toString().
     */
    @Override
    public String toString() {
        return this.properties.toString();
    }
}
