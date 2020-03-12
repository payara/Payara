package fish.payara.microprofile.metrics.cdi;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.junit.Test;

import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Tests the formal correctness of the {@link AnnotationReader} utility.
 *
 * Unlike in real world usage the tests often stack multiple FT annotations to test all of them in one go. They all
 * should show a identical behaviour with respect to the property tested.
 *
 * Which source is picked up as origin of annotations is based on conventions. If no bean is passed explicitly the test
 * itself is the bean and the test method is the annotated element. If a bean is passed it is expected to only have one
 * field or method or constructor. Either the bean class or that element (or one of its parameters) is annotated.
 *
 *
 * The {@link Metric} annotation however is special and needs dedicated test coverage.
 *
 * A number of tests are focused on verifying that the metric name is derived correctly according to the specification.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
@SuppressWarnings("synthetic-access")
public class AnnotationReaderTest {

    /*
     * Test metric names for methods
     */

    @Test
    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    @Gauge(unit = MetricUnits.NONE)
    public void relativeInferredNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.relativeInferredNameMethod");
    }

    @Test
    @ConcurrentGauge(name = "localMethod")
    @Counted(name = "localMethod")
    @Metered(name = "localMethod")
    @Timed(name = "localMethod")
    @SimplyTimed(name = "localMethod")
    @Gauge(name = "localMethod", unit = MetricUnits.NONE)
    public void relativeGivenLocalNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.localMethod");
    }

    @Test
    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    @Gauge(absolute = true, unit = MetricUnits.NONE)
    public void absoluteInferredNameMethod() {
        assertNamed("absoluteInferredNameMethod");
    }

    @Test
    @ConcurrentGauge(name = "localMethod", absolute = true)
    @Counted(name = "localMethod", absolute = true)
    @Metered(name = "localMethod", absolute = true)
    @Timed(name = "localMethod", absolute = true)
    @SimplyTimed(name = "localMethod", absolute = true)
    @Gauge(name = "localMethod", absolute = true, unit = MetricUnits.NONE)
    public void absoluteGivenLocalNameMethod() {
        assertNamed("localMethod");
    }

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class RelativeInferredContextNameMethod {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredContextNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeInferredContextNameMethod.name", RelativeInferredContextNameMethod.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class AbsoluteInferredContextNameMethod {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredContextNameMethod() {
        assertNamed("name", AbsoluteInferredContextNameMethod.class);
    }

    @ConcurrentGauge(name = "contextMethod")
    @Counted(name = "contextMethod")
    @Metered(name = "contextMethod")
    @Timed(name = "contextMethod")
    @SimplyTimed(name = "contextMethod")
    private static class RelativeGivenContextNameMethod {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenContextNameMethod() {
        assertNamed("contextMethod.name", RelativeGivenContextNameMethod.class);
    }

    @ConcurrentGauge(name = "ignoredContextMethod", absolute = true)
    @Counted(name = "ignoredContextMethod", absolute = true)
    @Metered(name = "ignoredContextMethod", absolute = true)
    @Timed(name = "ignoredContextMethod", absolute = true)
    @SimplyTimed(name = "ignoredContextMethod", absolute = true)
    private static class AbsoluteGivenContextNameMethod {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenContextNameMethod() {
        assertNamed("name", AbsoluteGivenContextNameMethod.class);
    }

    /*
     * Test metric names for constructors
     */

    private static class RelativeInferredNameConstructor {
        @ConcurrentGauge
        @Counted
        @Metered
        @Timed
        @SimplyTimed
        RelativeInferredNameConstructor() { /* parameters not important here */ }
    }
    @Test
    public void relativeInferredNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeInferredNameConstructor.RelativeInferredNameConstructor",
                RelativeInferredNameConstructor.class);
    }

    private static class RelativeGivenLocalNameConstructor {
        @ConcurrentGauge(name = "localConstructor")
        @Counted(name = "localConstructor")
        @Metered(name = "localConstructor")
        @Timed(name = "localConstructor")
        @SimplyTimed(name = "localConstructor")
        RelativeGivenLocalNameConstructor() { /* parameters not important here */ }
    }
    @Test
    public void relativeGivenLocalNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeGivenLocalNameConstructor.localConstructor",
                RelativeGivenLocalNameConstructor.class);
    }

    private static class AbsoluteInferredNameConstructor {
        @ConcurrentGauge(absolute = true)
        @Counted(absolute = true)
        @Metered(absolute = true)
        @Timed(absolute = true)
        @SimplyTimed(absolute = true)
        AbsoluteInferredNameConstructor() { /* parameters not important here */ }
    }
    @Test
    public void absoluteInferredNameConstructor() {
        assertNamed("AbsoluteInferredNameConstructor", AbsoluteInferredNameConstructor.class);
    }

    private static class AbsoluteGivenLocalNameConstructor {
        @ConcurrentGauge(name = "localConstructor", absolute = true)
        @Counted(name = "localConstructor", absolute = true)
        @Metered(name = "localConstructor", absolute = true)
        @Timed(name = "localConstructor", absolute = true)
        @SimplyTimed(name = "localConstructor", absolute = true)
        AbsoluteGivenLocalNameConstructor() { /* parameters not important here */ }
    }
    @Test
    public void absoluteGivenLocalNameConstructor() {
        assertNamed("localConstructor", AbsoluteGivenLocalNameConstructor.class);
    }

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class RelativeInferredContextNameConstructor {
        @SuppressWarnings("unused")
        RelativeInferredContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredContextNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeInferredContextNameConstructor.RelativeInferredContextNameConstructor",
                RelativeInferredContextNameConstructor.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class AbsoluteInferredContextNameConstructor {
        @SuppressWarnings("unused")
        AbsoluteInferredContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredContextNameConstructor() {
        assertNamed("AbsoluteInferredContextNameConstructor", AbsoluteInferredContextNameConstructor.class);
    }

    @ConcurrentGauge(name = "contextConstructor")
    @Counted(name = "contextConstructor")
    @Metered(name = "contextConstructor")
    @Timed(name = "contextConstructor")
    @SimplyTimed(name = "contextConstructor")
    private static class RelativeGivenContextNameConstructor {
        @SuppressWarnings("unused")
        RelativeGivenContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenContextNameConstructor() {
        assertNamed("contextConstructor.RelativeGivenContextNameConstructor", RelativeGivenContextNameConstructor.class);
    }

    @ConcurrentGauge(name = "ignoredContextConstructor", absolute = true)
    @Counted(name = "ignoredContextConstructor", absolute = true)
    @Metered(name = "ignoredContextConstructor", absolute = true)
    @Timed(name = "ignoredContextConstructor", absolute = true)
    @SimplyTimed(name = "ignoredContextConstructor", absolute = true)
    private static class AbsoluteGivenContextNameConstructor {
        @SuppressWarnings("unused")
        AbsoluteGivenContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenContextNameConstructor() {
        assertNamed("AbsoluteGivenContextNameConstructor", AbsoluteGivenContextNameConstructor.class);
    }


    /*
     * Test metric names for methods when annotated on super class
     */

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class RelativeInferredSuperContextNameMethodBase { /* not important here */ }
    private static class RelativeInferredSuperContextNameMethod extends RelativeInferredSuperContextNameMethodBase {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredSuperContextNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeInferredSuperContextNameMethod.name",
                RelativeInferredSuperContextNameMethod.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class AbsoluteInferredSuperContextNameMethodBase { /* not important here */ }
    private static class AbsoluteInferredSuperContextNameMethod extends AbsoluteInferredSuperContextNameMethodBase {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredSuperContextNameMethod() {
        assertNamed("name", AbsoluteInferredSuperContextNameMethod.class);
    }

    @ConcurrentGauge(name = "contextMethod")
    @Counted(name = "contextMethod")
    @Metered(name = "contextMethod")
    @Timed(name = "contextMethod")
    @SimplyTimed(name = "contextMethod")
    private static class RelativeGivenSuperContextNameMethodBase { /* not important here */ }
    private static class RelativeGivenSuperContextNameMethod extends RelativeGivenSuperContextNameMethodBase {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenSuperContextNameMethod() {
        assertNamed("contextMethod.name", RelativeGivenSuperContextNameMethod.class);
    }

    @ConcurrentGauge(name = "ignoredContextMethod", absolute = true)
    @Counted(name = "ignoredContextMethod", absolute = true)
    @Metered(name = "ignoredContextMethod", absolute = true)
    @Timed(name = "ignoredContextMethod", absolute = true)
    @SimplyTimed(name = "ignoredContextMethod", absolute = true)
    private static class AbsoluteGivenSuperContextNameMethodBase { /* not important here */ }
    private static class AbsoluteGivenSuperContextNameMethod extends AbsoluteGivenSuperContextNameMethodBase {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenSuperContextNameMethod() {
        assertNamed("name", AbsoluteGivenSuperContextNameMethod.class);
    }

    /*
     * Test metric names for constructors when annotated on super class
     */

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class RelativeInferredSuperContextNameConstructorBase { /* not important here */ }
    private static class RelativeInferredSuperContextNameConstructor extends RelativeInferredSuperContextNameConstructorBase {
        @SuppressWarnings("unused")
        RelativeInferredSuperContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredSuperContextNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.RelativeInferredSuperContextNameConstructor.RelativeInferredSuperContextNameConstructor",
                RelativeInferredSuperContextNameConstructor.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class AbsoluteInferredSuperContextNameConstructorBase { /* not important here */ }
    private static class AbsoluteInferredSuperContextNameConstructor extends AbsoluteInferredSuperContextNameConstructorBase {
        @SuppressWarnings("unused")
        AbsoluteInferredSuperContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredSuperContextNameConstructor() {
        assertNamed("AbsoluteInferredSuperContextNameConstructor", AbsoluteInferredSuperContextNameConstructor.class);
    }

    @ConcurrentGauge(name = "contextConstructor")
    @Counted(name = "contextConstructor")
    @Metered(name = "contextConstructor")
    @Timed(name = "contextConstructor")
    @SimplyTimed(name = "contextConstructor")
    private static class RelativeGivenSuperContextNameConstructorBase { /* not important here */ }
    private static class RelativeGivenSuperContextNameConstructor extends RelativeGivenSuperContextNameConstructorBase {
        @SuppressWarnings("unused")
        RelativeGivenSuperContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenSuperContextNameConstructor() {
        assertNamed("contextConstructor.RelativeGivenSuperContextNameConstructor", RelativeGivenSuperContextNameConstructor.class);
    }

    @ConcurrentGauge(name = "ignoredContextConstructor", absolute = true)
    @Counted(name = "ignoredContextConstructor", absolute = true)
    @Metered(name = "ignoredContextConstructor", absolute = true)
    @Timed(name = "ignoredContextConstructor", absolute = true)
    @SimplyTimed(name = "ignoredContextConstructor", absolute = true)
    private static class AbsoluteGivenSuperContextNameConstructorBase { /* not important here */ }
    private static class AbsoluteGivenSuperContextNameConstructor extends AbsoluteGivenSuperContextNameConstructorBase {
        @SuppressWarnings("unused")
        AbsoluteGivenSuperContextNameConstructor() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenSuperContextNameConstructor() {
        assertNamed("AbsoluteGivenSuperContextNameConstructor", AbsoluteGivenSuperContextNameConstructor.class);
    }


    /*
     * Metric names when using @Metric on Fields and Methods
     */

    private static class RelativeInferredLocalNameMetricMethod {

        @Produces
        @Metric
        Counter method() { return null; }
    }
    @Test
    public void relativeInferredLocalNameMetricMethod() {
        assertNamed("<this>.RelativeInferredLocalNameMetricMethod.method", RelativeInferredLocalNameMetricMethod.class);
    }

    private static class RelativeInferredLocalNameMetricField {
        @Metric
        Counter field;
    }
    @Test
    public void relativeInferredLocalNameMetricField() {
        assertNamed("<this>.RelativeInferredLocalNameMetricField.field", RelativeInferredLocalNameMetricField.class);
    }

    private static class RelativeGivenLocalNameMetricMethod {

        @Produces
        @Metric(name = "producer")
        Counter method() { return null; }
    }
    @Test
    public void relativeGivenLocalNameMetricMethod() {
        assertNamed("<this>.RelativeGivenLocalNameMetricMethod.producer", RelativeGivenLocalNameMetricMethod.class);
    }

    private static class RelativeGivenLocalNameMetricField {
        @Metric(name = "counter")
        Counter field;
    }
    @Test
    public void relativeGivenLocalNameMetricField() {
        assertNamed("<this>.RelativeGivenLocalNameMetricField.counter", RelativeGivenLocalNameMetricField.class);
    }

    private static class AbsoluteGivenLocalNameMetricMethod {

        @Produces
        @Metric(name = "producer", absolute = true)
        Counter method() { return null; }
    }
    @Test
    public void absoluteGivenLocalNameMetricMethod() {
        assertNamed("producer", AbsoluteGivenLocalNameMetricMethod.class);
    }

    private static class AbsoluteGivenLocalNameMetricField {
        @Metric(name = "counter", absolute = true)
        Counter field;
    }
    @Test
    public void absoluteGivenLocalNameMetricField() {
        assertNamed("counter", AbsoluteGivenLocalNameMetricField.class);
    }

    private static class AbsoluteInferredLocalNameMetricMethod {

        @Produces
        @Metric
        Counter method() { return null; }
    }
    @Test
    public void absoluteInferredLocalNameMetricMethod() {
        assertNamed("<this>.AbsoluteInferredLocalNameMetricMethod.method", AbsoluteInferredLocalNameMetricMethod.class);
    }

    private static class AbsoluteInferredLocalNameMetricField {
        @Metric
        Counter field;
    }
    @Test
    public void absoluteInferredLocalNameMetricField() {
        assertNamed("<this>.AbsoluteInferredLocalNameMetricField.field", AbsoluteInferredLocalNameMetricField.class);
    }

    /*
     * Metric on Parameters
     */

    private static class RelativeGivenLocalNameMetricMethodParameter {
        @SuppressWarnings("unused")
       void  method(@Metric(name = "c1") Counter counter) { /* ... */ }
    }
    @Test
    public void relativeGivenLocalNameMetricMethodParameter() {
        assertNamedParamter("<this>.RelativeGivenLocalNameMetricMethodParameter.c1", RelativeGivenLocalNameMetricMethodParameter.class);
    }

    private static class AbsoluteGivenLocalNameMetricMethodParameter {
        @SuppressWarnings("unused")
        void method(@Metric(name = "c1", absolute = true) Counter counter) { /* ... */ }
    }
    @Test
    public void absoluteGivenLocalNameMetricMethodParameter() {
        assertNamedParamter("c1", AbsoluteGivenLocalNameMetricMethodParameter.class);
    }

    private static class RelativeInferredLocalNameMethodParameter {
        @SuppressWarnings("unused")
        void method(@Metric Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void relativeInferredLocalNameMethodParameter() {
        assertNamedParamter("<this>.RelativeInferredLocalNameMethodParameter.arg0", RelativeInferredLocalNameMethodParameter.class);
    }

    private static class AbsoluteInferredLocalNameMethodParameter {
        @SuppressWarnings("unused")
        void method(@Metric(absolute = true) Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void absoluteInferredLocalNameMethodParameter() {
        assertNamedParamter("arg0", AbsoluteInferredLocalNameMethodParameter.class);
    }

    private static class RelativeGivenLocalNameMetricConstructorParameter {
        @SuppressWarnings("unused")
        RelativeGivenLocalNameMetricConstructorParameter(@Metric(name = "c1") Counter counter) { /* ... */ }
    }
    @Test
    public void relativeGivenLocalNameMetricConstructorParameter() {
        assertNamedParamter("<this>.RelativeGivenLocalNameMetricConstructorParameter.c1", RelativeGivenLocalNameMetricConstructorParameter.class);
    }

    private static class AbsoluteGivenLocalNameMetricConstructorParameter {
        @SuppressWarnings("unused")
        AbsoluteGivenLocalNameMetricConstructorParameter(@Metric(name = "c1", absolute = true) Counter counter) { /* ... */ }
    }
    @Test
    public void absoluteGivenLocalNameMetricConstructorParameter() {
        assertNamedParamter("c1", AbsoluteGivenLocalNameMetricConstructorParameter.class);
    }

    private static class RelativeInferredLocalNameConstructorParameter {
        @SuppressWarnings("unused")
        RelativeInferredLocalNameConstructorParameter(@Metric Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void relativeInferredLocalNameConstructorParameter() {
        assertNamedParamter("<this>.RelativeInferredLocalNameConstructorParameter.arg0", RelativeInferredLocalNameConstructorParameter.class);
    }

    private static class AbsoluteInferredLocalNameConstructorParameter {
        @SuppressWarnings("unused")
        AbsoluteInferredLocalNameConstructorParameter(@Metric(absolute = true) Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void absoluteInferredLocalNameConstructorParameter() {
        assertNamedParamter("arg0", AbsoluteInferredLocalNameConstructorParameter.class);
    }

    /*
     * Gauges...
     */

    /*
     * Test Metadata is correct (name is not tested again)
     */

    /*
     * Test annotated element annotations replace class level annotations
     */

    /*
     * Helpers...
     */

    private void assertNamed(String expected) {
        assertNamed(expected, getClass(), TestUtils.getTestMethod());
    }

    private void assertNamed(String expected, Class<?> bean) {
        // its a convention in this test that for a bean the first declared method is the annotated one
        int tested = 0;
        Method method = TestUtils.firstDeclaredMethod(bean);
        if (method != null) {
            assertNamed(expected, bean, method);
            tested++;
        }
        Field field = TestUtils.firstDeclaredField(bean);
        if (field != null) {
            assertNamed(expected, bean, field);
            tested++;
        }
        if (tested == 0) {
            Constructor<?> constructor = TestUtils.firstDeclaredConstructor(bean);
            if (constructor != null) {
                assertNamed(expected, bean, constructor);
                tested++;
            }
        }
        assertTrue("No members found to test", tested > 0);
    }

    private static final List<Class<? extends Annotation>> IGNORED_ANNOTATIONS = asList(Test.class, Produces.class, SuppressWarnings.class);
    private <E extends AnnotatedElement & Member> void assertNamed(String expected, Class<?> bean, E element) {
        expected = expected.replace("<this>", getClass().getCanonicalName());
        final Set<Class<?>> expectedReads = getExpectedReadAnnotations(bean, element);
        final Set<Class<?>> actualReads = new HashSet<>();
        String msg = "wrong name for " + element.getClass().getSimpleName() + " " + element.getName() + ", ";
        for (AnnotationReader<?> reader : AnnotationReader.readers()) {
            if (reader.isPresent(bean, element)) {
                assertNamed(msg, expected, bean, element, reader);
                actualReads.add(reader.annotationType());
            }
        }
        assertEquals("At least one annotation was not processed for bean class " + bean, expectedReads, actualReads);
    }

    private static <E extends AnnotatedElement & Member> Set<Class<?>> getExpectedReadAnnotations(Class<?> bean, E element) {
        final Set<Class<?>> expectedReads = asList(element.getAnnotations()).stream()
                .map(a -> a.annotationType())
                .filter(type -> !IGNORED_ANNOTATIONS.contains(type))
                .collect(toCollection(HashSet::new));
        expectedReads.addAll(asList(bean.getAnnotations()).stream().map(a -> a.annotationType()).collect(toSet()));
        return expectedReads;
    }

    private void assertNamedParamter(String expected, Class<?> bean) {
        expected = expected.replace("<this>", getClass().getCanonicalName());
        Method method = TestUtils.firstDeclaredMethod(bean);
        if (method != null && method.getParameterCount() == 1) {
            assertNamed(expected, method.getParameters()[0]);
            return;
        }
        Constructor<?> constructor = TestUtils.firstDeclaredConstructor(bean);
        if (constructor != null && constructor.getParameterCount() == 1) {
            assertNamed(expected, constructor.getParameters()[0]);
            return;
        }
        fail("No method or constructor with parameter found in bean class " + bean);
    }

    private static void assertNamed(String expected, Parameter param) {
        String msg = "wrong name for parameter " + param.getName() + ", ";
        InjectionPoint point = TestUtils.fakeInjectionPointFor(param);
        assertEquals(msg, expected, AnnotationReader.METRIC.name(point));
        assertEquals(msg, expected, AnnotationReader.METRIC.metricID(point).getName());
        assertEquals(msg, expected, AnnotationReader.METRIC.metadata(point).getName());
    }

    private static <E extends AnnotatedElement & Member, A extends Annotation> void assertNamed(String msg,
            String expected, Class<?> bean, E element, AnnotationReader<A> reader) {
        // test name resolved via bean and element
        assertEquals(msg, expected, reader.name(bean, element));
        assertEquals(msg, expected, reader.metricID(bean, element).getName());
        assertEquals(msg, expected, reader.metadata(bean, element).getName());

        // test name resolved via InjectionPoint
        InjectionPoint point = TestUtils.fakeInjectionPointFor(element, element);
        assertEquals(msg, expected, reader.name(point));
        assertEquals(msg, expected, reader.metricID(point).getName());
        assertEquals(msg, expected, reader.metadata(point).getName());

        // test name resolved via AnnotatedMember
        AnnotatedMember<?> annotated = TestUtils.fakeMemberOf(element, element);
        assertEquals(msg, expected, reader.name(annotated));
        assertEquals(msg, expected, reader.metricID(annotated).getName());
        assertEquals(msg, expected, reader.metadata(annotated).getName());
    }

}
