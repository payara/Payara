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
import com.sun.enterprise.container.common.spi.JavaEEInterceptorBuilderFactory;
import com.sun.enterprise.container.common.spi.InterceptorInvoker;
import com.sun.enterprise.container.common.spi.util.InterceptorInfo;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.codegen.EjbOptionalIntfGenerator;
import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import com.sun.logging.LogDomains;

import java.util.logging.Logger;

/**
 *
 */
@Service
public class JavaEEInterceptorBuilderFactoryImpl implements JavaEEInterceptorBuilderFactory {


    private static Logger _logger = LogDomains.getLogger(JavaEEInterceptorBuilderImpl.class,
            LogDomains.CORE_LOGGER);

    public JavaEEInterceptorBuilder createBuilder(InterceptorInfo info) throws Exception {

        Class targetObjectClass = info.getTargetClass();

        // Create an interface with all public methods of the target class
        // in order to create a dynamic proxy
        String subClassIntfName = EJBUtils.getGeneratedOptionalInterfaceName(targetObjectClass.getName());

        EjbOptionalIntfGenerator gen = new EjbOptionalIntfGenerator(targetObjectClass.getClassLoader());
        gen.generateOptionalLocalInterface(targetObjectClass, subClassIntfName);
        Class subClassIntf = gen.loadClass(subClassIntfName);

        String beanSubClassName = subClassIntfName + "__Bean__";

        // Generate a sub-class of the application's class.  Use an instance of this subclass
        // as the actual object passed back to the application.  The sub-class instance
        // delegates all public methods to the dyanamic proxy, which calls the
        // InvocationHandler.
        gen.generateOptionalLocalInterfaceSubClass(
            targetObjectClass, beanSubClassName, subClassIntf);

        Class subClass = gen.loadClass(beanSubClassName);


        // TODO do interceptor builder once per managed bean
        InterceptorManager interceptorManager = new InterceptorManager(_logger,
                targetObjectClass.getClassLoader(), targetObjectClass.getName(),
                info);


        JavaEEInterceptorBuilderImpl builderImpl =
                new JavaEEInterceptorBuilderImpl(info, interceptorManager,
                        gen, subClassIntf, subClass);

        return builderImpl;

    }

    /**
      * Tests if a given object is a client proxy associated with an interceptor invoker.
      */
    public boolean isClientProxy(Object obj) {

        Class clazz = obj.getClass();

        return (OptionalLocalInterfaceProvider.class.isAssignableFrom(clazz));
    }
    

}


