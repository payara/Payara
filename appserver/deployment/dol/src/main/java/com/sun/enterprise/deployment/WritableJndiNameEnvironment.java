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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.types.EjbReference;

/**
 * Objects implementing this interface allow their
 * environment properties, ejb references and resource
 * references to be written.
 * 
 * @author Danny Coward
 */

public interface WritableJndiNameEnvironment extends JndiNameEnvironment {

    /**  
     * Adds the specified environment property to the receiver.
     *   
     * @param environmentProperty the EnvironmentProperty to add.  
     *
     */
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty);
    
    /**  
     * Removes the specified environment property from receiver.
     *   
     * @param environmentProperty the EnvironmentProperty to remove.  
     *
     */
    public void removeEnvironmentProperty(
			EnvironmentProperty environmentProperty);
    
    /**  
     * Adds the specified ejb reference to the receiver.
     *   
     * @param ejbReference the EjbReferenceDescriptor to add.  
     *
     */
    public void addEjbReferenceDescriptor(EjbReference ejbReference);
    
    /**  
     * Removes the specificed ejb reference from the receiver.
     *   
     * @param ejbReference the EjbReferenceDescriptor to remove.  
     *
     */
    public void removeEjbReferenceDescriptor(
			EjbReference ejbReference);
    
    /**  
     * Adds the specified resource reference to the receiver.
     *   
     * @param resourceReference the ResourceReferenceDescriptor to add.  
     *
     */
    public void addResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference);
    
    /**  
     * Removes the specified resource reference from the receiver.
     *   
     * @param resourceReference the ResourceReferenceDescriptor to remove.  
     *
     */
    public void removeResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference);


    /**  
     * Adds the specified resource environment reference to the receiver.
     *   
     * @param the ResourceEnvReferenceDescriptor to add.  
     *
     */
    public void addResourceEnvReferenceDescriptor(
		ResourceEnvReferenceDescriptor resourceEnvReference);


    /**  
     * Removes the specified resource environment reference from the receiver.
     *   
     * @param the ResourceEnvReferenceDescriptor to remove.
     *
     */
    public void removeResourceEnvReferenceDescriptor(
		ResourceEnvReferenceDescriptor resourceEnvReference);

    /**  
     * Adds the specified message destination reference to the receiver.
     *   
     * @param the MessageDestinationReferenceDescriptor to add.  
     *
     */
    public void addMessageDestinationReferenceDescriptor
        (MessageDestinationReferenceDescriptor msgDestRef);

    /**  
     * Removes the specified message destination reference from the receiver.
     *   
     * @param ref MessageDestinationReferenceDescriptor to remove.
     *
     */
    public void removeMessageDestinationReferenceDescriptor
        (MessageDestinationReferenceDescriptor msgDestRef);
                                                         
    /** 
     * Adds the specified post-construct descriptor to the receiver.
     *  
     * @param the post-construct LifecycleCallbackDescriptor to add.
     *
     */
    public void addPostConstructDescriptor
        (LifecycleCallbackDescriptor postConstructDesc);

    /** 
     * Adds the specified pre-destroy descriptor to the receiver.
     *  
     * @param the pre-destroy LifecycleCallbackDescriptor to add.
     *
     */
    public void addPreDestroyDescriptor
        (LifecycleCallbackDescriptor preDestroyDesc);

    /**  
     * Adds the specified service reference to the receiver.
     *   
     * @param the ServiceReferenceDescriptor to add.  
     *
     */
    public void addServiceReferenceDescriptor(
	        ServiceReferenceDescriptor serviceReference);


    /**  
     * Removes the specified service reference from the receiver.
     *   
     * @param the ServiceReferenceDescriptor to remove.
     *
     */
    public void removeServiceReferenceDescriptor(
		ServiceReferenceDescriptor serviceReference);     

    public void addEntityManagerFactoryReferenceDescriptor(
                EntityManagerFactoryReferenceDescriptor reference);

    public void addEntityManagerReferenceDescriptor(
                EntityManagerReferenceDescriptor reference);

    /**  
     * Adds the specified descriptor to the receiver.
     *   
     * @param reference Descriptor to add.
     *
     */
    public void addResourceDescriptor(
            ResourceDescriptor reference);

    /**  
     * Removes the specified descriptor from the receiver.
     *   
     * @param reference Descriptor to remove.
     *
     */
    public void removeResourceDescriptor(
            ResourceDescriptor reference);
}

