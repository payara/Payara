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

package com.sun.enterprise.transaction.startup;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingException;

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.config.TransactionService;
import com.sun.logging.LogDomains;

/**
 * Service wrapper to only lookup the transaction recovery when there
 * are applications deployed since the actual service has ORB dependency.
 *
 * This is also responsible for binding (non java:comp) UserTransaction in naming tree.
 */
@Service
//todo: change value=10 to a public constant
@RunLevel( value=10 )
public class TransactionLifecycleService implements PostConstruct, PreDestroy {
//  public class TransactionLifecycleService implements Startup, PostConstruct, PreDestroy {

    @Inject
    ServiceLocator habitat;

    @Inject
    Events events;

    @Inject @Optional
    GlassfishNamingManager nm;

    static final String USER_TX_NO_JAVA_COMP = "UserTransaction";

    private static Logger _logger = LogDomains.getLogger(TransactionLifecycleService.class, LogDomains.JTA_LOGGER);

    private JavaEETransactionManager tm = null;

    @Override
    public void postConstruct() {
        EventListener glassfishEventListener = new EventListener() {
            @Override
            public void event(Event event) {
                if (event.is(EventTypes.SERVER_READY)) {
                    _logger.fine("TM LIFECYCLE SERVICE - ON READY");
                    onReady();
                } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {  
                    _logger.fine("TM LIFECYCLE SERVICE - ON SHUTDOWN");
                    onShutdown();
                }
            }
        };
        events.register(glassfishEventListener);
        if (nm != null) {
            try {
                nm.publishObject(USER_TX_NO_JAVA_COMP, new NamingObjectProxy.InitializationNamingObjectProxy() {
                    @Override
                    public Object create(Context ic) throws NamingException {
                        ActiveDescriptor<?> descriptor = habitat.getBestDescriptor(
                                BuilderHelper.createContractFilter("javax.transaction.UserTransaction"));
                        if (descriptor == null) return null;
                        
                        return habitat.getServiceHandle(descriptor).getService();
                    }
                }, false);
            } catch (NamingException e) {
                _logger.warning("Can't bind \"UserTransaction\" in JNDI");
            }
        }
    }

    @Override
    public void preDestroy() {
        if (nm != null) {
            try {
                nm.unpublishObject(USER_TX_NO_JAVA_COMP);
            } catch (NamingException e) {
                _logger.warning("Can't unbind \"UserTransaction\" in JNDI");
            }
        }
    }

    public void onReady() {
        _logger.fine("ON TM READY STARTED");

        TransactionService txnService = habitat.getService(TransactionService.class);
        if (txnService != null) {
            boolean isAutomaticRecovery = Boolean.valueOf(txnService.getAutomaticRecovery());
            if (isAutomaticRecovery) {
                _logger.fine("ON TM RECOVERY START");
                tm = habitat.getService(JavaEETransactionManager.class);
                tm.initRecovery(false);
                _logger.fine("ON TM RECOVERY END");
            }
        }

        _logger.fine("ON TM READY FINISHED");
    }

    public void onShutdown() {
        // Cleanup if TM was loaded
        if (tm == null) {
            ServiceHandle<JavaEETransactionManager> inhabitant =
                    habitat.getServiceHandle(JavaEETransactionManager.class);
            if (inhabitant != null && inhabitant.isActive()) {
                tm = inhabitant.getService();
            }
        }
        if (tm != null) {
            _logger.fine("ON TM SHUTDOWN STARTED");
            tm.shutdown();
            _logger.fine("ON TM SHUTDOWN FINISHED");
        }

    }

}
