/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.api.naming.NamingObjectsProvider;
//import org.glassfish.api.naming.GlassfishNamingManager;
//import org.glassfish.api.admin.ProcessEnvironment;
//import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;

import com.sun.enterprise.transaction.spi.TransactionOperationsManager;
import com.sun.logging.LogDomains;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;

import javax.naming.Context;
import javax.naming.NamingException;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Proxy for creating JTA instances that get registered in the naming manager.
 * NamingManager will call the handle() method when the JNDI name is looked up.
 * Will return the instance that corresponds to the known name.
 *
 * @author Marina Vatkina
 */
@Service
public class TransactionNamingProxy 
        implements NamedNamingObjectProxy, NamingObjectsProvider, PostConstruct {

    @Inject
    private Habitat habitat;

    @Inject
    private ExecutorService es;
    
    @Inject
    private org.glassfish.api.admin.ProcessEnvironment processEnv;

    private static Logger logger = LogDomains.getLogger(TransactionNamingProxy.class, LogDomains.JTA_LOGGER);

    private static final String USER_TX = "java:comp/UserTransaction";
    private static final String USER_TX_NO_JAVA_COMP = "UserTransaction";

    private static final String TRANSACTION_SYNC_REGISTRY 
            = "java:comp/TransactionSynchronizationRegistry";

    private static final String APPSERVER_TRANSACTION_SYNC_REGISTRY
            = "java:appserver/TransactionSynchronizationRegistry";

    private static final String TRANSACTION_MGR
            = "java:pm/TransactionManager";

    private static final String APPSERVER_TRANSACTION_MGR 
            = "java:appserver/TransactionManager";

    public void postConstruct() {
//        Issue 13108 (Cycle in our component chain involving GlassFishNamingManager
//        and TransactionNamingProxy).  Comment out the publishing of "UserTransaction"
//        and related field injections to break the circular dependency.

        if( processEnv.getProcessType().isServer()) {
            final Habitat h = habitat;
            // made the lookup of the naming manager asynchronous to avoid getting
            // in an infinite cyclic dependency (see IT 13108). By making the lookup
            // asynchronous, we ensure the NamingManager implementation has finished
            // initializing in *this* thread before getting returned by the
            // getComponent() call below.
            es.submit(new Runnable() {
                @Override
                public void run() {
                    GlassfishNamingManager namingMgr = h.getComponent(GlassfishNamingManager.class);
                    try {
                        namingMgr.publishObject(USER_TX_NO_JAVA_COMP,
                            new UserTransactionProxy(), true);
                    } catch (NamingException e) {
                       logger.warning("Can't bind \"UserTransaction\" in JNDI");
                    }
                }
            });
        }

    }

    public Object handle(String name) throws NamingException {

        if (USER_TX.equals(name)) {
            checkUserTransactionLookupAllowed();
            return habitat.getComponent(UserTransactionImpl.class);
        } else if (TRANSACTION_SYNC_REGISTRY.equals(name) || APPSERVER_TRANSACTION_SYNC_REGISTRY.equals(name)) {
            return habitat.getComponent(TransactionSynchronizationRegistryImpl.class);
        } else if (APPSERVER_TRANSACTION_MGR.equals(name)) {
            return habitat.getComponent(TransactionManagerHelper.class);
        }

        return null;
    }

    private class UserTransactionProxy implements NamingObjectProxy {

        public Object create(Context ic) throws NamingException {
            checkUserTransactionLookupAllowed();
            return habitat.getComponent(UserTransactionImpl.class);
        }
    }

    private void checkUserTransactionLookupAllowed() throws NamingException {
        InvocationManager iv = habitat.getByContract(InvocationManager.class);
        if (iv != null) {
            ComponentInvocation inv = iv.getCurrentInvocation();
            if (inv != null) {
                TransactionOperationsManager toMgr =
                        (TransactionOperationsManager)inv.getTransactionOperationsManager();
                if ( toMgr != null ) {
                    toMgr.userTransactionLookupAllowed();
                }
            }
        }
    }
}
