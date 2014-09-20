/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.types.EntityManagerFactoryReference;
import com.sun.enterprise.deployment.types.EntityManagerReference;
import com.sun.enterprise.deployment.util.ComponentVisitor;
import com.sun.enterprise.deployment.util.ApplicationValidator;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.node.RootXMLNode;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.security.common.Role;

import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * I am an abstract class representing all the deployment information common
 * to all component container structures held by an application.
 *
 * @author Danny Coward
 */

public abstract class BundleDescriptor extends RootDeploymentDescriptor implements Roles {

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(BundleDescriptor.class);

    private final static String DEPLOYMENT_DESCRIPTOR_DIR = "META-INF";
    private final static String WSDL_DIR = "wsdl";

     // the spec versions we should start to look at annotations
    private final static double ANNOTATION_RAR_VER = 1.6;
    private final static double ANNOTATION_EJB_VER = 3.0;
    private final static double ANNOTATION_WAR_VER = 2.5;
    private final static double ANNOTATION_CAR_VER = 5.0;

    private boolean fullFlag = false;
    private boolean fullAttribute = false;

    private final String PERSISTENCE_UNIT_NAME_SEPARATOR = "#";

    private Application application;
    private Set<Role> roles;
    private Set<MessageDestinationDescriptor> messageDestinations = new HashSet<MessageDestinationDescriptor>();
    private WebServicesDescriptor webServices = new WebServicesDescriptor();

    private Set<ManagedBeanDescriptor> managedBeans = new HashSet<ManagedBeanDescriptor>();

    // Physical entity manager factory corresponding to the unit name of 
    // each module-level persistence unit.  Only available at runtime.
    private Map<String, EntityManagerFactory> entityManagerFactories =
            new HashMap<String, EntityManagerFactory>();


    // table for caching InjectionInfo with the class name as index
    private Hashtable<InjectionInfoCacheKey, InjectionInfo> injectionInfos =
            new Hashtable<InjectionInfoCacheKey, InjectionInfo>();

    private boolean policyModified = false;

    private String compatValue;

    private boolean keepState = false; 

    protected HashMap<String, RootXMLNode> rootNodes = new HashMap<String, RootXMLNode>();

    /**
     * Construct a new BundleDescriptor
     */
    public BundleDescriptor() {
        super();
        webServices.setBundleDescriptor(this);
    }

    /**
     * Construct a new BundleDescriptor with a name and description
     */
    public BundleDescriptor(String name, String description) {
        super(name, description);
        webServices.setBundleDescriptor(this);
    }

    /**
     * Sets the application to which I belong.
     */
    public void setApplication(Application a) {
        application = a;
        for (List<? extends RootDeploymentDescriptor> extensionsByType : extensions.values()) {
            for (RootDeploymentDescriptor extension : extensionsByType) {
                if (extension instanceof BundleDescriptor) {
                    ((BundleDescriptor) extension).setApplication(a);
                }
            }
        }
    }

    public void addBundleDescriptor(BundleDescriptor bundleDescriptor) {
        getRoles().addAll(bundleDescriptor.getRoles());
        for (MessageDestinationDescriptor mdDesc: bundleDescriptor.getMessageDestinations()) {
            addMessageDestination(mdDesc);
        }
    }

    /**
     * Return true if the other bundle descriptor comes from the same module
     * @param other the other bundle descriptor
     * @return true if co-packaged in the same module
     */
    public boolean isPackagedAsSingleModule(BundleDescriptor other) {
        return getModuleDescriptor().equals(other.getModuleDescriptor());
    }

    /**
     * @return true if this module is an application object
     */
    public boolean isApplication() {
        return false;
    }

    /**
     * @return true if this module is a standalone deployment unit
     */
    public boolean isStandalone() {
        return application.isVirtual();
    }

    /**
     * The application to which I belong, or none if I am standalone.
     */
    public Application getApplication() {
        return application;
    }

    public void addRootNode(String ddPath, RootXMLNode rootNode) {
        rootNodes.put(ddPath, rootNode);
    }

    public RootXMLNode getRootNode(String ddPath) {
        return rootNodes.get(ddPath);
    }

