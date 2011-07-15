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

/*
 * ComponentValidator.java
 *
 * Created on August 15, 2002, 5:51 PM
 */

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author  dochez
 */
public class ComponentValidator extends DefaultDOLVisitor implements ComponentVisitor {
    
    /**
     * Visits a message destination referencer for the last J2EE 
     * component visited
     * @param the message destination referencer
     */
    public void accept(MessageDestinationReferencer msgDestReferencer) {

        // if it is linked to a logical destination
        if( msgDestReferencer.isLinkedToMessageDestination() ) {
            return;
        // if it is referred to a physical destination
        } else if (msgDestReferencer.ownedByMessageDestinationRef() && 
            msgDestReferencer.getMessageDestinationRefOwner(
            ).getJndiName() != null) {
            return;
        } else {
            MessageDestinationDescriptor msgDest = 
                msgDestReferencer.resolveLinkName();
            if( msgDest == null ) {
                String linkName = 
                    msgDestReferencer.getMessageDestinationLinkName();
                DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
                    new Object[] {"message-destination", linkName});
            } else {
                if (msgDestReferencer instanceof MessageDestinationReferenceDescriptor) {
                    ((MessageDestinationReferenceDescriptor)msgDestReferencer).setJndiName(msgDest.getJndiName());
                }
            }                                                 
        }
    }    

    /**
     * Visits a service reference for the last J2EE component visited
     * 
     * @param the service reference
     */
    public void accept(ServiceReferenceDescriptor serviceRef) {

        Set portsInfo = serviceRef.getPortsInfo();

        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();

            if( next.hasPortComponentLinkName() &&
                !next.isLinkedToPortComponent() ) {
                WebServiceEndpoint portComponentLink = next.resolveLinkName();
                if( portComponentLink == null ) {
                    String linkName = next.getPortComponentLinkName(); 
                    DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
                        new Object[] {"port-component" , linkName});
                }                                                   
            }

        }
    }    
    
}
