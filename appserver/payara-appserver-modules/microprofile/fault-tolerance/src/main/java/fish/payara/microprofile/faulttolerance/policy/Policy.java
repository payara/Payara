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
package fish.payara.microprofile.faulttolerance.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Contains general helper method for FT policy validation.
 * 
 * @author Jan Bernitt
 */
public abstract class Policy implements Serializable {

    public static void checkAtLeast(long minimum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, long value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                     + "value less than " + minimum + ": " + value);
        }
    }

    public static void checkAtLeast(double minimum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, double value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "value less than " + minimum + ": " + value);
        }
    }

    public static void checkAtLeast(String attribute1, long minimum, Method annotatedMethod,
            Class<? extends Annotation> annotationType, String attribute2, long value) {
        if (value < minimum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute2)
                    + "value less than or equal to the " + attribute(attribute1) + "value: " + value);
        }
    }

    public static void checkAtMost(double maximum, Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, double value) {
        if (value > maximum) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "value greater than " + maximum + ": " + value);
        }
    }

    public static void checkReturnsSameAs(Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, Class<?> valueType, String valueMethodName, Class<?>... valueParameterTypes) {
        try {
            Method actual = valueType.getDeclaredMethod(valueMethodName, valueParameterTypes);
            checkReturnsSameAs(annotatedMethod, annotationType, attribute, actual);
        } catch (NoSuchMethodException ex) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "refering to a method "+valueMethodName+" that does not exist for type: " + valueType.getName(), ex);
        }
    }

    public static void checkReturnsSameAs(Method annotatedMethod, Class<? extends Annotation> annotationType,
            String attribute, Method value) {
        if (value.getReturnType() != annotatedMethod.getReturnType()) {
            throw new FaultToleranceDefinitionException(describe(annotatedMethod, annotationType, attribute)
                    + "value whose return type of does not match.");
        }
    }

    protected static String describe(Method annotatedMethod, Class<? extends Annotation> annotationType, String attribute) {
        return "Method \"" + annotatedMethod.getName() + "\" in " + annotatedMethod.getDeclaringClass().getName()
                + " annotated with " + annotationType.getSimpleName()
                + (attribute.isEmpty() ? " " : " has a " + attribute(attribute));
    }

    private static String attribute(String attribute) {
        return "value".equals(attribute) ? "" : attribute + " ";
    }

    public static boolean isCaught(Exception ex, Class<? extends Throwable>[] caught) {
        if (caught.length == 0) {
            return false;
        }
        if (caught[0] == Throwable.class) {
            return true;
        }
        for (Class<? extends Throwable> caughtType : caught) {
            if (ex.getClass() == caughtType) {
                CircuitBreakerPolicy.logger.log(Level.FINER, "Exception {0} matches a Throwable", ex.getClass().getSimpleName());
                return true;
            }
            if (caughtType.isAssignableFrom(ex.getClass())) {
                CircuitBreakerPolicy.logger.log(Level.FINER, "Exception {0} is a child of a Throwable: {1}",
                        new String[] { ex.getClass().getSimpleName(), caughtType.getSimpleName() });
                return true;
            }
        }
        return false;
    }
}
