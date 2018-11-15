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

package com.sun.ejb.containers;

import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;
import com.sun.ejb.EJBUtils;

/**
 * Implementation of EJBContext for SessionBeans
 *
 * @author Mahesh Kannan
 */

public abstract class AbstractSessionContextImpl
        extends EJBContextImpl
        implements SessionContext {
    
    protected Object instanceKey;

    protected String ejbName;


    protected AbstractSessionContextImpl(Object ejb, BaseContainer container) {
        super(ejb, container);
        EjbSessionDescriptor sessionDesc =
                (EjbSessionDescriptor) getContainer().getEjbDescriptor();

        this.ejbName = sessionDesc.getName();
    }

    public Object getInstanceKey() {
        return instanceKey;
    }

    public void setInstanceKey(Object instanceKey) {
        this.instanceKey = instanceKey;
    }

    @Override
    public String toString() {
        return ejbName + "; id: " + instanceKey;
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {

        // Instance key is first set between after setSessionContext and
        // before ejbCreate
        if (instanceKey == null) {
            throw new IllegalStateException("Operation not allowed");
        }

        return EJBTimerService.getEJBTimerServiceWrapper(this);
    }

    @Override
    public UserTransaction getUserTransaction()
            throws IllegalStateException {
        // The state check ensures that an exception is thrown if this
        // was called from setSession/EntityContext. The instance key check
        // ensures that an exception is not thrown if this was called
        // from a stateless SessionBean's ejbCreate.
        if ((state == BeanState.CREATED) && (instanceKey == null))
            throw new IllegalStateException("Operation not allowed");

        return ((BaseContainer) getContainer()).getUserTransaction();
    }

    @Override
    public MessageContext getMessageContext() {
        InvocationManager invManager = EjbContainerUtilImpl.getInstance().getInvocationManager();
        try {
            ComponentInvocation inv = invManager.getCurrentInvocation();

            if ((inv != null) && isWebServiceInvocation(inv)) {
                return ((EjbInvocation) inv).messageContext;
            } else {
                throw new IllegalStateException("Attempt to access " +
                        "MessageContext outside of a web service invocation");
            }
        } catch (Exception e) {
            IllegalStateException ise = new IllegalStateException();
            ise.initCause(e);
            throw ise;
        }
    }

    @Override
    public <T> T getBusinessObject(Class<T> businessInterface)
            throws IllegalStateException {

        // businessInterface param can also be a class in the case of the
        // no-interface view

        // getBusinessObject not allowed for Stateless/Stateful beans
        // until after dependency injection
        if (instanceKey == null) {
            throw new IllegalStateException("Operation not allowed");
        }

        T businessObject = null;

        EjbDescriptor ejbDesc = container.getEjbDescriptor();

        if (businessInterface != null) {
            String intfName = businessInterface.getName();

            if ((ejbLocalBusinessObjectImpl != null) &&
                    ejbDesc.getLocalBusinessClassNames().contains(intfName)) {

                // Get proxy corresponding to this business interface.
                businessObject = (T) ejbLocalBusinessObjectImpl
                        .getClientObject(intfName);

            } else if ((ejbRemoteBusinessObjectImpl != null) &&
                    ejbDesc.getRemoteBusinessClassNames().contains(intfName)) {

                // Create a new client object from the stub for this
                // business interface.
                String generatedIntf = EJBUtils.getGeneratedRemoteIntfName(intfName);

                java.rmi.Remote stub =
                    ejbRemoteBusinessObjectImpl.getStub(generatedIntf);

                try {
                    businessObject = (T) EJBUtils.createRemoteBusinessObject
                        (container.getClassLoader(), intfName, stub);
                } catch(Exception e) {

                    IllegalStateException ise = new IllegalStateException
                        ("Error creating remote business object for " +
                         intfName);
                    ise.initCause(e);
                    throw ise;
                }

            } else if( ejbDesc.isLocalBean() && intfName.equals( ejbDesc.getEjbClassName() ) ) {

                businessObject = (T) optionalEjbLocalBusinessObjectImpl.
                        getClientObject(ejbDesc.getEjbClassName());

            }
        }

        if (businessObject == null) {
            throw new IllegalStateException("Invalid business interface : " +
                    businessInterface + " for ejb " + ejbDesc.getName());
        }

        return businessObject;
    }

    @Override
    public Class getInvokedBusinessInterface()
            throws IllegalStateException {

        Class businessInterface = null;

        try {
            ComponentInvocation inv = EjbContainerUtilImpl.getInstance().getCurrentInvocation();

            if ((inv != null) && (inv instanceof EjbInvocation)) {
                EjbInvocation invocation = (EjbInvocation) inv;
                if (invocation.isBusinessInterface) {
                    businessInterface = invocation.clientInterface;
                    if( container.isLocalBeanClass(invocation.clientInterface.getName()) ) {
                        businessInterface = container.getEJBClass();
                    }

                }
            }
        } catch (Exception e) {
            IllegalStateException ise = new IllegalStateException();
            ise.initCause(e);
            throw ise;
        }

        if (businessInterface == null) {
            throw new IllegalStateException("Attempt to call " +
                    "getInvokedBusinessInterface outside the scope of a business " +
                    "method");
        }

        return businessInterface;
    }

    @Override
    public boolean wasCancelCalled() {

        try {
            ComponentInvocation inv = EjbContainerUtilImpl.getInstance().getCurrentInvocation();

            if ((inv != null) && (inv instanceof EjbInvocation)) {
                EjbInvocation invocation = (EjbInvocation) inv;
                EjbFutureTask task = invocation.getEjbFutureTask();
                if (task == null) {
                    throw new IllegalStateException("Must be invoked from an async method");
                }
                if( (invocation.method.getReturnType() == Void.TYPE) ) {
                    throw new IllegalStateException("Must be invoked from a method with a Future<V> " +
                                                    "return type");
                }
                return invocation.getWasCancelCalled();
            }
        } catch (Exception e) {
            IllegalStateException ise = new IllegalStateException(e.getMessage());
            ise.initCause(e);
            throw ise;
        }

        throw new IllegalStateException("Attempt to invoke wasCancelCalled from " +
                                        "outside an ejb invocation");
    }

    @Override
    protected void checkAccessToCallerSecurity()
            throws IllegalStateException {
        if (state == BeanState.CREATED) {
            throw new IllegalStateException("Operation not allowed");
        }

    }

    @Override
    public void checkTimerServiceMethodAccess()
            throws IllegalStateException {
        // checks that apply to both stateful AND stateless
        if ((state == BeanState.CREATED) || inEjbRemove) {
            throw new IllegalStateException
                    ("EJB Timer method calls cannot be called in this context");
        }
    }

    protected ComponentInvocation getCurrentComponentInvocation() {
        BaseContainer container = (BaseContainer) getContainer();
        return container.invocationManager.getCurrentInvocation();
    }

    private boolean isWebServiceInvocation(ComponentInvocation inv) {
        return (inv instanceof EjbInvocation) && ((EjbInvocation) inv).isWebService;
    }

}
