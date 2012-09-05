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

/*
 * SunTransactionHelper.java
 *
 * Created on March 13, 2003.
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.transaction.*;
import javax.naming.InitialContext;


import com.sun.appserv.jdbc.DataSource;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.spi.ConnectorNamingEventListener;
import com.sun.appserv.connectors.internal.spi.ConnectorNamingEvent;
import com.sun.ejb.containers.EjbContainerUtil;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;
import org.glassfish.persistence.common.I18NHelper;
import org.glassfish.internal.api.Globals;


/** Sun specific implementation for TransactionHelper interface.
* This class has a special implementation for 
* <code>registerSynchronization</code>, because it uses a special 
* object that registers Synchronization instance to be processed after 
* any bean's or container beforeCompletion method, but before the corresponding 
* afterCompletion.
*/
public class SunTransactionHelper extends TransactionHelperImpl
        implements //ApplicationLoaderEventListener,
        ConnectorNamingEventListener
    {

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
        SunTransactionHelper.class.getClassLoader());

    private static List<PersistenceManagerFactory> pmf_list;
    
    private static EjbContainerUtil ejbContainerUtil;
    
    private final static Object pmf_listSyncObject = new Object();
    
    /**
     * Array of registered ApplicationLifeCycleEventListener 
     */ 
    private final List<ApplicationLifeCycleEventListener> applicationLifeCycleEventListeners = new ArrayList<ApplicationLifeCycleEventListener>();


    /** Garantees singleton.
     * Registers itself during initial load
     */
    static {
        SunTransactionHelper helper = new SunTransactionHelper();
        EJBHelper.registerTransactionHelper (helper);
        // Register with ApplicationLoaderEventNotifier to receive Sun
        // Application Server specific lifecycle events.
//        ApplicationLoaderEventNotifier.getInstance().addListener(helper);
        ConnectorRuntime connectorRuntime = Globals.getDefaultHabitat().getService(ConnectorRuntime.class);
        connectorRuntime.registerConnectorNamingEventListener(helper);
        
        pmf_list = new ArrayList<PersistenceManagerFactory>();

        ejbContainerUtil = Globals.getDefaultHabitat().getService(EjbContainerUtil.class);
    }
 
    /** Default constructor should not be public */
    SunTransactionHelper() { }

    // helper class for looking up the TransactionManager instances.
    static private class TransactionManagerFinder {
        
        // JNDI name of the TransactionManager used for managing local transactions.
        static private final String AS_TM_NAME = "java:appserver/TransactionManager"; //NOI18N

        // TransactionManager instance used for managing local transactions.
        static TransactionManager appserverTM = null;

        static {
            try {
                appserverTM = (TransactionManager) (new InitialContext()).lookup(AS_TM_NAME);
            } catch (Exception e) {
                throw new JDOFatalInternalException(e.getMessage());
            }
        }
    }

    /** SunTransactionHelper specific code */
    public Transaction getTransaction(){
       try{
            return TransactionManagerFinder.appserverTM.getTransaction();
        } catch (Exception e) {
            throw new JDOFatalInternalException(e.getMessage());
        } catch (ExceptionInInitializerError err) {
            throw new JDOFatalInternalException(err.getMessage());
        }
    }

    /** SunTransactionHelper specific code */
    public UserTransaction getUserTransaction() {
	try {
	    InitialContext ctx =
                (InitialContext) Class.forName("javax.naming.InitialContext").newInstance(); //NOI18N

            return (UserTransaction)ctx.lookup("java:comp/UserTransaction"); //NOI18N
	} catch (Exception e) {
	    throw new JDOFatalInternalException(e.getMessage());
	}
    }

    /** SunTransactionHelper specific code */
    public void registerSynchronization(Transaction jta, Synchronization sync)
            throws RollbackException, SystemException {
        ejbContainerUtil.registerPMSync(jta, sync);
    }

    /** SunTransactionHelper specific code */
    public PersistenceManagerFactory replaceInternalPersistenceManagerFactory(
	PersistenceManagerFactory pmf) {

        synchronized(pmf_listSyncObject) {
	    int i = pmf_list.indexOf(pmf);
	    if (i == -1) {
	        // New PersistenceManagerFactory. Remember it.
	        pmf_list.add(pmf);
	        return pmf;
	    }

	    return pmf_list.get(i);
        }
    }

    /** 
     * Returns name prefix for DDL files extracted from the info instance by the
     * application server specific code.
     * SunTransactionHelper specific code. Delegates the actual implementation
     * to DeploymentHelper#getDDLNamePrefix(Object);
     *   
     * @param info the instance to use for the name generation.
     * @return name prefix as String. 
     */   
    public String getDDLNamePrefix(Object info) { 
        return DeploymentHelper.getDDLNamePrefix(info);
    }

    /** Called in a managed environment to get a Connection from the application
     * server specific resource. In a non-managed environment returns null as
     * it should not be called.
     * SunTransactionHelper specific code uses com.sun.appserv.jdbc.DataSource
     * to get a Connection.
     *
     * @param resource the application server specific resource.
     * @param username the resource username. If null, Connection is requested
     * without username and password validation.
     * @param password the password for the resource username.
     * @return a Connection.
     * @throws java.sql.SQLException
     */
    public java.sql.Connection getNonTransactionalConnection(
            Object resource, String username, String password) 
            throws java.sql.SQLException {

        java.sql.Connection rc = null;
        // resource is expected to be com.sun.appserv.jdbc.DataSource
        if (resource instanceof DataSource) {
            DataSource ds = (DataSource)resource;
            if (username == null) {
                rc = ds.getNonTxConnection();
            } else {
                rc = ds.getNonTxConnection(username, password);
            }
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.SunTransactionHelper.wrongdatasourcetype", //NOI18N
                resource.getClass().getName())); 
        }
        return rc;
    }

    /** SunTransactionHelper specific code */
    public TransactionManager getLocalTransactionManager() {
        try {
            return TransactionManagerFinder.appserverTM;
        } catch (ExceptionInInitializerError err) {
                throw new JDOFatalInternalException(err.getMessage());
        }
    }
    
    /**
     * @inheritDoc
     */ 
    public void registerApplicationLifeCycleEventListener(
            ApplicationLifeCycleEventListener listener) {
        synchronized(applicationLifeCycleEventListeners) {
             applicationLifeCycleEventListeners.add(listener);
        }
    }
    //-------------------ApplicationLifeCycleEventListener Methods --------------//

    /**
     * @inheritDoc
     */
    public void notifyApplicationUnloaded(ClassLoader classLoader) {
        for (Iterator iterator = applicationLifeCycleEventListeners.iterator();
               iterator.hasNext();) {
            ApplicationLifeCycleEventListener applicationLifeCycleEventListener =
                    (ApplicationLifeCycleEventListener) iterator.next();
            applicationLifeCycleEventListener.notifyApplicationUnloaded(classLoader);
        }
    }

//    /**
//     * @inheritDoc
//     */
//    public void handleEjbContainerEvent(EjbContainerEvent event) {
//        //Ignore EjbContainerEvents
//    }
//
    /**
     * @inheritDoc
     */
    public void connectorNamingEventPerformed(ConnectorNamingEvent event){
        if(event.getEventType() == ConnectorNamingEvent.EVENT_OBJECT_REBIND){
            String dsName = ConnectorsUtil.getPMJndiName(event.getJndiName());
            cleanUpResources(dsName);
        } // Ignore all other events.
    }

    /** 
     * Removes all entries that correspond to the same connection factory name.
     * @param name the connection factory name.
     */
    private void cleanUpResources(String name) {
        synchronized(pmf_listSyncObject) {
            for (Iterator it = pmf_list.iterator(); it.hasNext(); ) {
                PersistenceManagerFactory pmf = (PersistenceManagerFactory)it.next();        
                if (pmf.getConnectionFactoryName().equals(name)) {
                    it.remove();
                }
            }
        }
    }

}
