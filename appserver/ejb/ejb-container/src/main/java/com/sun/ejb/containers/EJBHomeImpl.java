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

import com.sun.ejb.*;
import com.sun.ejb.portable.*;

import java.lang.reflect.Method;

import javax.ejb.*;

import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;

import java.util.logging.*;

/**
 * This class implements the EJBHome interface.
 * This class is also the base class for all generated concrete EJBHome
 * implementations.
 * At deployment time, one instance of the EJBHome is created
 * for each EJB class in a JAR that has a remote home.
 *
 */

public abstract class EJBHomeImpl
    implements javax.ejb.EJBHome
{

    protected static final Logger _logger = EjbContainerUtilImpl.getLogger();
    
    private BaseContainer container;

    /**
     * This constructor is called from an EJBHome implementation's constructor.
     */
    protected EJBHomeImpl()
        throws RemoteException
    {
    }
    
    /**
     * Called from EJBHome implementation.
     */
    protected final Container getContainer() {
        return container;
    }
    
    
    /**
     * Called from BaseContainer only.
     */
    final void setContainer(BaseContainer c) {
        container = c;
    }

    /**
     * Get the EJBHome corresponding to an EJBHomeImpl.
     * These objects are one and the same when the home is generated,
     * but distinct in the case of dynamic proxies.  Therefore, code can't
     * assume it can cast an EJBHomeImpl to the EJBHome that
     * the client uses,  and vice-versa.  This is overridden in the 
     * InvocationHandler.
     */
    protected EJBHome getEJBHome() {
        return this;
    }
    
    /**
     * Create a new EJBObject and new EJB if necessary.
     * This is called from the generated "HelloEJBHomeImpl" create method.
     * Return the EJBObject for the bean.
     */
    public EJBObjectImpl createEJBObjectImpl()
        throws RemoteException, CreateException
    {
        return container.createEJBObjectImpl();
    }

    public EJBObjectImpl createRemoteBusinessObjectImpl() 
        throws RemoteException, CreateException
    {
        return container.createRemoteBusinessObjectImpl();
    }
    
    
    /***************************************
***********************************
    The following are implementations of javax.ejb.EJBHome methods.
     **************************************************************************/
    
    /**
     * This is the implementation of the javax.ejb.EJBHome remove method.
     * @exception RemoveException on error during removal
     */
    public final void remove(Handle handle)
        throws RemoteException, RemoveException
    {
        container.authorizeRemoteMethod(BaseContainer.EJBHome_remove_Handle);
        
        EJBObject ejbo;
        try {
            ejbo = handle.getEJBObject();
        } catch ( RemoteException ex ) {
            _logger.log(Level.FINE, "Exception in method remove()", ex);
            NoSuchObjectException nsoe = 
                new NoSuchObjectException(ex.toString());
            nsoe.initCause(ex);
            throw nsoe;
        }
        ejbo.remove();
    }
    
    
    /**
     * This is the implementation of the javax.ejb.EJBHome remove method.
     * @exception RemoveException on error during removal
     */
    public final void remove(Object primaryKey)
        throws RemoteException, RemoveException
    {
        if (container.getContainerType() != BaseContainer.ContainerType.ENTITY) {
            // Session beans dont have primary keys. EJB2.0 Section 6.6
            throw new RemoveException("Invalid remove operation.");
        }
        
        container.authorizeRemoteMethod(BaseContainer.EJBHome_remove_Pkey);
        
        Method method=null;
        try {
            method = EJBHome.class.getMethod("remove",
                        new Class[]{Object.class});
        } catch ( NoSuchMethodException e ) {
            _logger.log(Level.FINE, "Exception in method remove()", e);
        }
        
        container.doEJBHomeRemove(primaryKey, method, false);
    }
    
    
    /**
     * This is the implementation of the javax.ejb.EJBHome method.
     */
    public final EJBMetaData getEJBMetaData()
        throws RemoteException
    {
        container.authorizeRemoteMethod(BaseContainer.EJBHome_getEJBMetaData);
        
        return container.getEJBMetaData();
    }
    
    /**
     * This is the implementation of the javax.ejb.EJBHome getHomeHandle
     * method.
     */
    public final HomeHandle getHomeHandle()
        throws RemoteException
    {
        container.authorizeRemoteMethod(BaseContainer.EJBHome_getHomeHandle);
        
        return new HomeHandleImpl(container.getEJBHomeStub());
    }
}
