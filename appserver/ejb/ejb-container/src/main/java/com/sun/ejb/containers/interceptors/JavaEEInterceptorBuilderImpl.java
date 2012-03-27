/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.ejb.containers.interceptors;

import com.sun.enterprise.container.common.spi.JavaEEInterceptorBuilder;
import com.sun.enterprise.container.common.spi.InterceptorInvoker;
import com.sun.enterprise.container.common.spi.util.InterceptorInfo;

import com.sun.ejb.codegen.EjbOptionalIntfGenerator;
import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import com.sun.logging.LogDomains;

import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 *
 */

public class JavaEEInterceptorBuilderImpl implements JavaEEInterceptorBuilder {

    private static Logger _logger = LogDomains.getLogger(JavaEEInterceptorBuilderImpl.class,
            LogDomains.CORE_LOGGER);

    private InterceptorInfo interceptorInfo;

    private InterceptorManager interceptorManager;

    private EjbOptionalIntfGenerator gen;

    private Class subClassIntf;

    private Class subClass;

    public JavaEEInterceptorBuilderImpl(InterceptorInfo intInfo, InterceptorManager manager,
                                        EjbOptionalIntfGenerator gen, Class subClassIntf,
                                        Class subClass) {
        interceptorInfo = intInfo;
        interceptorManager = manager;
        this.gen = gen;
        this.subClassIntf = subClassIntf;
        this.subClass = subClass;
    }

    public InterceptorInvoker createInvoker(Object instance) throws Exception {

        interceptorInfo.setTargetObjectInstance(instance);


        // Proxy invocation handler. Also implements InterceptorInvoker.
        InterceptorInvocationHandler invoker = new InterceptorInvocationHandler();

        Proxy proxy = (Proxy) Proxy.newProxyInstance(
            subClass.getClassLoader(), new Class[] { subClassIntf }, invoker);


        // Object passed back to the caller.
        OptionalLocalInterfaceProvider provider =
            (OptionalLocalInterfaceProvider) subClass.newInstance();
        provider.setOptionalLocalIntfProxy(proxy);

        invoker.init(instance, interceptorManager.createInterceptorInstances(),
                provider, interceptorManager);

        return invoker;

    }

    public void addRuntimeInterceptor(Object interceptor) {

        interceptorManager.registerRuntimeInterceptor(interceptor);

    }


}


