/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.logging.jul.cfg;

import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Replacement (wrapper) for {@link Properties} used in JUL.
 *
 * @author David Matejcek
 */
public class PayaraLogManagerConfiguration implements Serializable, Cloneable {

    /** If this key is set to true, PJULE will print really detailed tracing info to the standard output */
    public static final String KEY_TRACING_ENABLED = "fish.payara.logging.jul.tracingEnabled";
    private static final long serialVersionUID = 1L;
    private final Properties properties;


    /**
     * @param properties configuration to clone
     */
    public PayaraLogManagerConfiguration(final Properties properties) {
        this.properties = (Properties) properties.clone();
    }


    /**
     * @return all property names used in the current configuration.
     */
    public SortedSet<String> getPropertyNames() {
        return properties.keySet().stream().map(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * @param name proeprty name
     * @return null or configured value
     */
    public String getProperty(final String name) {
        PayaraLoggingTracer.trace(PayaraLogManagerConfiguration.class, () -> "getProperty(" + name + ")");
        return this.properties.getProperty(name, null);
    }


    /**
     * @return {@link Stream} of configuration entries (key and value)
     */
    public Stream<ConfigurationEntry> toStream() {
        return this.properties.entrySet().stream().map(ConfigurationEntry::new);
    }


    /**
     * @return cloned {@link Properties}
     */
    public Properties toProperties() {
        return (Properties) this.properties.clone();
    }


    /**
     * @return true if the logging of logging is enabled in this configuration. Doesn't affect error
     *         reporting, which is always enabled.
     */
    public boolean isTracingEnabled() {
        return Boolean.parseBoolean(this.properties.getProperty(KEY_TRACING_ENABLED));
    }


    /**
     * Creates clone of this instance.
     */
    @Override
    public PayaraLogManagerConfiguration clone() {
        try {
            return (PayaraLogManagerConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone failed!", e);
        }
    }


    @Override
    public String toString() {
        return this.properties.toString();
    }

    /**
     * Configuration entry, pair of a key and a value, both can be null (but it is not very useful).
     */
    public static final class ConfigurationEntry {

        private final String key;
        private final String value;

        ConfigurationEntry(final Entry<Object, Object> entry) {
            this.key = entry.getKey() == null ? null : entry.getKey().toString();
            this.value = entry.getValue() == null ? null : entry.getValue().toString();
        }

        /**
         * @return property key
         */
        public String getKey() {
            return key;
        }


        /**
         * @return property value
         */
        public String getValue() {
            return value;
        }


        /**
         * Returns key:value
         */
        @Override
        public String toString() {
            return getKey() + ":" + getValue();
        }
    }
}
