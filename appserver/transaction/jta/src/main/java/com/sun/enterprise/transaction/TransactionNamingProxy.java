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

import org.glassfish.api.naming.*;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.transaction.spi.TransactionOperationsManager;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.naming.NamingException;

/**
 * Proxy for creating JTA instances that get registered in the naming manager.
 * NamingManager will call the handle() method when the JNDI name is looked up.
 * Will return the instance that corresponds to the known name.
 *
 * @author Marina Vatkina
 */
@Service
@NamespacePrefixes({TransactionNamingProxy.USER_TX,
        TransactionNamingProxy.TRANSACTION_SYNC_REGISTRY,
        TransactionNamingProxy.APPSERVER_TRANSACTION_MGR,
        TransactionNamingProxy.APPSERVER_TRANSACTION_SYNC_REGISTRY})
public class TransactionNamingProxy 
        implements NamedNamingObjectProxy {

    @Inject
    private ServiceLocator habitat;
    
    @SuppressWarnings("unused")
    @Inject
    private org.glassfish.api.admin.ProcessEnvironment processEnv;  // Here for ordering

    static final String USER_TX = "java:comp/UserTransaction";
    static final String USER_TX_NO_JAVA_COMP = "UserTransaction";

    static final String TRANSACTION_SYNC_REGISTRY
            = "java:comp/TransactionSynchronizationRegistry";

    static final String APPSERVER_TRANSACTION_SYNC_REGISTRY
            = "java:appserver/TransactionSynchronizationRegistry";

    static final String TRANSACTION_MGR
            = "java:pm/TransactionManager";

    static final String APPSERVER_TRANSACTION_MGR
            = "java:appserver/TransactionManager";

    public Object handle(String name) throws NamingException {

        if (USER_TX.equals(name)) {
            checkUserTransactionLookupAllowed();
            return habitat.getService(UserTransactionImpl.class);
        } else if (TRANSACTION_SYNC_REGISTRY.equals(name) || APPSERVER_TRANSACTION_SYNC_REGISTRY.equals(name)) {
            return habitat.getService(TransactionSynchronizationRegistryImpl.class);
        } else if (APPSERVER_TRANSACTION_MGR.equals(name)) {
            return habitat.getService(TransactionManagerHelper.class);
        }

        return null;
    }

    private void checkUserTransactionLookupAllowed() throws NamingException {
        InvocationManager iv = habitat.getService(InvocationManager.class);
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
