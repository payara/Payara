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

package com.sun.enterprise.configapi.tests;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;

import java.util.Collection;
import java.util.List;

/**
 * This test will ensure that when a class is injected with a parent bean and a child
 * is added to the parent, anyone injected will that parent will be notified
 * correctly.
 *
 * User: Jerome Dochez
 */
public class ParentConfigListenerTest extends ConfigApiTest {

    ServiceLocator habitat;

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setup() {
        habitat = Utils.instance.getHabitat(this);
    }
    
    


    @Test
    public void addHttpListenerTest() throws TransactionFailure {
        NetworkListenersContainer container = habitat.getService(NetworkListenersContainer.class);

        ConfigSupport.apply(new SingleConfigCode<NetworkListeners>() {

            public Object run(NetworkListeners param) throws TransactionFailure {
                NetworkListener newListener = param.createChild(NetworkListener.class);
                newListener.setName("Funky-Listener");
                newListener.setPort("8078");
                param.getNetworkListener().add(newListener);
                return null;
            }
        }, container.httpService);

        getHabitat().<Transactions>getService(Transactions.class).waitForDrain();
        assertTrue(container.received);
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(container.httpService);

        // let's check that my newly added listener is available in the habitat.
        List<ServiceHandle<NetworkListener>> networkListeners = habitat.getAllServiceHandles(NetworkListener.class);
        boolean found = false;
        
        for (ServiceHandle<NetworkListener> nlSH : networkListeners) {
            NetworkListener nl = (NetworkListener) nlSH.getService();
            if (nl.getName().equals("Funky-Listener")) {
                found=true;
            }
        }
        Assert.assertTrue("Newly added listener not found", found);
        
        // direct access.
        NetworkListener nl = habitat.getService(NetworkListener.class, "Funky-Listener");
        Assert.assertTrue("Direct access to newly added listener failed", nl!=null);
        bean.removeListener(container);
    }
}
