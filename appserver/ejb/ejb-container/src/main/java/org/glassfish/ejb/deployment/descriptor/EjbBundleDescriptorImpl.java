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

package org.glassfish.ejb.deployment.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerFactoryReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.NamedReferencePair;
import com.sun.enterprise.deployment.OrderedSet;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.RoleReference;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.EntityManagerFactoryReference;
import com.sun.enterprise.deployment.types.EntityManagerReference;
import com.sun.enterprise.deployment.util.ComponentPostVisitor;
import com.sun.enterprise.deployment.util.ComponentVisitor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.util.EjbBundleVisitor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASPersistenceManagerDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.PersistenceManagerInUse;
import org.glassfish.ejb.deployment.node.EjbBundleNode;
import org.glassfish.ejb.deployment.util.EjbBundleTracerVisitor;
import org.glassfish.ejb.deployment.util.EjbBundleValidator;
import org.glassfish.security.common.Role;

/**
 * The concrete implementation of abstract super class com.sun.enterprise.deployment.EjbBundleDescriptor.
 * EjbBundleDescriptor could be changed from abstract class to an interface in the future, with this
 * class as its implementation.
 */
public class EjbBundleDescriptorImpl extends com.sun.enterprise.deployment.EjbBundleDescriptor {
    private long uniqueId;
    private Boolean disableNonportableJndiNames;
    private Set<EjbDescriptor> ejbs = new HashSet<EjbDescriptor>();
    private Set<Long> ejbIDs = null;
    private Set<RelationshipDescriptor> relationships = new HashSet<RelationshipDescriptor>();
    private String relationshipsDescription;
    private String ejbClientJarUri;
    
    // list of configured persistence manager
    private Vector configured_pms = null;
    private PersistenceManagerInUse pm_inuse = null;
    
    // the resource (database) to be used for persisting CMP EntityBeans
    // the same resource is used for all beans in this ejb jar.
    private ResourceReferenceDescriptor cmpResourceReference;

    // Application exceptions defined for the ejbs in this module.
    private Map<String, EjbApplicationExceptionInfo> applicationExceptions =
        new HashMap<String, EjbApplicationExceptionInfo>();

    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(EjbBundleDescriptorImpl.class);

    static Logger _logger = DOLUtils.getDefaultLogger();

    private List<SecurityRoleMapping> roleMaps = new ArrayList<SecurityRoleMapping>();

    // All interceptor classes defined within this ejb module, keyed by
    // interceptor class name.
    private Map<String, EjbInterceptor> interceptors = 
        new HashMap<String, EjbInterceptor>();
        
    private LinkedList<InterceptorBindingDescriptor> interceptorBindings =
        new LinkedList<InterceptorBindingDescriptor>();

    private List<NameValuePairDescriptor> enterpriseBeansProperties =
            new ArrayList<NameValuePairDescriptor>();

    // EJB module level dependencies 
    private Set<EnvironmentProperty> environmentProperties =
              new HashSet<EnvironmentProperty>();
    private Set<EjbReference> ejbReferences =
              new HashSet<EjbReference>();
    private Set<ResourceEnvReferenceDescriptor> resourceEnvReferences =
              new HashSet<ResourceEnvReferenceDescriptor>();
    private Set<MessageDestinationReferenceDescriptor> messageDestReferences =
              new HashSet<MessageDestinationReferenceDescriptor>();
    private Set<ResourceReferenceDescriptor> resourceReferences =
              new HashSet<ResourceReferenceDescriptor>();
    private Set<ServiceReferenceDescriptor> serviceReferences =
              new HashSet<ServiceReferenceDescriptor>();
    private Set<EntityManagerFactoryReferenceDescriptor>
              entityManagerFactoryReferences =
              new HashSet<EntityManagerFactoryReferenceDescriptor>();
    private Set<EntityManagerReferenceDescriptor>
              entityManagerReferences =
              new HashSet<EntityManagerReferenceDescriptor>();

    /**
     * True if EJB version is 2.x.  This is the default
     * for any new modules.
     */
    // XXX
    // this method is not true anymore now we have ejb3.0, keep this 
    // method as it is for now, will revisit once ejb30 persistence 
    // is implemented
    public boolean isEJB20() {
        return !isEJB11();
    }
    
