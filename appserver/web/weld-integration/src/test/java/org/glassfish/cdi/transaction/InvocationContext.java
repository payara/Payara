/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.cdi.transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.transaction.Transactional;

/**
 * User: paulparkinson
 * Date: 12/12/12
 * Time: 1:12 PM
 */
public class InvocationContext implements jakarta.interceptor.InvocationContext {

    private Method method;
    private Exception exceptionFromProceed;
    private TestInvocationContextTarget testInvocationContextTarget = new TestInvocationContextTarget();
    private Map<String, Object> contextData = new HashMap<>();

    public InvocationContext(Method method, Exception exceptionFromProceed, Class<?> targetClass) {
        this.method = method;
        this.exceptionFromProceed = exceptionFromProceed;
        contextData.put("org.jboss.weld.interceptor.bindings", Collections.singleton(
                method.getAnnotation(Transactional.class) != null?
                        method.getAnnotation(Transactional.class) :
                        targetClass.getAnnotation(Transactional.class)));
    }

    @Override
    public Object getTarget() {
        return testInvocationContextTarget;
    }

    class TestInvocationContextTarget {

    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor getConstructor() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public void setParameters(Object[] params) {

    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @Override
    public Object proceed() throws Exception {
        if (exceptionFromProceed != null) throw exceptionFromProceed;
        return null;
    }
}
