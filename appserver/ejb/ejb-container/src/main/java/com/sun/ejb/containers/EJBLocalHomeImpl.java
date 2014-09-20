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

package com.sun.ejb.containers;

import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;

import java.lang.reflect.Method;


import javax.ejb.*;

import java.util.logging.*;

import java.io.IOException;

/**
 * Implementation of the EJBLocalHome interface.
 * This class is also the base class for all generated concrete EJBLocalHome
 * implementations.
 * At deployment time, one instance of the EJBLocalHome is created
 * for each EJB class in a JAR that has a local home.
 *
 * @author Mahesh Kannan
 */
public abstract class EJBLocalHomeImpl
    implements EJBLocalHome, IndirectlySerializable
{
    protected BaseContainer container;

    private static final Logger _logger = EjbContainerUtilImpl.getLogger();
    
    /**
     * Called from BaseContainer only.
     */
    final void setContainer(BaseContainer c) {
        container = c;
    }
    
    /**
     * Called from concrete EJBLocalHome implementation.
     */
    protected final BaseContainer getContainer() {
        return container;
    }

    /**
     * Get the EJBLocalHome corresponding to an EJBLocalHomeImpl.
     * These objects are one and the same when the local home is generated,
     * but distinct in the case of dynamic proxies.  Therefore, code can't
     * assume it can cast an EJBLocalHomeImpl to the EJBLocalHome that
     * the client uses,  and vice-versa.  This is overridden in the 
     * InvocationHandler.
     */
    protected EJBLocalHome getEJBLocalHome() {
        return this;
    }

    /**
     * Create a new EJBLocalObjectImpl and new EJB if necessary.
     * This is called from the concrete "HelloEJBHomeImpl" create method.
     * Return the EJBObjectImpl for the bean.
     */
    protected EJBLocalObjectImpl createEJBLocalObjectImpl()
        throws CreateException
    {
        return container.createEJBLocalObjectImpl();
    }

    /**
     * Create a new EJBLocalBusinessObjectImpl and new EJB if necessary.
     * @param intfName
     */
    protected final EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(String intfName)
        throws CreateException
    {
        // intfName is the Generated interface name in the case of the no-interface view
        return container.createEJBLocalBusinessObjectImpl(intfName);
    }
    
    /**
     * This is the implementation of the javax.ejb.EJBLocalHome remove method.
     */
    public final void remove(Object primaryKey)
        throws RemoveException, EJBException
    {
        if (container.getContainerType() != BaseContainer.ContainerType.ENTITY) {
            // Session beans dont have primary keys. EJB2.0 Section 6.6.
            throw new RemoveException("Attempt to call remove(Object primaryKey) on a session bean.");
        }
        
        container.authorizeLocalMethod(BaseContainer.EJBLocalHome_remove_Pkey);
        
        Method method=null;
        try {
            method = EJBLocalHome.class.getMethod("remove",
                                                  new Class[]{Object.class});
        } catch ( NoSuchMethodException e ) {
            _logger.log(Level.FINE, "Exception in method remove()", e);
        }
        
        try {
            container.doEJBHomeRemove(primaryKey, method, true);
        } catch(java.rmi.RemoteException re) {
            // This should never be thrown for local invocations, but it's
            // part of the removeBean signature.  If for some strange
            // reason it happens, convert to EJBException
            EJBException ejbEx =new EJBException("unexpected RemoteException");
            ejbEx.initCause(re);
            throw ejbEx;
        }
    }
    
    public SerializableObjectFactory getSerializableObjectFactory() {
        return new SerializableLocalHome(
            container.getEjbDescriptor().getUniqueId());
    }

    public static final class SerializableLocalHome
        implements SerializableObjectFactory
    {
        private long ejbId;

        public SerializableLocalHome(long uniqueId) {
            this.ejbId = uniqueId;
        }

        public Object createObject()
            throws IOException
        {
            // Return the LocalHome by getting the target container based
            // on the ejb id.  Note that we can assume this is the
            // LocalHome rather than a LocalBusinessHome since the 
            // LocalBusinessHome is never visible to the application and
            // would never be stored in SFSB state.
            BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(ejbId);
            return container.getEJBLocalHome();
        }
    }
}

