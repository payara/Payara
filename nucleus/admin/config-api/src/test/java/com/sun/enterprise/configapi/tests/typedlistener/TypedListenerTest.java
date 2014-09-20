/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.configapi.tests.typedlistener;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.config.*;

/**
 * Test the listeners per type registration/events/un-registration.
 * @author Jerome Dochez
 */
public class TypedListenerTest extends ConfigApiTest {

    List<PropertyChangeEvent> events = null;
    AtomicInteger listenersInvoked = new AtomicInteger();

    @Override
    public String getFileName() {
        return "DomainTest";
    }

    @Test
    public void addElementTest() throws TransactionFailure {

        final Domain domain = getHabitat().getService(Domain.class);
        final ConfigListener configListener = new ConfigListener() {
            @Override
            public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                events = Arrays.asList(propertyChangeEvents);
                return null;
            }
        };

        Transactions transactions = getHabitat().getService(Transactions.class);

        try {
            transactions.addListenerForType(SystemProperty.class, configListener);

            assertTrue(domain!=null);

            // adding
            ConfigSupport.apply(new SingleConfigCode<Domain>() {

                        @Override
                        public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                            SystemProperty prop = domain.createChild(SystemProperty.class);
                            domain.getSystemProperty().add(prop);
                            prop.setName("Jerome");
                            prop.setValue("was here");
                            return prop;
                        }
                    }, domain);
            transactions.waitForDrain();

            assertTrue(events!=null);
            logger.log(Level.FINE, "Number of events {0}", events.size());
            assertTrue(events.size()==3);
            for (PropertyChangeEvent event : events) {
                logger.fine(event.toString());
            }
            events = null;

            // modification
            for (SystemProperty prop : domain.getSystemProperty()) {
                if (prop.getName().equals("Jerome")) {
                    ConfigSupport.apply(new SingleConfigCode<SystemProperty>() {
                                @Override
                                public Object run(SystemProperty param) throws PropertyVetoException, TransactionFailure {
                                    param.setValue("was also here");
                                    return null;
                                }
                            },prop);
                    break;
                }
            }
            assertTrue(events!=null);
            logger.log(Level.FINE, "Number of events {0}", events.size());
            assertTrue(events.size()==1);
            for (PropertyChangeEvent event : events) {
                logger.fine(event.toString());
            }

            events = null;

            // removal
            assertNotNull(ConfigSupport.apply(new SingleConfigCode<Domain>() {

                        @Override
                        public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                            for (SystemProperty prop : domain.getSystemProperty()) {
                                if (prop.getName().equals("Jerome")) {
                                    domain.getSystemProperty().remove(prop);
                                    return prop;
                                }
                            }
                            return null;
                        }
                    }, domain));
            transactions.waitForDrain();

            assertTrue(events!=null);
            logger.log(Level.FINE, "Number of events {0}", events.size());
            assertTrue(events.size()==1);
            for (PropertyChangeEvent event : events) {
                logger.fine(event.toString());
            }
        } finally {
            assertTrue(transactions.removeListenerForType(SystemProperty.class, configListener));
        }
    }

    @Test
    public void multipleListeners() throws TransactionFailure {
        final Domain domain = getHabitat().getService(Domain.class);
        final ConfigListener configListener1 = new ConfigListener() {
            @Override
            public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                listenersInvoked.incrementAndGet();
                return null;
            }
        };
        final ConfigListener configListener2 = new ConfigListener() {
            @Override
            public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                listenersInvoked.incrementAndGet();
                return null;
            }
        };

        Transactions transactions = getHabitat().getService(Transactions.class);

        try {
            transactions.addListenerForType(SystemProperty.class, configListener1);
            transactions.addListenerForType(SystemProperty.class, configListener2);

            assertTrue(domain!=null);

            // adding
            ConfigSupport.apply(new SingleConfigCode<Domain>() {

                        @Override
                        public Object run(Domain domain) throws PropertyVetoException, TransactionFailure {
                            SystemProperty prop = domain.createChild(SystemProperty.class);
                            domain.getSystemProperty().add(prop);
                            prop.setName("Jerome");
                            prop.setValue("was here");
                            return prop;
                        }
                    }, domain);
            transactions.waitForDrain();

            assertTrue(listenersInvoked.intValue()==2);
        } finally {
            assertTrue(transactions.removeListenerForType(SystemProperty.class, configListener1));
            assertTrue(transactions.removeListenerForType(SystemProperty.class, configListener2));
        }
    }
}
