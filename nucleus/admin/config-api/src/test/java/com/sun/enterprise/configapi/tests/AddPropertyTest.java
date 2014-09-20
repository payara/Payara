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

import com.sun.enterprise.config.serverbeans.Domain;
import org.jvnet.hk2.config.types.Property;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.TransactionListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Jerome Dochez
 * Date: Mar 25, 2008
 * Time: 1:32:35 PM
 */
public class AddPropertyTest extends ConfigApiTest {

    public String getFileName() {
        return "DomainTest";
    }

    List<PropertyChangeEvent> events = null;

    @Test
    public void transactionEvents() throws TransactionFailure {
        final Domain domain = getHabitat().getService(Domain.class);
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
            assertTrue(domain!=null);

            ConfigSupport.apply(new SingleConfigCode<Domain>() {

                public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                    Property prop = domain.createChild(Property.class);
                    domain.getProperty().add(prop);
                    prop.setName("Jerome");
                    prop.setValue("was here");
                    return prop;
                }
            }, domain);
            transactions.waitForDrain();

            assertTrue(events!=null);
            logger.fine("Number of events " + events.size());
            assertTrue(events.size()==3);
            for (PropertyChangeEvent event : events) {
                logger.fine(event.toString());
            }

            Map<String, String> configChanges = new HashMap<String, String>();
            configChanges.put("name", "julien");
            configChanges.put("value", "petit clown");
            ConfigBean domainBean = (ConfigBean) Dom.unwrap(domain);
            ConfigSupport.createAndSet(domainBean, Property.class, configChanges);

            
            transactions.waitForDrain();

            assertTrue(events!=null);
            logger.fine("Number of events " + events.size());
            assertTrue(events.size()==3);
            for (PropertyChangeEvent event : events) {
                logger.fine(event.toString());
            }

            final UnprocessedChangeEvents unprocessed =
                ConfigSupport.sortAndDispatch(events.toArray(new PropertyChangeEvent[0]), new Changed() {
                /**
                 * Notification of a change on a configuration object
                 *
                 * @param type            type of change : ADD mean the changedInstance was added to the parent
                 *                        REMOVE means the changedInstance was removed from the parent, CHANGE means the
                 *                        changedInstance has mutated.
                 * @param changedType     type of the configuration object
                 * @param changedInstance changed instance.
                 */
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
                    return new NotProcessed("unimplemented by AddPropertyTest");
                }
            }, logger);
        } finally {
            transactions.removeTransactionsListener(listener);
        }
    }    
}
