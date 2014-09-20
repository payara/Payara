/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ejb.EJBObject;

import com.sun.ejb.Container;
import com.sun.ejb.EjbInvocation;

/**
 *
 * @author mvatkina
 */

public class EJBContainerStateManager {

    Container container;

    EJBContainerStateManager(Container c) {
        container = c;
    }

    public boolean isNullEJBObject(EJBContextImpl context) {
        return context.getEJBObjectImpl() == null;
    }

    public boolean isNullEJBLocalObject(EJBContextImpl context) {
        return context.getEJBLocalObjectImpl() == null;
    }

    public boolean isRemovedEJBObject(EjbInvocation inv) {
        return inv.ejbObject.isRemoved();
    }

    public boolean isRemovedEJBObject(EJBContextImpl context) {
        return !isNullEJBObject(context) && context.getEJBObjectImpl().isRemoved();
    }

    public boolean isRemovedEJBLocalObject(EJBContextImpl context) {
        return !isNullEJBLocalObject(context) && context.getEJBLocalObjectImpl().isRemoved();
    }

    /**
     * Associate EJB Object with this invocation and this Context
     * Note that some of the calls do not have Context assosiated with this
     * invocation, so Context object is passed in separately
     */
    public void attachObject(EjbInvocation inv, EJBContextImpl context, 
            EJBObjectImpl ejbObjImpl, EJBLocalObjectImpl localObjImpl) {
        if ( ejbObjImpl != null && container.isRemoteObject() && (!inv.isLocal) ) {
            // associate the context with the ejbObject
            context.setEJBObjectImpl(ejbObjImpl);
            context.setEJBStub((EJBObject)ejbObjImpl.getStub());
        }

        if ( localObjImpl != null && container.isLocalObject() ) {
            // associate the context with the ejbLocalObject
            context.setEJBLocalObjectImpl(localObjImpl);
        }

        if ( inv.isLocal && localObjImpl != null ) {
            inv.ejbObject = localObjImpl;
        } else if (ejbObjImpl != null) {
            inv.ejbObject = ejbObjImpl;
        }
    }

    /**
     * Mark EJB Object associated with this Context as removed or not
     */
    public void markObjectRemoved(EJBContextImpl context, boolean removed) {
        if ( !isNullEJBObject(context) ) {
            context.getEJBObjectImpl().setRemoved(removed);
        }

        if ( !isNullEJBLocalObject(context) ) {
            context.getEJBLocalObjectImpl().setRemoved(removed);
        }
    }

    /**
     * Disconnect context from EJB(Local)Object so that
     * context.getEJBObject() will throw exception.
     */
    public void disconnectContext(EJBContextImpl context) {
        if ( !isNullEJBObject(context) ) {
            // reset flag in case EJBObject is used again
            context.getEJBObjectImpl().setRemoved(false);
            context.setEJBObjectImpl(null);
            context.setEJBStub(null);
        }

        if ( !isNullEJBLocalObject(context) ) {
            // reset flag in case EJBLocalObject is used again
            context.getEJBLocalObjectImpl().setRemoved(false);
            context.setEJBLocalObjectImpl(null);
        }
    }
    
    /**
     * Clear EJB Object references in the context
     */
    public void clearContext(EJBContextImpl context) {
        context.setEJBLocalObjectImpl(null);
        context.setEJBObjectImpl(null);
        context.setEJBStub(null);
    }
    
}
