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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import jakarta.interceptor.InvocationContext;

/**
 * A {@link InvocationContext} used during static analysis.
 *
 * @author Jan Bernitt
 */
public final class StaticAnalysisContext implements InvocationContext {

    private final Class<?> targetClass;
    private final Method annotated;
    private Object[] arguments;
    private transient Object target;

    public StaticAnalysisContext(Class<?> targetClass, Method annotated) {
        this(null, targetClass, annotated);
    }

    public StaticAnalysisContext(Object target, Method annotated, Object... arguments) {
        this(target, target.getClass(), annotated, arguments);
    }

    private StaticAnalysisContext(Object target, Class<?> targetClass, Method annotated, Object... arguments) {
        this.target = target;
        this.targetClass = targetClass;
        this.annotated = annotated;
        this.arguments = arguments;
    }

    @Override
    public Object getTarget() {
        if (target == null) {
            try {
                target = targetClass.newInstance();
            } catch (Exception e) {
                target = new UnsupportedOperationException();
            }
        }
        if (target instanceof UnsupportedOperationException) {
            throw (UnsupportedOperationException) target;
        }
        return target;
    }

    @Override
    public Object getTimer() {
        return null; // no timer
    }

    @Override
    public Method getMethod() {
        return annotated;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null; // this is not an AroundConstruct interception context
    }

    @Override
    public Object[] getParameters() {
        return arguments;
    }

    @Override
    public void setParameters(Object[] params) {
        this.arguments = params;
    }

    @Override
    public Map<String, Object> getContextData() {
        return Collections.emptyMap();
    }

    @Override
    public Object proceed() throws Exception {
        if (arguments.length != annotated.getParameterCount()) {
            throw new UnsupportedOperationException();
        }
        try {
            return annotated.invoke(getTarget(), arguments);
        } catch (InvocationTargetException ex) {
            throw (Exception) ex.getTargetException();
        }
    }

    @Override
    public String toString() {
        return target.getClass().getSimpleName() + "." + annotated.getName() + "@" + System.identityHashCode(target);
    }
}