    /**
     * Set the physical entity manager factory for a persistence unit
     * within this module.
     */
    public void addEntityManagerFactory(String unitName,
                                        EntityManagerFactory emf) {

        entityManagerFactories.put(unitName, emf);
    }


    /**
     * Retrieve the physical entity manager factory associated with the
     * unitName of a persistence unit within this module.   Returns null if
     * no matching entry is found.
     */
    public EntityManagerFactory getEntityManagerFactory(String unitName) {

        return entityManagerFactories.get(unitName);
    }

    /**
     * Returns the set of physical entity manager factories associated
     * with persistence units in this module.
     */
    public Set<EntityManagerFactory> getEntityManagerFactories() {

        return new HashSet<EntityManagerFactory>
                (entityManagerFactories.values());

    }

    public void addManagedBean(ManagedBeanDescriptor desc) {
        if (!hasManagedBeanByBeanClass(desc.getBeanClassName())) {
            managedBeans.add(desc);
            desc.setBundle(this);
        }
    }

    public boolean hasManagedBeanByBeanClass(String beanClassName) {
        ManagedBeanDescriptor descriptor = getManagedBeanByBeanClass(beanClassName);
        return (descriptor != null);
    }

    public ManagedBeanDescriptor getManagedBeanByBeanClass(String beanClassName) {
        ManagedBeanDescriptor match = null;

        for(ManagedBeanDescriptor next : managedBeans) {
            if( beanClassName.equals(next.getBeanClassName()) ) {
                match = next;
                break;
            }
        }

        return match;
    }

    public Set<ManagedBeanDescriptor> getManagedBeans() {
        return new HashSet<ManagedBeanDescriptor>(managedBeans);
    }

    /**
     * Return web services defined for this module.  Not applicable for
     * application clients.
     */
    public WebServicesDescriptor getWebServices() {
        return webServices;
    }

    public WebServiceEndpoint getWebServiceEndpointByName(String name) {
        return webServices.getEndpointByName(name);
    }

    /**
     * @return true if this bundle descriptor defines web service clients
     */
    public boolean hasWebServiceClients() {
        return false;
    }

    /**
     * @return true if this bundle descriptor defines web services
     */
    public boolean hasWebServices() {
        return getWebServices().hasWebServices();
    }


    /**
     * Return the Set of message destinations I have
     */
    public Set<MessageDestinationDescriptor> getMessageDestinations() {
        if (messageDestinations == null) {
            messageDestinations = new HashSet<MessageDestinationDescriptor>();
        }
        return messageDestinations;
    }

