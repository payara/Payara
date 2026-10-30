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
package fish.payara.microprofile.faulttolerance.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;

public class TestUtils {

    public static Object[] createNullArgumentsFor(Executable method) {
        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameterTypes()[i].isPrimitive()) {
                args[i] = Integer.valueOf(0);
            }
        }
        return args;
    }

    public static Method getMethod(Class<?> target, String name) {
        for (Method m : target.getDeclaredMethods()) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        if (target.getSuperclass() != Object.class) {
            return getMethod(target.getSuperclass(), name);
        }
        fail("Test setup failure: no method with name: "+name);
        return null;
    }

    public static void assertAnnotationInvalid(String expectedErrorMessage) {
        try {
            Method annotatedMethod = getAnnotatedMethod();
            FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(annotatedMethod.getDeclaringClass(),
                    annotatedMethod);
            fail("Annotation should be invalid for " + annotatedMethod + " but got: " + policy);
        } catch (FaultToleranceDefinitionException ex) {
            String message = ex.getMessage();
            int endGeneralPart = message.indexOf(" annotated with ");
            assertTrue(endGeneralPart > 0);
            assertEquals(expectedErrorMessage, message.substring(endGeneralPart + 16));
        }
    }

    public static Method getAnnotatedMethod() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int i = 0;
        while (!isTestMethod(stackTraceElements[i])) {
            i++;
        }
        StackTraceElement testMethodElement = stackTraceElements[i];
        String testName = testMethodElement.getMethodName();
        try {
            return TestUtils.getMethod(Class.forName(testMethodElement.getClassName()), testName + "_Method");
        } catch (Exception e) {
            throw new AssertionError("Failed to find annotated method in test class: ", e);
        }
    }

    private static boolean isTestMethod(StackTraceElement element) {
        if (!element.getClassName().endsWith("Test")) {
            return false;
        }
        try {
            Class<?> testClass = Class.forName(element.getClassName());
            Method elementMethod = testClass.getMethod(element.getMethodName());
            return elementMethod.isAnnotationPresent(org.junit.Test.class);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses text form of a {@link MetricID} back to object.
     *
     * This supports both the result of calling {@link MetricID#toString()} as well as a simplified more user friendly form illustrated in the examples below:
     *
     * <pre>
     * my-metric-name
     * my-metric-name[tag=value,tag=value]
     * </pre>
     *
     * @param metric text form of a {@link MetricID}
     * @return
     */
    public static MetricID parseMetricID(String metric) {
        int startOfTags = metric.indexOf('[');
        if (startOfTags < 0) { // no tags, must be simple format
            return new MetricID(metric);
        }
        int endOfTags = metric.indexOf(']');
        String[] tagNameValues = metric.substring(startOfTags + 1, endOfTags).split(",");
        Tag[] tags = new Tag[tagNameValues.length];
        for (int i = 0; i < tagNameValues.length; i++) {
            String tag = tagNameValues[i];
            int endOfName = tag.indexOf('=');
            String value = tag.substring(endOfName + 1);
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() -1);
            }
            tags[i] = new Tag(tag.substring(0, endOfName), value);
        }
        if (metric.startsWith("MetricID{")) {
            int startOfName = metric.indexOf('\'');
            return new MetricID(metric.substring(startOfName+1, metric.indexOf('\'', startOfName+1)), tags);
        }
        return new MetricID(metric.substring(0, startOfTags), tags);
    }
}
