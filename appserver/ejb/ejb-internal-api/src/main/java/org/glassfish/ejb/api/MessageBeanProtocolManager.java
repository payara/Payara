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

package org.glassfish.ejb.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.appserv.connectors.internal.api.ResourceHandle;

/**
 * MessageBeanProtocolManager is implemented by the MessageBeanContainer
 * and allows MessageBeanClients to create message bean listeners 
 * capable of receiving messages.  Each MessageBeanListener logically
 * represents a single message-driven bean instance, although there is
 * no guarantee as to exactly when the container creates that instance.
 * MessageBeanListeners are single-threaded.  Each MessageBeanListener is
 * held exclusively by a MessageBeanClient.
 *
 * @author Kenneth Saks
 */
public interface MessageBeanProtocolManager {

    /**
     * Create a MessageBeanListener.  
     *
     * @param resourceHandle handle associated with this listener.  can be null.
     * 
     * @throws Exception if the MessageBeanContainer was not able to create
     * the MessageBeanListener
     */
    MessageBeanListener createMessageBeanListener(ResourceHandle resourceHandle)
      throws ResourcesExceededException;

    /**
     * Return the MessageBeanListener to the container.  Since a
     * MessageBeanListener is typically associated with active resources
     * in the MessageBeanContainer, it is the responsibility of the 
     * MessageBeanClient to manage them judiciously.
     */ 
    void destroyMessageBeanListener(MessageBeanListener listener);

    Object createMessageBeanProxy(InvocationHandler handler) throws Exception;

    /**
     * This is used by the message provider to find out whether message 
     * deliveries will be transacted or not. The message delivery preferences 
     * must not change during the lifetime of a message endpoint. This 
     * information is only a hint and may be useful to perform optimizations 
     * on message delivery.
     *
     * @param method One of the methods used to deliver messages, e.g.
     *               onMessage method for javax.jms.MessageListener.
     *               Note that if the <code>method</code> is not one 
     *               of the methods for message delivery, the behavior 
     *               of this method is not defined.
     */
    boolean isDeliveryTransacted (Method method) ;


    /**
     * Returns the message-bean container's pool properties. 
     */
    BeanPoolDescriptor getPoolDescriptor();

}
