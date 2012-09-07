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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

/**
 * This bean has a map to store JMSContext instances based on the injection
 * point, that makes sure in one class, the injected JMSContext beans of
 * different injection point will not share the same request scoped JMSContext
 * instance in a request.
 */
@RequestScoped
public class JMSContextManager implements Serializable {
    private final static Logger logger = LogDomains.getLogger(JMSContextManager.class, LogDomains.JMS_LOGGER);
    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(JMSContextManager.class);

    private final Map<String, JMSContext> contexts;

    public JMSContextManager() {
        contexts = new HashMap<String, JMSContext>();
    }

    public JMSContext getContext(String id, JMSContextMetadata metadata, ConnectionFactory connectionFactory) {
        JMSContext context = contexts.get(id);
        if (context == null) {
            int sessionMode = metadata.getSessionMode();
            String userName = metadata.getUserName();
            if (userName == null) {
                context = connectionFactory.createContext(sessionMode);
            } else {
                String password = metadata.getPassword();
                context = connectionFactory.createContext(userName, password, sessionMode);
            }
            logger.log(Level.FINE, localStrings.getLocalString("JMSContext.impl.create", 
                                   "Created new JMSContext instance associated with id {0}: {1}.", 
                                   id, context.toString()));
            contexts.put(id, context);
        }
        return context;
    }

    JMSContext getContext(String id) {
        return contexts.get(id);
    }

   // Close and remove the JMSContext instances
   @PreDestroy
   public void cleanup() {
       for (Entry<String, JMSContext> entry : contexts.entrySet()) {
           String id = entry.getKey();
           JMSContext context = entry.getValue();
           try {
               context.close();
               logger.log(Level.FINE, localStrings.getLocalString("JMSContext.impl.close", 
                                      "Closed JMSContext instance associated with id {0}: {1}.", 
                                      id, context.toString()));
           } catch (Exception e) {
               logger.log(Level.SEVERE, localStrings.getLocalString("JMSContext.impl.close.failure", 
                                        "Failed to close JMSContext instance associated with id {0}: {1}.", 
                                        id, context.toString()), e);
           }
       }
       contexts.clear();
   }
}
