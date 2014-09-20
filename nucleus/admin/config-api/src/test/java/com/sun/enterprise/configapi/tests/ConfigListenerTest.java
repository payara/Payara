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

package com.sun.enterprise.configapi.tests;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.config.support.ConfigConfigBeanListener;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;

import com.sun.enterprise.config.serverbeans.Config;

/**
 * Simple ConfigListener tests
 */
public class ConfigListenerTest extends ConfigApiTest {

    ServiceLocator habitat;

    @Override
    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setup() {
        habitat = Utils.instance.getHabitat(this);
        
        // make sure the ConfigConfigListener exists
        ServiceHandle<ConfigConfigBeanListener> i = habitat.getServiceHandle(ConfigConfigBeanListener.class);
        ConfigConfigBeanListener ccbl = i.getService();
        assertTrue(ccbl != null);
    }
    
    private HttpListenerContainer registerAndCreateHttpListenerContainer(ServiceLocator locator) {
        HttpListenerContainer retVal = locator.getService(HttpListenerContainer.class);
        if (retVal != null) return retVal;
        
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        Assert.assertNotNull(dcs);
        
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.addActiveDescriptor(HttpListenerContainer.class);
        
        config.commit();
        
        return locator.getService(HttpListenerContainer.class);
    }


    @Test
    public void changedTest() throws TransactionFailure {

        Transactions transactions = getHabitat().getService(Transactions.class);

        HttpListenerContainer container = registerAndCreateHttpListenerContainer(habitat);

        ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {

            @Override
            public Object run(NetworkListener param) {
                param.setPort("8989");
                return null;
            }
        }, container.httpListener);

        transactions.waitForDrain();
        assertTrue(container.received);
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(container.httpListener);
        bean.removeListener(container);

        // put back the right values in the domain to avoid test collisions
        ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {

            @Override
            public Object run(NetworkListener param) {
                param.setPort("8080");
                return null;
            }
        }, container.httpListener);
    }

    @Test
    public void removeListenerTest() throws TransactionFailure {

        Transactions transactions = getHabitat().getService(Transactions.class);
        
        HttpListenerContainer container = registerAndCreateHttpListenerContainer(habitat);

        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(container.httpListener);
        bean.removeListener(container);

        ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {

            @Override
            public Object run(NetworkListener param) {
                param.setPort("8989");
                return null;
            }
        }, container.httpListener);

        transactions.waitForDrain();
        assertFalse(container.received);

        // put back the right values in the domain to avoid test collisions        
        ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {

            @Override
            public Object run(NetworkListener param) {
                param.setPort("8080");
                return null;
            }
        }, container.httpListener);
    }
}
