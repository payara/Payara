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
 * SunContainerHelper.java
 *
 * Created on March 12, 2003.
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.Iterator;
import java.util.ResourceBundle;

import javax.ejb.EJBObject;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.EntityContext;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.sql.DataSource;

import com.sun.enterprise.deployment.*;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil; //TODO Dependency on connector-internal-api needs to be removed

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ApplicationInfo;

import com.sun.ejb.Container;
import com.sun.ejb.containers.EjbContainerUtil;
import org.glassfish.persistence.ejb.entitybean.container.spi.CascadeDeleteNotifier;

import com.sun.jdo.api.persistence.support.JDOException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOFatalUserException;
import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;

import com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerFactoryImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.utility.NumericConverter;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import org.glassfish.internal.api.Globals;

/** Implementation for Sun specific CMP and Container interactions as defined
* by the ContainerHelper interface.
* 
* IMPORTANT: This class extends SunTransactionHelper class. Any changes to the 
* TransactionHelper implementation must be done in the SunTransactionHelper.
*
*/
public class SunContainerHelper extends SunTransactionHelper implements ContainerHelper
    {

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
        SunContainerHelper.class.getClassLoader());

    /** The logger */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

    /** Garantees singleton.
     * Registers itself during initial load
     */  
    static {
        CMPHelper.registerContainerHelper (new SunContainerHelper());
    }

    /** Default constructor should not be public */
    SunContainerHelper() { }

    /** Get a Container helper instance that will be passed unchanged to the
     * required methods.
     * This is SunContainerHelper specific code.
     *
     * The info argument is an Object array that consistes of a class to use
     * for the class loader and concreteImpl bean class name.
     * @see getEJBObject(Object, Object)
     * @see getEJBLocalObject(Object, Object)
     * @see getEJBLocalObject(Object, Object, EJBObject)
     * @see removeByEJBLocalObject(EJBLocalObject, Object)
     * @see removeByPK(Object, Object)
     * @param info Object with the request information that is application server
     * specific.
     * @return a Container helper instance as an Object.
     */
    public Object getContainer(Object info) {

        Object[] params = (Object[])info;
        String appName = (String)params[0];

        ServiceLocator habitat = Globals.getDefaultHabitat();
        ApplicationRegistry reg = habitat.getService(ApplicationRegistry.class);
        ApplicationInfo appInfo = reg.get(appName);
        Application app = appInfo.getMetaData(Application.class);

        EjbDescriptor desc = app.getEjbByName((String)params[1]);

        return habitat.<EjbContainerUtil>getService(EjbContainerUtil.class).getContainer(desc.getUniqueId());
    }

    /** Get an EJBObject reference for this primary key and Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @return a corresponding EJBObject instance to be used by the client.
     */
    public EJBObject getEJBObject(Object pk, Object container) {
        try {
            return ((Container)container).getEJBObjectForPrimaryKey(pk);
        } catch (Exception ex) {
            throw new JDOFatalInternalException(ex.getMessage(), ex);
        }
    }

    /** Get an EJBLocalObject reference for this primary key and Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a helper instance for the request.
     * @return a corresponding EJBLocalObject instance to be used by the client.
     */
    public EJBLocalObject getEJBLocalObject(Object pk, Object container) {
        try {
            return ((Container)container).getEJBLocalObjectForPrimaryKey(pk);
        } catch (Exception ex) {
            throw new JDOFatalInternalException(ex.getMessage(), ex);
        }
    }

    /** Get an EJBLocalObject reference for this primary key and Container helper,
     * and EJBContext of the calling bean.
     * Allows the container to check if this method is called during ejbRemove
     * that is part of a cascade-delete remove.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a helper instance for the request.
     * @param context an EJBContext of the calling bean.
     * @return a corresponding EJBLocalObject instance to be used by the client.
     */
    public EJBLocalObject getEJBLocalObject(Object pk, Object container, EJBContext context) {
        EJBLocalObject rc = null;
        try {
            rc = ((Container)container).getEJBLocalObjectForPrimaryKey(pk, context);
        } catch (Exception ex) {
            processContainerException(ex);
        }

        return rc;
    }

    /** Remove a bean for a given EJBLocalObject and Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param ejb the EJBLocalObject for the bean to be removed.
     * @param container a Container instance for the request.
     */
    public void removeByEJBLocalObject(EJBLocalObject ejb, Object container) {
        try {
            ((Container)container).removeBeanUnchecked(ejb);
        } catch (Exception ex) {
            processContainerException(ex);
        }
    }

    /** Remove a bean for a given primary key and Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param pk the primary key for the bean to be removed.
     * @param container a Container instance for the request.
     */
    public void removeByPK(Object pk, Object container) {
        try {
            ((Container)container).removeBeanUnchecked(pk);
        } catch (Exception ex) {
            processContainerException(ex);
        }
    }

    /** Verify that this instance is of a valid local interface type for
     * a given Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param o the instance to be verified.
     * @param container a Container instance for the request.
     */
    public void assertValidLocalObject(Object o, Object container) {
        ((Container)container).assertValidLocalObject(o);
    }

    /** Verify that this instance is of a valid remote interface type for
     * a given Container helper.
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param o the instance to be verified.
     * @param container a Container instance for the request.
     */
    public void assertValidRemoteObject(Object o, Object container) {
        ((Container)container).assertValidRemoteObject(o);
    }

    /** Mark the bean as already removed during cascade-delete
     * operation for a given EntityContext.
     * Called by the generated ejbRemove method before calling ejbRemove of the
     * related beans (via removeByEJBLocalObject) that are to be cascade-deleted.
     *
     * This is SunContainerHelper specific code.
     *
     * @param context the EntityContext of the bean beeing removed.
     */
    public void setCascadeDeleteAfterSuperEJBRemove(EntityContext context) {
        try {
            ((CascadeDeleteNotifier)context).setCascadeDeleteAfterSuperEJBRemove(true);
        } catch (Exception ex) {
            processContainerException(ex);
        }
    }

    /** Called in a CMP supported environment. Notifies the container that
     * ejbSelect had been called.
     *
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */
    public void preSelect(Object container) {
        ((Container)container).preSelect();
    }

    /** Called in a CMP environment to lookup PersistenceManagerFactory
     * referenced by this Container instance as the CMP resource.
     *
     * This is SunContainerHelper specific code.
     *
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */
    public PersistenceManagerFactory getPersistenceManagerFactory(Object container) {
        Object rc = null;
        PersistenceManagerFactoryImpl pmf = null;

        ResourceReferenceDescriptor cmpResource = ((Container)container).getEjbDescriptor().
            getEjbBundleDescriptor().getCMPResourceReference();

        String name = cmpResource.getJndiName();

        try {
            InitialContext ic = new InitialContext();
            rc = ic.lookup(name);

            if (rc instanceof PersistenceManagerFactoryImpl) {
                pmf = (PersistenceManagerFactoryImpl)rc;

            } else if (rc instanceof javax.sql.DataSource) {
                pmf = new PersistenceManagerFactoryImpl();
                pmf.setConnectionFactoryName(ConnectorsUtil.getPMJndiName(name));

                Iterator it = cmpResource.getProperties();
                if (it != null) {
                    while (it.hasNext()) {
                        NameValuePairDescriptor prop = (NameValuePairDescriptor)it.next();
                        String n = prop.getName();

                        // Any value that is not "true" is treated as "false":
                        boolean value = Boolean.valueOf(prop.getValue()).booleanValue();
                        pmf.setBooleanProperty(n, value);

                    }
                }

            } else {
                RuntimeException e = new JDOFatalUserException(I18NHelper.getMessage(
                    messages, "ejb.jndi.unexpectedinstance", //NOI18N
                    name, rc.getClass().getName()));
                logger.severe(e.toString());

                throw e;
            }
        } catch (javax.naming.NamingException ex) {
            RuntimeException e = new JDOFatalUserException(I18NHelper.getMessage(
                messages, "ejb.jndi.lookupfailed", name), ex); //NOI18N
            logger.severe(e.toString());

            throw e;
        }

        return pmf;

    }

    /**
     * Called in CMP environment to get NumericConverter policy referenced
     * by this Container instance.
     * @see getContainer(Object)
     * @param container a Container instance for the request
     * @return a valid NumericConverter policy type
     */
    public int getNumericConverterPolicy(Object container) {
        return NumericConverter.DEFAULT_POLICY;
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     * This is a generic implementation.
     * Application server may like to overwrite this if necessary.
     *
     * @param pm PersistenceManager
     */
    public void beginInternalTransaction(PersistenceManager pm) {
        pm.currentTransaction().begin();
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     * This is a generic implementation.
     * Application server may like to overwrite this if necessary.
     *
     * @param pm PersistenceManager
     */
    public void commitInternalTransaction(PersistenceManager pm) {
        pm.currentTransaction().commit();
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     * This is a generic implementation.
     * Application server may like to overwrite this if necessary.
     *
     * @param pm PersistenceManager
     */
    public void rollbackInternalTransaction(PersistenceManager pm) {
        pm.currentTransaction().rollback();
    }

    /** Called from read-only beans to suspend current transaction.
     * This will guarantee that PersistenceManager is not bound to
     * any transaction.
     *
     * @return javax.transaction.Transaction object representing 
     * the suspended transaction.
     * Returns null if the calling thread is not associated
     * with a transaction.
     */
    public javax.transaction.Transaction suspendCurrentTransaction() {
        javax.transaction.Transaction tx = null;
        try {
            tx = getLocalTransactionManager().suspend();
        } catch (Exception ex) {
            processContainerException(ex);
        }

        return tx;
    }

    /** Called from read-only beans to resume current transaction.
     * This will guarantee that the transaction continues to run after
     * read-only bean accessed its PersistenceManager.
     *
     * @param tx - The javax.transaction.Transaction object that 
     * represents the transaction to be resumed. If this object had been
     * returned by #suspendCurrentTransaction() call it will be null in
     * case calling thread was not associated with a transaction.
     */
    public void resumeCurrentTransaction(javax.transaction.Transaction tx) {
        try {
            // Resume only real (i.e. not null transaction)
            if (tx != null) {
                getLocalTransactionManager().resume(tx);
            }
        } catch (Exception ex) {
            processContainerException(ex);
        }
    }

    /**
     * Checks the caught Exception, and rethrows it if it is
     * of one of known types, or converts to an EJBException
     * otherwise.
     *
     * @param ex the Exception to process.
     * @throws RuntimeException of the appropriate type.
     */
    private void processContainerException(Exception ex) {
        if (ex instanceof EJBException) {
            throw (EJBException)ex;

        } else if (ex instanceof IllegalArgumentException
                || ex instanceof IllegalStateException) {
            throw (RuntimeException)ex;

        } else if (ex instanceof JDOException) {
            throw (JDOException)ex;

        } else {
            throw new EJBException(ex);
        }
    }
}
