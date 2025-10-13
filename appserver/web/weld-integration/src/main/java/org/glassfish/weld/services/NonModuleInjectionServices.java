/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */

package org.glassfish.weld.services;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jboss.weld.injection.spi.InjectionContext;
import org.jboss.weld.injection.spi.InjectionServices;

import jakarta.enterprise.inject.spi.*;

/**
 * The InjectionServices for a non-module bda (library or rar).  A non-module bda has no associated bundle so we
 * cannot reuse the InjectionServicesImpl for injecting into a bean that's resides in an application library
 * (not WEB-INF/lib) or rar
 *
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class NonModuleInjectionServices implements InjectionServices {

    private InjectionManager injectionManager;

    private static final Logger logger = Logger.getLogger(InjectionServicesImpl.class.getName());

    public NonModuleInjectionServices(InjectionManager injectionMgr) {
        injectionManager = injectionMgr;
    }

    @Override
    public <T> void aroundInject(InjectionContext<T> injectionContext) {
        try {
            ServiceLocator serviceLocator = Globals.getDefaultHabitat();
            ComponentEnvManager compEnvManager = serviceLocator.getService(ComponentEnvManager.class);

            JndiNameEnvironment componentEnv = compEnvManager.getCurrentJndiNameEnvironment();

            Object target = injectionContext.getTarget();
            String targetClass = target.getClass().getName();

            if( componentEnv == null ) {
                //throw new IllegalStateException("No valid EE environment for injection of " + targetClass);
                logger.log(Level.FINE, "No valid EE environment for injection of {0}. The methods that is missing the context is {1}", new Object[]{targetClass, injectionContext.getAnnotatedType().getMethods()});
                injectionContext.proceed();
                return;
            }

            injectionManager.injectInstance(target, componentEnv, false);
            injectionContext.proceed();

        } catch(InjectionException ie) {
            throw new IllegalStateException(ie.getMessage(), ie);
        }
    }

    @Override
    public <T> void registerInjectionTarget(jakarta.enterprise.inject.spi.InjectionTarget<T> injectionTarget, AnnotatedType<T> annotatedType) {
    }

    @Override
    public void cleanup() {
    }

}
