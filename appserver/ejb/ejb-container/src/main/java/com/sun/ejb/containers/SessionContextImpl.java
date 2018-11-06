/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import com.sun.ejb.EjbInvocation;
import com.sun.ejb.spi.container.StatefulEJBContext;
import com.sun.enterprise.container.common.impl.PhysicalEntityManagerWrapper;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import org.glassfish.api.invocation.ComponentInvocation;

import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.persistence.EntityManagerFactory;
import java.util.*;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.sun.ejb.containers.StatefulSessionContainer.EEMRefInfo;

/**
 * Implementation of EJBContext for SessionBeans
 *
 * @author Mahesh Kannan
 */

public final class SessionContextImpl
    extends AbstractSessionContextImpl
    implements StatefulEJBContext
{

    private boolean completedTxStatus;
    private boolean afterCompletionDelayed=false;
    private boolean committing=false;
    private boolean inAfterCompletion=false;
    private boolean isStateless = false;
    private boolean isStateful  = false;

    private boolean existsInSessionStore = false; 
    private transient int refCount = 0;

    private boolean txCheckpointDelayed;
    private long    lastPersistedAt;

    private long version;
    
    // Do not call Session Synchronization callbacks when in transactional
    // lifecycle callbacks
    private boolean inLifeCycleCallback = false;

    // Map of entity managers with extended persistence context 
    // for this stateful session bean.
    private transient Map<EntityManagerFactory, PhysicalEntityManagerWrapper> extendedEntityManagerMap;

    private transient Set<EntityManagerFactory> emfsRegisteredWithTx;

    //Used during activation to populate entries in the above maps
    //Also, EEMRefInfo implements IndirectlySerializable
    private Collection<EEMRefInfo> eemRefInfos = new HashSet<EEMRefInfo>();

    // Used to provide serialized access to an SFSB instance.
    private transient ReentrantReadWriteLock statefulSerializedAccessLock;
    
    SessionContextImpl(Object ejb, BaseContainer container) {
        super(ejb, container);
        EjbSessionDescriptor sessionDesc =
            (EjbSessionDescriptor) getContainer().getEjbDescriptor();
        isStateless = sessionDesc.isStateless();
        isStateful  = sessionDesc.isStateful();
        if( isStateful ) {
            initializeStatefulWriteLock();   
        }
    }

    public Map<EntityManagerFactory, PhysicalEntityManagerWrapper> getExtendedEntityManagerMap() {
        if( extendedEntityManagerMap == null ) {
            extendedEntityManagerMap = new HashMap<>();
        }
        return extendedEntityManagerMap;
    }


    Collection<EEMRefInfo> getAllEEMRefInfos() {
        return eemRefInfos;
    }

    void setEEMRefInfos(Collection<EEMRefInfo> val) {
        if (val != null) {
            eemRefInfos = val;
	}
    }

    public void addExtendedEntityManagerMapping(EntityManagerFactory emf,
		    EEMRefInfo refInfo) {
        getExtendedEntityManagerMap().put(emf, new PhysicalEntityManagerWrapper(refInfo.getEntityManager(),
                refInfo.getSynchronizationType()) );
    }


    public PhysicalEntityManagerWrapper getExtendedEntityManager(EntityManagerFactory emf) {
        return getExtendedEntityManagerMap().get(emf);
    }

    public Collection<PhysicalEntityManagerWrapper> getExtendedEntityManagers() {
        return getExtendedEntityManagerMap().values();
    }

    private Set<EntityManagerFactory> getEmfsRegisteredWithTx() {
        if( emfsRegisteredWithTx == null ) {
            emfsRegisteredWithTx = new HashSet<EntityManagerFactory>();
        }
        return emfsRegisteredWithTx;
    }

    public void setEmfRegisteredWithTx(EntityManagerFactory emf, boolean flag)
    {
        if( flag ) {
            getEmfsRegisteredWithTx().add(emf);
        } else {
            getEmfsRegisteredWithTx().remove(emf);
        }
    }

    public boolean isEmfRegisteredWithTx(EntityManagerFactory emf) {
        return getEmfsRegisteredWithTx().contains(emf);
    }


    public void initializeStatefulWriteLock() {
        statefulSerializedAccessLock = new ReentrantReadWriteLock(true);
    }

    public ReentrantReadWriteLock.WriteLock getStatefulWriteLock() {
        return statefulSerializedAccessLock.writeLock();
    }

    public void setStatefulWriteLock(SessionContextImpl other) {
        statefulSerializedAccessLock = other.statefulSerializedAccessLock;
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        if( isStateful ) {
            throw new IllegalStateException
                ("EJBTimer Service is not accessible to Stateful Session ejbs");
        }

        // Instance key is first set between after setSessionContext and
        // before ejbCreate
        if ( instanceKey == null ) {
            throw new IllegalStateException("Operation not allowed");
        }

        return EJBTimerService.getEJBTimerServiceWrapper(this);
    }

    @Override
    protected void checkAccessToCallerSecurity()
        throws IllegalStateException
    {
        
        if( isStateless ) {
            // This covers constructor, setSessionContext, ejbCreate,
            // and ejbRemove. NOTE : For stateless session beans,
            // instances don't move past CREATED until after ejbCreate.
            if( (state == BeanState.CREATED) || inEjbRemove ) {
                throw new IllegalStateException("Operation not allowed");
            }
        } else {
            // This covers constructor and setSessionContext.
            // For stateful session beans, instances move past
            // CREATED after setSessionContext.
            if( state == BeanState.CREATED ) {
                throw new IllegalStateException("Operation not allowed");
            }
        }
        
    }
    
    @Override
    public void checkTimerServiceMethodAccess()
        throws IllegalStateException
    {
        // checks that only apply to stateful session beans
        ComponentInvocation compInv = getCurrentComponentInvocation();
        if (isStateful) {
            if (
            inStatefulSessionEjbCreate(compInv) ||
            inActivatePassivate(compInv) ||
            inAfterCompletion ) {
                throw new IllegalStateException
                ("EJB Timer methods for stateful session beans cannot be " +
                " called in this context");
            }
        }
        
        // checks that apply to both stateful AND stateless
        if ( (state == BeanState.CREATED) || inEjbRemove ) {
            throw new IllegalStateException
            ("EJB Timer method calls cannot be called in this context");
        }
    }
    
    boolean getCompletedTxStatus() {
        return completedTxStatus;
    }
    
    void setCompletedTxStatus(boolean s) {
        this.completedTxStatus = s;
    }
    
    boolean isAfterCompletionDelayed() {
        return afterCompletionDelayed;
    }
    
    void setAfterCompletionDelayed(boolean s) {
        this.afterCompletionDelayed = s;
    }
    
    boolean isTxCompleting() {
        return committing;
    }
    
    void setTxCompleting(boolean s) {
        this.committing = s;
    }
    
    void setInAfterCompletion(boolean flag) {
        inAfterCompletion = flag;
    }
    
    void setInLifeCycleCallback(boolean s) {
        inLifeCycleCallback = s;
    }
    
    boolean getInLifeCycleCallback() {
        return inLifeCycleCallback;
    }
    
    // Used to check if stateful session bean is in ejbCreate.
    // Since bean goes to READY state before ejbCreate is called by
    // EJBHomeImpl and EJBLocalHomeImpl, we can't rely on getState()
    // being CREATED for operations matrix checks.
    private boolean inStatefulSessionEjbCreate(ComponentInvocation inv) {
        boolean inEjbCreate = false;
        if ( inv instanceof EjbInvocation ) {
            Class clientIntf = ((EjbInvocation)inv).clientInterface;
            // If call came through a home/local-home, this can only be a
            // create call.
            inEjbCreate = ((EjbInvocation)inv).isHome &&
                (javax.ejb.EJBHome.class.isAssignableFrom(clientIntf) ||
                 javax.ejb.EJBLocalHome.class.isAssignableFrom(clientIntf));
        }
        return inEjbCreate;
    }
    
    void setTxCheckpointDelayed(boolean val) {
	this.txCheckpointDelayed = val;
    }

    boolean isTxCheckpointDelayed() {
	return this.txCheckpointDelayed;
    }

    long getLastPersistedAt() {
	return lastPersistedAt;
    }

    void setLastPersistedAt(long val) {
	this.lastPersistedAt = val;
    }

    public long getVersion() {
        return version;
    }
    
    public long incrementAndGetVersion() {
        return ++version;
    }

    public void setVersion(long newVersion) {
        this.version = newVersion;
    }

    /*************************************************************************/
    /************ Implementation of StatefulEJBContext ***********************/
    /*************************************************************************/

    public long getLastAccessTime() {
        return getLastTimeUsed();
    }

    public boolean canBePassivated() {
        return (state == EJBContextImpl.BeanState.READY);
    }
    
    public boolean hasExtendedPC() {
        return (this.getExtendedEntityManagerMap().size() != 0);
    }

    public SessionContext getSessionContext() {
        return this;
    }

    public boolean existsInStore() {
        return existsInSessionStore ;
    }

    public void setExistsInStore(boolean val) {
        this.existsInSessionStore = val;
    }

    public final void incrementRefCount() {
        refCount++;
    }

    public final void decrementRefCount() {
        refCount--;
    }

    public final int getRefCount() {
        return refCount;
    }
    
}
