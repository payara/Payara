/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.types;

import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;

/** 
 * 
 *
 * @author Kenneth Saks
 */

public interface MessageDestinationReferencer {

    /**
     * @return true if this referencer is linked to a message destination
     * and false otherwise.
     */
    public boolean isLinkedToMessageDestination();

    /** 
     * Gets the link name of the reference. Points to the associated
     * message destination within the J2EE application. Can be NULL
     * if link is not set.
     * @return the link name.
     */
    public String getMessageDestinationLinkName();

    /** 
     * Sets the link name of the reference. Points to the associated
     * message destination within the J2EE application.  Can be NULL
     * if link is not set.
     * @param the link name.
     */
    public void setMessageDestinationLinkName(String linkName);
    
    /** 
     * Sets the name of the message destination to which I refer.
     * @param resolve if true,  *try* to resolve link to the target message
     * destination.  
     *
     * @return MessageDestination to which link was resolved, or null if 
     * link name resolution failed.
     */
    public MessageDestinationDescriptor setMessageDestinationLinkName
        (String linkName, boolean resolve);

    /** 
     * Try to resolve the current link name value to a MessageDestination
     * object.
     *
     * @return MessageDestination to which link was resolved, or null if 
     * link name resolution failed.
     */
    public MessageDestinationDescriptor resolveLinkName();

    /**
     * @return the message destination object to which this message destination
     * ref is linked.  Can be NULL.
     */
    public MessageDestinationDescriptor getMessageDestination();

    /**
     * @param destination set the message destination object to which this 
     * message destination ref is linked.  Can be NULL.
     * 
     */
    public void setMessageDestination(MessageDestinationDescriptor destination);

    /**
     * True if the owner is a message destination reference.
     */ 
    public boolean ownedByMessageDestinationRef();

    /**
     * Get the descriptor for the message destination reference owner.
     */ 
    public MessageDestinationReferenceDescriptor getMessageDestinationRefOwner
        ();

    /**
     * True if the owner is a message-driven bean.
     */ 
    public boolean ownedByMessageBean();

    /**
     * Get the descriptor for the message-driven bean owner.
     */ 
    public EjbMessageBeanDescriptor getMessageBeanOwner();

}

