/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.enterprise.container.common.spi.util;

import java.util.*;

import com.sun.enterprise.deployment.InterceptorDescriptor;

import java.lang.reflect.Method;

/**
 */

public class InterceptorInfo {

    private List<? extends InterceptorDescriptor> aroundConstructInterceptors = new LinkedList<>();
    private List<? extends InterceptorDescriptor> postConstructInterceptors = new LinkedList<>();
    private List<? extends InterceptorDescriptor> preDestroyInterceptors = new LinkedList<>();
    private Map<Method, List<? extends InterceptorDescriptor>> aroundInvokeChains = new HashMap<>();
    private Set<String> interceptorClassNames = new HashSet<>();

    // True if a system interceptor needs to be added dynamically
    private boolean supportRuntimeDelegate;

    private Object targetObjectInstance;
    private Class<?> targetClass;

    private boolean hasTargetClassAroundInvoke = false;

    public void setTargetObjectInstance(Object instance) {
        targetObjectInstance = instance;
    }

    public Object getTargetObjectInstance() {
        return targetObjectInstance;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    public void setAroundConstructInterceptors(List<? extends InterceptorDescriptor> interceptors) {
        aroundConstructInterceptors = interceptors;
    }

    public List<? extends InterceptorDescriptor> getAroundConstructInterceptors() {
        return new LinkedList<>(aroundConstructInterceptors);
    }

    public void setPostConstructInterceptors(List<? extends InterceptorDescriptor> interceptors) {
        postConstructInterceptors = interceptors;
    }

    public List<? extends InterceptorDescriptor> getPostConstructInterceptors() {
        return new LinkedList<>(postConstructInterceptors);
    }

    public void setPreDestroyInterceptors(List<? extends InterceptorDescriptor> interceptors) {
        preDestroyInterceptors = interceptors;
    }

    public List<? extends InterceptorDescriptor> getPreDestroyInterceptors() {
        return new LinkedList<>(preDestroyInterceptors);
    }

    public void setInterceptorClassNames(Set<String> names) {
        interceptorClassNames = new HashSet<>(names);
    }

    public Set<String> getInterceptorClassNames() {
        return interceptorClassNames;
    }

    public void setAroundInvokeInterceptorChains(Map<Method, List<? extends InterceptorDescriptor>> chains) {
        aroundInvokeChains = new HashMap<>(chains);
    }


    public void setHasTargetClassAroundInvoke(boolean flag) {
        hasTargetClassAroundInvoke = flag;
    }

    public boolean getHasTargetClassAroundInvoke() {
        return hasTargetClassAroundInvoke;
    }

    public List<? extends InterceptorDescriptor> getAroundInvokeInterceptors(Method m) {
        return aroundInvokeChains.get(m);
    }

    public boolean getSupportRuntimeDelegate() {
        return supportRuntimeDelegate;
    }

    public void setSupportRuntimeDelegate(boolean flag) {
        supportRuntimeDelegate = flag;
    }
}
