package fish.payara.microprofile.faulttolerance.service;

import static fish.payara.microprofile.faulttolerance.test.TestUtils.getAnnotatedMethod;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.policy.StaticAnalysisContext;

/**
 * Tests that properties can be used to override annotation attributes.
 * 
 * In response to:
 * 
 * - https://github.com/payara/Payara/issues/3762 
 * - https://github.com/payara/Payara/issues/3821
 * 
 * @author Jan Bernitt
 */
public class ConfigOverrideTest implements Config {

    private BindableFaultToleranceConfig config = new BindableFaultToleranceConfig(this, null);
    private final Properties overrides = new Properties();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        String value = overrides.getProperty(propertyName);
        if (value == null) {
            return Optional.empty();
        }
        if (propertyType == String.class) {
            return (Optional<T>) Optional.of(value);
        }
        if (propertyType == Integer.class) {
            return (Optional<T>) Optional.of(Integer.valueOf(value));
        }
        if (propertyType.isEnum()) {
            return (Optional<T>) Optional.of(enumValue(propertyType, value));
        }
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <E extends Enum<E>> Enum<?> enumValue(Class type, String value) {
        return Enum.valueOf(type, value.toUpperCase());
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        throw new UnsupportedOperationException();
    }

    @Test
    public void circuitBreakerRequestVolumeThresholdOverride() {
        assertOverridden(CircuitBreaker.class, "requestVolumeThreshold", 42, 13,
                (config, annotation) -> config.requestVolumeThreshold(annotation));
    }

    @CircuitBreaker(requestVolumeThreshold = 42)
    public String circuitBreakerRequestVolumeThresholdOverride_Method() {
        return "test";
    }

    @Test
    public void timeoutUnitOverride() {
        assertOverridden(Timeout.class, "unit", ChronoUnit.DECADES, ChronoUnit.HOURS,
                (config, annotation) -> config.unit(annotation));
    }

    @Timeout(unit = ChronoUnit.DECADES)
    public String timeoutUnitOverride_Method() {
        return "test";
    }

    private <T, A extends Annotation> void assertOverridden(Class<A> annotationType, String propertyName, T annotated,
            T overridden, BiFunction<FaultToleranceConfig, A, T> property) {
        Method annotatedMethod = getAnnotatedMethod();
        A annotation = annotatedMethod.getAnnotation(annotationType);
        FaultToleranceConfig boundConfig = config.bindTo(new StaticAnalysisContext(getClass(), annotatedMethod));
        // check we get the expected annotated value
        assertEquals(annotated, property.apply(boundConfig, annotation));
        // make the override
        overrides.put(String.format("%s/%s/%s/%s", annotatedMethod.getDeclaringClass().getName(),
                annotatedMethod.getName(), annotationType.getSimpleName(), propertyName), overridden.toString());
        // now check that we get the expected overridden value
        assertEquals(overridden, property.apply(boundConfig, annotation));
    }
}
