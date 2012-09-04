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
import java.util.List;

import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.TransactionListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * @author Jerome Dochez
 */
public class UnprocessedEventsTest  extends ConfigApiTest
        implements ConfigListener, TransactionListener {

    ServiceLocator habitat = Utils.instance.getHabitat(this);
    UnprocessedChangeEvents unprocessed = null;

    /**
     * Returns the DomainTest file name without the .xml extension to load the test configuration
     * from.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }

    @Test
     public void unprocessedEventsTest() throws TransactionFailure {

        // let's find our target
        NetworkConfig service = habitat.getService(NetworkConfig.class);
        NetworkListener listener = service.getNetworkListener("http-listener-1");
        assertNotNull(listener);

        // Let's register a listener
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(listener);
        bean.addListener(this);
        Transactions transactions = getHabitat().getService(Transactions.class);

        try {
            transactions.addTransactionsListener(this);

            ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
                public Object run(NetworkListener param) {
                    param.setPort("8908");
                    return null;
                }
            }, listener);

            // check the result.
            String port = listener.getPort();
            assertEquals(port, "8908");

            // ensure events are delivered.
            transactions.waitForDrain();
            assertNotNull(unprocessed);

            // finally
            bean.removeListener(this);
        } finally {

            // check we recevied the event
            transactions.removeTransactionsListener(this);
        }

    }

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        assertEquals("Array size", propertyChangeEvents.length, 1 );
        
        final UnprocessedChangeEvent unp = new UnprocessedChangeEvent(
            propertyChangeEvents[0], "Java NIO port listener cannot reconfigure its port dynamically" );
        unprocessed = new UnprocessedChangeEvents( unp );
        return unprocessed;
    }

    public void transactionCommited(List<PropertyChangeEvent> changes) {
        // don't care...
    }

    public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {
        assertTrue(changes.size()==1);
    }
}
