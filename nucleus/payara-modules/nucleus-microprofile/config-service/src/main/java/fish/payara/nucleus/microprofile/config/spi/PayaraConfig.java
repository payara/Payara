/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import fish.payara.nucleus.microprofile.config.util.ConfigValueType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolverImpl.getCacheKey;
import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolverImpl.throwWhenNotExists;
import static java.lang.System.currentTimeMillis;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final String MP_CONFIG_CACHE_DURATION = "mp.config.cache.duration";
    private static final String MP_CONFIG_EXPANSION_ENABLED_STRING = "mp.config.property.expressions.enabled";
    private static final String MP_CONFIG_PROFILE_NAME_STRING = "mp.config.profile";

    private static final Logger log = Logger.getLogger(PayaraConfig.class.getName());

    private static final class CacheEntry {
        final ConfigValueImpl value;
        final long expires;

        CacheEntry(ConfigValueImpl value, long expires) {
            this.value = value;
            this.expires = expires;
        }
    }

    private final List<ConfigSource> sources;
    private final Map<Class<?>, Converter<?>> converters;
    private final long defaultCacheDurationMilliSeconds;

    private final Map<String, CacheEntry> cachedValuesByProperty = new ConcurrentHashMap<>();

    private volatile long configuredCacheDurationMilliSeconds;
    private volatile long configuredCacheDurationMilliSecondsExpires = 0; // force value reload at start
    private final Object configuredCacheValueLock = new Object();
    
    private final String profile;

    public PayaraConfig(List<ConfigSource> sources, Map<Class<?>, Converter<?>> converters, long defaultCacheDurationMilliSeconds) {
        this.sources = sources;
        this.converters = new ConcurrentHashMap<>(converters);
        this.defaultCacheDurationMilliSeconds = defaultCacheDurationMilliSeconds;
        Collections.sort(sources, new ConfigSourceComparator());

        profile = getConfigValue(MP_CONFIG_PROFILE_NAME_STRING).getValue();
    }

    @SuppressWarnings("unchecked")
    public long getCacheDurationMilliSeconds() {
        final Optional<Converter<Long>> converter = Optional.ofNullable((Converter<Long>) converters.get(Long.class));
        if (converter.isPresent()) {
            long currentTimeMillis = currentTimeMillis();
            // If the value has been found and it hasn't expired
            if (currentTimeMillis < configuredCacheDurationMilliSecondsExpires) {
                return configuredCacheDurationMilliSeconds;
            } else {
                // Atomic block to modify the cached value
                synchronized (configuredCacheValueLock) {
                    // double check if situation didn't change
                    if (currentTimeMillis < configuredCacheDurationMilliSecondsExpires) {
                        return configuredCacheDurationMilliSeconds;
                    }
                    // Fetch the value from config
                    final ConfigValue value = searchConfigSources(MP_CONFIG_CACHE_DURATION, null);
                    if (value.getValue() != null) {
                        // If it's found, cache it
                        configuredCacheDurationMilliSeconds = convertValue(value, null, converter);
                    } else {
                        // Cache the default value (usually that's from the server config)
                        configuredCacheDurationMilliSeconds = defaultCacheDurationMilliSeconds;
                    }
                    configuredCacheDurationMilliSecondsExpires = currentTimeMillis + configuredCacheDurationMilliSeconds;
                    long endTimeMillis = currentTimeMillis();
                    log.log(Level.FINER, () -> "getCacheDurationSeconds took about " + (endTimeMillis - currentTimeMillis) + " ms");
                    return configuredCacheDurationMilliSeconds;
                }
            }
        }
        return defaultCacheDurationMilliSeconds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        if (propertyType == ConfigValueResolver.class) {
            return (T) new ConfigValueResolverImpl(this, propertyName);
        }
        T value = getValueInternal(propertyName, propertyType);
        if (value != null && propertyType.isArray()) {
            if (Array.getLength(value) == 0) {
                throw new NoSuchElementException("No value for key of" + propertyName);
            }
        }
        throwWhenNotExists(propertyName, value);
        return value;
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return getValue(propertyName, ConfigValue.class);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        T internalValue = getValueInternal(propertyName, propertyType, ConfigValueType.OPTIONAL);
        if (internalValue != null && propertyType.isArray()) {
            if (Array.getLength(internalValue) == 0) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(internalValue);
    }

    @Override
    public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> propertyType) {
        return Config.super.getOptionalValues(propertyName, propertyType).filter(list -> !list.isEmpty());
    }

    private <T> T getValueInternal(String propertyName, Class<T> propertyType) {
        return getValueInternal(propertyName, propertyType, ConfigValueType.NORMAL);
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueInternal(String propertyName, Class<T> propertyType, ConfigValueType type) {
        if (propertyType == ConfigValue.class) {
            return (T) getConfigValue(propertyName, getCacheKey(propertyName, propertyType), null, null, type);
        }
        Optional<Converter<T>> converter = getConverter(propertyType);
        if (!converter.isPresent()) {
            throw new IllegalArgumentException("Unable to convert value to type " + propertyType.getName());
        }
        return getValue(propertyName, getCacheKey(propertyName, propertyType), null, null, () -> converter);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        List<String> result = new ArrayList<>();
        for (ConfigSource configSource : sources) {
            result.addAll(configSource.getPropertyNames());
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

    protected <T> T getValue(String propertyName, String cacheKey, Long ttl, String defaultValue,
            Supplier<Optional<Converter<T>>> converter) {

        final ConfigValue result = getConfigValue(propertyName, cacheKey, ttl, defaultValue);
        return convertValue(result, defaultValue, converter.get());
    }

    protected ConfigValueImpl getConfigValue(String propertyName, String cacheKey, Long ttl, String defaultValue) {
        return getConfigValue(propertyName, cacheKey, ttl, defaultValue, ConfigValueType.NORMAL);
    }

    protected ConfigValueImpl getConfigValue(String propertyName, String cacheKey, Long ttl, String defaultValue, ConfigValueType type) {
        long entryTTL = ttl != null ? ttl : getCacheDurationMilliSeconds();
        
        if (entryTTL <= 0) {
            return searchConfigSources(propertyName, defaultValue);
        }

        final String entryKey = cacheKey + (defaultValue != null ? ":" + defaultValue : "") + ":" + (entryTTL / 1000) + "s";
        final long now = currentTimeMillis();
        long expires = now + entryTTL;

        CacheEntry cacheEntry = cachedValuesByProperty.get(entryKey);
        // entry found and valid
        if (cacheEntry != null && now < cacheEntry.expires) {
            return cacheEntry.value;
        }
        // entry not found or expired
        boolean isExpansionEnabled = isExpansionEnabled(propertyName);
        // searchConfigSources can cause recursive call to getConfigValue when expansion is enabled
        ConfigValueImpl newValue = searchConfigSources(propertyName, defaultValue, isExpansionEnabled, type);
        CacheEntry newCacheEntry = new CacheEntry(newValue, expires);
        // put the new cache entry, if there is not a newer value from other thread
        cacheEntry = cachedValuesByProperty.compute(entryKey, (key, entry) -> {
            if (entry != null && newCacheEntry.expires < entry.expires) {
                return entry;
            }
            return newCacheEntry;
        });
        return cacheEntry.value;
    }

    private <T> T convertValue(ConfigValue configValue, String defaultValue,
            Optional<Converter<T>> optionalConverter) {
        final String sourceValue = configValue.getValue();

        if (sourceValue == null || sourceValue.isEmpty()) {
            return null;
        }

        if (!optionalConverter.isPresent()) {
            throw new IllegalArgumentException(String.format(
                "Unable to find converter for property %s with value %s.",
                configValue.getName(),
                sourceValue
            ));
        }

        final Converter<T> converter = optionalConverter.get();

        try {
            if (sourceValue != null) {
                return converter.convert(sourceValue);
            }
        } catch (IllegalArgumentException ex) {
            if (defaultValue == null) {
                throw ex;
            }
        }
        return converter.convert(defaultValue);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Optional<Converter<T>> getConverter(Class<T> propertyType) {
        if (propertyType.isArray()) {
            return (Optional) createArrayConverter(propertyType.getComponentType());
        }
        Class<T> type = (Class<T>) boxedTypeOf(propertyType);
        Converter<T> converter = (Converter<T>) converters.get(type);
        if (converter != null) {
            return Optional.of(converter);
        }

        // see if a common sense converter can be created
        Optional<Converter<T>> automaticConverter = AutomaticConverter.forType(type);
        if (automaticConverter.isPresent()) {
            converters.put(type, automaticConverter.get());
            return automaticConverter;
        }

        return Optional.empty();
    }
    
    public void clearCache() {
        cachedValuesByProperty.clear();
    }

    private <E> Optional<Converter<Object>> createArrayConverter(Class<E> elementType) {
        final Optional<Converter<E>> elementConverter = getConverter(elementType);
        if (!elementConverter.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new ArrayConverter<>(elementType, getConverter(elementType).get()));
    }

    private ConfigValueImpl searchConfigSources(String propertyName, String defaultValue) {
        return searchConfigSources(propertyName, defaultValue, ConfigValueType.NORMAL);
    }

    /**
     * Do not call this method during compute as it may modify the cachedValuesByProperty map.
     */
    private ConfigValueImpl searchConfigSources(String propertyName, String defaultValue, ConfigValueType type) {
        boolean expansionEnabled = isExpansionEnabled(propertyName);
        return searchConfigSources(propertyName, defaultValue, expansionEnabled);
    }

    private boolean isExpansionEnabled(String propertyName) {
        final boolean expansionEnabled = !MP_CONFIG_CACHE_DURATION.equals(propertyName)
                && !MP_CONFIG_EXPANSION_ENABLED_STRING.equals(propertyName)
                && getOptionalValue(MP_CONFIG_EXPANSION_ENABLED_STRING, Boolean.class)
                        .orElse(true);
        return expansionEnabled;
    }

    private ConfigValueImpl searchConfigSources(String propertyName, String defaultValue, boolean expansionEnabled) {
        return searchConfigSources(propertyName, defaultValue, expansionEnabled, ConfigValueType.NORMAL);
    }

    private ConfigValueImpl searchConfigSources(String propertyName, String defaultValue, boolean expansionEnabled, ConfigValueType type) {
        return new ConfigExpressionResolver(sources, expansionEnabled, profile)
                .resolve(propertyName, defaultValue, type);
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> type) {
        if (type == PayaraConfig.class) {
            return (T) this;
        }
        throw new IllegalArgumentException("Unable to cast config source to type " + type);
    }
    
    /**
     * The Mp Config profile in use.
     * @return may be null
     */
    public String getProfile() {
        return profile;
    }
    
    /**
     * Add another config source.
     * Package-private, used by ConfigProviderResolver to add profile-specific
     * config sources.
     * @param added new config source to add.
     */
    void addConfigSource(ConfigSource added) {
        sources.add(added);
        Collections.sort(sources, new ConfigSourceComparator());
    }
    
}
