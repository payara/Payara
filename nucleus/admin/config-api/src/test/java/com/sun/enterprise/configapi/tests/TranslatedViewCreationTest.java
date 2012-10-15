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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;

import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.TransactionListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * User: Jerome Dochez
 * Date: Jun 24, 2008
 * Time: 8:27:29 PM
 */
public class TranslatedViewCreationTest extends ConfigApiTest {

    final static String propName = "com.sun.my.chosen.docroot";

    public String getFileName() {
        return "DomainTest";
    }

    HttpService httpService = null;
    List<PropertyChangeEvent> events;
    ServiceLocator habitat;

    @Before
    public void setup() {
        System.setProperty(propName, "/foo/bar/docroot");
        habitat = Utils.instance.getHabitat(this);

    }

    @Override
    public ServiceLocator getBaseServiceLocator() {
        return habitat;
    }

    @Test
    public void createVirtualServerTest() throws TransactionFailure {
        httpService = getHabitat().getService(HttpService.class);
        final TransactionListener listener = new TransactionListener() {
                public void transactionCommited(List<PropertyChangeEvent> changes) {
                    events = changes;
                }

            public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {
            }
        };

        Transactions transactions = getHabitat().getService(Transactions.class);

        try {
            transactions.addTransactionsListener(listener);
            assertTrue(httpService!=null);
            ConfigSupport.apply(new SingleConfigCode<HttpService>() {

                public Object run(HttpService param) throws PropertyVetoException, TransactionFailure {
                    VirtualServer newVirtualServer = param.createChild(VirtualServer.class);
                    newVirtualServer.setDocroot("${"+propName+"}");
                    newVirtualServer.setId("translated-view-creation");
                    param.getVirtualServer().add(newVirtualServer);
                    return null;
                }
            }, httpService);

            // first let check that our new virtual server has the right translated value
            VirtualServer vs = httpService.getVirtualServerByName("translated-view-creation");
            assertTrue(vs!=null);
            String docRoot = vs.getDocroot();
            assertTrue("/foo/bar/docroot".equals(docRoot));

            transactions.waitForDrain();

            assertTrue(events!=null);
            logger.fine("Number of events " + events.size());
            assertTrue(events.size()==3);
            for (PropertyChangeEvent event : events) {
                if ("virtual-server".equals(event.getPropertyName())) {
                    VirtualServer newVS = (VirtualServer) event.getNewValue();
                    assertTrue(event.getOldValue()==null);
                    docRoot = newVS.getDocroot();
                    assertTrue("/foo/bar/docroot".equals(docRoot));

                    VirtualServer rawView = GlassFishConfigBean.getRawView(newVS);
                    assertTrue(rawView!=null);
                    assertTrue(rawView.getDocroot().equalsIgnoreCase("${" + propName + "}"));
                    return;
                }
            }
            assertTrue(false);

        } finally {
            transactions.removeTransactionsListener(listener);
        }

    }

    @After
    public void tearDown() {
        System.setProperty(propName, "");
    }
}
