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

package com.sun.ejb;

import java.rmi.Remote;

import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBObject;
import javax.ejb.FinderException;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/**
 * A Container stores EJB instances and is responsible for
 * the lifecycle, state management, concurrency, transactions, security,
 * naming, resource management, etc.
 * It does the above by interposing actions before
 * and after invocations on EJBs.
 * It uses the ProtocolManager, SecurityManager, TransactionManager,
 * NamingManager for help with the above responsibilities.
 * There are four types of Containers:
 * StatefulSessionContainer, StatelessSessionContainer,
 * EntityContainer, and MessageBeanContainer.
 * Note: the term "Container" here refers
 * to an instance of one of the above container classes.
 * In the EJB spec "container" refers to a process or JVM which
 * hosts EJB instances.
 * <p>
 * There is one instance of the Container for each EJB type (deployment desc).
 * When a JAR is deployed on the EJB server, a Container instance is created 
 * for each EJB declared in the ejb-jar.xml for the EJB JAR.
 * <p>
 * The Container interface provides methods called from other parts of
 * the RI as well as from generated EJBHome/EJBObject implementations.
 *
 */
public interface Container {

    // These values are for the transaction attribute of a bean method    
    public int TX_NOT_INITIALIZED = 0; // default
    public int TX_NOT_SUPPORTED = 1;
    public int TX_BEAN_MANAGED = 2;
    public int TX_REQUIRED = 3;
    public int TX_SUPPORTS = 4;
    public int TX_REQUIRES_NEW = 5;
    public int TX_MANDATORY = 6;
    public int TX_NEVER = 7;

    // Must match the values of the tx attributes above.
    public String[] txAttrStrings = { "TX_NOT_INITIALIZED",
                                      "TX_NOT_SUPPORTED",
                                      "TX_BEAN_MANAGED",
                                      "TX_REQUIRED",
                                      "TX_SUPPORTS",
                                      "TX_REQUIRES_NEW",
                                      "TX_MANDATORY",
                                      "TX_NEVER" };

    // These values are for the security attribute of a bean method    
    public int SEC_NOT_INITIALIZED = 0; // default
    public int SEC_UNCHECKED = 1;
    public int SEC_EXCLUDED = 2;
    public int SEC_CHECKED = 3;

    public String[] secAttrStrings = { "SEC_NOT_INITIALIZED",
                                       "SEC_UNCHECKED", 
                                       "SEC_EXCLUDED", 
                                       "SEC_CHECKED" };


    
    /**
     * Return the EJBObject/EJBHome for the given instanceKey.
     * @param remoteBusinessIntf True if this invocation is for the RemoteHome
     * view of the bean.  False if for the RemoteBusiness view.
     * Called from the ProtocolManager when a remote invocation arrives.
     */
    Remote getTargetObject(byte[] instanceKey, String remoteBusinessIntf);

    /**
     * Release the EJBObject/EJBHome object.
     * Called from the ProtocolManager after a remote invocation completes.
     */
    void releaseTargetObject(Remote remoteObj);
   
    /**
     * Performs pre external invocation setup such as setting application 
     * context class loader.  Called by getTargetObject() and web service inv
     */
    public void externalPreInvoke();

    /**
     * Performs post external invocation cleanup such as restoring the original
     * class loader.  Called by releaseTargetObject() and web service inv
     */
    public void externalPostInvoke();

    /**
     * Obtain an Entity EJBObject corresponding to the primary key.
     * Used by the PersistenceManager.
     */
    EJBObject getEJBObjectForPrimaryKey(Object pkey);

    /**
     * Obtain an Entity EJBLocalObject corresponding to the primary key.
     * Used by the PersistenceManager.
     */
    EJBLocalObject getEJBLocalObjectForPrimaryKey(Object pkey, EJBContext ctx);
    EJBLocalObject getEJBLocalObjectForPrimaryKey(Object pkey);

    /**
     * Verify that a given object is an EJBLocalObject of an ejb from this
     * ejb container.  The given object must be an EJBLocalObject and have
     * the same ejb type ( meaning same ejb-jar and same ejb-name ) as this
     * container.  Note that for entity beans this equality check is independent of
     * primary key.
     *
     * @exception EJBException Thrown when the assertion fails.
     */
    void assertValidLocalObject(Object o) throws EJBException;

    /**
     * Verify that a given object is an EJBObject of an ejb from this
     * ejb container.  The given object must be an EJBObject and have
     * the same ejb type ( meaning same ejb-jar and same ejb-name ) as this
     * container.  Note that for entity beans this equality check is independent of
     * primary key.
     *
     * @exception EJBException Thrown when the assertion fails.
     */
    void assertValidRemoteObject(Object o) throws EJBException;

    /**
     * Remove a bean. Used by the PersistenceManager.
     */
    void removeBeanUnchecked(EJBLocalObject bean);

    /**
     * Remove a bean given primary key. Used by the PersistenceManager.
     */
    void removeBeanUnchecked(Object pkey);

