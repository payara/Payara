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

import com.sun.ejb.Container;
import com.sun.ejb.ContainerFactory;
import com.sun.ejb.containers.builder.StatefulContainerBuilder;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbEntityDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.runtime.IASEjbExtraDescriptors;
import com.sun.enterprise.security.SecurityContext;
import com.sun.logging.LogDomains;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.ejb.config.EjbContainer;

@Service
public final class ContainerFactoryImpl implements ContainerFactory {

    @Inject
    private Habitat services;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private EjbContainer ejbContainerDesc;
    
    private static final Logger _logger = 
    	LogDomains.getLogger(ContainerFactoryImpl.class, LogDomains.EJB_LOGGER);
    
    public Container createContainer(EjbDescriptor ejbDescriptor,
				     ClassLoader loader, 
				     EJBSecurityManager sm,
				     DeploymentContext dynamicConfigContext)
	     throws Exception 
    {
        BaseContainer container = null;

        try {
            // instantiate container class
            if (ejbDescriptor instanceof EjbSessionDescriptor) {
                EjbSessionDescriptor sd = (EjbSessionDescriptor)ejbDescriptor;
                if ( sd.isStateless() ) {
                    container = new StatelessSessionContainer(ejbDescriptor, loader);
                } else if( sd.isStateful() ) {
                    StatefulContainerBuilder sfsbBuilder = services.byType(
                            StatefulContainerBuilder.class).get();
                    sfsbBuilder.buildContainer(dynamicConfigContext, ejbDescriptor, loader);
                    container = sfsbBuilder.getContainer();
                } else {

                    if (sd.hasContainerManagedConcurrency() ) {
                        container = new CMCSingletonContainer(ejbDescriptor, loader);
                    } else {
                        container = new BMCSingletonContainer(ejbDescriptor, loader);
                    }
                }
            } else if ( ejbDescriptor instanceof EjbMessageBeanDescriptor) {
                container = new MessageBeanContainer(ejbDescriptor, loader);
            } else {
                    if (((EjbEntityDescriptor)ejbDescriptor).getIASEjbExtraDescriptors()
                        .isIsReadOnlyBean()) { 

                        EjbEntityDescriptor robDesc = (EjbEntityDescriptor) ejbDescriptor;                    
                        container = new ReadOnlyBeanContainer (ejbDescriptor, loader);
                    } else {
                    	String commitOption = null;
                        IASEjbExtraDescriptors iased = ((EjbEntityDescriptor)ejbDescriptor).
                                getIASEjbExtraDescriptors();
                        if (iased != null) {
                            commitOption = iased.getCommitOption();    	
                        }
                        if (commitOption == null) {
                            commitOption = ejbContainerDesc.getCommitOption();  
                        }
                        if (commitOption.equals("A")) {
                            _logger.log(Level.WARNING, 
                                            "ejb.commit_option_A_not_supported",
                                            new Object []{ejbDescriptor.getName()}
                                            );
                            container = new EntityContainer(ejbDescriptor, loader);
                        } else if (commitOption.equals("C")) {
                            _logger.log(Level.FINE, "Using commit option C for: " 
                                        + ejbDescriptor.getName());
                            container = new CommitCEntityContainer(ejbDescriptor, loader);
                        } else {
                            _logger.log(Level.FINE,"Using commit option B for: " + 
                                            ejbDescriptor.getName());
                            container = new EntityContainer(ejbDescriptor, loader);
                        }
            	}
            }

       
            container.setSecurityManager(sm);
            
            container.initializeHome();

            return container;
        } catch ( Exception ex ) {
            throw ex;
        }
    }

} //ContainerFactoryImpl


class BeanContext {
    ClassLoader previousClassLoader;
    boolean classLoaderSwitched;
    SecurityContext
            previousSecurityContext;
}

class ArrayListStack
    extends ArrayList
{
    /**
     * Creates a stack with the given initial size
     */
    public ArrayListStack(int size) {
        super(size);
    }
    
    /**
     * Creates a stack with a default size
     */
    public ArrayListStack() {
        super();
    }

    /**
     * Pushes an item onto the top of this stack. This method will internally
     * add elements to the <tt>ArrayList</tt> if the stack is full.
     *
     * @param   obj   the object to be pushed onto this stack.
     * @see     java.util.ArrayList#add
     */
    public void push(Object obj) {
        super.add(obj);
    }

    /**
     * Removes the object at the top of this stack and returns that 
     * object as the value of this function. 
     *
     * @return     The object at the top of this stack (the last item 
     *             of the <tt>ArrayList</tt> object). Null if stack is empty.
     */
    public Object pop() {
        int sz = super.size();
        return (sz > 0) ? super.remove(sz-1) : null;
    }
    
    /**
     * Tests if this stack is empty.
     *
     * @return  <code>true</code> if and only if this stack contains 
     *          no items; <code>false</code> otherwise.
     */
    public boolean empty() {
        return super.size() == 0;
    }

    /**
     * Looks at the object at the top of this stack without removing it 
     * from the stack. 
     *
     * @return     the object at the top of this stack (the last item 
     *             of the <tt>ArrayList</tt> object).  Null if stack is empty.
     */
    public Object peek() {
        int sz = size();
        return (sz > 0) ? super.get(sz-1) : null;
    }



} //ArrayListStack