    /**
     * True if EJB version is 1.x.
     */
    public boolean isEJB11() {
        return getSpecVersion().startsWith("1");
    }

    /**
     * @return the default version of the deployment descriptor
     * loaded by this descriptor
     */
    @Override
    public String getDefaultSpecVersion() {
        return EjbBundleNode.SPEC_VERSION;
    }

    /**
    * Return the empty String or the entry name of the ejb client JAR
    * in my archive if I have one.
    */
    public String getEjbClientJarUri() {
    if (ejbClientJarUri == null) {
        ejbClientJarUri = "";
    }
    return ejbClientJarUri;
    }

    @Override
    public boolean isEmpty() {
        return ejbs.isEmpty();
    }

    public void setEjbClientJarUri(String ejbClientJarUri) {
        this.ejbClientJarUri = ejbClientJarUri;
    }

    public void addApplicationException(EjbApplicationExceptionInfo appExc) {
        applicationExceptions.put(appExc.getExceptionClassName(), appExc);
    }

    public Map<String, EjbApplicationExceptionInfo> getApplicationExceptions() {
        return new HashMap<String, EjbApplicationExceptionInfo>(applicationExceptions);
    }
     
    /**
    * Return the set of NamedDescriptors that I have.
    */
    public Collection getNamedDescriptors() {
        Collection namedDescriptors = new Vector();
        for (EjbDescriptor ejbDescriptor : getEjbs()) {
            namedDescriptors.add(ejbDescriptor);
            namedDescriptors.addAll(super.getNamedDescriptorsFrom(ejbDescriptor));
        }
        return namedDescriptors;
    }

    /**
    * Return all the named descriptors I have together with the descriptor
    * that references each one in a Vector of NameReferencePairs.
    */
    
    public Vector<NamedReferencePair> getNamedReferencePairs() {
    Vector<NamedReferencePair> pairs = new Vector<NamedReferencePair>();
        for (EjbDescriptor ejbDescriptor : getEjbs()) {
            pairs.add(NamedReferencePair.createEjbPair(ejbDescriptor,
                    ejbDescriptor));
            pairs.addAll(super.getNamedReferencePairsFrom(ejbDescriptor));
        }
    return pairs;
    } 
    
