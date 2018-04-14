/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.configapi.tests;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Transport;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * HttpListener related tests
 *
 * @author Jerome Dochez
 */
public class HttpListenerTest extends ConfigApiTest {


    public String getFileName() {
        return "DomainTest";
    }

    private NetworkListener listener;

    @Before
    public void setup() {
        NetworkListeners service = getHabitat().getService(NetworkListeners.class);
        assertTrue(service != null);
        
        for (NetworkListener item : service.getNetworkListener()) {
            if ("http-listener-1".equals(item.getName())) {
                listener = item;
                break;
            }
        }

        logger.fine("listener = " + listener);
        assertTrue(listener != null);           
    }
    
    @Test
    public void portTest() {
        logger.fine("port = " + listener.getPort());
        assertTrue("8080".equals(listener.getPort()));
    }

    @Test
    public void validTransaction() throws TransactionFailure {
        
        ConfigSupport.apply(new SingleConfigCode<Transport>() {
            public Object run(Transport okToChange) {
                okToChange.setAcceptorThreads("2");
                logger.fine("ID inside the transaction is " + okToChange.getName());
                return null;
            }
        }, listener.findTransport());
        
        logger.fine("ID outside the transaction is " + listener.getName());
        assertTrue("2".equals(listener.findTransport().getAcceptorThreads()));
    }    
}
