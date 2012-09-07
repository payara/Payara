/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jms.injection;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.jboss.weld.context.ContextNotActiveException;

/**
 * This bean is the JMSContext wrapper which user gets by injection.
 * It can read metadata of injection point for it is dependent scoped.
 * It delegates all business methods of JMSContext interface to the 
 * JMSContext API via request scopd JMSContextManager bean.
 */
public class InjectableJMSContext extends ForwardingJMSContext implements Serializable {
    // Note: since this bean is dependent-scoped, instances are liable to be passivated
    // All fields are therefore either serializable or transient

    private final static Logger logger = LogDomains.getLogger(InjectableJMSContext.class, LogDomains.JMS_LOGGER);
    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(InjectableJMSContext.class);
    private final static String DEFAULT_CONNECTION_FACTORY = "java:comp/defaultJMSConnectionFactory";

    private final String id;

    // CDI proxy so serializable
    private final JMSContextManager manager;

    // We need to ensure this is serialiable
    private final JMSContextMetadata metadata;

    /*
     * We cache the ConnectionFactory here to avoid repeated JNDI lookup
     * If the bean is passivated/activated the field will be set to null 
     * and re-initialised lazily. (Though as a ConnectionFactory is required
     * to be Serializable this may not be needed)
     */
    private transient ConnectionFactory connectionFactory;

    @Inject
    public InjectableJMSContext(InjectionPoint ip, JMSContextManager manager) {
        JMSConnectionFactory jmsConnectionFactoryAnnot = ip.getAnnotated().getAnnotation(JMSConnectionFactory.class);
        JMSSessionMode                sessionModeAnnot = ip.getAnnotated().getAnnotation(JMSSessionMode.class);
        JMSPasswordCredential          credentialAnnot = ip.getAnnotated().getAnnotation(JMSPasswordCredential.class);

        id = UUID.randomUUID().toString();
        this.manager = manager;
        metadata = new JMSContextMetadata(jmsConnectionFactoryAnnot, sessionModeAnnot, credentialAnnot);
        logger.log(Level.FINE, localStrings.getLocalString("JMSContext.injection.initialization", 
                   "Injecting JMSContext wrapper with id {0} and metadata [{1}].", id, metadata));
    }

    @Override
    protected JMSContext delegate() {
        return manager.getContext(id, metadata, getConnectionFactory());
    }

    @Override
    public String toString() {
        JMSContext context = manager.getContext(id);
        StringBuffer sb = new StringBuffer();
        sb.append("JMSContext Wrapper ").append(id).append(" with metadata [").append(metadata).append("]");
        if (context != null)
            sb.append(", around ").append(context);
        return sb.toString();
    }

    @PreDestroy
    public void cleanup() {
        try {
            manager.cleanup();
            logger.log(Level.FINE, localStrings.getLocalString("JMSContext.injection.cleanup", 
                       "Cleaning up JMSContext wrapper with id {0} and metadata [{1}].", 
                       id, metadata.getLookup()));
        } catch (ContextNotActiveException cnae) {
            // ignore the ContextNotActiveException when the application is undeployed.
        } catch (Exception e) {
            logger.log(Level.SEVERE, localStrings.getLocalString("JMSContext.injection.cleanup.failure", 
                       "Failed to cleaning up JMSContext wrapper with id {0} and metadata [{1}]. Reason: {2}.", 
                        id, metadata.getLookup(), e.toString()));
        }
    }

    private ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            String jndiName;
            if (metadata.getLookup() == null || "".equals(metadata.getLookup())) {
                // Use platform default connection factory
                // Java EE 7: see http://java.net/jira/browse/JAVAEE_SPEC-2
                jndiName = DEFAULT_CONNECTION_FACTORY;
            } else {
                jndiName = metadata.getLookup();
            }

            InitialContext initialContext = null;
            try {
                initialContext = new InitialContext();
            } catch (NamingException ne) {
                throw new RuntimeException(localStrings.getLocalString("initialContext.init.exception", 
                                           "Cannot create InitialContext."), ne);
            }

            try {
                connectionFactory = (ConnectionFactory) initialContext.lookup(jndiName);
            } catch (NamingException ne) {
                throw new RuntimeException(localStrings.getLocalString("connectionFactory.not.found", 
                                           "ConnectionFactory not found with lookup {0}.", 
                                           jndiName), ne);
            }
        }
        return connectionFactory;
    }
}
