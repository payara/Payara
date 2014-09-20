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


import java.util.logging.Logger;

import javax.ejb.*;

import com.sun.ejb.*;

/**
 * Implementation common to EJBObjects and EJBLocalObjects.
 * It is extended by EJBObjectImpl and EJBLocalObjectImpl.
 *
 */
public abstract class EJBLocalRemoteObject
{
    protected static final boolean debug = false;


    transient protected static final Logger _logger = EjbContainerUtilImpl.getLogger();
    
    transient protected BaseContainer container;
    transient protected Object primaryKey;
    transient private boolean removed=false;

    // Only used for stateful SessionBeans
    transient private SessionContextImpl context;
    
    //Used only for SFSBs. 
    private long sfsbClientVersion;
    

    final void setContainer(Container container)
    {
        this.container = (BaseContainer)container;
    }

    /**
     * Container needs to be accessed from generated code as well 
     * as from other classes in this package.  Rather than having one
     * public method, we have a protected one that is used from generated
     * code and a package-private one used within other container classes.
     * 
     */

    protected final Container getContainer()
    {
        return container;
    }

    final Container _getContainerInternal()
    {
        return container;
    }
    
    final void setRemoved(boolean r)
    {
        removed = r;
    }

    final boolean isRemoved()
    {
        return removed;
    }

    final void setKey(Object key)
    {
        primaryKey = key;
    }

    final Object getKey()
    {
        return primaryKey;
    }

    // Only used for stateful SessionBeans
    final SessionContextImpl getContext()
    {
        return context;
    }

    // Only used for stateful SessionBeans
    final void setContext(SessionContextImpl ctx)
    {
        context = ctx;
    }

    // Only used for stateful SessionBeans
    final void clearContext()
    {
        context = null;
    }
    
    //This is called when a local ref is serialized
    public long getSfsbClientVersion() {
        return this.sfsbClientVersion;
    }

    //This is called when the assocaited SFSB context is 
    //  checkpointed / passivated
    public void setSfsbClientVersion(long sfsbClientVersion) {
        this.sfsbClientVersion = sfsbClientVersion;
    }

}
