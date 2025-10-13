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
// Portions Copyright [2019-2020] Payara Foundation and/or affiliates

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.node.appclient.AppClientNode;
import com.sun.enterprise.deployment.runtime.JavaWebStartAccessDescriptor;
import com.sun.enterprise.deployment.types.*;
import com.sun.enterprise.deployment.util.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.deployment.common.JavaEEResourceType;

import java.util.*;

/**
 * I represent all the deployment information about
 * an application client [{0}].
 * @author Danny Coward
 */
public class ApplicationClientDescriptor extends CommonResourceBundleDescriptor
            implements WritableJndiNameEnvironment,
                       ResourceReferenceContainer,
                       EjbReferenceContainer,
                       ResourceEnvReferenceContainer,
                       ServiceReferenceContainer,
                       MessageDestinationReferenceContainer
{
    
    private Set<EnvironmentProperty> environmentProperties;
    private Set<EjbReference> ejbReferences;
    private Set<ResourceEnvReferenceDescriptor> resourceEnvReferences;
    private Set<MessageDestinationReferenceDescriptor> messageDestReferences;
    private Set<ResourceReferenceDescriptor> resourceReferences;
    private Set<ServiceReferenceDescriptor> serviceReferences;
    private Set<EntityManagerFactoryReferenceDescriptor> entityManagerFactoryReferences = new HashSet<>();
    private Set<EntityManagerReferenceDescriptor> entityManagerReferences = new HashSet<>();
    private Set<LifecycleCallbackDescriptor> postConstructDescs = new HashSet<>();
    private Set<LifecycleCallbackDescriptor> preDestroyDescs = new HashSet<>();
    private String mainClassName=null;
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ApplicationClientDescriptor.class);
    private String callbackHandler = null;
    private JavaWebStartAccessDescriptor jwsAccessDescriptor = null;

    /**
    * Return true if there is runtime information in this
    * object that must be saved.
    */
    public boolean hasRuntimeInformation() {
	for (Iterator itr = this.getNamedDescriptors().iterator(); itr.hasNext();) {
	    NamedDescriptor next = (NamedDescriptor) itr.next();
	    if (!"".equals(next.getJndiName())) {
		return true;
	    }
	}
	return false;
    }

    /**
     * @return the default version of the deployment descriptor
     * loaded by this descriptor
     */
    @Override
    public String getDefaultSpecVersion() {
        return AppClientNode.SPEC_VERSION;
    }

    @Override
    public boolean isEmpty() {
        return mainClassName==null;
    }

    /**
    * Return the fq Java classname of this application client [{0}].
    */
    public String getMainClassName() {
	if (this.mainClassName == null) {
            this.mainClassName = "";
	}
	return this.mainClassName;
    }

    /**
    * Sets the main classname of this app client.
    */
    public void setMainClassName(String mainClassName) {
	this.mainClassName = mainClassName;
    }

    /**
     * Get the classname of the callback handler.
     */
     public String getCallbackHandler() {
	return callbackHandler;
     }

    /**
     * Set the classname of the callback handler.
     */
     public void setCallbackHandler(String handler) {
	callbackHandler = handler;

     }

    /**
    * Return the set of named descriptors I reference.
    */

    public Collection getNamedDescriptors() {
	return super.getNamedDescriptorsFrom(this);
    }

    /**
    * Return the set of named reference pairs I reference.
    */
    public Vector<NamedReferencePair> getNamedReferencePairs() {
	return super.getNamedReferencePairsFrom(this);
    }

    /**
    * Returns the set of environment properties of this app client.
    */
    @Override
    public Set<EnvironmentProperty> getEnvironmentProperties() {
	if (this.environmentProperties == null) {
	    this.environmentProperties = new OrderedSet<>();
	}
	return this.environmentProperties = new OrderedSet<>(this.environmentProperties);
    }

    /**
     * Returns the environment property object searching on the supplied key.
     * throws an illegal argument exception if no such environment property exists.
     */
    @Override
    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
        for (EnvironmentProperty ev : this.getEnvironmentProperties()) {
            if (ev.getName().equals(name)) {
                return ev;
            }
        }
	throw new IllegalArgumentException(localStrings.getLocalString(
            "enterprise.deployment.exceptionappclienthasnoenvpropertybyname",
            "This application client [{0}] has no environment property by the name of [{1}]",
            new Object[] {getName(), name}));
    }


    /**
    * Adds an environment property to this application client [{0}].
    */

    @Override
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().add(environmentProperty);

    }

    /**
    * Remove the given environment property
    */
    @Override
    public void removeEnvironmentProperty(EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().remove(environmentProperty);
    }

    /**
    * Return the set of references to ejbs that I have.
    */
    @Override
    public Set<EjbReference> getEjbReferenceDescriptors() {
	if (this.ejbReferences == null) {
	    this.ejbReferences = new OrderedSet<>();
	}
	return this.ejbReferences = new OrderedSet<>(this.ejbReferences);
    }

    /**
    * Add a reference to an ejb.
    */
    public void addEjbReferenceDescriptor(EjbReferenceDescriptor ejbReference) {
        addEjbReferenceDescriptor((EjbReference) ejbReference);
    }

    @Override
    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
	this.getEjbReferenceDescriptors().add(ejbReference);
        ejbReference.setReferringBundleDescriptor(this);

    }

    /**
    * Removes the given reference to an ejb.
    */
    public void removeEjbReferenceDescriptor(EjbReferenceDescriptor ejbReference) {
        removeEjbReferenceDescriptor((EjbReference) ejbReference);
    }

    @Override
    public void removeEjbReferenceDescriptor(EjbReference ejbReference) {
	this.getEjbReferenceDescriptors().remove(ejbReference);
	ejbReference.setReferringBundleDescriptor(null);

    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors() {
        return postConstructDescs;
    }

    @Override
    public void addPostConstructDescriptor(LifecycleCallbackDescriptor postConstructDesc) {
        String className = postConstructDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
             getPostConstructDescriptors()) {
            if ( (next.getLifecycleCallbackClass() != null) &&
                next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPostConstructDescriptors().add(postConstructDesc);
        }
    }

    @Override
    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        return getPostConstructDescriptorByClass(className, this);
    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return preDestroyDescs;
    }


    @Override
    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor preDestroyDesc) {
        String className = preDestroyDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
             getPreDestroyDescriptors()) {
            if ( (next.getLifecycleCallbackClass() != null) &&
                next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPreDestroyDescriptors().add(preDestroyDesc);
        }
    }

    @Override
    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        return getPreDestroyDescriptorByClass(className, this);
    }

    @Override
    public InjectionInfo getInjectionInfoByClass(Class clazz) {
        return getInjectionInfoByClass(clazz, this);
    }

    @Override
    public Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors() {
        if( this.serviceReferences == null ) {
            this.serviceReferences = new OrderedSet<>();
        }
        return this.serviceReferences = new OrderedSet<>(this.serviceReferences);
    }

    @Override
    public void addServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef) {
        serviceRef.setBundleDescriptor(this);
        this.getServiceReferenceDescriptors().add(serviceRef);

    }

    @Override
    public void removeServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef) {
        this.getServiceReferenceDescriptors().remove(serviceRef);
    }

    /**
     * Looks up an service reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    @Override
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
            "enterprise.deployment.exceptionappclienthasnoservicerefbyname",
            "This application client [{0}] has no service reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    @Override
    public Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors() {
        if( this.messageDestReferences == null ) {
            this.messageDestReferences = new OrderedSet<>();
        }
        return this.messageDestReferences = new OrderedSet<>(this.messageDestReferences);
    }

    @Override
    public void addMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor messageDestRef) {
        messageDestRef.setReferringBundleDescriptor(this);
        this.getMessageDestinationReferenceDescriptors().add(messageDestRef);
    }

    @Override
    public void removeMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor msgDestRef) {
        this.getMessageDestinationReferenceDescriptors().remove(msgDestRef);
    }

    /**
     * Looks up an message destination reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    @Override
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
		"exceptionappclienthasnomsgdestrefbyname",
                "This application client [{0}] has no message destination reference by the name of [{1}]",
                new Object[] {getName(), name}));
    }

   /**
	* Return the set of resource environment references this ejb declares.
	*/
    @Override
    public Set<ResourceEnvReferenceDescriptor> getResourceEnvReferenceDescriptors() {
	if (this.resourceEnvReferences == null) {
	    this.resourceEnvReferences = new OrderedSet<>();
	}
	return this.resourceEnvReferences = new OrderedSet<>(this.resourceEnvReferences);
    }

    @Override
    public void addResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvReference) {
	this.getResourceEnvReferenceDescriptors().add(resourceEnvReference);

    }

    @Override
    public void removeResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvReference) {
	this.getResourceEnvReferenceDescriptors().remove(resourceEnvReference);

    }

    /**
    * Looks up an ejb reference with the given name. Throws an IllegalArgumentException
    * if it is not found.
    */
    public EjbReferenceDescriptor getEjbReferenceByName(String name) {
	for (Iterator itr = this.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
	    EjbReferenceDescriptor ejr = (EjbReferenceDescriptor) itr.next();
	    if (ejr.getName().equals(name)) {
		return ejr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
            "exceptionappclienthasnoejbrefbyname",
            "This application client [{0}] has no ejb reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    @Override
    public Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors() {
        return entityManagerFactoryReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     * @param name
     */
    public EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name) {
        for (EntityManagerFactoryReferenceDescriptor next :
             getEntityManagerFactoryReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
	throw new IllegalArgumentException(localStrings.getLocalString(
            "exceptionappclienthasnoentitymgrfactoryrefbyname",
            "This application client [{0}] has no entity manager factory reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    @Override
    public void addEntityManagerFactoryReferenceDescriptor
        (EntityManagerFactoryReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        this.getEntityManagerFactoryReferenceDescriptors().add(reference);

    }

    @Override
    public Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors() {
        return entityManagerReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    @Override
    public EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name) {
	throw new IllegalArgumentException(localStrings.getLocalString(
            "exceptionappclienthasnoentitymgrrefbyname",
            "This application client [{0}] has no entity manager reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    @Override
    public void addEntityManagerReferenceDescriptor(EntityManagerReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        this.getEntityManagerReferenceDescriptors().add(reference);

    }

    @Override
    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        return getInjectableResourcesByClass(className, this);
    }

    /**
    * Looks up an ejb reference with the given name. Throws an IllegalArgumentException
    * if it is not found.
    */
    @Override
    public EjbReference getEjbReference(String name) {
	for (Iterator itr = this.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
	    EjbReference ejr = (EjbReference) itr.next();
	    if (ejr.getName().equals(name)) {
		return ejr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
            "exceptionappclienthasnoejbrefbyname",
            "This application client [{0}] has no ejb reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    /**
    * Return a resource environment reference by the same name or throw an IllegalArgumentException.
    */
    @Override
    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name) {
	for (Iterator itr = this.getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
	    ResourceEnvReferenceDescriptor jdr = (ResourceEnvReferenceDescriptor) itr.next();
	    if (jdr.getName().equals(name)) {
		return jdr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
	    "enterprise.deployment.exceptionappclienthasnoesourceenvrefbyname",
	    "This application client [{0}] has no resource environment reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    /**
    * Return the set of references to resources that I have.
    */
    @Override
    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
	if (this.resourceReferences == null) {
	    this.resourceReferences = new OrderedSet<>();
	}
	return this.resourceReferences = new OrderedSet<>(this.resourceReferences);
    }

    /**
    * Looks up a reference to a resource by its name (getName()). Throws an IllegalArgumentException
    * if no such descriptor is found.
    */
    @Override
    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
	for (Iterator itr = this.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
	    ResourceReferenceDescriptor rr = (ResourceReferenceDescriptor) itr.next();
	    if (rr.getName().equals(name)) {
		return rr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
	    "exceptionappclienthasnoresourcerefbyname",
	    "This application client [{0}] has no resource reference by the name of [{1}]",
            new Object[] {getName(), name}));
    }

    /**
    * Adds a reference to a resource.
    */

    @Override
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().add(resourceReference);

    }

    /**
    * Removes the given resource reference from this app client.
    */
    @Override
    public void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().remove(resourceReference);

    }

    /**
     * @return a set of class names that need to have full annotation processing
     */
    public Set<String> getComponentClassNames() {
        Set set = new HashSet<String>();
        set.add(getMainClassName());
        return set;
    }


    /**
     * @return true if this bundle descriptor defines web service clients
     */
    @Override
    public boolean hasWebServiceClients() {
        return !(getServiceReferenceDescriptors().isEmpty());
    }

    /**
     * @return true if this bundle descriptor defines web services
     */
    @Override
    public boolean hasWebServices() {
        return false;
    }

    @Override
    public void print(StringBuilder toStringBuilder) {
        toStringBuilder.append("Application Client Descriptor");
	toStringBuilder.append("\n ");
        super.print(toStringBuilder);
	toStringBuilder.append("\n environmentProperties ").append(environmentProperties);
	toStringBuilder.append("\n ejbReferences ");
        if(ejbReferences != null)
            printDescriptorSet(ejbReferences,toStringBuilder);
        toStringBuilder.append("\n resourceEnvReferences ");
        if(resourceEnvReferences != null)
            printDescriptorSet(resourceEnvReferences,toStringBuilder);
        toStringBuilder.append("\n messageDestReferences ");
        if(messageDestReferences != null)
            printDescriptorSet(messageDestReferences,toStringBuilder);
	toStringBuilder.append("\n resourceReferences ");
        if(resourceReferences != null)
            printDescriptorSet(resourceReferences,toStringBuilder);
        toStringBuilder.append("\n serviceReferences ");
        if(serviceReferences != null)
            printDescriptorSet(serviceReferences,toStringBuilder);
	toStringBuilder.append("\n mainClassName ").append(mainClassName);
    }
    
    private void printDescriptorSet(Set descSet, StringBuilder sbuf){
        for(Iterator itr = descSet.iterator(); itr.hasNext();){
            Object obj = itr.next();
            if(obj instanceof Descriptor)
                ((Descriptor)obj).print(sbuf);
            else
                sbuf.append(obj);
        }
    }

    /**
     * visit the descriptor and all sub descriptors with a DOL visitor implementation
     *
     * @param aVisitor a visitor to traverse the descriptors
     */
    @Override
    public void visit(DescriptorVisitor aVisitor) {
        if (aVisitor instanceof AppClientVisitor ||
            aVisitor instanceof ComponentPostVisitor) {
            visit((ComponentVisitor) aVisitor);
        } else {
            super.visit(aVisitor);
        }
    }

    /**
     * @return the module type for this bundle descriptor
     */
    @Override
    public ArchiveType getModuleType() {
        return DOLUtils.carType();
    }

    /**
     * @return the visitor for this bundle descriptor
     */
    @Override
    public ComponentVisitor getBundleVisitor() {
        return new AppClientValidator();
    }

    /**
     * @return the tracer visitor for this descriptor
     */
    @Override
    public DescriptorVisitor getTracerVisitor() {
        return new AppClientTracerVisitor();
    }

    public JavaWebStartAccessDescriptor getJavaWebStartAccessDescriptor() {
        if (jwsAccessDescriptor == null) {
            jwsAccessDescriptor = new JavaWebStartAccessDescriptor();
            jwsAccessDescriptor.setBundleDescriptor(this);
        }
        return jwsAccessDescriptor;
    }

    public void setJavaWebStartAccessDescriptor(JavaWebStartAccessDescriptor descr) {
        descr.setBundleDescriptor(this);
        jwsAccessDescriptor = descr;

    }

    /**
     * This method is used to find out the precise list of PUs that are
     * referenced by the appclient. An appclient can not use container
     * managed EM as there is no support for JTA in our ACC, so this method
     * only returns the list of PUs referenced via @PersistenceUnit or
     * <persistence-unit-ref>.
     *
     * @return list of PU that are actually referenced by the appclient.
     */
    @Override
    public Collection<? extends PersistenceUnitDescriptor> findReferencedPUs() {
        return findReferencedPUsViaPURefs(this);
    }

    @Override
    public Set<ResourceDescriptor> getResourceDescriptors(JavaEEResourceType type) {
        switch(type) {
            case CFD:
                throw new UnsupportedOperationException(localStrings.getLocalString(
                            "enterprise.deployment.exceptionappclientnotsupportconnectionfactorydefinition",
                            "The application client [{0}] do not support connection factory definitions",
                                new Object[] {getName()}));
            case AODD:
                throw new UnsupportedOperationException(localStrings.getLocalString(
                            "enterprise.deployment.exceptionappclientnotsupportadministeredobjectdefinition",
                            "The application client [{0}] do not support administered object definitions",
                                new Object[] {getName()}));

        }
        return super.getResourceDescriptors(type);
    }

    @Override
    public void addResourceDescriptor(ResourceDescriptor descriptor) {

        if(descriptor.getResourceType().equals(JavaEEResourceType.CFD)){
            throw new UnsupportedOperationException(localStrings.getLocalString(
            			    "enterprise.deployment.exceptionappclientnotsupportconnectionfactorydefinition",
            			    "The application client [{0}] do not support connection factory definitions",
            		            new Object[] {getName()}));
        } else if (descriptor.getResourceType().equals(JavaEEResourceType.AODD)) {
            throw new UnsupportedOperationException(localStrings.getLocalString(
                            "enterprise.deployment.exceptionappclientnotsupportadministeredobjectdefinition",
                            "The application client [{0}] do not support administered object definitions",
                                new Object[] {getName()}));

        } else {
            super.addResourceDescriptor(descriptor);
        }

    }

    @Override
    public void removeResourceDescriptor(ResourceDescriptor descriptor) {
        if(descriptor.getResourceType().equals(JavaEEResourceType.CFD)){
            throw new UnsupportedOperationException(localStrings.getLocalString(
            			    "enterprise.deployment.exceptionappclientnotsupportconnectionfactorydefinition",
            			    "The application client [{0}] do not support connection factory definitions",
            		            new Object[] {getName()}));

        } else if (descriptor.getResourceType().equals(JavaEEResourceType.AODD)) {
            throw new UnsupportedOperationException(localStrings.getLocalString(
                            "enterprise.deployment.exceptionappclientnotsupportadministeredobjectdefinition",
                            "The application client [{0}] do not support administered object definitions",
                                new Object[] {getName()}));
        } else {
            super.removeResourceDescriptor(descriptor);
        }
    }
}
