/*
 * Copyright 2017-2019 Rudy De Busscher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2020] Payara Foundation and/or affiliates
package fish.payara.microprofile.metrics;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import static java.util.Collections.emptyList;

import java.util.*;
import org.eclipse.microprofile.config.ConfigValue;

/**
 * {@link Config} implementation using a Map for unit test usages. Use {@link TestConfig#addConfigValue(String, String)} to specify some configuration parameters.
 * The default converts are specified with {@link TestConfig#registerDefaultConverters()} and additional ones can be defined using {@link TestConfig#registerConverter(Converter)}.
 * <p>
 * After the unit test has run, one should remove the configuration parameters with the method {@link TestConfig#resetConfig()}.
 * 
 * @author Rudy De Busscher (original version https://github.com/atbashEE/atbash-config/blob/main/test/src/main/java/be/atbash/config/test/TestConfig.java)
 * @author Jan Bernitt (stripped converters/hard coded converters only)
 */
public class TestConfig implements Config {

    private static Map<String, String> configValues = new HashMap<>();

    /**
     * Add a configuration parameter value, which will be picked up a call to {@link Config#getValue(String, Class)}.
     *
     * @param key   Parameter key value
     * @param value Configuration parameter value.
     */
    public static void addConfigValue(String key, String value) {
        configValues.put(key, value);
    }

    /**
     * Add configuration parameter values, which will be picked up a call to {@link Config#getValue(String, Class)}.
     *
     * @param values Configuration values to add.
     */
    public static void addConfigValues(Map<String, String> values) {
        configValues.putAll(values);
    }

    /**
     * Reset all Configuration parameter values so that the tests keep on being independent.
     */
    public static void resetConfig() {
        configValues.clear();
    }

    /**
     * Register a {@link Converter} to convert the Configuration parameter value to a certain type.
     *
     * @param converter The converter to be registered.
     */
    public static void registerConverter(Converter<?> converter) {
        throw convertersNotSupported();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        if (!configValues.containsKey(propertyName)) {
            throw new NoSuchElementException(String.format("Key %s does not exists", propertyName));
        }
        String value = configValues.get(propertyName);
        if (propertyType == String.class) {
            return (T) value;
        }
        if (propertyType == Boolean.class || propertyType == boolean.class) {
            return (T) Integer.valueOf(value);
        }
        if (propertyType == Long.class || propertyType == long.class) {
            return (T) Long.valueOf(value);
        }
        if (propertyType == Float.class || propertyType == float.class) {
            return (T) Float.valueOf(value);
        }
        if (propertyType == Double.class || propertyType == double.class) {
            return (T) Double.valueOf(value);
        }
        if (propertyType.isEnum()) {
            return (T) getEnumValue(propertyName, propertyType);
        }
        throw convertersNotSupported();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <E extends Enum<E>> E getEnumValue(String propertyName, Class propertyType) {
        return (E) Enum.valueOf(propertyType, propertyName);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        String value = configValues.get(propertyName);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(getValue(propertyName, propertyType));
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return configValues.keySet();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return emptyList();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Unwrap not supported by test config.");
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ConfigValue getConfigValue(String string) {
        return new TestConfigValue(string, getValue(PROFILE, String.class));
    }
    
    class TestConfigValue implements ConfigValue {
        
        String name;
        String value;

        public TestConfigValue(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getRawValue() {
            return value;
        }

        @Override
        public String getSourceName() {
            return "test";
        }

        @Override
        public int getSourceOrdinal() {
            return 100;
        }
        
    }
    
    
    private static UnsupportedOperationException convertersNotSupported() {
        return new UnsupportedOperationException("Converters are not supported by the test config");
    }

}
