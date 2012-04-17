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

package com.sun.enterprise.deployment.util;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.*;

import java.util.Iterator;

/**
 * Default implementation of DescriptorVisitor interface for convenience
 *
 * @author  Jerome Dochez
 * @version 
 */

public class DefaultDOLVisitor implements DescriptorVisitor {

    /**
     * visits a J2EE descriptor
     * @param the descriptor
     */
    public void accept(Descriptor descriptor) {
    }

    /**
     * get the visitor for its sub descriptor
     * @param sub descriptor to return visitor for
     */
    public DescriptorVisitor getSubDescriptorVisitor(Descriptor subDescriptor) {
        return this;
    }

    protected void accept(BundleDescriptor bundleDescriptor) {
        if (bundleDescriptor instanceof JndiNameEnvironment) {
            JndiNameEnvironment nameEnvironment = (JndiNameEnvironment)bundleDescriptor;
            for (Iterator<EjbReference> itr = nameEnvironment.getEjbReferenceDescriptors().iterator();itr.hasNext();) {
                accept(itr.next());
            }

            for (Iterator<ResourceReferenceDescriptor> itr = nameEnvironment.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
                accept(itr.next());
            }

            for (Iterator<ResourceEnvReferenceDescriptor> itr= nameEnvironment.getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
                accept(itr.next());
            }

            for (Iterator<MessageDestinationReferencer> itr = nameEnvironment.getMessageDestinationReferenceDescriptors().iterator();itr.hasNext();) {
                accept(itr.next());
            }

            for (Iterator<MessageDestinationDescriptor> itr = bundleDescriptor.getMessageDestinations().iterator(); itr.hasNext();) {
                accept(itr.next());
            }

            for (Iterator<ServiceReferenceDescriptor> itr = nameEnvironment.getServiceReferenceDescriptors().iterator();itr.hasNext();) {
                accept(itr.next());
            }
        }
    }

    /**
     * visits an ejb reference for the last J2EE component visited
     * @param the ejb reference
     */
    protected void accept(EjbReference ejbRef) {
    }

    /**
     * visits a web service reference descriptor
     * @param serviceRef
     */
    protected void accept(ServiceReferenceDescriptor serviceRef) {
    }


    /**
     * visits an resource reference for the last J2EE component visited
     * @param the resource reference
     */
    protected void accept(ResourceReferenceDescriptor resRef) {
    }

    /**
     * visits an resource environment reference for the last J2EE component visited
     * @param the resource environment reference
     */
    protected void accept(ResourceEnvReferenceDescriptor resourceEnvRef) {
    }

    protected void accept(MessageDestinationReferencer msgDestReferencer) {
    }

    /**
     * visits an message destination for the last J2EE component visited
     * @param the message destination
     */
    protected void accept(MessageDestinationDescriptor msgDest) {
    }
}
