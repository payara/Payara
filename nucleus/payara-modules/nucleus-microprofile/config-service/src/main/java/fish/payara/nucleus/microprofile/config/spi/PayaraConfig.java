/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.microprofile.config.spi;

import fish.payara.nucleus.microprofile.config.converters.AutomaticConverter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Standard implementation for MP {@link Config}.
 *
 * This implementation usually caches values for 1 min to avoid resolving and converting values each time
 * {@link #getValue(String, Class)} is called. This cache can be bypassed by constructing the {@link PayaraConfig} with
 * a TTL of zero (or negative).
 *
 * @author Steve Millidge (Payara Foundation)
 * @author Jan Bernitt (caching part)
 */
public class PayaraConfig implements Config {

    private static final class CacheEntry {
        final Object value;
        final long expires;

        CacheEntry(Object value, long expires) {
            this.value = value;
            this.expires = expires;
        }
    }

    /**
     * Duration a {@link CacheEntry} is valid, when it becomes invalid the entry is updated with value from
     * {@link ConfigSource} on next request.
     */
    private static final int DEFAULT_TTL = 60;

    private final List<ConfigSource> sources;
    private final Map<Class<?>, Converter<?>> converters;
    private final long ttl;
    private final Map<String, CacheEntry> cachedValuesByProperty = new ConcurrentHashMap<>();

    public PayaraConfig(List<ConfigSource> configSources, Map<Class<?>,Converter<?>> converters) {
        this(configSources, converters, TimeUnit.SECONDS.toMillis(DEFAULT_TTL));
    }

    public PayaraConfig(List<ConfigSource> sources, Map<Class<?>,Converter<?>> converters, long ttl) {
        this.sources = sources;
        this.converters = new ConcurrentHashMap<>(converters);
        this.ttl = ttl;
        Collections.sort(sources, new ConfigSourceComparator());
    }

    public long getTTL() {
        return ttl;
    }

    private static String getCacheKey(String propertyName, Class<?> propertyType, Class<?> elementType) {
        String key = propertyType.getName();
        if (elementType != null) {
            key += ":" + elementType.getName();
        }
        return key + ":" + propertyName;
    }

    private <T> T getValueUncached(String propertyName, Class<T> propertyType) {
        String stringValue = getStringValue(propertyName);
        return stringValue == null ? null : convertString(stringValue, propertyType);
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueCached(String propertyName, Class<T> propertyType, Class<?> elementType, BiFunction<String, Class<T>, T> getUncached) {
        return ttl > 0
                ? (T) cachedValuesByProperty.compute(getCacheKey(propertyName, propertyType, elementType),
                    (key, entry) -> entry != null && currentTimeMillis() < entry.expires
                        ? entry
                        : new CacheEntry(getUncached.apply(propertyName, propertyType), currentTimeMillis() + ttl)).value
                : getUncached.apply(propertyName, propertyType);
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        T value = getValueCached(propertyName, propertyType, null, this::getValueUncached);
        if (value == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
        return value;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return Optional.ofNullable(getValueCached(propertyName, propertyType, null, this::getValueUncached));
    }

    @Override
    public Iterable<String> getPropertyNames() {
        List<String> result = new ArrayList<>();
        for (ConfigSource configSource : sources) {
            result.addAll(configSource.getProperties().keySet());
        }
        return result;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return sources;
    }

    public Set<Class<?>> getConverterTypes() {
        return converters.keySet();
    }

    public <T> List<T> getListValues(String propertyName, String defaultValue, Class<T> elementType) {
        @SuppressWarnings("unchecked")
        List<T> value = getValueCached(propertyName, List.class, elementType, (property, type) -> {
            String stringValue = getStringValue(property);
            if (stringValue == null) {
                stringValue = defaultValue;
            }
            return convertToList(stringValue, elementType);
        });
        if (value == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
        return value;
    }

    private <T> List<T> convertToList(String stringValue, Class<T> elementType) {
        if (stringValue == null) {
            return null;
        }
        String keys[] = splitValue(stringValue);
        List<T> result = new ArrayList<>(keys.length);
        for (String key : keys) {
            result.add(convertString(key, elementType));
        }
        return result;
    }

    public <T> Set<T> getSetValues(String propertyName, String defaultValue, Class<T> elementType) {
        @SuppressWarnings("unchecked")
        Set<T> value = getValueCached(propertyName, Set.class, elementType, (property, type) ->  {
            String stringValue = getStringValue(property);
            if (stringValue == null) {
                stringValue = defaultValue;
            }
            return convertToSet(stringValue, elementType);
        });
        if (value == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
        return value;
    }

    private <T> Set<T> convertToSet(String stringValue, Class<T> elementType) {
        if (stringValue == null) {
            return null;
        }
        String keys[] = splitValue(stringValue);
        Set<T> result = new HashSet<>(keys.length);
        for (String key : keys) {
            result.add(convertString(key, elementType));
        }
        return result;
    }

    public <T> T getValue(String propertyName, String defaultValue, Class<T> propertyType) {
        return getValueCached(propertyName, propertyType, null, (property, type) -> {
            String stringValue = getStringValue(property);
            if (stringValue == null) {
                stringValue = defaultValue;
            }
            return stringValue == null ? null : convertString(stringValue, type);
        });
    }

    private String getStringValue(String propertyName) {
        for (ConfigSource configSource : sources) {
            String value = configSource.getValue(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T> getConverter(Class<T> propertyType) {
        Class<T> type = (Class<T>) boxedTypeOf(propertyType);

        Converter<?> converter = converters.get(type);

        if (converter != null) {
            return (Converter<T>) converter;
        }

        // see if a common sense converter can be created
        Optional<Converter<T>> automaticConverter = AutomaticConverter.forType(type);
        if (automaticConverter.isPresent()) {
            converters.put(type, automaticConverter.get());
            return automaticConverter.get();
        }

        throw new IllegalArgumentException("Unable to convert value to type " + type.getTypeName());
    }



    private static Class<?> boxedTypeOf(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        // That's really strange config variable you got there
        return Void.class;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertString(String value, Class<T> propertyType) {
        // if it is an array convert arrays
        if (propertyType.isArray()) {
            // find converter for the array type
            Class<?> componentClazz = propertyType.getComponentType();
            Converter<?> converter = getConverter(componentClazz);

            // array convert
            String keys[] = splitValue(value);
            Object arrayResult = Array.newInstance(componentClazz, keys.length);
            for (int i = 0; i < keys.length; i++) {
                Array.set(arrayResult, i, converter.convert(keys[i]));
            }
            return (T) arrayResult;
        }
        // find a converter
        Converter<T> converter = getConverter(propertyType);
        if (converter == null) {
            throw new IllegalArgumentException("No converter for class " + propertyType);
        }
        return converter.convert(value);
    }

    private static String[] splitValue(String value) {
        String keys[] = value.split("(?<!\\\\),");
        for (int i=0; i < keys.length; i++) {
            keys[i] = keys[i].replaceAll("\\\\,", ",");
        }
        return keys;
    }

}
