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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package org.glassfish.persistence.ejb.entitybean.container;

import java.lang.reflect.Method;
import javax.ejb.*;
import org.glassfish.api.invocation.ComponentInvocation;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBTimerService;
import org.glassfish.persistence.ejb.entitybean.container.spi.CascadeDeleteNotifier;

/**
 * Implementation of EJBContext for EntityBeans
 *
 */

public class EntityContextImpl
    extends EJBContextImpl
    implements EntityContext, CascadeDeleteNotifier
{
    private int lastTxStatus=-1;
    private boolean newlyActivated = false;
    private int nCallsInProgress = 0;
    private boolean dirty = false;
    private boolean inUnsetEntityContext = false;
    private boolean inEjbLoad = false;
    private boolean inEjbStore = false;
    
    private boolean cascadeDeleteBeforeEJBRemove = false;
    private boolean cascadeDeleteAfterSuperEJBRemove = false;

    //The following member variables are used to directly cache
    //	the EntityContext instead of enclosing it within a wrapper class.
    private Object		    _primaryKey;
    private int			    _pkHashCode;
    private EntityContextImpl	    _next;

    transient private EntityContainer _container = null;

    EntityContextImpl(Object ejb, BaseContainer container) {
        super(ejb, container);
        _container = (EntityContainer) container;
    }
    
    int getLastTransactionStatus() {
        return lastTxStatus;
    }
    
    void setLastTransactionStatus(int status) {
        lastTxStatus = status;
    }
    
    void setInUnsetEntityContext(boolean flag) {
        inUnsetEntityContext = flag;
    }
    
    void setInEjbLoad(boolean flag) {
        inEjbLoad = flag;
    }
    
    void setInEjbStore(boolean flag) {
        inEjbStore = flag;
    }
    
    boolean isDirty() {
        return dirty;
    }
    
    void setDirty(boolean b) {
        dirty = b;
    }
    
    // overrides EJBContextImpl.setState
    void setState(BeanState s) {
        state = s;
        if ( state == BeanState.POOLED || 
            state == BeanState.DESTROYED )
        {
            dirty = false;
        }
    }
    
    
    boolean isNewlyActivated() {
        return newlyActivated;
    }
    
    void setNewlyActivated(boolean b) {
        newlyActivated = b;
    }
    
    boolean hasReentrantCall() {
        return (nCallsInProgress > 1);
    }
    
    synchronized void decrementCalls() {
        nCallsInProgress--;
    }
    
    synchronized void incrementCalls() {
        nCallsInProgress++;
    }
    
    boolean hasIdentity() {
        return( (ejbObjectImpl != null) || (ejbLocalObjectImpl != null) );
    }
    
    /**
     * Implementation of EntityContext method.
     */
    public Object getPrimaryKey() throws IllegalStateException {
        if ( ejbObjectImpl == null && ejbLocalObjectImpl == null ) {
            // There is no ejbObjectImpl/localObject in ejbCreate, ejbFind,
            // setEntityCtx etc
            throw new IllegalStateException("Primary key not available");
        }
        
        return getKey();
    }
    
    /**
     * Implementation of EntityContext method, overrides EJBContextImpl method.
     */
    public EJBObject getEJBObject()
        throws IllegalStateException
    {
        if (! isRemoteInterfaceSupported) {
            throw new IllegalStateException("EJBObject not available");
        }

        if ( ejbStub == null ) {
            Object pkey = getPrimaryKey(); // throws IllegalStateException
            ejbStub = _container.getEJBObjectStub(pkey, null);
        }

        return ejbStub;
    }

    public TimerService getTimerService() throws IllegalStateException {
        if( state == BeanState.CREATED || inUnsetEntityContext || inFinder() ) {
            throw new IllegalStateException("Operation not allowed");
        }
     
        return EJBTimerService.getEJBTimerServiceWrapper(this);
    }
    
    protected void checkAccessToCallerSecurity()
        throws IllegalStateException
    {
        if( state == BeanState.CREATED || inUnsetEntityContext ) {
            throw new IllegalStateException("Operation not allowed");
        }
        checkActivatePassivate();
        
        if (inEjbLoad || inEjbStore) {
            // Security access is allowed from these two methods.  In the
            // case that they are invoked as part of an ejbTimeout call,
            // getCallerPrincipal will return null and isCallerInRole will
            // be false
            return;
        }
    }
    
    public void checkTimerServiceMethodAccess()
        throws IllegalStateException
    {
        
        // Prohibit access from constructor, setEntityContext, ejbCreate,
        // ejbActivate, ejbPassivate, unsetEntityContext, ejbFind
        if( (state == BeanState.CREATED) ||
        inUnsetEntityContext ||
        inFinder() ||
        inActivatePassivate() ||
        !hasIdentity() ) {
            throw new IllegalStateException("Operation not allowed");
        }
        
    }
    
    public final boolean isCascadeDeleteAfterSuperEJBRemove() {
        return cascadeDeleteAfterSuperEJBRemove;
    }

    public final void setCascadeDeleteAfterSuperEJBRemove(boolean value) {
        this.cascadeDeleteAfterSuperEJBRemove = value;
    }

    public final boolean isCascadeDeleteBeforeEJBRemove() {
        return cascadeDeleteBeforeEJBRemove;
    }

    public final void setCascadeDeleteBeforeEJBRemove(boolean value) {
        this.cascadeDeleteBeforeEJBRemove = value;
    }

    private boolean inFinder() {
        boolean inFinder = false;
        ComponentInvocation ci = _container.getCurrentInvocation();
        if ( ci instanceof EjbInvocation ) {
            EjbInvocation inv = (EjbInvocation) ci;
            Method currentMethod = inv.method;
            inFinder = ( (currentMethod != null) && inv.isHome &&
                         currentMethod.getName().startsWith("find") );
        }
        return inFinder;
    }

    //Called from EntityContainer after an ejb is obtained from the pool.
    final void cachePrimaryKey() {
	Object pk = getPrimaryKey();
	this._primaryKey = pk;
	this._pkHashCode = pk.hashCode();
    }

    final void clearCachedPrimaryKey() {
	this._primaryKey = null;
    }

    //Called from IncompleteTxCache to get an already cached context
    final boolean doesMatch(BaseContainer baseContainer, int pkHashCode, Object pk) {
	return (
	    (container == baseContainer)
	    && (_pkHashCode == pkHashCode)
	    && (_primaryKey.equals(pk))
	);
    }

    final void _setNext(EntityContextImpl val) {
	this._next = val;
    }

    final EntityContextImpl _getNext() {
	return _next;
    }

    final int _getPKHashCode() {
	return this._pkHashCode;
    }

    final boolean isInState(BeanState value) {
	return getState() == value;
    }

}