    /**
    * Return the set of references to resources held by ejbs defined in this module.
    */
    public Set<ResourceReferenceDescriptor> getEjbResourceReferenceDescriptors() {
    Set<ResourceReferenceDescriptor> resourceReferences = new HashSet<ResourceReferenceDescriptor>();
    for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        EjbDescriptor ejbDescriptor = (EjbDescriptor) itr.next();
        resourceReferences.addAll(ejbDescriptor.getResourceReferenceDescriptors());
    }
    return resourceReferences;
    }
    
    /**
    * Return true if I reference other ejbs, false else.
    */
    public boolean hasEjbReferences() {
    for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        EjbDescriptor nextEjbDescriptor = (EjbDescriptor) itr.next();
        if (!nextEjbDescriptor.getEjbReferenceDescriptors().isEmpty()) {
        return true;
        }
    }
    return false;
    }

    /**
    * Return the Set of ejb descriptors that I have.
    */
    @Override
    public Set<EjbDescriptor> getEjbs() {
    return Collections.unmodifiableSet(ejbs);
    }

    /**
     * Setup EJB Ids during deployment and shouldn't be called at runtime
     */
    public void setupDataStructuresForRuntime() {
        Set<Long> ids = new HashSet<Long>();
        for (EjbDescriptor ejbDescriptor : ejbs) {
            ids.add(ejbDescriptor.getUniqueId());
        }
        ejbIDs = Collections.unmodifiableSet(ids);
    }

    /**
    * Returns true if I have an ejb descriptor by that name.
    */
    @Override
    public boolean hasEjbByName(String name) {
    for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        Descriptor next = (Descriptor) itr.next();
        if (next.getName().equals(name)) {
        return true;
        }
    }
    return false;
    }
    
    /**
    * Returns an ejb descriptor that I have by the same name, otherwise 
    * throws an IllegalArgumentException
    */
    @Override
    public EjbDescriptor getEjbByName(String name) {
        return getEjbByName(name, false);
    }

    /**
     * Returns an ejb descriptor that I have by the same name. 
     * Create a DummyEjbDescriptor if requested, otherwise
     * throws an IllegalArgumentException
     */
    public EjbDescriptor getEjbByName(String name, boolean isCreateDummy) {
        for (EjbDescriptor next : getEjbs()) {
            if (next.getName().equals(name)) {
                return next;
            }
        }

         if (!isCreateDummy) {   
             throw new IllegalArgumentException(localStrings.getLocalString(
                 "enterprise.deployment.exceptionbeanbundle",
                 "Referencing error: this bundle has no bean of name: {0}",
                     name));
         }

         // there could be cases where the annotation defines the ejb component
         // and the ejb-jar.xml just uses it 
         // we have to create a dummy version of the ejb descriptor in this 
         // case as we process xml before annotations.
         _logger.log(Level.FINE, "enterprise.deployment_dummy_ejb_descriptor",
                         new Object[] {name});
         DummyEjbDescriptor dummyEjbDesc = new DummyEjbDescriptor();
         dummyEjbDesc.setName(name);
         addEjb(dummyEjbDesc);
         return dummyEjbDesc;
     }

    /**
     * Returns all ejb descriptors that has a give Class name.
     * It returns an empty array if no ejb is found.
     */
    @Override
    public EjbDescriptor[] getEjbByClassName(String className) {
        ArrayList<EjbDescriptor> ejbList = new ArrayList<EjbDescriptor>();
    for (Object ejb : getEjbs()) {
            if (ejb instanceof EjbDescriptor) {
                EjbDescriptor ejbDesc = (EjbDescriptor)ejb;
                if (className.equals(ejbDesc.getEjbClassName())) {
                    ejbList.add(ejbDesc);
                }
            }
    }
        return ejbList.toArray(new EjbDescriptor[ejbList.size()]);
    }
    
    /**
     * Returns all ejb descriptors that has a given Class name as
     * the web service endpoint interface.
     * It returns an empty array if no ejb is found.
     */
    @Override
    public EjbDescriptor[] getEjbBySEIName(String className) {
        ArrayList<EjbDescriptor> ejbList = new ArrayList<EjbDescriptor>();
    for (Object ejb : getEjbs()) {
            if (ejb instanceof EjbDescriptor) {
                EjbDescriptor ejbDesc = (EjbDescriptor)ejb;
                if (className.equals(ejbDesc.getWebServiceEndpointInterfaceName())) {
                    ejbList.add(ejbDesc);
                }
            }
    }
        return ejbList.toArray(new EjbDescriptor[ejbList.size()]);
    }

    /**
     *
     * @return Collection of unique ID of EJBs within the same module
     */
    public Collection<Long> getDescriptorIds() {
        return ejbIDs;
    }

    public void addEjb(EjbDescriptor ejbDescriptor) {
        ejbDescriptor.setEjbBundleDescriptor(this);
        ejbs.add(ejbDescriptor);
    
    }
    
    /**
    * Remove the given ejb descriptor from my (by equality).
    */
    
    public void removeEjb(EjbDescriptor ejbDescriptor) {
        ejbDescriptor.setEjbBundleDescriptor(null);
        ejbs.remove(ejbDescriptor);
    }

    /**
     * @return true if this bundle descriptor contains at least one CMP
     * EntityBean
     */
    public boolean containsCMPEntity() {
        
        Set ejbs = getEjbs();
        for (Iterator ejbsItr = ejbs.iterator();ejbsItr.hasNext();) {
            if (ejbsItr.next() instanceof EjbCMPEntityDescriptor) {
                return true;
            }
        }
        return false;
    }

    public void addInterceptor(EjbInterceptor interceptor) {
        EjbInterceptor ic =
            getInterceptorByClassName(interceptor.getInterceptorClassName());
        if (ic == null) {
            interceptor.setEjbBundleDescriptor(this);
            interceptors.put(interceptor.getInterceptorClassName(), interceptor);
        }                 
    }
    
    @Override
    public EjbInterceptor getInterceptorByClassName(String className) {

        return interceptors.get(className);

    }

    public boolean hasInterceptors() {

        return (interceptors.size() > 0);

    }

    @Override
    public Set<EjbInterceptor> getInterceptors() {

        return new HashSet<EjbInterceptor>(interceptors.values());

    }

    public void prependInterceptorBinding(InterceptorBindingDescriptor binding)
    {
        interceptorBindings.addFirst(binding);
    }

    public void appendInterceptorBinding(InterceptorBindingDescriptor binding)
    {
        interceptorBindings.addLast(binding);
    }

    public List<InterceptorBindingDescriptor> getInterceptorBindings() {
        return new LinkedList<InterceptorBindingDescriptor>
            (interceptorBindings);
    }

    public void setInterceptorBindings(List<InterceptorBindingDescriptor>
                                       bindings) {
        interceptorBindings = new LinkedList<InterceptorBindingDescriptor>();
        interceptorBindings.addAll(bindings);
    }

    /**
    * Checks whether the role references my ejbs have reference roles that I have.
    */
    
    public boolean areResourceReferencesValid() {
    // run through each of the ejb's role references, checking that the roles exist in this bundle
    for (EjbDescriptor ejbDescriptor : getEjbs()) {
        for (Iterator roleRefs = ejbDescriptor.getRoleReferences().iterator(); roleRefs.hasNext();) {
        RoleReference roleReference = (RoleReference) roleRefs.next();
        Role referredRole = roleReference.getRole();
        if (!referredRole.getName().equals("") 
            && !super.getRoles().contains(referredRole) ) {
            
            _logger.log(Level.FINE,localStrings.getLocalString(
               "enterprise.deployment.badrolereference",
               "Warning: Bad role reference to {0}", new Object[] {referredRole}));
            _logger.log(Level.FINE,"Roles:  "+ getRoles());
            return false;
        }
        }
    }
    return true;
    }
    
    /**
    * Removes the given org.glassfish.security.common.Role object from me.
    */
    @Override
    public void removeRole(Role role) {
    if (super.getRoles().contains(role)) {
        for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        EjbDescriptor ejbDescriptor = (EjbDescriptor) itr.next();
        ejbDescriptor.removeRole(role);
        }
        super.removeRole(role);
    }
    }
    
    /**
    * Returns true if I have Roles to which method permissions have been assigned.
    */
    public boolean hasPermissionedRoles() {
    for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        EjbDescriptor nextEjbDescriptor = (EjbDescriptor) itr.next();
        if (!nextEjbDescriptor.getPermissionedMethodsByPermission().isEmpty()) {
        return true;
        }
    }
    return false;
    }
    
    /**
    * Return true if any of my ejb's methods have been assigned transaction attributes.
    */
    public boolean hasContainerTransactions() {
    for (Iterator itr = getEjbs().iterator(); itr.hasNext();) {
        EjbDescriptor nextEjbDescriptor = (EjbDescriptor) itr.next();
        if (!nextEjbDescriptor.getMethodContainerTransactions().isEmpty()) {
        return true;
        }
    }
    return false;
    }
    
    /**
    * Return true if I have roles, permissioned roles or container transactions.
    */
    public boolean hasAssemblyInformation() {
        return (!getRoles().isEmpty()) || hasPermissionedRoles() || hasContainerTransactions();
    }

    /**
     * Add a RelationshipDescriptor which describes a CMR field
     * between a bean/DO/entityRef in this ejb-jar.
     */
    public void addRelationship(RelationshipDescriptor relDesc)
    {
        relationships.add(relDesc);

    }

    /**
     * Add a RelationshipDescriptor which describes a CMR field
     * between a bean/DO/entityRef in this ejb-jar.
     */
    public void removeRelationship(RelationshipDescriptor relDesc)
    {
        relationships.remove(relDesc);

    }

 
    /**
     * EJB2.0: get description for <relationships> element.
     */
    public String getRelationshipsDescription() {
    if ( relationshipsDescription == null )
        relationshipsDescription = "";
    return relationshipsDescription;
    }
 
    /**
     * EJB2.0: set description for <relationships> element.
     */
    public void setRelationshipsDescription(String relationshipsDescription) {
        this.relationshipsDescription = relationshipsDescription;
    }
    

    /**
     * Get all relationships in this ejb-jar.
     * @return a Set of RelationshipDescriptors.
     */
    public Set<RelationshipDescriptor> getRelationships()
    {
        return relationships;
    }

    public boolean hasRelationships()
    {
    return (relationships.size() > 0);
    }

    /**
     * Returns true if given relationship is already part of this
     * ejb-jar.
     */
    public boolean hasRelationship(RelationshipDescriptor rd) {
        return relationships.contains(rd);
    }

    /**
     * Return the Resource I use for CMP.
     */
    public ResourceReferenceDescriptor getCMPResourceReference() {
        return cmpResourceReference;
    }
    
    /**
     * Sets the resource reference I use for CMP.
     */
    public void setCMPResourceReference(ResourceReferenceDescriptor resourceReference) {
        this.cmpResourceReference = resourceReference;
    }


    public Descriptor getDescriptorByName(String name)
    {        
        try {
            return getEjbByName(name);
        } catch(IllegalArgumentException iae) {
            // Bundle doesn't contain ejb with the given name.
            return null;
        }
    }

    /**
    * Returns my name.
    */

    @Override
    public String getName() {
    if ("".equals(super.getName())) {
        super.setName("Ejb1");
    }
    return super.getName();
    }

        // START OF IASRI 4645310 
    /**
     * Sets the unique id for a stand alone ejb module. It traverses through 
     * all the ejbs in this stand alone module and sets the unique id for
     * each of them. The traversal order is done in ascending element order.
     *
     * <p> Note: This method will not be called for application.
     *
     * @param    id    unique id for stand alone module
     */
    public void setUniqueId(long id) 
    {
        uniqueId  = id;

        // First sort the beans in alphabetical order.
        EjbDescriptor[] descs = ejbs.toArray(new EjbDescriptor[ejbs.size()]);


        // The sorting algorithm used by this api is a modified mergesort.
        // This algorithm offers guaranteed n*log(n) performance, and 
        // can approach linear performance on nearly sorted lists. 
        Arrays.sort(descs, 
            new Comparator<EjbDescriptor>() {
                @Override
                public int compare(EjbDescriptor o1, EjbDescriptor o2) {
                    return o2.getName().compareTo(o1.getName());
                }
            }
        );

        for (int i=0; i<descs.length; i++)
        {
            // 2^16 beans max per stand alone module
            descs[i].setUniqueId( (id | i) );
        }
    }

    /**
     * Returns the unique id used in a stand alone ejb module.
     * For application, this will return zero.
     *
     * @return    the unique if used in stand alone ejb module
     */
    public long getUniqueId()
    {
        return uniqueId;
    }

    public static int getIdFromEjbId(long ejbId)
    {
    long id = ejbId >> 32;  
    return (int)id;
    }
    
    /**
     * @return true if this bundle descriptor defines web service clients
     */
    @Override
    public boolean hasWebServiceClients() {
        for (EjbDescriptor next : getEjbs()) {
            Collection serviceRefs = next.getServiceReferenceDescriptors();
            if (!(serviceRefs.isEmpty())) {
                return true;
            }
        }
        return false;
    }  
    
    /**
     * @return a set of service-ref from ejbs contained in this bundle this bundle or empty set
     * if none
     */
    @Override
    public Set<ServiceReferenceDescriptor> getEjbServiceReferenceDescriptors() {
        Set<ServiceReferenceDescriptor> serviceRefs = new OrderedSet<ServiceReferenceDescriptor>();
        for (EjbDescriptor next : getEjbs()) {
            serviceRefs.addAll(next.getServiceReferenceDescriptors());
        }
        return serviceRefs;        
    }    
    
    /** 
    * Returns a formatted String representing my state.
    */
    @Override
    public void print(StringBuffer toStringBuffer) {
    toStringBuffer.append("EjbBundleDescriptor\n");
        super.print(toStringBuffer);
        if (cmpResourceReference!=null) {
            toStringBuffer.append("\ncmp resource ");
            cmpResourceReference.print(toStringBuffer);
        }
    toStringBuffer.append("\nclient JAR ").append(getEjbClientJarUri());
        for (Descriptor o : getEjbs()) {
            toStringBuffer.append("\n------------\n");
            o.print(toStringBuffer);
            toStringBuffer.append("\n------------");
        }
    }
    
    @Override
    public DescriptorVisitor getTracerVisitor() {
        return new EjbBundleTracerVisitor();
    }


    /**
     * @return the visitor for this bundle descriptor
     */
    @Override
    public ComponentVisitor getBundleVisitor() {
        return new EjbBundleValidator();
    }

    /** 
     * visit the descriptor and all sub descriptors with a DOL visitor implementation
     * 
     * @param aVisitor a visitor to traverse the descriptors
     */
    @Override
    public void visit(DescriptorVisitor aVisitor) {
        if (aVisitor instanceof EjbBundleVisitor ||
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
        return DOLUtils.ejbType();
    }  

    public void setPersistenceManagerInuse(String id,String ver)
    {
    pm_inuse=new PersistenceManagerInUse(id, ver);
        if (_logger.isLoggable(Level.FINE))
        _logger.fine("***IASEjbBundleDescriptor"
                + ".setPersistenceManagerInUse done -#- ");
    }
    
    public void setPersistenceManagerInUse(PersistenceManagerInUse inuse) {
    pm_inuse = inuse;
    }
        
    public PersistenceManagerInUse getPersistenceManagerInUse()
    {
        return pm_inuse;
    }
                
    public void addPersistenceManager(IASPersistenceManagerDescriptor pmDesc)
    {
        if (configured_pms==null) {
            configured_pms=new Vector();
        }
        configured_pms.add(pmDesc);
        if (_logger.isLoggable(Level.FINE))
            _logger.fine("***IASEjbBundleDescriptor"
               + ".addPersistenceManager done -#- ");
    }
        
    public IASPersistenceManagerDescriptor getPreferredPersistenceManager()
    {
        boolean debug = _logger.isLoggable(Level.FINE);

        if (configured_pms == null || configured_pms.size() == 0) {
            // return the default persistence manager descriptor
            return null;
        }

        String pminuse_id   = pm_inuse.get_pm_identifier().trim();
        String pminuse_ver  = pm_inuse.get_pm_version().trim();
        if (debug) {
             _logger.fine("IASPersistenceManagerDescriptor.getPreferred - inid*" + 
                pminuse_id.trim() + "*"); 
             _logger.fine("IASPersistenceManagerDescriptor.getPreferred - inver*" + 
                pminuse_ver.trim() + "*"); 
        }

        int size = configured_pms.size();
        for(int i = 0; i < size; i++) {
            IASPersistenceManagerDescriptor pmdesc=(IASPersistenceManagerDescriptor)configured_pms.elementAt(i);
        String pmdesc_id    = pmdesc.getPersistenceManagerIdentifier();
        String pmdesc_ver   = pmdesc.getPersistenceManagerVersion();

            if (debug) {
            _logger.fine("IASPersistenceManagerDescriptor.getPreferred - pmid*" + 
                    pmdesc_id.trim() + "*"); 
            _logger.fine("IASPersistenceManagerDescriptor.getPreferred - pmver*" + 
                    pmdesc_ver.trim() + "*"); 
            }


            if( ((pmdesc_id.trim()).equals(pminuse_id)) && 
                ((pmdesc_ver.trim()).equals(pminuse_ver)) ) {

                if (debug)
            _logger.fine("***IASEjbBundleDescriptor.getPreferredPersistenceManager done -#- ");

                return pmdesc;
        }
    }
    throw new IllegalArgumentException(localStrings.getLocalString(
       "enterprise.deployment.nomatchingpminusefound",
       "No PersistenceManager found that matches specified PersistenceManager in use."));
    }

    public Vector getPersistenceManagers()
    {
        if (_logger.isLoggable(Level.FINE))
        _logger.fine("***IASEjbBundleDescriptor.getPersistenceManagers done -#- ");
    return configured_pms;
    }
    
    public void addSecurityRoleMapping(SecurityRoleMapping roleMapping) {
        roleMaps.add(roleMapping);
    }

    public List<SecurityRoleMapping> getSecurityRoleMappings() {
        return roleMaps;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<? extends PersistenceUnitDescriptor>
            findReferencedPUs() {
        Collection<PersistenceUnitDescriptor> pus =
                new HashSet<PersistenceUnitDescriptor>();
        // Iterate through all the ejbs
        for (EjbDescriptor ejb : getEjbs()) {
            pus.addAll(findReferencedPUsViaPURefs(ejb));
            pus.addAll(findReferencedPUsViaPCRefs(ejb));
        }

        // Add bundle level artifacts added by e.g. CDDI
        for (EntityManagerFactoryReference emfRef : getEntityManagerFactoryReferenceDescriptors()) {
            pus.add( findReferencedPUViaEMFRef(emfRef) );
        }

        for (EntityManagerReference emRef : getEntityManagerReferenceDescriptors()) {
            pus.add( findReferencedPUViaEMRef(emRef) );
        }
        return pus;
    }

/**
     * Returns the generated XML directory feturn the set of ejb references this ejb declares.
     */
    @Override
    public Set<EjbReference> getEjbReferenceDescriptors() {
        return ejbReferences;
    }

    /**
     * Adds a reference to another ejb to me.
     */

    @Override
    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
        ejbReferences.add(ejbReference);
        ejbReference.setReferringBundleDescriptor(this);
    }

    @Override
    public void removeEjbReferenceDescriptor(EjbReference ejbReference) {
        ejbReferences.remove(ejbReference);
    }

    /**
     * Return a reference to another ejb by the same name or throw an IllegalArgumentException.
     */
    public EjbReference getEjbReferenceByName(String name) {
        return (EjbReferenceDescriptor) getEjbReference(name);
    }

    @Override
    public EjbReference getEjbReference(String name) {
        for (EjbReference er : getEjbReferenceDescriptors()) {
            if (er.getName().equals(name)) {
                return er;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnoejbrefbyname",
                "This ejb jar [{0}] has no ejb reference by the name of [{1}] ", new Object[]{getName(), name}));
    }

    @Override
    public Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors() {
        return serviceReferences;
    }

    @Override
    public void addServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceRef.setBundleDescriptor(this);
        serviceReferences.add(serviceRef);
    }

    @Override
    public void removeServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceReferences.remove(serviceRef);
    }

    /**
     * Looks up an service reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    @Override
    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
        for (Iterator itr = getServiceReferenceDescriptors().iterator();
             itr.hasNext();) {
            ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor)
                    itr.next();
            if (srd.getName().equals(name)) {
                return srd;
            }
        }
        throw new IllegalArgumentException("No service ref of name " + name);
    }

    @Override
    public Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors() {
        return messageDestReferences;
    }

    @Override
    public void addMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor messageDestRef) {
        messageDestRef.setReferringBundleDescriptor(this);
        messageDestReferences.add(messageDestRef);
    }

    @Override
    public void removeMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor msgDestRef) {
        messageDestReferences.remove(msgDestRef);
    }

    /**
     * Looks up an message destination reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    @Override
    public MessageDestinationReferenceDescriptor
        getMessageDestinationReferenceByName(String name) {

        for (MessageDestinationReferenceDescriptor mdr : messageDestReferences) {
            if (mdr.getName().equals(name)) {
                return mdr;
            }
        }
        throw new IllegalArgumentException("No message destination ref of name " + name);
    }

    /**
     * Return the set of resource environment references this ejb declares.
     */
    @Override
    public Set<ResourceEnvReferenceDescriptor> getResourceEnvReferenceDescriptors() {
        return resourceEnvReferences;
    }

    @Override
    public void addResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvReference) {
        resourceEnvReferences.add(resourceEnvReference);
    }

    @Override
    public void removeResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvReference) {
        resourceEnvReferences.remove(resourceEnvReference);
    }

    /**
     * Return a reference to another ejb by the same name or throw an IllegalArgumentException.
     */
    @Override
    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name) {
        for (Iterator itr = getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceEnvReferenceDescriptor jdr = (ResourceEnvReferenceDescriptor) itr.next();
            if (jdr.getName().equals(name)) {
                return jdr;

            }
        }
        throw new IllegalArgumentException("No resource env ref of name " + name);
    }


    /**
     * Return the set of resource references this ejb declares.
     */
    @Override
    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
        return resourceReferences;
    }
    /**
     * Adds a resource reference to me.
     */
    @Override
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        resourceReferences.add(resourceReference);
    }

    /**
     * Removes the given resource reference from me.
     */
    @Override
    public void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        resourceReferences.remove(resourceReference);
    }

    /**
     * Return the resource object corresponding to the supplied name or throw an illegal argument exception.
     */
    @Override
    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
        for (Iterator itr = getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceReferenceDescriptor next = (ResourceReferenceDescriptor) itr.next();
            if (next.getName().equals(name)) {
                return next;
            }
        }
        throw new IllegalArgumentException("no resource ref of name " + name);
    }

    /**
     * Returns the environment property object searching on the supplied key.
     * throws an illegal argument exception if no such environment property exists.
     */
    @Override
    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
        for (Iterator itr = getEnvironmentProperties().iterator(); itr.hasNext();) {
            EnvironmentProperty ev = (EnvironmentProperty) itr.next();
            if (ev.getName().equals(name)) {
                return ev;
            }
        }
        throw new IllegalArgumentException("no env-entry of name " + name);
    }

    /**
     * Return a copy of the structure holding the environment properties.
     */
    @Override
    public Set<EnvironmentProperty> getEnvironmentProperties() {
        return environmentProperties;
    }

    @Override
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
        environmentProperties.add(environmentProperty);
    }

    /**
     * Removes the given environment property from me.
     */

    @Override
    public void removeEnvironmentProperty(EnvironmentProperty environmentProperty) {
        getEnvironmentProperties().remove(environmentProperty);

    }

    @Override
    public Set<EntityManagerFactoryReferenceDescriptor>
        getEntityManagerFactoryReferenceDescriptors() {

        return entityManagerFactoryReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    @Override
    public EntityManagerFactoryReferenceDescriptor
        getEntityManagerFactoryReferenceByName(String name) {
        for (EntityManagerFactoryReferenceDescriptor next :
                getEntityManagerFactoryReferenceDescriptors()) {
            if (next.getName().equals(name)) {
                return next;
            }
        }
        throw new IllegalArgumentException("No entity manager factory reference of name " + name);
    }

    @Override
    public void addEntityManagerFactoryReferenceDescriptor
            (EntityManagerFactoryReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        entityManagerFactoryReferences.add(reference);
    }

    @Override
    public Set<EntityManagerReferenceDescriptor>
        getEntityManagerReferenceDescriptors() {

        return entityManagerReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    @Override
    public EntityManagerReferenceDescriptor
        getEntityManagerReferenceByName(String name) {
        for (EntityManagerReferenceDescriptor next :
                getEntityManagerReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
        throw new IllegalArgumentException("No entity manager reference of name " + name);
    }

    @Override
    public void addEntityManagerReferenceDescriptor
            (EntityManagerReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        getEntityManagerReferenceDescriptors().add(reference);
    }

    @Override
    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        return (getInjectableResourcesByClass(className, this));
    }

    @Override
    public InjectionInfo getInjectionInfoByClass(Class clazz) {
        return (getInjectionInfoByClass(clazz, this));
    }

    public Boolean getDisableNonportableJndiNames() {
        return disableNonportableJndiNames;
    }

    public void setDisableNonportableJndiNames(String disableOrNot) {
        disableNonportableJndiNames = Boolean.valueOf(disableOrNot);
    }

    //
    // There is still some redundant DOL processing of the modules that can result in these
    // being called so just treat them as no-ops.
    //

    //@Override
    public Set<LifecycleCallbackDescriptor> getAroundConstructDescriptors() {
        return new HashSet<LifecycleCallbackDescriptor>();
    }

    //@Override
    public void addAroundConstructDescriptor(LifecycleCallbackDescriptor
        aroundConstructDesc) {
        // no-op
    }

    //@Override
    public LifecycleCallbackDescriptor
        getAroundConstructDescriptorByClass(String className) {
        return null;
    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors() {
        return new HashSet<LifecycleCallbackDescriptor>();
    }

    @Override
    public void addPostConstructDescriptor(LifecycleCallbackDescriptor
        postConstructDesc) {
        // no-op
    }

    @Override
    public LifecycleCallbackDescriptor
        getPostConstructDescriptorByClass(String className) {
        return null;
    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return new HashSet<LifecycleCallbackDescriptor>();
    }

    @Override
    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor preDestroyDesc) {
        // no-op
    }

    @Override
    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        return null;
    }

    //
    //  end no-op PostConstruct/PreDestroy methods
    //
    public String getEnterpriseBeansProperty(String key) {
        for(NameValuePairDescriptor p : enterpriseBeansProperties) {
            if(p.getName().equals(key)) {
                return p.getValue();
            }
        }
        return null;
    }

    public void addEnterpriseBeansProperty(NameValuePairDescriptor newProp) {
        enterpriseBeansProperties.add(newProp);
    }

    public List<NameValuePairDescriptor> getEnterpriseBeansProperties() {
        return enterpriseBeansProperties;
    }

    @Override
    protected List<InjectionCapable> getInjectableResourcesByClass(String className,
            JndiNameEnvironment jndiNameEnv) {
        return super.getInjectableResourcesByClass(className, jndiNameEnv);
    }

}
