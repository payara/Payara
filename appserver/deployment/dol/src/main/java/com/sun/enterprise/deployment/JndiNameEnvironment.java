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

import org.glassfish.deployment.common.JavaEEResourceType;
import java.util.List;
import java.util.Set;

    /**
    * Objects implementing this interface allow their
    * environment properties, ejb references and resource
    * references to be read.
    * 
    *@author Danny Coward
    */

public interface JndiNameEnvironment {

    /** 
     * Return a set of environment properties.
     *
     * @return java.util.Set of EnvironmentProperty objects
     */
    public Set getEnvironmentProperties();


    /** 
     * Return the env-entry with the given name
     *
     * @return EnvironmentProperty descriptor
     */
    public EnvironmentProperty getEnvironmentPropertyByName(String name);
	
    /** 
     * Return a set of ejb reference descriptors.
     *
     * @return java.util.Set of EjbReferenceDescriptor objects
     */
    public Set getEjbReferenceDescriptors();

/** 
     * Return a set of service reference descriptors.
     *
     * @return java.util.Set of ServiceReferenceDescriptor objects
     */
	
    public Set getServiceReferenceDescriptors();
    
     
    /** 
     * Return the Service reference descriptor corresponding to 
     * the given name.
     *
     * @return ServiceReferenceDescriptor object
     */
    public ServiceReferenceDescriptor getServiceReferenceByName(String name);
    
    /** 
     * Return a set of resource reference descriptors.
     *
     * @return java.util.Set of ResourceReferenceDescriptor objects
     */
	
    public Set getResourceReferenceDescriptors();
    
     
    /** 
     * Return a set of resource environment reference descriptors.
     *
     * @return java.util.Set of ResourceEnvReferenceDescriptor objects
     */
	
    public Set getResourceEnvReferenceDescriptors();


    /** 
     * Return the resource environment reference descriptor corresponding to 
     * the given name.
     *
     * @return ResourceEnvReferenceDescriptor object
     */
    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name);

    /** 
     * Return a set of message destination reference descriptors.
     *
     * @return java.util.Set of MessageDestinationReferenceDescriptor objects
     */
	
    public Set getMessageDestinationReferenceDescriptors();


    /** 
     * Return the message destination reference descriptor corresponding to 
     * the given name.
     *
     * @return MessageDestinationReferenceDescriptor object
     */
    public MessageDestinationReferenceDescriptor getMessageDestinationReferenceByName(String name);

    /**
     * Return a set of post-construct descriptors.
     *
     * @return java.util.Set of LifecycleCallbackDescriptor post-construct objects
     */
    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors();

    /**
     * Return the post-construct descriptor corresponding to
     * the given name.
     *
     * @return LifecycleCallbackDescriptor post-construct object
     */
    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className);

    /**
     * Return a set of pre-destroy descriptors.
     *
     * @return java.util.Set of LifecycleCallbackDescriptor pre-destroy objects
     */
    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors();

    /**
     * Return a set of descriptors based on the type.
     *
     * @return java.util.Set of descriptor objects
     */
    public Set<ResourceDescriptor> getResourceDescriptors(JavaEEResourceType type);

    /**
     * Return a set of descriptors based on the class value.
     *
     * @return java.util.Set of descriptor objects
     */
    public Set<ResourceDescriptor> getAllResourcesDescriptors(Class givenClass);

    /**
     * Return a set of descriptors.
     *
     * @return java.util.Set of descriptor objects
     */
    public Set<ResourceDescriptor> getAllResourcesDescriptors();

    /**
     * Return the pre-destroy descriptor corresponding to
     * the given name.
     *
     * @return LifecycleCallbackDescriptor pre-destroy object
     */
    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className);

    /**
     * Return a set of entity manager factory reference descriptors.
     */
    public Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors();

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name);


    /**
     * Return a set of entity manager reference descriptors.
     */
    public Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors();

    /**
     * Return the entity manager reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name);

    /**
     *
     */
    public List<InjectionCapable> getInjectableResourcesByClass(String className);
    public InjectionInfo getInjectionInfoByClass(Class clazz);
}
