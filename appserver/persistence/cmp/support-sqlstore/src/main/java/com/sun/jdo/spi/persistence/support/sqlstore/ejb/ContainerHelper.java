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

/*
 * ContainerHelper.java
 *
 * Created on April 25, 2002
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import javax.ejb.*;

import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;

    /** Provide an implementation that supports CMP integration with information 
     * required for such support.  This is an interface that a helper class
     * implements that is specific to an application server.
     * <P><B>This interface is subject to change without notice.  In particular, 
     * as additional
     * experience is gained with specific application servers, this interface
     * may have methods added and removed, even with patch releases.
     * Therefore, this interface should be considered very volatile, and
     * any class that implements it might have to be reimplemented whenever
     * an upgrade to either the application server or internal code occurs.</B></P>
     * The class that implements this interface must register itself
     * by a static method at class initialization time.  For example,
     * <blockquote><pre>
     * import com.sun.jdo.spi.persistence.support.sqlstore.ejb.*;
     * class blackHerringContainerHelper implements ContainerHelper {
     *    static CMPHelper.register(new blackHerringContainerHelper());
     *    ...
     * }
     * </pre></blockquote>
     */
    public interface ContainerHelper {

    /** Called in a CMP-supported environment to get a Container helper instance that
     * will be passed unchanged to the required methods.  In a non-managed environment
     * should not be called and throws JDOFatalInternalException.
     * The info argument can be an array of Objects if necessary.
     *
     * @see getEJBObject(Object, Object)
     * @see getEJBLocalObject(Object, Object)
     * @see getEJBLocalObject(Object, Object, EJBContext)
     * @see removeByEJBLocalObject(EJBLocalObject, Object)
     * @see removeByPK(Object, Object)
     * @param info Object with the request information that is application server
     * specific.
     * @throws JDOFatalInternalException.
     * @return a Container instance as an Object.
     */
    Object getContainer(Object info);

    /** Called in a CMP-supported environment to get an EJBObject reference for this
     * primary key instance and Container instance. In a non-managed environment
     * is not called.
     * 
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @return a corresponding EJBObject instance (as an Object) to be used by 
     * the client.
     */   
    EJBObject getEJBObject(Object pk, Object container);

    /** Called in a CMP-supported environment to get an EJBLocalObject reference for this 
     * primary key instance and Container instance. In a non-managed environment 
     * is not called.
     * @see getContainer(Object)
     * @param pk the primary key instance. 
     * @param container a Container instance for the request. 
     * @return a corresponding EJBLocalObject (as an Object) instance to be used 
     * by the client. 
     */    
    EJBLocalObject getEJBLocalObject(Object pk, Object container);

    /** Called in a CMP-supported environment to get an EJBLocalObject reference for this
     * primary key instance, Container object, and EJBContext of the calling bean.
     * Allows the container to check if this method is called during ejbRemove
     * that is part of a cascade-delete remove.
     *
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @param context an EJBContext of the calling bean.
     * @return a corresponding EJBLocalObject (as an Object) to be used by
     * the client.
     */  
    EJBLocalObject getEJBLocalObject(Object pk, Object container,
        EJBContext context);

    /** Called in a CMP-supported environment to remove a bean for a given 
     * EJBLocalObject and Container instance.
     * 
     * @see getContainer(Object)
     * @param ejb the EJBLocalObject for the bean to be removed.
     * @param containerHelper a Container instance for the request. 
     */ 
    void removeByEJBLocalObject(EJBLocalObject ejb, Object containerHelper);

    /** Called in a CMP-supported environment to remove a bean for a given primary key
     * and Container instance.
     * 
     * @see getContainer(Object)
     * @param pk the primary key for the bean to be removed.
     * @param container a Container instance for the request. 
     */  
    void removeByPK(Object pk, Object container);

    /** Called in a CMP-supported environment to verify that the specified object
     * is of a valid local interface type.
     *
     * @see getContainer(Object)
     * @param o the instance to validate.
     * @param container a Container instance for the request.
     */
    void assertValidLocalObject(Object o, Object container);

    /** Called in a CMP-supported environment to verify that the specified object
     * is of a valid remote interface type.
     *
     * @see getContainer(Object)
     * @param o the instance to validate.
     * @param container a Container instance for the request.
     */
    void assertValidRemoteObject(Object o, Object container);

    /** Called in a CMP-supported environment to mark EntityContext as
     * already removed during cascade-delete operation.
     * Called by the generated ejbRemove method before calling ejbRemove of the 
     * related beans (via removeByEJBLocalObject) that are to be cascade-deleted.
     *
     *   
     * @param context the EntityContext of the bean beeing removed.
     */  
    void setCascadeDeleteAfterSuperEJBRemove(EntityContext context);

    /** Called in a CMP supported environment. Notifies the container that 
     * ejbSelect had been called.
     *
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */
    void preSelect(Object container);

    /** Called in a CMP environment to lookup PersistenceManagerFactory
     * referenced by this Container instance as the CMP resource.
     *   
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */  
    PersistenceManagerFactory getPersistenceManagerFactory(Object container);

    /**
     * Called in CMP environment to get NumericConverter policy referenced
     * by this Container instance.
     * @see getContainer(Object)
     * @param container a Container instance for the request
     * @return a valid NumericConverter policy type
     */
    int getNumericConverterPolicy(Object container);

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    void beginInternalTransaction(PersistenceManager pm);

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    void commitInternalTransaction(PersistenceManager pm);

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    void rollbackInternalTransaction(PersistenceManager pm);

    /** Called from read-only beans to suspend current transaction.
     * This will guarantee that PersistenceManager is not bound to
     * any transaction.
     *
     * @return javax.transaction.Transaction object representing 
     * the suspended transaction.
     * Returns null if the calling thread is not associated
     * with a transaction.
     */
    javax.transaction.Transaction suspendCurrentTransaction();

    /** Called from read-only beans to resume current transaction.
     * This will guarantee that the transaction continues to run after
     * read-only bean accessed its PersistenceManager.
     *
     * @param tx - The javax.transaction.Transaction object that 
     * represents the transaction to be resumed.
     */
    void resumeCurrentTransaction(javax.transaction.Transaction tx);
}
