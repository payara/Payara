/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.collect;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fish.payara.monitoring.store.MonitoringDataConsumer;
import fish.payara.monitoring.store.ConsumingMonitoringDataCollector;

/**
 * Component test testing the {@link ConsumingMonitoringDataCollector} implementation semantics.
 * 
 * This means does the combination of {@link MonitoringDataCollector#tag(CharSequence, CharSequence)} and
 * {@link MonitoringDataCollector#collect(CharSequence, long)} calls used by a {@link MonitoringDataSource} leed to the
 * expected sequence of {@link MonitoringDataConsumer#accept(CharSequence, long)} calls.
 * 
 * @author Jan Bernitt
 */
public class ConsumingMonitoringDataCollectorTest implements MonitoringDataSource {

    /**
     * The test class itself is the source used to test
     */
    @Override
    public void collect(MonitoringDataCollector collector) {
        // testing tags and context
        collector
            .collect("plain", 1)
            .collect("plainChained", 2);
        collector
            .collect("plainRestarted", 3);
        collector.tag("sub", "one")
            .collect("subFirst", 4)
            .collect("subChained", 5);
        collector.tag("sub", "one")
            .collect("subRestarted", 6)
            .tag("sub", "two").collect("resubbed", 7)
                .tag("subsub", "three").collect("doubleTagged", 9)
                .tag("subsub", "four").collect("doubleTaggedReplaced", 10)
            .tag("sub", "five").collect("reset", 11);
        collector
            .collect("plainAfterSub", 8)
            .tag("ignoredTagSinceNull", null).collect("plainIgnoredTag", 13L)
            .tag("igniredTagSinceEmpty", "").collect("plainIgnoredEmptyTag", 14L);
        collector.tag("complex", "sp aced; str,\u1F408ange").collect("sub", 1);

        // testing simple value conversion
        collector
            .collectNonZero("ignoredSinceZero", 0L)
            .collectNonZero("notIgnoredSinceNonZero", 12)
            .collect("true", true)
            .collect("false", false)
            .collect("ignoredBooleanSinceNull", (Boolean) null)
            .collect("notIgnoredBooleanSinceNonNull", Boolean.TRUE)
            .collect("character", 'a')
            .collect("double", 42.8765d)
            .collect("float", 1.3f)
            .collect("ignoredInstantSinceNull", (Instant) null)
            .collect("notIgnoredInstantSinceNonNull", Instant.ofEpochMilli(42L))
            .collect("ignoredNumberSinceNull", (Number) null)
            .collect("doubleNumber", new Double(45.6789d))
            .collect("floatNumber", new Float(12.34f))
            .collect("integerNumber", new Integer(13))
            .collect("longNumber", new Long(999L));

        // testing objects 
        collector
            .group("obj")
            .collectObject(null, (col, obj) -> { fail("Should never be called for null object."); })
            .collectObject("SomeObject", (col, obj) -> col.tag("sub", obj)
                    .collect("length", obj.length())
                    .collect("mIsAt", obj.indexOf('m')));

        // testing collections of objects
        collector
            .group("obj")
            .collectObjects(null, (col, obj) -> { fail("Should never be called for null object."); })
            .collectObjects(emptyList(), (col, obj) -> { fail("Should never be called for empty list."); })
            .collectObjects(asList("Foo", "Bar"), (col, obj) -> col.tag("sub", obj)
                    .collect("length", obj.length()));
        collector
            .group("emptyMapEntry")
            .collectAll(null, (col, obj) -> { fail("Should never be called for null map"); })
            .collectAll(emptyMap(), (col, obj) -> { fail("Should never be called for empty map"); })
            .group("mapEntry")
            .collectAll(singletonMap("foo", "bar"), (col, value) -> col
                    .collect("valueLength", value.length()));
    }

    private final Map<String, Long> dataPoints = new HashMap<>();

    @Before
    public void collectDataPoints() {
        dataPoints.clear();
        MonitoringDataCollector collector = new ConsumingMonitoringDataCollector((series, value) -> dataPoints.put(series.toString(), value));
        collect(collector);
    }

    @Test
    public void plainMetricHasNoTags() {
        assertDataPoint("plain", 1L);
    }

    @Test
    public void plainChainedHasNoTags() {
        assertDataPoint("plainChained", 2L);
    }

    @Test
    public void plainRestartedHasNoTags() {
        assertDataPoint("plainRestarted", 3L);
    }

    @Test
    public void plainAfterSubHasNoTags() {
        assertDataPoint("plainAfterSub", 8L);
    }

