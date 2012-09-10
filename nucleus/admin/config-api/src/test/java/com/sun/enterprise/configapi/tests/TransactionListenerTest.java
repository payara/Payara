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

package com.sun.enterprise.configapi.tests;

import com.sun.enterprise.config.serverbeans.HttpService;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.TransactionListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Jan 23, 2008
 * Time: 10:48:55 PM
 */
public class TransactionListenerTest extends ConfigApiTest {
    public String getFileName() {
        return "DomainTest";
    }

    HttpService httpService = null;
    List<PropertyChangeEvent> events = null;

    @Test
    public void transactionEvents() throws Exception, TransactionFailure {
        httpService = getHabitat().getService(HttpService.class);
        NetworkConfig networkConfig = getHabitat().getService(NetworkConfig.class);
        final NetworkListener netListener = networkConfig.getNetworkListeners()
            .getNetworkListener().get(0);
        final Http http = netListener.findHttpProtocol().getHttp();
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
            logger.fine("Max connections = " + http.getMaxConnections());
            ConfigSupport.apply(new SingleConfigCode<Http>() {

                public Object run(Http param) {
                    param.setMaxConnections("500");
                    return null;
                }
            }, http);
            assertTrue("500".equals(http.getMaxConnections()));

            transactions.waitForDrain();
            
            assertTrue(events!=null);
            logger.fine("Number of events " + events.size());
            assertTrue(events.size()==1);
            PropertyChangeEvent event = events.iterator().next();
            assertTrue("max-connections".equals(event.getPropertyName()));
            assertTrue("500".equals(event.getNewValue().toString()));
            assertTrue("250".equals(event.getOldValue().toString()));
        } catch(Exception t) {
            t.printStackTrace();
            throw t;
        }finally {
            transactions.removeTransactionsListener(listener);
        }

        // put back the right values in the domain to avoid test collisions
        ConfigSupport.apply(new SingleConfigCode<Http>() {

            public Object run(Http param) {
                param.setMaxConnections("250");
                return null;
            }
        }, http);

    }
}
