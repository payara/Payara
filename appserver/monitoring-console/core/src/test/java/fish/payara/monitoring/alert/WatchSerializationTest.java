/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.alert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import com.sun.xml.internal.xsom.impl.scd.Iterators.Map;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.Unit;

/**
 * Tests the correctness of the {@link Watch#toJSON()} and {@link Watch#fromJSON(String)} methods and those of the
 * parts.
 * 
 * @author Jan Bernitt
 */
public class WatchSerializationTest {

    private Watch sample1 = new Watch("name", new Metric(new Series("series"), Unit.COUNT), false,
            new Circumstance(Level.RED, new Condition(Operator.GT, 200).forLastMillis(3000)),
            new Circumstance(Level.AMBER, new Condition(Operator.LE, 300).forLastTimes(5)),
            new Circumstance(Level.GREEN, new Condition(Operator.GE, 0)));

    private Watch sample2 = new Watch("name", new Metric(new Series("series"), Unit.COUNT), false,
            new Circumstance(Level.RED, new Condition(Operator.GT, 200).forLastMillis(3000), 
                    new Condition(Operator.LT, 100).forLastMillis(2000).onAverage()),
            new Circumstance(Level.AMBER, new Condition(Operator.LE, 300).forLastTimes(0), 
                    new Condition(Operator.EQ, -4).forLastTimes(-5)),
            new Circumstance(Level.GREEN, new Condition(Operator.GE, 0).forLastTimes(4).onAverage()),
            new Metric(new Series("capturedSeries"), Unit.MILLIS));

    @Test
    public void singlePass() {
        assertEqualWatches(sample1, toAndFromJson(sample1));
        assertEqualWatches(sample2, toAndFromJson(sample2));
    }

    @Test
    public void doublePass() {
        assertEqualWatches(sample1, toAndFromJson(toAndFromJson(sample1)));
        assertEqualWatches(sample2, toAndFromJson(toAndFromJson(sample2)));
    }

    private static void assertEqualWatches(Watch expected, Watch actual) {
        assertNotSame(expected, actual);
        assertEquals(expected, actual);
        assertEqualFields(expected, actual);
        assertEquals(expected.toJSON().toString(), actual.toJSON().toString());
        assertEquals(expected.toString(), actual.toString());
        assertEquals(expected.isDisabled(), actual.isDisabled());
        assertEquals(expected.isStopped(), actual.isStopped());
        assertEquals(expected.isProgrammatic(), actual.isProgrammatic());
    }

    private static Watch toAndFromJson(Watch w) {
        return Watch.fromJSON(w.toJSON().toString());
    }

    private static <T> void assertEqualFields(T a, T b) {
        if (a == null || b == null) {
            assertEquals(a, b);
            return;
        }
        Class<?> type = a.getClass();
        for (Field f : type.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                try {
                    f.setAccessible(true);
                    Class<?> fieldType = f.getType();
                    Object aVal = f.get(a);
                    Object bVal = f.get(b);
                    if (isSimpleType(fieldType) ) {
                        assertEquals(aVal, bVal);
                    } else if (fieldType.isArray()) {
                        assertArrayEquals((Object[]) aVal, (Object[]) bVal);
                    } else {
                        assertEqualFields(aVal, bVal);
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new AssertionError("Failed to compare field " + f.getName() + " of " + type.getSimpleName(),
                            ex);
                }
            }
        }
    }

    private static boolean isSimpleType(Class<?> fieldType) {
        return fieldType.isPrimitive() 
                || fieldType.isEnum()
                || fieldType == String.class 
                || fieldType == Metric.class 
                || Number.class.isAssignableFrom(fieldType) 
                || Map.class.isAssignableFrom(fieldType);
    }
}