    @Test
    public void subFirstHasTag() {
        assertDataPoint("sub:one subFirst", 4L);
    }

    @Test
    public void subChainedHasTag() {
        assertDataPoint("sub:one subChained", 5L);
    }

    @Test
    public void subRestartedHasTag() {
        assertDataPoint("sub:one subRestarted", 6L);
    }

    @Test
    public void sameTagDoesReplaceExistingTag() {
        assertDataPoint("sub:two resubbed", 7L);
    }

    @Test
    public void differntTagDoesNotReplaceExistingTag() {
        assertDataPoint("sub:two subsub:three doubleTagged", 9L);
    }

    @Test
    public void sameSubTagDoesReplaceExistingSubTag() {
        assertDataPoint("sub:two subsub:four doubleTaggedReplaced", 10L);
    }

    @Test
    public void sameTagDoesReplaceExistingTagFromThatTagOn() {
        assertDataPoint("sub:five reset", 11L);
    }
    
    @Test
    public void tagwithUnusualCharacters() {
        assertDataPoint("complex:sp_aced__str_\u1F408ange sub", 1);
    }

    @Test
    public void tagWithNullValueIsIgnored() {
        assertDataPoint("plainIgnoredTag", 13L);
    }

    @Test
    public void tagWithEmptyValueIsIgnored() {
        assertDataPoint("plainIgnoredEmptyTag", 14L);
    }

    @Test
    public void zeroIsIgnoredWhenUsingNonZero() {
        assertNoDataPoint("ignoredSinceZero");
    }

    @Test
    public void nonZeroIsNotIgnoredWhenUsingNonZero() {
        assertDataPoint("notIgnoredSinceNonZero", 12L);
    }

    @Test
    public void trueBecomesOne() {
        assertDataPoint("true", 1L);
    }

    @Test
    public void falseBecomesZero() {
        assertDataPoint("false", 0L);
    }

    @Test
    public void nullBooleanIsIgnored() {
        assertNoDataPoint("ignoredBooleanSinceNull");
    }

    @Test
    public void nonNullBooleanIsNotIgnored() {
        assertDataPoint("notIgnoredBooleanSinceNonNull", 1L);
    }

    @Test
    public void characterBecomesItsNumericValue() {
        assertDataPoint("character", 97L);
    }

    @Test
    public void doubleHasFourFixedDecimals() {
        assertDataPoint("double", 42_8765L);
    }

    @Test
    public void floatHasFourFixedDecimals() {
        assertDataPoint("float", 1_3000L);
    }

    @Test
    public void nullInstantIsIgnored() {
        assertNoDataPoint("ignoredInstantSinceNull");
    }

    @Test
    public void nonNullInstantIsNotIgnored() {
        assertDataPoint("notIgnoredInstantSinceNonNull", 42L);
    }

    @Test
    public void nullNumberIsIgnored() {
        assertNoDataPoint("ignoredNumberSinceNull");
    }

    @Test
    public void nonNullDoubleNumberIsHandledAsPrimitiveDouble() {
        assertDataPoint("doubleNumber", 45_6789L);
    }

    @Test
    public void nonNullFloatNumberIsHandledAsPrimitiveDouble() {
        assertDataPoint("floatNumber", 12_3400L);
    }

    @Test
    public void nonNullIntegerNumberIsHandledAsPrimitiveLong() {
        assertDataPoint("integerNumber", 13L);
    }

    @Test
    public void nonNullLongNumberIsHandledAsPrimitiveLong() {
        assertDataPoint("longNumber", 999L);
    }

    @Test
    public void objectUsesCurrentContext() {
        assertDataPoint("@:obj sub:SomeObject length", 10L);
        assertDataPoint("@:obj sub:SomeObject mIsAt", 2L);
    }

    @Test
    public void objectsRunsConsumerForAllItemsInCollectionWhileUsingTheCurrentContext() {
        assertDataPoint("@:obj sub:Foo length", 3L);
        assertDataPoint("@:obj sub:Bar length", 3L);
    }

    @Test
    public void mapEntriesAddGroupTag() {
        assertDataPoint("@:foo valueLength", 3L);
    }

    private void assertDataPoint(String key, long value) {
        Long actual = dataPoints.get(key);
        assertNotNull("No value for key: " + key, actual);
        assertEquals(value, actual.longValue());
    }

    private void assertNoDataPoint(String key) {
        assertTrue(!dataPoints.containsKey(key));
    }

}