    /**
     * Notification from persistence manager than an ejbSelect
     * query is about to be invoked on a bean of the ejb type
     * for this container.    This allows the ejb container
     * to perform the same set of actions as take place before a
     * finder method, such as calling ejbStore on bean instances.
     * (See EJB 2.1, Section 10.5.3 ejbFind,ejbStore)
     *
     * @exception javax.ejb.EJBException  Thrown if an error occurs
     *          during the preSelect actions performed by the container.
     *          If thrown, the remaining select query steps should be 
     *          aborted and an EJBException should be propagated
     *          back to the application code.
     */
    void preSelect() throws javax.ejb.EJBException;


    /**
     * Called by the EJB(Local)Object/EJB(Local)Home before an invocation 
     * on a bean.
     */
    void preInvoke(EjbInvocation inv);

    /**
     * Called by the EJB(Local)Object/EJB(Local)Home after an invocation 
     * on a bean.
     */
    void postInvoke(EjbInvocation inv);

    /**
     * Called by webservice code to do ejb invocation post processing.
     */
    void webServicePostInvoke(EjbInvocation inv);

    /**
     * Called by the EJB(Local)Home after invoking ejbCreate on an EntityBean.
     * After this postCreate the EJB(Local)Home can call ejbPostCreate on
     * the EntityBean.
     * @param primaryKey the value returned from ejbCreate.
     */
    void postCreate(EjbInvocation inv, Object primaryKey)
	throws CreateException;

    /**
     * Called by the EJB(Local)Home after invoking ejbFind* on an EntityBean.
     * @param primaryKeys the primaryKey or collection of primaryKeys 
     *        (Collection/Enumeration) returned from ejbFind.
     * @param findParams the parameters to the ejbFind method.
     * @return an EJBObject reference or Collection/Enumeration of EJBObjects.
     */
    Object postFind(EjbInvocation inv, Object primaryKeys, Object[] findParams)
	throws FinderException;
   
    /**
     * @return the EjbDescriptor containing deployment information 
     * for the EJB type corresponding to this Container instance.
     */
    EjbDescriptor getEjbDescriptor();

    /**
     * @return the MetaData for this EJB type.
     */
    EJBMetaData getEJBMetaData();

    /**
     * @return the classloader of this container instance.
     */
    ClassLoader getClassLoader();

    /**
     * @return the EJBHome object reference for this container instance.
     */
    EJBHome getEJBHome();

    /**
     * @return A SecurityManager object for this container. 
     */
    com.sun.enterprise.security.SecurityManager getSecurityManager();

    /**
     * EJB spec makes a distinction between access to the UserTransaction
     * object itself and access to its methods.  getUserTransaction covers
     * the first check and this method covers the second.  It is called
     * by the UserTransaction implementation to verify access.
     */
    boolean userTransactionMethodsAllowed(ComponentInvocation inv);

    /**
     * Called from the TM when an EJB with Bean-Managed transactions starts a tx
     */
    void doAfterBegin(ComponentInvocation ci);


    /**
     * Called after all the components in the container's application
     * have loaded successfully.  Allows containers to delay
     * any instance creation or external invocations until the second
     * phase of deployment.  Note that this callback occurs at a point
     * that is still considered within deployment.  Failures should still
     * still be treated as a deployment error.
     * @param deploy true if this method is called during application deploy
     */
    void startApplication(boolean deploy);

    /** 
     * Called from EJB JarManager when an application is undeployed.
     */
    void undeploy();

    /**  
     * Called when server instance is Ready
     */
    void onReady();

    /** 
     * Called when server instance is shuting down
     */
    void onShutdown();

    /** 
     * Called when server instance is terminating. This method is the last
     * one called during server shutdown.
     */
    void onTermination();

    /** 
     * Called from NamingManagerImpl during java:comp/env lookup.
     */
    String getComponentId();

    /**
     * Start servicing invocations for EJB instances in this Container.

     */
     void setStartedState();

    /**
     * Stop servicing invocations for EJB instances in this Container.
     * Subsequent EJB invocations will receive exceptions.
     * Invocations already in progress will be allowed to complete eventually.
     */
     void setStoppedState();

    /**
     * Stop servicing invocations for EJB instances in this Container as the 
     * container is being undeployed.
     * No new EJB invocations will be accepted from now on.
     * Invocations already in progress will be allowed to complete eventually.
     */
     void setUndeployedState();

    /**
     * Used by EjbInvocation during JACC EnterpriseBean policy handler request
     * for target EnterpriseBean instance.  
     *
     * @return EnterpriseBean instance or null if not applicable for this
     *         invocation.
     */
    Object getJaccEjb(EjbInvocation inv);

    /**
     * Go through ejb container to do ejb security manager authorization.
     */
    boolean authorize(EjbInvocation inv);

    /**
     * Returns true if this Container uses EJB Timer Service.
     */
    boolean isTimedObject();

    /**
     * Returns true if the bean associated with this Container has a LocalHome/Local view
     * OR a Local business view OR both.
     */
    boolean isLocalObject();

    /**
     * Returns true if the bean associated with this Container has a RemoteHome/Remote view
     * OR a Remote business view OR both.
     */
    boolean isRemoteObject();
}
