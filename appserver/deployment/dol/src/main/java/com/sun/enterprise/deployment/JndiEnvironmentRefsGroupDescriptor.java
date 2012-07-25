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
import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.types.MessageDestinationReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import java.util.logging.Logger;

import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

/**
 * Contains information about jndiEnvironmentRefsGroup.
 */ 

public abstract class JndiEnvironmentRefsGroupDescriptor extends CommonResourceDescriptor
        implements EjbReferenceContainer, ResourceReferenceContainer,
        MessageDestinationReferenceContainer, WritableJndiNameEnvironment
{
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(JndiEnvironmentRefsGroupDescriptor.class);

    private static final Logger _logger = DOLUtils.getDefaultLogger();

    protected Map<CallbackType,
                Set<LifecycleCallbackDescriptor>> callbackDescriptors
        = new HashMap<CallbackType, Set<LifecycleCallbackDescriptor>>();

    protected BundleDescriptor bundleDescriptor;

    protected Set environmentProperties;
    protected Set ejbReferences;
    protected Set resourceEnvReferences;
    protected Set messageDestReferences;
    protected Set resourceReferences;
    protected Set serviceReferences;
    protected Set<EntityManagerFactoryReferenceDescriptor> 
        entityManagerFactoryReferences;
    protected Set<EntityManagerReferenceDescriptor> 
        entityManagerReferences;

    public void setBundleDescriptor(BundleDescriptor desc) {
        bundleDescriptor = desc;
    }

    public BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    // callbacks
    public void addCallbackDescriptor(CallbackType type,
            LifecycleCallbackDescriptor llcDesc) {
        Set<LifecycleCallbackDescriptor> llcDescs =
            getCallbackDescriptors(type);
        boolean found = false;       
        for (LifecycleCallbackDescriptor llcD : llcDescs) {
            if ((llcDesc.getLifecycleCallbackClass() != null) &&
                llcDesc.getLifecycleCallbackClass().equals(
                    llcD.getLifecycleCallbackClass())) {
                found = true;
            }
        }

        if (!found) {
            llcDescs.add(llcDesc);
        }
    }

    public void addCallbackDescriptors(CallbackType type, 
                                  Set<LifecycleCallbackDescriptor> lccSet) {
        for (LifecycleCallbackDescriptor lcc : lccSet) {
            addCallbackDescriptor(type, lcc);
        }
    }

    public Set<LifecycleCallbackDescriptor> getCallbackDescriptors(
            CallbackType type)
    {
        Set<LifecycleCallbackDescriptor> lccDescs =
            callbackDescriptors.get(type);
        if (lccDescs == null) {
            lccDescs = new HashSet<LifecycleCallbackDescriptor>();
            callbackDescriptors.put(type, lccDescs);
        }
        return lccDescs;
    }

    public boolean hasCallbackDescriptor(CallbackType type) {
        return (getCallbackDescriptors(type).size() > 0);
    }

    public void addPostConstructDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.POST_CONSTRUCT, lcDesc);
    }

    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        throw new UnsupportedOperationException();
    }

    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors() {
        return getCallbackDescriptors(CallbackType.POST_CONSTRUCT);
    }

    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.PRE_DESTROY, lcDesc);
    }

    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        throw new UnsupportedOperationException();
    }

    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return getCallbackDescriptors(CallbackType.PRE_DESTROY);
    }

    // ejb ref
    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
	    this.getEjbReferenceDescriptors().add(ejbReference);
	    ejbReference.setReferringBundleDescriptor(getBundleDescriptor());
    }

    public EjbReference getEjbReference(String name) {
	for (Iterator itr = this.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
	    EjbReference er = (EjbReference) itr.next();
	    if (er.getName().equals(name)) {
		return er;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoejbrefbyname",
                "This class has no ejb reference by the name of {0}",
                new Object[] {name}));
    }

    public Set getEjbReferenceDescriptors() {
	if (this.ejbReferences == null) {
	    this.ejbReferences = new OrderedSet();
	}
	return this.ejbReferences = new OrderedSet(this.ejbReferences);
    }

    public void removeEjbReferenceDescriptor(EjbReference ejbReference) {
	this.getEjbReferenceDescriptors().remove(ejbReference);
	ejbReference.setReferringBundleDescriptor(null);
    }

    // message destination ref
    public void addMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor msgDestReference) {
        if( getBundleDescriptor() != null ) {
            msgDestReference.setReferringBundleDescriptor
                (getBundleDescriptor());
        }
        this.getMessageDestinationReferenceDescriptors().add(msgDestReference);
    }

    public MessageDestinationReferenceDescriptor getMessageDestinationReferenceByName(String name) {
	for (Iterator itr = 
                 this.getMessageDestinationReferenceDescriptors().iterator(); 
             itr.hasNext();) {
	    MessageDestinationReferenceDescriptor mdr = 
                (MessageDestinationReferenceDescriptor) itr.next();
	    if (mdr.getName().equals(name)) {
		return mdr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnomsgdestrefbyname",
                "This class has no message destination reference by the name of {0}",
                new Object[] {name}));
    }

    public Set getMessageDestinationReferenceDescriptors() {
        if( this.messageDestReferences == null ) {
            this.messageDestReferences = new OrderedSet();
        }
        return this.messageDestReferences = 
            new OrderedSet(this.messageDestReferences);
    }

    public void removeMessageDestinationReferenceDescriptor
        (MessageDestinationReferenceDescriptor msgDestRef) { 
        this.getMessageDestinationReferenceDescriptors().remove(msgDestRef);
    }

    // env property
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().add(environmentProperty);
    }

    public Set getEnvironmentProperties() {
	if (this.environmentProperties == null) {
	    this.environmentProperties = new OrderedSet();
	}
	return this.environmentProperties = new OrderedSet(this.environmentProperties);
    }

    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
	for (Iterator itr = this.getEnvironmentProperties().iterator(); itr.hasNext();) {
	    EnvironmentProperty ev = (EnvironmentProperty) itr.next();
	    if (ev.getName().equals(name)) {
		return ev;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoenvpropertybyname",
                "This class has no environment property by the name of {0}",
                new Object[] {name}));
    }

    public void removeEnvironmentProperty(
			EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().remove(environmentProperty);
    }

    // service ref
    public void addServiceReferenceDescriptor(
	        ServiceReferenceDescriptor serviceReference) {
        serviceReference.setBundleDescriptor(getBundleDescriptor());
        this.getServiceReferenceDescriptors().add(serviceReference);
    }

    public Set getServiceReferenceDescriptors() {
        if( this.serviceReferences == null ) {
            this.serviceReferences = new OrderedSet();
        }
        return this.serviceReferences = new OrderedSet(this.serviceReferences);
    }

    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
	for (Iterator itr = this.getServiceReferenceDescriptors().iterator(); 
             itr.hasNext();) {
	    ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) 
                itr.next();
	    if (srd.getName().equals(name)) {
		return srd;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoservicerefbyname",
                "This class has no service reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeServiceReferenceDescriptor(
		ServiceReferenceDescriptor serviceReference) {
        this.getServiceReferenceDescriptors().remove(serviceReference);
    }

    // resource ref
    public void addResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().add(resourceReference);
    }

    public Set getResourceReferenceDescriptors() {
	if (this.resourceReferences == null) {
	    this.resourceReferences = new OrderedSet();
	}
	return this.resourceReferences = new OrderedSet(this.resourceReferences);
    }

    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
	for (Iterator itr = this.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
	    ResourceReferenceDescriptor next = (ResourceReferenceDescriptor) itr.next();
	    if (next.getName().equals(name)) {
		return next;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoresourcerefbyname",
                "This class has no resource reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().remove(resourceReference);
    }

    // resource environment ref
    public void addResourceEnvReferenceDescriptor(
		ResourceEnvReferenceDescriptor resourceEnvinationReference) {
	this.getResourceEnvReferenceDescriptors().add(resourceEnvinationReference);
    }

    public Set getResourceEnvReferenceDescriptors() {
	if (this.resourceEnvReferences == null) {
	    this.resourceEnvReferences = new OrderedSet();
	}
	return this.resourceEnvReferences = new OrderedSet(this.resourceEnvReferences);
    }

    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name) {
	for (Iterator itr = this.getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
	    ResourceEnvReferenceDescriptor jdr = (ResourceEnvReferenceDescriptor) itr.next();
	    if (jdr.getName().equals(name)) {
		return jdr;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoresourceenvrefbyname",
                "This class has no resource environment reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeResourceEnvReferenceDescriptor(
		ResourceEnvReferenceDescriptor resourceEnvinationReference) {
	this.getResourceEnvReferenceDescriptors().remove(resourceEnvinationReference);
    }

    // entity manager factory ref 
    public void addEntityManagerFactoryReferenceDescriptor(
                EntityManagerFactoryReferenceDescriptor reference) {
        if( getBundleDescriptor() != null ) {
            reference.setReferringBundleDescriptor
                (getBundleDescriptor());
        }
        this.getEntityManagerFactoryReferenceDescriptors().add(reference);
    }

    public Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors() {
        if( this.entityManagerFactoryReferences == null ) {
            this.entityManagerFactoryReferences = 
                new HashSet<EntityManagerFactoryReferenceDescriptor>();
        }
        return entityManagerFactoryReferences; 
    }

    public EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name) {
	for (EntityManagerFactoryReferenceDescriptor next :
             getEntityManagerFactoryReferenceDescriptors()) {

	    if (next.getName().equals(name)) {
		return next;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoentitymgrfactoryrefbyname",
                "This class has no entity manager factory reference by the name of {0}",
                new Object[] {name}));
    }

    //  entity manager ref
    public void addEntityManagerReferenceDescriptor(
                EntityManagerReferenceDescriptor reference) {
        if( getBundleDescriptor() != null ) {
            reference.setReferringBundleDescriptor
                (getBundleDescriptor());
        }
        this.getEntityManagerReferenceDescriptors().add(reference);
    }

    public Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors() {
        if( this.entityManagerReferences == null ) {
            this.entityManagerReferences = 
                new HashSet<EntityManagerReferenceDescriptor>();
        }
        return entityManagerReferences; 
    }

    public EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name) {
	for (EntityManagerReferenceDescriptor next :
             getEntityManagerReferenceDescriptors()) {

	    if (next.getName().equals(name)) {
		return next;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoentitymgrrefbyname",
                "This class has no entity manager reference by the name of {0}",
                new Object[] {name}));
    }

    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        throw new UnsupportedOperationException();
    }    

    public InjectionInfo getInjectionInfoByClass(Class clazz) {
        throw new UnsupportedOperationException();
    }
}