    /**
     * Returns true if I have an message destiation by that name.
     */
    public boolean hasMessageDestinationByName(String name) {
        for (MessageDestinationDescriptor mtd : getMessageDestinations()) {
            if (mtd.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a message destination descriptor that I have by the
     * same name, or throws an IllegalArgumentException
     */
    public MessageDestinationDescriptor getMessageDestinationByName
            (String name) {
        for (MessageDestinationDescriptor mtd : getMessageDestinations()) {
            if (mtd.getName().equals(name)) {
                return mtd;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionmessagedestbundle",
                "Referencing error: this bundle has no message destination of name: {0}", new Object[]{name}));
    }

    /**
     * Add a message destination to me.
     */
    public void addMessageDestination(MessageDestinationDescriptor
            messageDestination) {
        messageDestination.setBundleDescriptor(this);
        this.getMessageDestinations().add(messageDestination);
    }

    /**
     * Remove the given message destination descriptor from my (by equality).
     */
    public void removeMessageDestination(MessageDestinationDescriptor msgDest) {
        msgDest.setBundleDescriptor(null);
        this.getMessageDestinations().remove(msgDest);
    }

    /**
     * Return the set of com.sun.enterprise.deployment.Role objects
     * I have plus the ones from application
     */
    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new OrderedSet<Role>();
        }
        if (application != null) {
            roles.addAll(application.getAppRoles());
        }

        return roles;
    }

    /**
     * Adds a role object to me.
     */
    public void addRole(Role role) {
        this.getRoles().add(role);
    }

    /**
     * Adds a Role object based on the supplied SecurityRoleDescriptor.
     * <p/>
     * A change in SecurityRoleNode to fix bug 4933385 causes the DOL to use SecurityRoleDescriptor, rather
     * than Role, to contain information about security roles.  To minimize the impact on BundleDescriptor,
     * this method has been added for use by the DOL as it processes security-role elements.
     * <p/>
     * This method creates a new Role object based on the characteristics of the SecurityRoleDescriptor
     * and then delegates to addRole(Role) to preserve the rest of the behavior of this class.
     *
     * @param descriptor SecurityRoleDescriptor that describes the username and description of the role
     */
    public void addRole(SecurityRoleDescriptor descriptor) {
        Role role = new Role(descriptor.getName());
        role.setDescription(descriptor.getDescription());
        this.addRole(role);
    }

    /**
     * Removes a role object from me.
     */
    public void removeRole(Role role) {
        this.getRoles().remove(role);
    }

    /**
     * Utility method for iterating the set of named descriptors in the supplied nameEnvironment
     */
    protected Collection getNamedDescriptorsFrom(JndiNameEnvironment nameEnvironment) {
        Collection namedDescriptors = new Vector();
        for (Iterator itr = nameEnvironment.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceReferenceDescriptor resourceReference = (ResourceReferenceDescriptor) itr.next();
            namedDescriptors.add(resourceReference);
        }
        for (Iterator itr = nameEnvironment.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReferenceDescriptor ejbReference = (EjbReferenceDescriptor) itr.next();
            namedDescriptors.add(ejbReference);
        }
        for (Iterator itr = nameEnvironment.getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceEnvReferenceDescriptor resourceEnvRef =
                    (ResourceEnvReferenceDescriptor) itr.next();
            namedDescriptors.add(resourceEnvRef);
        }

        return namedDescriptors;
    }

    /**
     * Utility method for iterating the set of NameReference pairs in the supplied nameEnvironment
     */
    protected Vector<NamedReferencePair> getNamedReferencePairsFrom(JndiNameEnvironment nameEnvironment) {
        Vector<NamedReferencePair> pairs = new Vector<NamedReferencePair>();
        for (Iterator itr = nameEnvironment.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceReferenceDescriptor resourceReference = (ResourceReferenceDescriptor) itr.next();
            pairs.add(NamedReferencePair.createResourceRefPair((Descriptor) nameEnvironment, resourceReference));
        }
        for (Iterator itr = nameEnvironment.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReferenceDescriptor ejbReference = (EjbReferenceDescriptor) itr.next();
            pairs.add(NamedReferencePair.createEjbRefPair((Descriptor) nameEnvironment, ejbReference));
        }
        for (Iterator itr = nameEnvironment.getResourceEnvReferenceDescriptors().iterator(); itr.hasNext();) {
            ResourceEnvReferenceDescriptor resourceEnvRef =
                    (ResourceEnvReferenceDescriptor) itr.next();
            pairs.add(NamedReferencePair.createResourceEnvRefPair((Descriptor) nameEnvironment, resourceEnvRef));
        }

        return pairs;
    }

    private static final class InjectionInfoCacheKey {
        String beanName;
        Class clazz;
        int hc;

        InjectionInfoCacheKey(String beanName, Class clazz) {
            this.beanName = beanName;
            this.clazz = clazz;
            hc = beanName.hashCode();
        }

        public int hashCode() {
            return hc;
        }

        public boolean equals(Object o) {
            boolean result = false;
            if (o instanceof InjectionInfoCacheKey) {
                InjectionInfoCacheKey other = (InjectionInfoCacheKey) o;
                if (hc == other.hc) {
                    return ((clazz == other.clazz) && (beanName.equals(other.beanName)));
                }
            }
            return result;
        }
    }

    public InjectionInfo getInjectionInfoByClass(Class clazz,
                                                 JndiNameEnvironment jndiNameEnv) {

        // first look in the cache
        InjectionInfoCacheKey key = null;
        if (jndiNameEnv instanceof EjbDescriptor) {
            EjbDescriptor jndiEjbDesc = (EjbDescriptor) jndiNameEnv;
            key = new InjectionInfoCacheKey(jndiEjbDesc.getName(), clazz);
        } else {
            key = new InjectionInfoCacheKey(clazz.getName(), clazz);
        }

        InjectionInfo injectionInfo = injectionInfos.get(key);
        if (injectionInfo != null) {
            return injectionInfo;
        }

        String className = clazz.getName();

        // if it's not in the cache, create a new one
        LifecycleCallbackDescriptor postConstructDesc =
                getPostConstructDescriptorByClass(className, jndiNameEnv);
        String postConstructMethodName = (postConstructDesc != null) ?
                postConstructDesc.getLifecycleCallbackMethod() : null;
        LifecycleCallbackDescriptor preDestroyDesc =
                getPreDestroyDescriptorByClass(className, jndiNameEnv);
        String preDestroyMethodName = (preDestroyDesc != null) ?
                preDestroyDesc.getLifecycleCallbackMethod() : null;
        injectionInfo = new InjectionInfo(className,
                postConstructMethodName, preDestroyMethodName,
                getInjectableResourcesByClass(className,
                        jndiNameEnv));

        // store it in the cache and return
        injectionInfos.put(key, injectionInfo);
        return injectionInfo;
    }

    public LifecycleCallbackDescriptor
    getPostConstructDescriptorByClass(String className,
                                      JndiNameEnvironment jndiNameEnv) {
        for (LifecycleCallbackDescriptor next :
                jndiNameEnv.getPostConstructDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    public LifecycleCallbackDescriptor
    getPreDestroyDescriptorByClass(String className,
                                   JndiNameEnvironment jndiNameEnv) {
        for (LifecycleCallbackDescriptor next :
                jndiNameEnv.getPreDestroyDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    public List<InjectionCapable> getInjectableResources
            (JndiNameEnvironment jndiNameEnv) {

        List<InjectionCapable> injectables =
                new LinkedList<InjectionCapable>();

        addJndiNameEnvironmentInjectables(jndiNameEnv, injectables);

        return injectables;
    }

    private void addJndiNameEnvironmentInjectables(JndiNameEnvironment jndiNameEnv,
                                                   List<InjectionCapable> injectables) {

        Collection allEnvProps = new HashSet();

        for (Iterator envEntryItr =
                jndiNameEnv.getEnvironmentProperties().iterator();
             envEntryItr.hasNext();) {
            EnvironmentProperty envEntry = (EnvironmentProperty)
                    envEntryItr.next();

            // Only env-entries that have been assigned a value are
            // eligible for injection.
            // If the jndiNameEnv is an EjbBundleDescriptor then we have to account for this because
            // there can be injection points on classes inside the ejb jar but not accounted for
            // in the deployment descriptor.
            if (envEntry.hasAValue() || ( jndiNameEnv instanceof EjbBundleDescriptor ) ){
                allEnvProps.add(envEntry);
            }
        }

        allEnvProps.addAll(jndiNameEnv.getEjbReferenceDescriptors());
        allEnvProps.addAll(jndiNameEnv.getServiceReferenceDescriptors());
        allEnvProps.addAll(jndiNameEnv.getResourceReferenceDescriptors());
        allEnvProps.addAll(jndiNameEnv.getResourceEnvReferenceDescriptors());
        allEnvProps.addAll(jndiNameEnv.getMessageDestinationReferenceDescriptors());

        allEnvProps.addAll(jndiNameEnv.getEntityManagerFactoryReferenceDescriptors());
        allEnvProps.addAll(jndiNameEnv.getEntityManagerReferenceDescriptors());

        for (Iterator envItr = allEnvProps.iterator(); envItr.hasNext();) {
            InjectionCapable next = (InjectionCapable) envItr.next();
            if (next.isInjectable()) {
                injectables.add(next);
            }
        }

    }

    /**
     * Define implementation of getInjectableResourceByClass here so it
     * isn't replicated across appclient, web, ejb descriptors.
     */
    protected List<InjectionCapable> getInjectableResourcesByClass(String className,
                                  JndiNameEnvironment jndiNameEnv) {
        List<InjectionCapable> injectables =
                new LinkedList<InjectionCapable>();

        for (InjectionCapable next : getInjectableResources(jndiNameEnv)) {
            if (next.isInjectable()) {
                for (InjectionTarget target : next.getInjectionTargets()) {
                    if (target.getClassName().equals(className)) {
                        injectables.add(next);
                    }
                }
            }
        }

        return injectables;
    }

    /**
     * @return the class loader associated with this module
     */
    public ClassLoader getClassLoader() {
        if (classLoader != null) {
            return classLoader;
        }
        if (application != null) {
            return application.getClassLoader();
        }

        return classLoader;
    }

    /**
     * Prints a formatted string representing my state.
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\n");
        super.print(toStringBuffer);
        toStringBuffer.append("\n Roles[] = ").append(roles);
        if (getWebServices().hasWebServices()) {
            toStringBuffer.append("\n WebServices ");
            ((Descriptor) (getWebServices())).print(toStringBuffer);
        }
    }

    /**
     * @return the  type of this bundle descriptor
     */
    public abstract ArchiveType getModuleType();

    /**
     * @return the visitor for this bundle descriptor
     */
    public ComponentVisitor getBundleVisitor() {
        return new ApplicationValidator();
    }

    /**
     * visitor API implementation
     */
    public void visit(ComponentVisitor aVisitor) {
        aVisitor.accept(this);
    }

    /**
     * @return the module ID for this module descriptor
     */
    public String getModuleID() {
        if (moduleID == null) {
            moduleID = getModuleDescriptor().getArchiveUri();
        }
        if (getModuleDescriptor().isStandalone()) {
            return moduleID;
        }
        if (application != null && !application.isVirtual()) {
            return application.getRegistrationName() + "#" + 
                getModuleDescriptor().getArchiveUri();
        } else {
            return moduleID;
        }
    }

    /**
     * @return the deployment descriptor directory location inside
     *         the archive file
     */
    public String getDeploymentDescriptorDir() {
        return DEPLOYMENT_DESCRIPTOR_DIR;
    }


    /**
     * @return the wsdl directory location inside the archive file
     */
    public String getWsdlDir() {
        return getDeploymentDescriptorDir() + "/" + WSDL_DIR;
    }


    /**
     * This method returns all the persistence units that are referenced
     * by this module. Depending on the type of component, a PU can be
     * referenced by one of the four following ways:
     * <persistence-context-ref>, @PersistenceContext,
     * <persistence-unit-ref> and @PersistenceUnit
     * Only EjbBundleDescriptor, ApplicationClientDescriptor and
     * WebBundleDescriptor have useful implementation of this method.
     *
     * @return persistence units that are referenced by this module
     */
    public Collection<? extends PersistenceUnitDescriptor> findReferencedPUs() {
        return Collections.EMPTY_LIST;
    }

    /**
     * helper method: find all PUs referenced via @PersistenceUnit or
     * <persistence-unit-ref>
     */
    protected static Collection<? extends PersistenceUnitDescriptor>
    findReferencedPUsViaPURefs(JndiNameEnvironment component) {
        Collection<PersistenceUnitDescriptor> pus =
                new HashSet<PersistenceUnitDescriptor>();
        for (EntityManagerFactoryReference emfRef :
                component.getEntityManagerFactoryReferenceDescriptors()) {
            PersistenceUnitDescriptor pu = findReferencedPUViaEMFRef(emfRef);
            pus.add(pu);
        }
        return pus;
    }

    protected static PersistenceUnitDescriptor findReferencedPUViaEMFRef(EntityManagerFactoryReference emfRef) {
        final String unitName = emfRef.getUnitName();
        final BundleDescriptor bundle =
                emfRef.getReferringBundleDescriptor();
        PersistenceUnitDescriptor pu = bundle.findReferencedPU(unitName);
        if (pu == null) {
            throw new RuntimeException(localStrings.getLocalString(
                    "enterprise.deployment.exception-unresolved-pu-ref", "xxx", // NOI18N
                    new Object[]{emfRef.getName(),
                            bundle.getName()})
            );
        }
        return pu;
    }

    /**
     * helper method: find all PUs referenced via @PersistenceContext or
     * <persistence-context-ref>
     */
    protected static Collection<? extends PersistenceUnitDescriptor>
    findReferencedPUsViaPCRefs(JndiNameEnvironment component) {
        Collection<PersistenceUnitDescriptor> pus =
                new HashSet<PersistenceUnitDescriptor>();
        for (EntityManagerReference emRef :
                component.getEntityManagerReferenceDescriptors()) {
            PersistenceUnitDescriptor pu = findReferencedPUViaEMRef(emRef);
            pus.add(pu);
        }
        return pus;
    }

    protected static PersistenceUnitDescriptor findReferencedPUViaEMRef(EntityManagerReference emRef) {
        final String unitName = emRef.getUnitName();
        final BundleDescriptor bundle =
                emRef.getReferringBundleDescriptor();
        PersistenceUnitDescriptor pu = bundle.findReferencedPU(unitName);
        if (pu == null) {
            throw new RuntimeException(localStrings.getLocalString(
                    "enterprise.deployment.exception-unresolved-pc-ref", "xxx", // NOI18N
                    new Object[]{emRef.getName(),
                            bundle.getName()})
            );
        }
        if ("RESOURCE_LOCAL".equals(pu.getTransactionType())) { // NOI18N
            throw new RuntimeException(localStrings.getLocalString(
                    "enterprise.deployment.exception-non-jta-container-managed-em", "xxx", // NOI18N
                    new Object[]{emRef.getName(),
                            bundle.getName(),
                            pu.getName()})
            );
        }
        return pu;
    }

    /**
     * It accepts both a quailified (e.g.) "lib/a.jar#FooPU" as well as
     * unqualified name (e.g.) "FooPU". It then searched all the
     * PersistenceUnits that are defined in the scope of this bundle
     * descriptor to see which one matches the give name.
     *
     * @param unitName as used in @PersistenceUnit, @PersistenceContext
     *                 <persistence-context-ref> or <persistence-unit-name>.
     *                 If null, this method returns the default PU, if available.
     *                 The reason it accepts null for default PU is because "" gets converted to
     *                 null in EntityManagerReferenceHandler.processNewEmRefAnnotation.
     * @return PersistenceUnitDescriptor that this unitName resolves to.
     *         Returns null, if unitName could not be resolved.
     */
    public PersistenceUnitDescriptor findReferencedPU(String unitName) {
        if (unitName == null || unitName.length() == 0) { // uses default PU.
            return findDefaultPU();
        } else {
            return findReferencedPU0(unitName);
        }
    }

    /**
     * This method is responsible for finding default persistence unit for
     * a bundle descriptor.
     *
     * @return the default persistence unit for this bundle. returns null,
     *         if there isno PU defined or default can not be calculated because there
     *         are more than 1 PUs defined.
     */
    public PersistenceUnitDescriptor findDefaultPU() {
        // step #1: see if we have only one PU in the local scope.
        PersistenceUnitDescriptor pu = null;
        int totalNumberOfPUInBundle = 0;
        for (PersistenceUnitsDescriptor nextPUs :
                getModuleDescriptor().getDescriptor().getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            for (PersistenceUnitDescriptor nextPU :
                    nextPUs.getPersistenceUnitDescriptors()) {
                pu = nextPU;
                totalNumberOfPUInBundle++;
            }
        }
        if (totalNumberOfPUInBundle == 1) { // there is only one PU in this bundle.
            return pu;
        } else if (totalNumberOfPUInBundle == 0) { // there are no PUs in this bundle.
            // step #2: see if we have only one PU in the ear.
            int totalNumberOfPUInEar = 0;
            for (PersistenceUnitsDescriptor nextPUs :
                    getApplication().getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
                for (PersistenceUnitDescriptor nextPU :
                        nextPUs.getPersistenceUnitDescriptors()) {
                    pu = nextPU;
                    totalNumberOfPUInEar++;
                }
            }
            if (totalNumberOfPUInEar == 1) {
                return pu;
            }
        }
        return null;
    }

    /**
     * Internal method.
     * This method is used to find referenced PU with a given name.
     * It does not accept null or empty unit name.
     *
     * @param unitName
     * @return
     */
    private PersistenceUnitDescriptor findReferencedPU0(String unitName) {
        int separatorIndex =
                unitName.lastIndexOf(PERSISTENCE_UNIT_NAME_SEPARATOR);

        if (separatorIndex != -1) { // qualified name
            // uses # => must be defined in a utility jar at ear scope.
            String unqualifiedUnitName =
                    unitName.substring(separatorIndex + 1);
            String path = unitName.substring(0, separatorIndex);
            // it' necessary to call getTargetUri as that takes care of
            // converting ././b to canonical forms.
            String puRoot = getTargetUri(this, path);
            final PersistenceUnitsDescriptor pus =
                    getApplication().getExtensionsDescriptors(PersistenceUnitsDescriptor.class, puRoot);
            if (pus != null) {
                for (PersistenceUnitDescriptor pu :
                        pus.getPersistenceUnitDescriptors()) {
                    if (pu.getName().equals(unqualifiedUnitName)) {
                        return pu;
                    }
                }
            }
        } else { // uses unqualified name.
            // first look to see if there is a match with unqualified name,
            // b'cos local scope takes precedence.
            Map<String, PersistenceUnitDescriptor> visiblePUs =
                    getVisiblePUs();
            PersistenceUnitDescriptor result = visiblePUs.get(unitName);
            if (result != null) return result;

            // next look to see if there is unique match in ear scope.
            int sameNamedEarScopedPUCount = 0;
            Set<Map.Entry<String, PersistenceUnitDescriptor>> entrySet =
              visiblePUs.entrySet();
            Iterator <Map.Entry<String, PersistenceUnitDescriptor>> entryIt =
              entrySet.iterator();
            while (entryIt.hasNext()) {
              Map.Entry<String, PersistenceUnitDescriptor> entry =
                entryIt.next();
              String s = entry.getKey();
              int idx = s.lastIndexOf(PERSISTENCE_UNIT_NAME_SEPARATOR);
              if (idx != -1 // ear scoped
                  && s.substring(idx + 1).matches(unitName)) {
                result = entry.getValue();
                sameNamedEarScopedPUCount++;
              }
            }
            // if there are more than one ear scoped PU with same name (this
            // is possible when PU is inside two different library jar),
            // then user can not use unqualified name.
            if (sameNamedEarScopedPUCount == 1) {
                return result;
            }
        }
        return null;
    }

    /**
     * This method returns all the PUs that are defined in this bundle as well
     * as the PUs defined in the ear level. e.g. for the following ear:
     * ear/lib/a.jar#defines FooPU
     * /lib/b.jar#defines FooPU
     * ejb.jar#defines FooPU
     * for the EjbBundleDescriptor (ejb.jar), the map will contain
     * {(lib/a.jar#FooPU, PU1), (lib/b.jar#FooPU, PU2), (FooPU, PU3)}.
     *
     * @return a map of names to PUDescriptors that are visbible to this
     *         bundle descriptor. The name is a qualified name for ear scoped PUs
     *         where as it is in unqualified form for local PUs.
     */
    public Map<String, PersistenceUnitDescriptor> getVisiblePUs() {
        Map<String, PersistenceUnitDescriptor> result =
                new HashMap<String, PersistenceUnitDescriptor>();

        // local scoped PUs
        for (PersistenceUnitsDescriptor pus :
                getModuleDescriptor().getDescriptor().getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            for (PersistenceUnitDescriptor pu :
                    pus.getPersistenceUnitDescriptors()) {
                // for local PUs, use unqualified name.
                result.put(pu.getName(), pu);
            }
        }

        // ear scoped PUs
        final Application application = getApplication();
        if (application != null) {
            for (PersistenceUnitsDescriptor pus :
                    application.getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
                for (PersistenceUnitDescriptor pu :
                        pus.getPersistenceUnitDescriptors()) {
                    // use fully qualified name for ear scoped PU
                    result.put(pu.getPuRoot() + PERSISTENCE_UNIT_NAME_SEPARATOR + pu.getName(), pu);
                }
            }
        }
        return result;
    }

    /**
     * Get the uri of a target based on a source module and a a relative uri
     * from the perspective of that source module.
     *
     * @param origin            bundle descriptor within an application
     * @param relativeTargetUri relative uri from the given bundle
     *                          descriptor
     * @return target uri
     */
    private String getTargetUri(BundleDescriptor origin,
                                String relativeTargetUri) {
        try {
            String archiveUri = origin.getModuleDescriptor().getArchiveUri();
            return new URI(archiveUri).resolve(relativeTargetUri).getPath();
        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        }
    }


    public String getModuleName() {
        String moduleName = null;

        // for standalone jars, return its registration name
        // for applications, return the module uri
        if (getApplication().isVirtual()) {
            moduleName = getApplication().getRegistrationName();
        } else {
            moduleName = getModuleDescriptor().getArchiveUri();
        }
        return moduleName;
    }

    // return a short unique representation of this BundleDescriptor
    public String getUniqueFriendlyId() {
        return FileUtils.makeFriendlyFilename(getModuleName());
    }

    public boolean isPolicyModified() {
        return policyModified;
    }

    public void setPolicyModified(boolean policyModified) {
        this.policyModified = policyModified;
    }

    public String getCompatibility() {
        return compatValue;
    }

    public void setCompatibility(String compatValue) {
        this.compatValue = compatValue;
    }

    public boolean getKeepState() {
        return keepState;
    }

    public void setKeepState(String keepStateVal) {
        this.keepState = Boolean.valueOf(keepStateVal);
    }

    /**
     * Sets the full flag of the bundle descriptor. Once set, the annotations
     * of the classes contained in the archive described by this bundle
     * descriptor will be ignored.
     * @param flag a boolean to set or unset the flag
     */
     public void setFullFlag(boolean flag) {
         fullFlag=flag;
     }

    /**
     * Sets the full attribute of the deployment descriptor
     * @param value the full attribute
     */
    public void setFullAttribute(String value) {
        fullAttribute = Boolean.valueOf(value);
    }

    /**
     * Get the full attribute of the deployment descriptor
     * @return the full attribute
     */
    public boolean isFullAttribute() {
        return fullAttribute;
    }

    /**
     * @ return true for following cases:
     *   1. When the full attribute is true. This attribute only applies to
     *      ejb module with schema version equal or later than 3.0;
            web module and schema version equal or later than than 2.5;
            appclient module and schema version equal or later than 5.0.
     *   2. When it's been tagged as "full" when processing annotations.
     *   3. When DD has a version which doesn't allowed annotations.
     *   return false otherwise.
     */
    public boolean isFullFlag() {
        // if the full attribute is true or it's been tagged as full,
        // return true
        if (fullAttribute == true || fullFlag == true) {
            return true;
        }
        return isDDWithNoAnnotationAllowed();
    }


    /**
     * @ return true for following cases:
     *   a. ejb module and schema version earlier than 3.0;
     *   b. web module and schema version earlier than 2.5;
     *   c. appclient module and schema version earlier than 5.0.
     *   d. connector module and schema version earlier than 1.6
     */
    public boolean isDDWithNoAnnotationAllowed() {
        ArchiveType mType = getModuleType();
        if (mType == null) return false;
        double specVersion = Double.parseDouble(getSpecVersion());

            // we do not process annotations for earlier versions of DD
            if ( (mType.equals(DOLUtils.ejbType()) &&
                  specVersion < ANNOTATION_EJB_VER) ||
                 (mType.equals(DOLUtils.warType()) &&
                  specVersion < ANNOTATION_WAR_VER) ||
                 (mType.equals(DOLUtils.carType()) &&
                  specVersion < ANNOTATION_CAR_VER)  ||
                 (mType.equals(DOLUtils.rarType()) &&
                  specVersion < ANNOTATION_RAR_VER)) {
                return true;
            } else {
                return false;
            }
    }

}
