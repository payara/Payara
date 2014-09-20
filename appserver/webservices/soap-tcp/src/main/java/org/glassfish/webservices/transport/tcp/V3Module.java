/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.webservices.transport.tcp;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.tcp.server.TCPAdapter;
import com.sun.xml.ws.transport.tcp.server.WSTCPDelegate;
import com.sun.xml.ws.transport.tcp.server.WSTCPModule;
import com.sun.xml.ws.transport.tcp.servicechannel.ServiceChannelWSImpl;
import java.util.List;
import javax.xml.namespace.QName;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.webservices.WebServiceDeploymentListener;
import org.glassfish.webservices.WebServiceEjbEndpointRegistry;
import org.glassfish.webservices.WebServicesDeployer;
import org.xml.sax.EntityResolver;

/**
 *
 * @author oleksiys
 */
public class V3Module extends WSTCPModule {
    private final WSTCPDelegate delegate;

    V3Module() {
        WSTCPModule.setInstance(this);

        WebServicesDeployer.getDeploymentNotifier().
            addListener(new WebServiceDeploymentListener() {

            @Override
            public void onDeployed(WebServiceEndpoint endpoint) {
                if (endpoint.getWebComponentImpl() != null) {
                    endpoint.getWebComponentImpl().setLoadOnStartUp(0);
                }
            }

            @Override
            public void onUndeployed(WebServiceEndpoint endpoint) {
            }
        });

        AppServRegistry.getInstance();
        delegate = new WSTCPDelegate();
        delegate.setCustomWSRegistry(WSTCPAdapterRegistryImpl.getInstance());
    }

    @Override
    public void register(String contextPath, List<TCPAdapter> adapters) {
        delegate.registerAdapters(contextPath, adapters);
    }

    @Override
    public void free(String contextPath, List<TCPAdapter> adapters) {
        delegate.freeAdapters(contextPath, adapters);
    }

    @Override
    public int getPort() {
        return -1;
    }

    public WSTCPDelegate getDelegate() {
        return delegate;
    }

    @Override
    public WSEndpoint<ServiceChannelWSImpl> createServiceChannelEndpoint() {
        Class<ServiceChannelWSImpl> serviceEndpointClass = ServiceChannelWSImpl.class;
        final QName serviceName = WSEndpoint.getDefaultServiceName(ServiceChannelWSImpl.class);
        final QName portName = WSEndpoint.getDefaultPortName(serviceName, ServiceChannelWSImpl.class);
        final BindingID bindingId = BindingID.parse(ServiceChannelWSImpl.class);
        final WSBinding binding = bindingId.createBinding();

//        final Invoker inv= (new InstanceResolverImpl(serviceEndpointClass)).createInvoker();

        return WSEndpoint.create(serviceEndpointClass, false,
                    null,
                    serviceName, portName, Container.NONE, binding,
                    null, null, (EntityResolver) null, true);
    }


    public static WebServiceEjbEndpointRegistry getWSEjbEndpointRegistry() {
        return (WebServiceEjbEndpointRegistry) org.glassfish.internal.api.Globals.getDefaultHabitat().getService(
                    WSEjbEndpointRegistry.class);
    }

    public static InvocationManager getInvocationManager() {
        return (InvocationManager) org.glassfish.internal.api.Globals.getDefaultHabitat().getService(
                    InvocationManager.class);
    }
}
