/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  file and include the License.
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
package fish.payara.microprofile.metrics.cdi;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.junit.Test;

import fish.payara.microprofile.metrics.impl.ConcurrentGaugeImpl;
import fish.payara.microprofile.metrics.impl.CounterImpl;
import fish.payara.microprofile.metrics.impl.GaugeImpl;
import fish.payara.microprofile.metrics.impl.HistogramImpl;
import fish.payara.microprofile.metrics.impl.MeterImpl;
import fish.payara.microprofile.metrics.impl.SimpleTimerImpl;
import fish.payara.microprofile.metrics.impl.TimerImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Tests the formal correctness of the {@link AnnotationReader} utility.
 *
 * Unlike in real world usage the tests often stack multiple FT annotations to test all of them in one go. They all
 * should show a identical behaviour with respect to the property tested.
 *
 * Which annotated element is picked up is based on conventions. If no bean is passed explicitly the test
 * itself is the bean and the test method is the annotated element. If a bean is passed it is expected to only have one
 * field or method or constructor. Either the bean class or that element (or one of its parameters) is annotated.
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
    private static class BeanA {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredContextNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanA.name", BeanA.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class BeanB {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredContextNameMethod() {
        assertNamed("BeanB.name", BeanB.class);
    }

    @ConcurrentGauge(name = "contextMethod")
    @Counted(name = "contextMethod")
    @Metered(name = "contextMethod")
    @Timed(name = "contextMethod")
    @SimplyTimed(name = "contextMethod")
    private static class BeanC {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenContextNameMethod() {
        assertNamed(BeanC.class.getPackage().getName() + ".contextMethod.name", BeanC.class);
    }

    @ConcurrentGauge(name = "contextMethod", absolute = true)
    @Counted(name = "contextMethod", absolute = true)
    @Metered(name = "contextMethod", absolute = true)
    @Timed(name = "contextMethod", absolute = true)
    @SimplyTimed(name = "contextMethod", absolute = true)
    private static class BeanD {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenContextNameMethod() {
        assertNamed("contextMethod.name", BeanD.class);
    }

    /*
     * Test metric names for constructors
     */

    private static class BeanE {
        @ConcurrentGauge
        @Counted
        @Metered
        @Timed
        @SimplyTimed
        BeanE() { /* parameters not important here */ }
    }
    @Test
    public void relativeInferredNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanE.BeanE", BeanE.class);
    }

    private static class BeanF {
        @ConcurrentGauge(name = "localConstructor")
        @Counted(name = "localConstructor")
        @Metered(name = "localConstructor")
        @Timed(name = "localConstructor")
        @SimplyTimed(name = "localConstructor")
        BeanF() { /* parameters not important here */ }
    }
    @Test
    public void relativeGivenLocalNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanF.localConstructor", BeanF.class);
    }

    private static class BeanG {
        @ConcurrentGauge(absolute = true)
        @Counted(absolute = true)
        @Metered(absolute = true)
        @Timed(absolute = true)
        @SimplyTimed(absolute = true)
        BeanG() { /* parameters not important here */ }
    }
    @Test
    public void absoluteInferredNameConstructor() {
        assertNamed("BeanG", BeanG.class);
    }

    private static class BeanH {
        @ConcurrentGauge(name = "localConstructor", absolute = true)
        @Counted(name = "localConstructor", absolute = true)
        @Metered(name = "localConstructor", absolute = true)
        @Timed(name = "localConstructor", absolute = true)
        @SimplyTimed(name = "localConstructor", absolute = true)
        BeanH() { /* parameters not important here */ }
    }
    @Test
    public void absoluteGivenLocalNameConstructor() {
        assertNamed("localConstructor", BeanH.class);
    }

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class BeanI {
        @SuppressWarnings("unused")
        BeanI() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredContextNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanI.BeanI", BeanI.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class BeanJ {
        @SuppressWarnings("unused")
        BeanJ() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredContextNameConstructor() {
        assertNamed("BeanJ.BeanJ", BeanJ.class);
    }

    @ConcurrentGauge(name = "contextConstructor")
    @Counted(name = "contextConstructor")
    @Metered(name = "contextConstructor")
    @Timed(name = "contextConstructor")
    @SimplyTimed(name = "contextConstructor")
    private static class BeanK {
        @SuppressWarnings("unused")
        BeanK() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenContextNameConstructor() {
        assertNamed(BeanK.class.getPackage().getName() + ".contextConstructor.BeanK", BeanK.class);
    }

    @ConcurrentGauge(name = "contextConstructor", absolute = true)
    @Counted(name = "contextConstructor", absolute = true)
    @Metered(name = "contextConstructor", absolute = true)
    @Timed(name = "contextConstructor", absolute = true)
    @SimplyTimed(name = "contextConstructor", absolute = true)
    private static class BeanL {
        @SuppressWarnings("unused")
        BeanL() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenContextNameConstructor() {
        assertNamed("contextConstructor.BeanL", BeanL.class);
    }


    /*
     * Test metric names for methods when annotated on super class
     */

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class BeanMx { /* not important here */ }
    private static class BeanM extends BeanMx {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredSuperContextNameMethod() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanM.name", BeanM.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class BeanNx { /* not important here */ }
    private static class BeanN extends BeanNx {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredSuperContextNameMethod() {
        assertNamed("BeanN.name", BeanN.class);
    }

    @ConcurrentGauge(name = "contextMethod")
    @Counted(name = "contextMethod")
    @Metered(name = "contextMethod")
    @Timed(name = "contextMethod")
    @SimplyTimed(name = "contextMethod")
    private static class BeanOx { /* not important here */ }
    private static class BeanO extends BeanOx {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenSuperContextNameMethod() {
        assertNamed(BeanO.class.getPackage().getName() + ".contextMethod.name", BeanO.class);
    }

    @ConcurrentGauge(name = "contextMethod", absolute = true)
    @Counted(name = "contextMethod", absolute = true)
    @Metered(name = "contextMethod", absolute = true)
    @Timed(name = "contextMethod", absolute = true)
    @SimplyTimed(name = "contextMethod", absolute = true)
    private static class BeanPX { /* not important here */ }
    private static class BeanP extends BeanPX {
        @SuppressWarnings("unused")
        void name() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenSuperContextNameMethod() {
        assertNamed("contextMethod.name", BeanP.class);
    }

    /*
     * Test metric names for constructors when annotated on super class
     */

    @ConcurrentGauge
    @Counted
    @Metered
    @Timed
    @SimplyTimed
    private static class BeanQx { /* not important here */ }
    private static class BeanQ extends BeanQx {
        @SuppressWarnings("unused")
        BeanQ() { /* signature not important here */ }
    }
    @Test
    public void relativeInferredSuperContextNameConstructor() {
        // OBS "<this>" is substituted with canonical name of test class (shorthand for better readability)
        assertNamed("<this>.BeanQ.BeanQ", BeanQ.class);
    }

    @ConcurrentGauge(absolute = true)
    @Counted(absolute = true)
    @Metered(absolute = true)
    @Timed(absolute = true)
    @SimplyTimed(absolute = true)
    private static class BeanRx { /* not important here */ }
    private static class BeanR extends BeanRx {
        @SuppressWarnings("unused")
        BeanR() { /* signature not important here */ }
    }
    @Test
    public void absoluteInferredSuperContextNameConstructor() {
        assertNamed("BeanR.BeanR", BeanR.class);
    }

    @ConcurrentGauge(name = "contextConstructor")
    @Counted(name = "contextConstructor")
    @Metered(name = "contextConstructor")
    @Timed(name = "contextConstructor")
    @SimplyTimed(name = "contextConstructor")
    private static class BeanSx { /* not important here */ }
    private static class BeanS extends BeanSx {
        @SuppressWarnings("unused")
        BeanS() { /* signature not important here */ }
    }
    @Test
    public void relativeGivenSuperContextNameConstructor() {
        assertNamed(BeanS.class.getPackage().getName() + ".contextConstructor.BeanS", BeanS.class);
    }

    @ConcurrentGauge(name = "contextConstructor", absolute = true)
    @Counted(name = "contextConstructor", absolute = true)
    @Metered(name = "contextConstructor", absolute = true)
    @Timed(name = "contextConstructor", absolute = true)
    @SimplyTimed(name = "contextConstructor", absolute = true)
    private static class BeanTx { /* not important here */ }
    private static class BeanT extends BeanTx {
        @SuppressWarnings("unused")
        BeanT() { /* signature not important here */ }
    }
    @Test
    public void absoluteGivenSuperContextNameConstructor() {
        assertNamed("contextConstructor.BeanT", BeanT.class);
    }


    /*
     * Metric names when using @Metric on Fields and Methods
     */

    private static class BeanU {

        @Produces
        @Metric
        Counter method() { return null; }
    }
    @Test
    public void relativeInferredLocalNameMetricMethod() {
        assertNamed("<this>.BeanU.method", BeanU.class);
    }

    private static class BeanV {
        @Metric
        Counter field;
    }
    @Test
    public void relativeInferredLocalNameMetricField() {
        assertNamed("<this>.BeanV.field", BeanV.class);
    }

    private static class BeanW {

        @Produces
        @Metric(name = "producer")
        Counter method() { return null; }
    }
    @Test
    public void relativeGivenLocalNameMetricMethod() {
        assertNamed("<this>.BeanW.producer", BeanW.class);
    }

    private static class BeanX {
        @Metric(name = "counter")
        Counter field;
    }
    @Test
    public void relativeGivenLocalNameMetricField() {
        assertNamed("<this>.BeanX.counter", BeanX.class);
    }

    private static class BeanY {

        @Produces
        @Metric(name = "producer", absolute = true)
        Counter method() { return null; }
    }
    @Test
    public void absoluteGivenLocalNameMetricMethod() {
        assertNamed("producer", BeanY.class);
    }

    private static class BeanZ {
        @Metric(name = "counter", absolute = true)
        Counter field;
    }
    @Test
    public void absoluteGivenLocalNameMetricField() {
        assertNamed("counter", BeanZ.class);
    }

    private static class BeanAA {

        @Produces
        @Metric(absolute = true)
        Counter method() { return null; }
    }
    @Test
    public void absoluteInferredLocalNameMetricMethod() {
        assertNamed("method", BeanAA.class);
    }

    private static class BeanAB {
        @Metric(absolute = true)
        Counter field;
    }
    @Test
    public void absoluteInferredLocalNameMetricField() {
        assertNamed("field", BeanAB.class);
    }

    /*
     * Metric on Parameters
     */

    private static class BeanAC {
        @SuppressWarnings("unused")
       void  method(@Metric(name = "c1") Counter counter) { /* ... */ }
    }
    @Test
    public void relativeGivenLocalNameMetricMethodParameter() {
        assertNamedParamter("<this>.BeanAC.c1", BeanAC.class);
    }

    private static class BeanAD {
        @SuppressWarnings("unused")
        void method(@Metric(name = "c1", absolute = true) Counter counter) { /* ... */ }
    }
    @Test
    public void absoluteGivenLocalNameMetricMethodParameter() {
        assertNamedParamter("c1", BeanAD.class);
    }

    private static class BeanAE {
        @SuppressWarnings("unused")
        void method(@Metric Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void relativeInferredLocalNameMethodParameter() {
        assertNamedParamter("<this>.BeanAE.arg0", BeanAE.class);
    }

    private static class BeanAF {
        @SuppressWarnings("unused")
        void method(@Metric(absolute = true) Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void absoluteInferredLocalNameMethodParameter() {
        assertNamedParamter("arg0", BeanAF.class);
    }

    private static class BeanAG {
        @SuppressWarnings("unused")
        BeanAG(@Metric(name = "c1") Counter counter) { /* ... */ }
    }
    @Test
    public void relativeGivenLocalNameMetricConstructorParameter() {
        assertNamedParamter("<this>.BeanAG.c1", BeanAG.class);
    }

    private static class BeanAH {
        @SuppressWarnings("unused")
        BeanAH(@Metric(name = "c1", absolute = true) Counter counter) { /* ... */ }
    }
    @Test
    public void absoluteGivenLocalNameMetricConstructorParameter() {
        assertNamedParamter("c1", BeanAH.class);
    }

    private static class BeanAI {
        @SuppressWarnings("unused")
        BeanAI(@Metric Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void relativeInferredLocalNameConstructorParameter() {
        assertNamedParamter("<this>.BeanAI.arg0", BeanAI.class);
    }

    private static class BeanAJ {
        @SuppressWarnings("unused")
        BeanAJ(@Metric(absolute = true) Counter arg0) {
            /* naming it arg0 is somewhat of a hack to make this work on older and newer JREs */ }
    }
    @Test
    public void absoluteInferredLocalNameConstructorParameter() {
        assertNamedParamter("arg0", BeanAJ.class);
    }

    /*
     * Test Metadata is correct (name is not tested again)
     */


    @Test
    @Counted(name = "name", absolute = true, description = "description", displayName = "displayName",
        reusable = true, unit = "unit", tags = { "a=b", "c=d" })
    @ConcurrentGauge(name = "name", absolute = true, description = "description", displayName = "displayName",
        reusable = true, unit = "unit", tags = { "a=b", "c=d" })
    @Metered(name = "name", absolute = true, description = "description", displayName = "displayName",
        reusable = true, unit = "unit", tags = { "a=b", "c=d" })
    @Timed(name = "name", absolute = true, description = "description", displayName = "displayName",
        reusable = true, unit = "unit", tags = { "a=b", "c=d" })
    @SimplyTimed(name = "name", absolute = true, description = "description", displayName = "displayName",
        reusable = true, unit = "unit", tags = { "a=b", "c=d" })
    @Gauge(name = "name", absolute = true, description = "description", displayName = "displayName",
        unit = "unit", tags = { "a=b", "c=d" })
    public void metadataFromMethod() {
        assertMetadata();
    }

    private static class BeanAK {
        @Counted(name = "name", absolute = true, description = "description", displayName = "displayName",
                reusable = true, unit = "unit", tags = { "a=b", "c=d" })
        @ConcurrentGauge(name = "name", absolute = true, description = "description", displayName = "displayName",
            reusable = true, unit = "unit", tags = { "a=b", "c=d" })
        @Metered(name = "name", absolute = true, description = "description", displayName = "displayName",
            reusable = true, unit = "unit", tags = { "a=b", "c=d" })
        @Timed(name = "name", absolute = true, description = "description", displayName = "displayName",
            reusable = true, unit = "unit", tags = { "a=b", "c=d" })
        @SimplyTimed(name = "name", absolute = true, description = "description", displayName = "displayName",
            reusable = true, unit = "unit", tags = { "a=b", "c=d" })
        BeanAK() { /* not important */ }
    }
    @Test
    public void metadataFromConstructor() {
        assertMetadata(BeanAK.class);
    }

    /*
     * Test annotated element annotations replace class level annotations
     */

    @Counted(name = "not used")
    private static class BeanAL {

        @Counted(name = "used", absolute = true)
        void method() { /* not important */ }
    }
    @Test
    public void elementLevelOverridesClassLevelAnnotations() {
        assertNamed("used", BeanAL.class);
    }

    /*
     * @Metric is inferred to correct MetricType in Metadata
     */

    private static Map<Class<?>, MetricType> getMetricInterfaceExpectedMapping() {
        Map<Class<?>, MetricType> expected = new HashMap<>();
        expected.put(Counter.class, MetricType.COUNTER);
        expected.put(org.eclipse.microprofile.metrics.ConcurrentGauge.class, MetricType.CONCURRENT_GAUGE);
        expected.put(Meter.class, MetricType.METERED);
        expected.put(Timer.class, MetricType.TIMER);
        expected.put(SimpleTimer.class, MetricType.SIMPLE_TIMER);
        expected.put(Histogram.class, MetricType.HISTOGRAM);
        expected.put(org.eclipse.microprofile.metrics.Gauge.class, MetricType.GAUGE);
        return expected;
    }

    private static Map<Class<?>, MetricType> getMetricImplementationExpectedMapping() {
        Map<Class<?>, MetricType> expected = new HashMap<>();
        expected.put(CounterImpl.class, MetricType.COUNTER);
        expected.put(ConcurrentGaugeImpl.class, MetricType.CONCURRENT_GAUGE);
        expected.put(MeterImpl.class, MetricType.METERED);
        expected.put(TimerImpl.class, MetricType.TIMER);
        expected.put(SimpleTimerImpl.class, MetricType.SIMPLE_TIMER);
        expected.put(HistogramImpl.class, MetricType.HISTOGRAM);
        expected.put(GaugeImpl.class, MetricType.GAUGE);
        return expected;
    }

    private static class BeanAM {

        @Metric
        Counter counter() { return null; }
        @Metric
        org.eclipse.microprofile.metrics.ConcurrentGauge concurrentGauge() { return null; }
        @Metric
        Meter meter() { return null; }
        @Metric
        Timer timer() { return null; }
        @Metric
        SimpleTimer simpleTimer() { return null; }
        @Metric
        Histogram histogram() { return null; }
        @Metric
        org.eclipse.microprofile.metrics.Gauge<Long> gauge() { return null; }
    }
    @Test
    public void metricOnMethodReturningInterfaceType() {
        assertMetricType(getMetricInterfaceExpectedMapping(), BeanAM.class.getDeclaredMethods(), Method::getReturnType);
    }

    private static class BeanAN {

        @Metric
        CounterImpl counter() { return null; }
        @Metric
        ConcurrentGaugeImpl concurrentGauge() { return null; }
        @Metric
        MeterImpl meter() { return null; }
        @Metric
        TimerImpl timer() { return null; }
        @Metric
        SimpleTimerImpl simpleTimer() { return null; }
        @Metric
        HistogramImpl histogram() { return null; }
        @Metric
        GaugeImpl<?> gauge() { return null; }
    }
    @Test
    public void metricOnMethodReturningImplementationType() {
        assertMetricType(getMetricImplementationExpectedMapping(), BeanAN.class.getDeclaredMethods(), Method::getReturnType);
    }

    private static class BeanAO {

        @Metric
        Counter counter;
        @Metric
        org.eclipse.microprofile.metrics.ConcurrentGauge concurrentGauge;
        @Metric
        Meter meter;
        @Metric
        Timer timer;
        @Metric
        SimpleTimer simpleTimer;
        @Metric
        Histogram histogram;
        @Metric
        org.eclipse.microprofile.metrics.Gauge<Long> gauge;
    }
    @Test
    public void metricOFieldReturningInterfaceType() {
        assertMetricType(getMetricInterfaceExpectedMapping(), BeanAO.class.getDeclaredFields(), Field::getType);
    }

    private static class BeanAP {

        @Metric
        CounterImpl counter;
        @Metric
        ConcurrentGaugeImpl concurrentGauge;
        @Metric
        MeterImpl meter;
        @Metric
        TimerImpl timer;
        @Metric
        SimpleTimerImpl simpleTimer;
        @Metric
        HistogramImpl histogram;
        @Metric
        GaugeImpl<?> gauge;
    }
    @Test
    public void metricOnFidleReturningImplementationType() {
        assertMetricType(getMetricImplementationExpectedMapping(), BeanAP.class.getDeclaredFields(), Field::getType);
    }

    /*
     * Helpers...
     */

    private static <E extends AnnotatedElement & Member> void assertMetricType(Map<Class<?>, MetricType> expected,
            E[] elements, Function<E, Class<?>> actualType) {
        AnnotationReader<Metric> reader = AnnotationReader.METRIC;
        for (E element : elements) {
            if (!element.isSynthetic()) {
                MetricType expectedType = expected.get(actualType.apply(element));
                Class<?> bean = element.getDeclaringClass();
                assertEquals(expectedType, reader.metadata(bean, element).getTypeRaw());
                assertEquals(expectedType, reader.metadata(TestUtils.fakeInjectionPointFor(element, element)).getTypeRaw());
                assertEquals(expectedType, reader.metadata(TestUtils.fakeMemberOf(element, element)).getTypeRaw());
            }
        }
    }

    private void assertMetadata() {
        assertMetadata(getClass(), TestUtils.getTestMethod());
    }

    private static void assertMetadata(Class<?> bean) {
        Method method = TestUtils.firstDeclaredMethod(bean);
        if (method != null) {
            assertMetadata(bean, method);
            return;
        }
        Constructor<?> constructor = TestUtils.firstDeclaredConstructor(bean);
        if (constructor != null) {
            assertMetadata(bean, constructor);
            return;
        }
        fail("No method or constructor found to test");
    }

    private static <E extends AnnotatedElement & Member> void assertMetadata(Class<?> bean, E element) {
        final Set<Class<?>> expectedReads = getExpectedReadAnnotations(bean, element);
        final Set<Class<?>> actualReads = new HashSet<>();
        for (AnnotationReader<?> reader : AnnotationReader.readers()) {
            if (reader.isPresent(bean, element)) {
                assertData(reader.type(), bean, element, reader);
                actualReads.add(reader.annotationType());
            }
        }
        assertEquals("At least one annotation was not processed for bean class " + bean, expectedReads, actualReads);
    }

    private static final Tag[] expectedTags = new Tag[] { new Tag("a", "b"), new Tag("c", "d") };

    private static <E extends AnnotatedElement & Member, A extends Annotation> void assertData(MetricType expected,
            Class<?> bean, E element, AnnotationReader<A> reader) {
        assertMetadataOverride(expected, reader.metadata(bean, element));
        assertMetadataOverride(expected, reader.metadata(TestUtils.fakeMemberOf(element, element)));
        InjectionPoint point = TestUtils.fakeInjectionPointFor(element, element);
        assertMetadataOverride(expected, reader.metadata(point));
        assertArrayEquals(expectedTags, reader.tags(reader.annotation(point)));
    }

    private static void assertMetadataOverride(MetricType expected, Metadata metadata) {
        assertEquals(expected, metadata.getTypeRaw());
        assertEquals("name", metadata.getName());
        assertEquals("description", metadata.getDescription().get());
        assertEquals("displayName", metadata.getDisplayName());
        assertEquals("unit", metadata.getUnit().get());
        assertEquals("reuasable", expected != MetricType.GAUGE, metadata.isReusable());
    }

    private void assertNamed(String expected) {
        assertNamed(expected, getClass(), TestUtils.getTestMethod());
    }

    private void assertNamed(String expected, Class<?> bean) {
        Method method = TestUtils.firstDeclaredMethod(bean);
        if (method != null) {
            assertNamed(expected, bean, method);
            return;
        }
        Field field = TestUtils.firstDeclaredField(bean);
        if (field != null) {
            assertNamed(expected, bean, field);
            return;
        }
        Constructor<?> constructor = TestUtils.firstDeclaredConstructor(bean);
        if (constructor != null) {
            assertNamed(expected, bean, constructor);
            return;
        }
        fail("No method, field or constructor found to test");
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
        assertEquals("At least one annotation was not processed for " + element, expectedReads, actualReads);
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

        // additional test that any setup never leaves the Metadata with invalid type
        assertNotEquals(MetricType.INVALID, reader.metadata(point).getTypeRaw());
        assertNotEquals(MetricType.INVALID, reader.metadata(annotated).getTypeRaw());
        assertNotEquals(MetricType.INVALID, reader.metadata(bean, element).getTypeRaw());
    }

}
