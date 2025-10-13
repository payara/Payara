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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package com.sun.enterprise.deployment;

import java.util.Set;

import com.sun.enterprise.deployment.types.MessageDestinationReferencer;

/**
 * Interface for Message-Driven Beans
 * <p>
 * Some methods are only in the implementation
 * @see org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor
 */
public interface EjbMessageBeanDescriptor extends EjbDescriptor, MessageDestinationReferencer {

    String TYPE = "Message-driven";

    /**
     * 
     * @return "{@linkplain jakarta.jms.MessageListener}" if not set
     */
    String getMessageListenerType();

    /**
     * Gets the type of the destination set
     * @return Either "{@linkplain jakarta.jms.Queue}" or "{@linkplain jakarta.jms.Topic}"
     */
    String getDestinationType();

    String getDurableSubscriptionName();

    /**
     * Gets the module id of the resource adapter set with the MDB
     * @return {@code null} if not set
     */
    String getResourceAdapterMid();

    /**
     * Returns the JNDI name of the connection factory used to create the
     * Message-Driven Bean.
     * @return
     */
    String getMdbConnectionFactoryJndiName();

    /**
     * Returns true if the destination type is of {@linkplain jakarta.jms.Queue}
     * @return
     * @see org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor#hasTopicDest()
     */
    public boolean hasQueueDest();

    /**
     * Sets the resource adapter to use with the MDB
     * @param resourceAdapterMid the module ID of the resource adapter to use
     */
    void setResourceAdapterMid(String resourceAdapterMid);

    /**
     * Returns a set of the activation config properties that have been set for the MDB
     * @return
     * @see jakarta.ejb.ActivationConfigProperty
     */
    Set<EnvironmentProperty> getActivationConfigProperties();

    /**
     * Gets the value of a specific {@link jakarta.ejb.ActivationConfigProperty}
     * @param name the name of the property
     * @return the actual value of the property
     */
    String getActivationConfigValue(String name);

    Set<EnvironmentProperty> getRuntimeActivationConfigProperties();

    void putRuntimeActivationConfigProperty(EnvironmentProperty prop);
}
