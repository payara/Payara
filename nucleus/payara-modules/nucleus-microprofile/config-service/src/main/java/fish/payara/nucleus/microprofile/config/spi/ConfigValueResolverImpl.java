package fish.payara.nucleus.microprofile.config.spi;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Implementation for the {@link ConfigValueResolver} which uses the non public API of
 * {@link PayaraConfig#getValue(String, String, Long, String, Supplier)} to implement its utility methods.
 *
 * @author Jan Bernitt
 */
final class ConfigValueResolverImpl implements ConfigValueResolver {

    private final PayaraConfig config;
    private final String propertyName;
    private boolean throwsOnMissingProperty;
    private boolean throwOnFailedConversion;
    private Long ttl;
    private String rawDefault;

    ConfigValueResolverImpl(PayaraConfig config, String propertyName) {
        this.config = config;
        this.propertyName = propertyName;
    }

    @Override
    public ConfigValueResolver withDefault(String value) {
        rawDefault = ConfigProperty.UNCONFIGURED_VALUE.equals(value) ? null : value;
        return this;
    }

    @Override
    public ConfigValueResolver throwOnMissingProperty() {
        throwsOnMissingProperty = true;
        return this;
    }

    @Override
    public ConfigValueResolver throwOnFailedConversion() {
        throwOnFailedConversion = true;
        return this;
    }

    @Override
    public <T> T as(Class<T> type, T defaultValue) {
        return asValue(propertyName, getCacheKey(propertyName, type), ttl, defaultValue, () -> config.getConverter(type));
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {
        return Optional.ofNullable(as(type, null));
    }

    @Override
    public <E> List<E> asList(Class<E> elementType) {
        return asList(elementType, emptyList());
    }

    @Override
    public <E> List<E> asList(Class<E> elementType, List<E> defaultValue) {
        return asValue(propertyName, getCacheKey(propertyName, List.class, elementType), ttl, defaultValue,
                () -> createListConverter(getArrayConverter(elementType)));
    }

    @Override
    public <E> Set<E> asSet(Class<E> elementType) {
        return asSet(elementType, emptySet());
    }

    @Override
    public <E> Set<E> asSet(Class<E> elementType, Set<E> defaultValue) {
        return asValue(propertyName, getCacheKey(propertyName, Set.class, elementType), ttl, defaultValue,
                () -> createSetConverter(getArrayConverter(elementType)));
    }

    @Override
    public <T> T asConvertedBy(Function<String, T> converter, T defaultValue) {
        String sourceValue = asValue(propertyName, getCacheKey(propertyName, String.class), ttl, getRawDefault(),
                () -> value -> value);
        if (sourceValue == null) {
            if (throwsOnMissingProperty) {
                throwWhenNotExists(propertyName, null);
            }
            return defaultValue;
        }
        try {
            return converter.apply(sourceValue);
        } catch (Exception ex) {
            if (throwOnFailedConversion) {
                throw ex;
            }
            return defaultValue;
        }
    }

    private <T> T asValue(String propertyName, String cacheKey, Long ttl, T defaultValue, Supplier<? extends Converter<T>> converter) {
        try {
            T value = config.getValue(propertyName, cacheKey, ttl, getRawDefault(), converter);
            if (value != null) {
                return value;
            }
            if (throwsOnMissingProperty) {
                throwWhenNotExists(propertyName, null);
            }
            return defaultValue;
        } catch (IllegalArgumentException ex) {
            if (throwOnFailedConversion) {
                throw ex;
            }
            return defaultValue;
        }
    }

    private String getRawDefault() {
        return throwsOnMissingProperty ? null : rawDefault;
    }

    private <E> Converter<E[]> getArrayConverter(Class<E> elementType) {
        return config.getConverter(arrayTypeOf(elementType));
    }

    static void throwWhenNotExists(String propertyName, Object value) {
        if (value == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
    }

    static String getCacheKey(String propertyName, Class<?> propertyType) {
        String key = propertyType.getName() + ":" + propertyName;
        return key;
    }

    static <E> String getCacheKey(String propertyName, Class<?> collectionType, Class<E> elementType) {
        return collectionType.getName() + ":" + getCacheKey(propertyName, elementType);
    }

    static <E> Converter<List<E>> createListConverter(Converter<E[]> arrayConverter) {
        return sourceValue -> Arrays.asList(arrayConverter.convert(sourceValue));
    }

    static <E> Converter<Set<E>> createSetConverter(Converter<E[]> arrayConverter) {
        return sourceValue ->  new HashSet<>(Arrays.asList(arrayConverter.convert(sourceValue)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <E> Class<E[]> arrayTypeOf(Class<E> elementType) {
        if (elementType.isPrimitive()) {
            return (Class) arrayTypeOf(PayaraConfig.boxedTypeOf(elementType));
        }
        return (Class<E[]>) Array.newInstance(elementType, 0).getClass();
    }
}
