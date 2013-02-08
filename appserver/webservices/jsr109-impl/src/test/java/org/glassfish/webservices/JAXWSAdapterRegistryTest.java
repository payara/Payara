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
package org.glassfish.webservices;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.Component;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.FiberContextSwitchInterceptor;
import com.sun.xml.ws.api.pipe.ServerTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.ThrowableContainerPropertySet;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.wsdl.OperationDispatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.xml.namespace.QName;
import javax.xml.ws.EndpointReference;
import org.glassfish.gmbal.ManagedObjectManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 *
 * @author ljungman
 */
public class JAXWSAdapterRegistryTest {

    public JAXWSAdapterRegistryTest() {
    }

    /**
     * Test of addAdapter method, of class JAXWSAdapterRegistry.
     *
     * http://java.net/jira/browse/GLASSFISH-17836
     * Putting load on freshly-started Glassfish web-app messes up its initialization process
     */
    @Test
    public void testAddAdapter() {
        final String contextRoot = "/cr";
        final String urlPattern = "/up";
        final JAXWSAdapterRegistry registry = JAXWSAdapterRegistry.getInstance();
        int size = 25;
        Thread[] ts = new Thread[size];
        for (int i = 0; i < size; i++) {
            final int j = i;
            ts[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    registry.addAdapter(contextRoot, urlPattern + j, new A(j));
                }
            });
        }
        
        for (int i = 0; i < size; i++) ts[i].start();
        
        for (int i = 0; i < size; i++) {
            try {
                ts[i].join();
            } catch (InterruptedException ex) {
            }
        }
        
        for (int i = 0; i < size; i++) {
            Adapter a = registry.getAdapter(contextRoot, urlPattern + i, urlPattern + i);
            Assert.assertNotNull("No adapter for '" + contextRoot + urlPattern + i + "'", a);
            Assert.assertEquals(i, ((A)a).getX());
        }
    }

    private class A extends Adapter {

        private int x;

        public A(int x) {
            super(new WSE());
            this.x = x;
        }

        public int getX() {
            return x;
        }

        @Override
        protected Toolkit createToolkit() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class WSE extends WSEndpoint {

        @NotNull
        @Override
        public Set<Component> getComponents() {
            return new HashSet<Component>();
        }

        @Override
        public Codec createCodec() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public QName getServiceName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public QName getPortName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Class getImplementationClass() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public WSBinding getBinding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Container getContainer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public WSDLPort getPort() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setExecutor(Executor exctr) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void schedule(Packet packet, CompletionCallback cc, FiberContextSwitchInterceptor fcsi) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PipeHead createPipeHead() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceDefinition getServiceDefinition() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set getComponentRegistry() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public SEIModel getSEIModel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PolicyMap getPolicyMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ManagedObjectManager getManagedObjectManager() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void closeManagedObjectManager() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServerTubeAssemblerContext getAssemblerContext() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EndpointReference getEndpointReference(Class clazz, String address, String wsdlAddress, Element... referenceParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EndpointReference getEndpointReference(Class clazz, String address, String wsdlAddress, List metadata, List referenceParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public OperationDispatcher getOperationDispatcher() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Packet createServiceResponseForException(ThrowableContainerPropertySet tcps, Packet packet, SOAPVersion soapv, WSDLPort wsdlp, SEIModel seim, WSBinding wsb) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
