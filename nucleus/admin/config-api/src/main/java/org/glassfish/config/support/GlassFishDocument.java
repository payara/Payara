/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.config.*;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.io.IOException;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.appserv.server.util.Version;

/**
 * plug our Dom implementation
 *
 * @author Jerome Dochez
 * 
 */
public class GlassFishDocument extends DomDocument<GlassFishConfigBean> {

    Logger logger = ConfigApiLoggerInfo.getLogger();

    public GlassFishDocument(final ServiceLocator habitat, final ExecutorService executor) {
        super(habitat);

        ServiceLocatorUtilities.addOneConstant(habitat, executor, "transactions-executor", ExecutorService.class);
        ServiceLocatorUtilities.addOneConstant(habitat, this, null, DomDocument.class);

        final DomDocument doc = this;
        
        habitat.<Transactions>getService(Transactions.class).addTransactionsListener(new TransactionListener() {
            public void transactionCommited(List<PropertyChangeEvent> changes) {
                if (!isGlassFishDocumentChanged(changes)) {
                    return;
                }
                
                for (ConfigurationPersistence pers : habitat.<ConfigurationPersistence>getAllServices(ConfigurationPersistence.class)) {
                    try {
                        if (doc.getRoot().getProxyType().equals(Domain.class)) {
                            Dom domainRoot = doc.getRoot();
                            domainRoot.attribute("version", Version.getBuildVersion());
                        }
                        pers.save(doc);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, 
                        	ConfigApiLoggerInfo.glassFishDocumentIOException,e);
                    } catch (XMLStreamException e) {
                        logger.log(Level.SEVERE, 
                        	ConfigApiLoggerInfo.glassFishDocumentXmlException,e);
                    }
                }
            }

            // make sure domain.xml is changed
            private boolean isGlassFishDocumentChanged(
                    List<PropertyChangeEvent> changes) {
                for (PropertyChangeEvent event : changes) {
                    ConfigBeanProxy source = (ConfigBeanProxy) event.getSource();
                    if (Dom.unwrap(source) instanceof GlassFishConfigBean) {
                        return true;
                    }
                }
                return false;
            }

            public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {

            }
        });
    }

    @Override
    public GlassFishConfigBean make(final ServiceLocator habitat, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
        // by default, people get the translated view.
        return new GlassFishConfigBean(habitat, this, dom, configModel, xmlStreamReader);
    }
}
