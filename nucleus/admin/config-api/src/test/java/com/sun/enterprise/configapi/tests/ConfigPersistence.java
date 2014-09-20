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

import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.glassfish.config.support.ConfigurationPersistence;
import org.glassfish.tests.utils.Utils;
import org.junit.After;
import org.junit.Test;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.TransactionListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * User: Jerome Dochez
 * Date: Mar 25, 2008
 * Time: 11:36:46 AM
 */
public abstract class ConfigPersistence extends ConfigApiTest {

    @After
    public void tearDown() {
    	Utils.instance.shutdownServiceLocator(this);
    }
    
    @Test
    public void test() throws TransactionFailure {

        final DomDocument document = getDocument(getHabitat());
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.reset();

        final ConfigurationPersistence testPersistence =  new ConfigurationPersistence() {
            public void save(DomDocument doc) throws IOException, XMLStreamException {
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = factory.createXMLStreamWriter(baos);
                doc.writeTo(new IndentingXMLStreamWriter(writer));
                writer.close();
            }
        };

        TransactionListener testListener = new TransactionListener() {
            public void transactionCommited(List<PropertyChangeEvent> changes) {
                try {
                    testPersistence.save(document);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (XMLStreamException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {

            }
        };
        Transactions transactions = getHabitat().getService(Transactions.class);
        
        try {
            transactions.addTransactionsListener(testListener);

            doTest();
        } catch(TransactionFailure f) {
            f.printStackTrace();
            throw f;

        } finally {
            transactions.waitForDrain();
            transactions.removeTransactionsListener(testListener);
        }

        // now check if we persisted correctly...

        final String resultingXml = baos.toString();
        
        logger.fine(resultingXml);
        assertTrue("assertResult from " + getClass().getName() + " was false from " + resultingXml, assertResult(resultingXml));
    }

    public abstract void doTest() throws TransactionFailure;

    public abstract boolean assertResult(String resultingXml);    
}
