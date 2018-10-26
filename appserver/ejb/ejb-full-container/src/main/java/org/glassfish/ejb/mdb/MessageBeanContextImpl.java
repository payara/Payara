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

package org.glassfish.ejb.mdb;

import javax.ejb.*;
import javax.transaction.UserTransaction;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.EJBObjectImpl;
import com.sun.ejb.containers.EJBTimerService;
import org.glassfish.api.invocation.ComponentInvocation;

/**
 * Implementation of EJBContext for message-driven beans
 *
 * @author Kenneth Saks
 */

public final class MessageBeanContextImpl
        extends EJBContextImpl
        implements MessageDrivenContext
{

    private boolean afterSetContext = false;

    MessageBeanContextImpl(Object ejb, BaseContainer container)
    {
        super(ejb, container);
    }    

    void setEJBStub(EJBObject ejbStub)
    {
	throw new RuntimeException("No stubs for Message-driven beans");
    }

    void setEJBObjectImpl(EJBObjectImpl ejbo)
    {
	throw new RuntimeException("No EJB Object for Message-driven beans");
    }

    //FIXME later
    EJBObjectImpl getEJBObjectImpl() {
	    throw new RuntimeException("No EJB Object for Message-driven beans");
    }

    public void setContextCalled() {
        this.afterSetContext = true;
    }

    /*****************************************************************
     *    The following are implementations of EJBContext methods.
     ******************************************************************/

    /**
     * 
     */
    public UserTransaction getUserTransaction()
	throws java.lang.IllegalStateException
    {
	// The state check ensures that an exception is thrown if this
	// was called from the constructor or setMessageDrivenContext.
	// The remaining checks are performed by the container.
	if ( !this.afterSetContext ) {
	    throw new java.lang.IllegalStateException("Operation not allowed");
        }

	return ((BaseContainer)getContainer()).getUserTransaction();
    }

    /*
     * Doesn't make any sense to get EJBHome object for 
     * a message-driven ejb.
     */
    public EJBHome getEJBHome() 
    {
        RuntimeException exception = new java.lang.IllegalStateException
            ("getEJBHome not allowed for message-driven beans");
        throw exception;
    }


    protected void checkAccessToCallerSecurity()
        throws java.lang.IllegalStateException
    {
        // A message-driven ejb's state transitions past UNINITIALIZED
        // AFTER ejbCreate
        if ( !operationsAllowed() ) {
            throw new java.lang.IllegalStateException("Operation not allowed");
        }

    }

    public boolean isCallerInRole(String roleRef) {
        if ( roleRef == null )
            throw new IllegalStateException("Argument is null");

        checkAccessToCallerSecurity();

        ComponentInvocation inv =
                    EjbContainerUtilImpl.getInstance().getCurrentInvocation();
        if ( inv instanceof EjbInvocation) {
            EjbInvocation ejbInv = (EjbInvocation) inv;
            if( ejbInv.isTimerCallback ) {
                throw new IllegalStateException("isCallerInRole not allowed from timer callback");
            }

        } else {
            throw new IllegalStateException("not invoked from within a message-bean context");
        }

        com.sun.enterprise.security.SecurityManager sm = container.getSecurityManager();
	    return sm.isCallerInRole(roleRef);
    }
    
    public TimerService getTimerService() 
        throws java.lang.IllegalStateException {

        if( !afterSetContext ) {
            throw new java.lang.IllegalStateException("Operation not allowed");
        }

        return EJBTimerService.getEJBTimerServiceWrapper(this);
    }

    public void checkTimerServiceMethodAccess()
        throws java.lang.IllegalStateException {

        // A message-driven ejb's state transitions past UNINITIALIZED
        // AFTER ejbCreate
        if ( !operationsAllowed() ) {
            throw new java.lang.IllegalStateException
                ("EJB Timer Service method calls cannot be called in " +
                 " this context");
        } 
    }

    final boolean isInState(BeanState value) {
        return getState() == value;
    }

    void setState(BeanState s) {
        state = s;
    }

    void setInEjbRemove(boolean beingRemoved) {
        inEjbRemove = beingRemoved;
    }

    boolean operationsAllowed() {
        return !(isUnitialized() || inEjbRemove);
    }

    /**
     * Returns true if this context has NOT progressed past its initial
     * state.
     */
    private boolean isUnitialized() {
        return (state == EJBContextImpl.BeanState.CREATED);
    }

}
