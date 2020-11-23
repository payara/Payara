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

import fish.payara.nucleus.microprofile.config.converters.ArrayConverter;
import fish.payara.nucleus.microprofile.config.converters.AutomaticConverter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolverImpl.getCacheKey;
import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolverImpl.throwWhenNotExists;
import static java.lang.System.currentTimeMillis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Standard implementation for MP {@link Config}.
 *
 * This implementation usually caches values for 1 min to avoid resolving and converting values each time
 * {@link #getValue(String, Class)} is called. This cache can be bypassed by constructing the {@link PayaraConfig} with
 * a TTL of zero (or negative).
 *
 * @author Steve Millidge (Payara Foundation)
 * @author Jan Bernitt (caching part, ConfigValueResolver)
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

    long getTTL() {
        return ttl;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        if (propertyType == ConfigValueResolver.class) {
            return (T) new ConfigValueResolverImpl(this, propertyName);
        }
        T value = getValueInternal(propertyName, propertyType);
        throwWhenNotExists(propertyName, value);
        return value;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return Optional.ofNullable(getValueInternal(propertyName, propertyType));
    }

    private <T> T getValueInternal(String propertyName, Class<T> propertyType) {
        return getValue(propertyName, getCacheKey(propertyName, propertyType), ttl, null,
                () -> getConverter(propertyType));
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

    @SuppressWarnings("unchecked")
    public <T> T getValue(String propertyName, String cacheKey, Long ttl, String defaultValue, Supplier<? extends Converter<T>> converter) {
        long entryTTL = ttl == null ? this.ttl : ttl.longValue();
        if (entryTTL <= 0) {
            return getValueConverted(propertyName, defaultValue, converter.get());
        }
        final String entryKey = cacheKey + (defaultValue != null ? ":" + defaultValue : "")  + ":" + (entryTTL / 1000) + "s";
        final long now = currentTimeMillis();
        return (T) cachedValuesByProperty.compute(entryKey, (key, entry) -> {
            if (entry != null && now < entry.expires) {
                return entry;
            }
            return new CacheEntry(getValueConverted(propertyName, defaultValue, converter.get()), now + entryTTL);
        }).value;
    }

    private <T> T getValueConverted(String propertyName, String defaultValue, Converter<T> converter) {
        String sourceValue = getSourceValue(propertyName);
        // NOTE: when empty is considered missing by MP this condition needs to add "|| sourceValue.isEmpty()"
        if (sourceValue == null) {
            return defaultValue == null ? null : converter.convert(defaultValue);
        }
        try {
            return converter.convert(sourceValue);
        } catch (IllegalArgumentException ex) {
            if (defaultValue == null) {
                throw ex;
            }
            return converter.convert(defaultValue);
        }
    }

    private String getSourceValue(String propertyName) {
        for (ConfigSource source : sources) {
            String sourceValue = source.getValue(propertyName);
            if (sourceValue != null) {
                return sourceValue;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getConverter(Class<T> propertyType) {
        if (propertyType.isArray()) {
            return (Converter<T>) createArrayConverter(propertyType.getComponentType());
        }
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

    private <E> Converter<Object> createArrayConverter(Class<E> elementType) {
        return new ArrayConverter<>(elementType, getConverter(elementType));
    }

    static Class<?> boxedTypeOf(Class<?> type) {
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
}
