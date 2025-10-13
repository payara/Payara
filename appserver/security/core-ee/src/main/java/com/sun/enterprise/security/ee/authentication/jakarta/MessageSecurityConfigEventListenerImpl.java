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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2024] [Payara Foundation]

package com.sun.enterprise.security.ee.authentication.jakarta;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;

import jakarta.security.auth.message.config.AuthConfigFactory;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.logging.LogDomains;

/**
 * Listener class to handle admin message-security-config element events.
 * 
 * @author Nithya Subramanian
 */

@Service
@RunLevel(StartupRunLevel.VAL)
public class MessageSecurityConfigEventListenerImpl implements ConfigListener {

    private static Logger logger = LogDomains.getLogger(MessageSecurityConfigEventListenerImpl.class, SECURITY_LOGGER, false);

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private SecurityService service;
    
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        ConfigSupport.sortAndDispatch(events, new Changed() {

            /**
             * Notification of a change on a configuration object
             * 
             * @param type type of change : ADD mean the changedInstance was added to the parent REMOVE means the changedInstance
             * was removed from the parent, CHANGE means the changedInstance has mutated.
             * @param changedType type of the configuration object
             * @param changedInstance changed instance.
             */
            @Override
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
                NotProcessed notProcessed = null;
                switch (type) {
                case ADD:
                    logger.fine("A new " + changedType.getName() + " was added : " + " " + changedInstance);
                    notProcessed = handle(changedInstance);
                    break;
                case CHANGE:
                    logger.fine("A " + changedType.getName() + " was changed : " + changedInstance);
                    notProcessed = handle(changedInstance);
                    break;
                case REMOVE:
                    logger.fine("A " + changedType.getName() + " was removed : " + changedInstance);
                    notProcessed = handle(changedInstance);
                    break;
                }
                
                return notProcessed;
            }
        }, logger);

        return null;
    }

    private <T extends ConfigBeanProxy> NotProcessed handle(T instance) {
        if (instance instanceof MessageSecurityConfig) {
            AuthConfigFactory factory = AuthConfigFactory.getFactory();
            if (factory != null) {
                factory.refresh();
            }
            return null;
        }

        return new NotProcessed("unimplemented: unknown instance: " + instance.getClass().getName());
    }
}
