/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

/*
 * InstanceResolverImpl.java
 *
 * Created on May 29, 2007, 10:41 AM
 *
 * @author Mike Grogan
 */

package org.glassfish.webservices;
import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.ResourceInjector;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceException;


public final class InstanceResolverImpl<T> extends InstanceResolver<T> {
   
    //delegate to this InstanceResolver
    private  InstanceResolver<T> resolver;
    private  T instance;
    private final Class<T> classtobeResolved;

    private WSWebServiceContext wsc;
    private WSEndpoint endpoint;

    private final InjectionManager injManager = WebServiceContractImpl.getInstance().getInjectionManager();

    public  InstanceResolverImpl(@NotNull Class<T> clasz) {
        this.classtobeResolved = clasz;
    }

    @Override
    public @NotNull T resolve(Packet request) {
        //See iss 9721
        //Injection and instantiation is now done lazily
        if (resolver == null) {
            try {
                //Bug18998101. inject() call below also calls @PostConstruct method.
                instance = injManager.createManagedObject(classtobeResolved, false);
            } catch (InjectionException e) {
                throw new WebServiceException(e);
            }
            resolver = InstanceResolver.createSingleton(instance);
            getResourceInjector(endpoint).inject(wsc, instance);
        }
        return resolver.resolve(request);
    }

    @Override
    public void start(WSWebServiceContext wsc, WSEndpoint endpoint) {
        this.wsc = wsc;
        this.endpoint = endpoint;
    }

    @Override
    public void dispose() {
        try {
            if(instance != null) {//instance can be null as it is created laziily
                injManager.destroyManagedObject(instance);
            }
        } catch (InjectionException e) {
            throw new WebServiceException(e);
        }
    }
    
    private ResourceInjector getResourceInjector(WSEndpoint endpoint) {
        ResourceInjector ri = endpoint.getContainer().getSPI(ResourceInjector.class);
        if (ri == null) {
            ri = ResourceInjector.STANDALONE;
        }
        return ri;
    }
    
     /**
     * Wraps this {@link InstanceResolver} into an {@link Invoker}.
     */
	public  //TODO - make this package private.  Cannot do it until this method is removed from base
		//       class com.sun.xml.ws.api.server.InstanceResolver
     @Override
     @NotNull Invoker createInvoker() {
        return new Invoker() {
            @Override
            public void start(@NotNull WSWebServiceContext wsc, @NotNull WSEndpoint endpoint) {
                InstanceResolverImpl.this.start(wsc,endpoint);
            }

            @Override
            public void dispose() {
                InstanceResolverImpl.this.dispose();
            }

            @Override
            public Object invoke(Packet p, Method m, Object... args) throws InvocationTargetException, IllegalAccessException {
                return m.invoke( resolve(p), args );
            }

            @Override
            public <T> T invokeProvider(@NotNull Packet p, T arg) {
                return ((Provider<T>)resolve(p)).invoke(arg);
            }

            @Override
            public String toString() {
                return "Default Invoker over "+InstanceResolverImpl.this.toString();
            }
        };
    }
}
