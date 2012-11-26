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

package com.sun.enterprise.deployment.annotation.context;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.EjbReference;
import org.glassfish.deployment.common.JavaEEResourceType;

import java.util.Set;

/**
 * This interface provides an abstraction for handle resource references.
 *
 * @Author Shing Wai Chan
 */
public interface ResourceContainerContext extends ServiceReferenceContainerContext {
    /**
     * Add a ejb reference.
     *
     * @param the ejb reference
     */
    public void addEjbReferenceDescriptor(EjbReference ejbReference);
                                                                               
    /**
     * Looks up an ejb reference with the given name.
     * Return null if it is not found.
     *
     * @param the name of the ejb-reference
     */
    public EjbReference getEjbReference(String name);

    /**
     * Add a resource reference
     *
     * @param the resource reference
     */
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor 
                                               resReference);
                                                                               
    /**
     * Looks up an resource reference with the given name.
     * Return null if it is not found.
     *
     * @param the name of the resource-reference
     */
    public ResourceReferenceDescriptor getResourceReference(String name);

    /**
     * Add a message-destination-ref
     *
     * @param the msgDestRef
     */
    public void addMessageDestinationReferenceDescriptor
        (MessageDestinationReferenceDescriptor msgDestRef);
                                               
                                                                               
    /**
     * Looks up a message-destination-ref with the given name.
     * Return null if it is not found.
     *
     * @param the name of the message-destination-ref
     */
    public MessageDestinationReferenceDescriptor getMessageDestinationReference
        (String name);


   /**
     * Add a resource-env-ref
     *
     * @param the resourceEnvRef
     */
    public void addResourceEnvReferenceDescriptor
        (ResourceEnvReferenceDescriptor resourceEnvRef);


    /**
     * Looks up a resource-env-ref with the given name.
     * Return null if it is not found.
     *
     * @param the name of the resource-env-ref
     */
    public ResourceEnvReferenceDescriptor getResourceEnvReference
        (String name);

    /**
     * Add an env-entry
     *
     * @param the env-entry
     */
    public void addEnvEntryDescriptor(EnvironmentProperty envEntry);
                                               
                                                                               
    /**
     * Looks up an env-entry with the given name.
     * Return null if it is not found.
     *
     * @param the name of the env-entry
     */
    public EnvironmentProperty getEnvEntry(String name);


    public void addEntityManagerFactoryReferenceDescriptor
        (EntityManagerFactoryReferenceDescriptor emfRefDesc);
                                                                               
    /**
     * Looks up an entity manager factory reference with the given name.
     * Return null if it is not found.
     *
     * @param the name of the emf reference
     */
    public EntityManagerFactoryReferenceDescriptor 
        getEntityManagerFactoryReference(String name);

    public void addEntityManagerReferenceDescriptor
        (EntityManagerReferenceDescriptor emRefDesc);
                                                                               
    /**
     * Looks up an entity manager reference with the given name.
     * Return null if it is not found.
     *
     * @param the name of the emf reference
     */
    public EntityManagerReferenceDescriptor getEntityManagerReference
        (String name);

   /**
     * @param postConstructDesc
     */
    public void addPostConstructDescriptor(
            LifecycleCallbackDescriptor postConstructDesc);

    /**
     * Look up an post-construct LifecycleCallbackDescriptor with the
     * given name.  Return null if it is not found
     * @param className
     */
    public LifecycleCallbackDescriptor getPostConstruct(String className);

   /**
     * @param preDestroyDesc
     */
    public void addPreDestroyDescriptor(
            LifecycleCallbackDescriptor preDestroyDesc);

    /**
     * Look up an pre-destroy LifecycleCallbackDescriptor with the
     * given name.  Return null if it is not found
     * @param className
     */
    public LifecycleCallbackDescriptor getPreDestroy(String className);

    /**
     * Adds the specified descriptor to the receiver.
     * @param desc ResourceDescriptor to add.
     */
    public void addResourceDescriptor(ResourceDescriptor desc);

    /**
     * get all descriptors based on the type
     * @return Set of ResourceDescriptor
     */
    public Set<ResourceDescriptor> getResourceDescriptors(JavaEEResourceType type);

    public void addManagedBean(ManagedBeanDescriptor managedBeanDesc);
}
