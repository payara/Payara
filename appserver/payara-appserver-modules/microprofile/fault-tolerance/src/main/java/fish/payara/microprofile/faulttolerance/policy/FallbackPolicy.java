/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.util.Objects;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import javassist.Modifier;

/**
 * The resolved "cached" information of a {@link Fallback} annotation an a specific method.
 *
 * @author Jan Bernitt
 */
public final class FallbackPolicy extends Policy {

    public final Class<? extends FallbackHandler<?>> value;
    public final String fallbackMethod;
    public final Method method;
    private final Class<? extends Throwable>[] applyOn;
    private final Class<? extends Throwable>[] skipOn;

    @SuppressWarnings("unchecked")
    public FallbackPolicy(Method annotated, Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        this(annotated, value, fallbackMethod, new Class[] { Throwable.class }, new Class[0]);
    }

    public FallbackPolicy(Method annotated, Class<? extends FallbackHandler<?>> value, String fallbackMethod,
            Class<? extends Throwable>[] applyOn, Class<? extends Throwable>[] skipOn) {
        checkUnambiguous(annotated, value, fallbackMethod);
        this.value = value;
        this.fallbackMethod = fallbackMethod;
        if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
            method = MethodLookupUtils.findMethodWithMatchingNameAndArguments(fallbackMethod, annotated);
            if (method == null) {
                throw new FaultToleranceDefinitionException(describe(annotated, Fallback.class, "fallbackMethod")
                        + "value referring to a method that is not defined or has a incompatible method signature.");
            }
            checkReturnsSameAs(annotated, Fallback.class, "fallbackMethod", method);
            checkAccessible(annotated, method);
        } else {
            method = null;
        }
        if (isHandlerPresent()) {
            checkReturnsSameAs(annotated, Fallback.class, "value", value, "handle", ExecutionContext.class);
        }
        this.applyOn = applyOn;
        this.skipOn = skipOn;
    }

    public static FallbackPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if (config.isAnnotationPresent(Fallback.class) && config.isEnabled(Fallback.class)) {
            Fallback annotation = config.getAnnotation(Fallback.class);
            return new FallbackPolicy(context.getMethod(),
                    config.value(annotation),
                    config.fallbackMethod(annotation),
                    config.applyOn(annotation),
                    config.skipOn(annotation));
        }
        return null;
    }

    private static void checkUnambiguous(Method annotated, Class<? extends FallbackHandler<?>> value, String fallbackMethod) {
        if (fallbackMethod != null && !fallbackMethod.isEmpty() && value != null && value != Fallback.DEFAULT.class) {
            throw new FaultToleranceDefinitionException(
                    describe(annotated, Fallback.class, "") + "defined both a fallback handler and a fallback method.");
        }
    }

    private static void checkAccessible(Method annotated, Method fallback) {
        boolean samePackage = Objects.equals(
            fallback.getDeclaringClass().getPackage(),
            annotated.getDeclaringClass().getPackage()
        );
        boolean sameClass = fallback.getDeclaringClass().equals(annotated.getDeclaringClass());
        if (Modifier.isPackage(fallback.getModifiers()) && !samePackage
                || Modifier.isPrivate(fallback.getModifiers()) && !sameClass) {
            throw new FaultToleranceDefinitionException(describe(annotated, Fallback.class, "fallbackMethod")
                    + "value referring to a method that is not accessible.");
        }
    }

    public boolean isHandlerPresent() {
        return value != null && value != Fallback.DEFAULT.class;
    }

    /**
     * Helper method that checks whether or not the given exception is should trigger applying the fallback or not.
     * 
     * Relevant section from {@link Fallback} javadocs:
     * <blockquote>
     * When a method returns and the Fallback policy is present, the following rules are applied:
     * <ol>
     * <li>If the method returns normally (doesn't throw), the result is simply returned.
     * <li>Otherwise, if the thrown object is assignable to any value in the {@link #skipOn()} parameter, the thrown object will be rethrown.
     * <li>Otherwise, if the thrown object is assignable to any value in the {@link #applyOn()} parameter, 
     * the Fallback policy, detailed above, will be applied.
     * <li>Otherwise the thrown object will be rethrown.
     * </ol>
     * </blockquote>
     * 
     * @param ex The exception to check
     * @return true, when fallback should be applied, false to rethrow the exception
     */
    public boolean isFallbackApplied(Throwable ex) {
        return !Policy.isCaught(ex, skipOn) && Policy.isCaught(ex, applyOn);
    }
}
