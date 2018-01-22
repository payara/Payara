/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
package org.glassfish.weld.services;

import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import org.jboss.weld.construction.api.AroundConstructCallback;
import org.jboss.weld.construction.api.ConstructionHandle;
import org.jboss.weld.exceptions.WeldException;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.*;

/**
 * This calls back into the ejb container to perform the around construct interception.  When that's finished the
 * ejb itself is then created.
 */
public class JCDIAroundConstructCallback<T> implements AroundConstructCallback<T> {
    private BaseContainer container;
    private EJBContextImpl ejbContext;

    // The AroundConstruct interceptor method can access the constructed instance using
    // InvocationContext.getTarget method after the InvocationContext.proceed completes.
    private final AtomicReference<T> target = new AtomicReference<T>();

    private ConstructionHandle<T> handle;
    private Object[] parameters;

    public JCDIAroundConstructCallback(BaseContainer container, EJBContextImpl ejbContext) {
        this.container = container;
        this.ejbContext = ejbContext;
    }

    @Override
    public T aroundConstruct(final ConstructionHandle<T> handle, AnnotatedConstructor<T> constructor, Object[] parameters, Map<String, Object> data) {
        this.handle = handle;
        this.parameters = parameters;
        T ejb;
        try {
            container.intercept( LifecycleCallbackDescriptor.CallbackType.AROUND_CONSTRUCT, ejbContext );

            // all the interceptors were invoked, call the constructor now
            if ( target.get() == null ) {
                ejb = handle.proceed( parameters, new HashMap<String, Object>() );
                target.set( ejb );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new WeldException(e);
        }
        return target.get();
    }

    public T createEjb() {
	T instance =null;
	if( null != handle ) {
            instance = handle.proceed(parameters, new HashMap<String, Object>() );
	}        
	target.set(instance);
        return instance;
    }
}
