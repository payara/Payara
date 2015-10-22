/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.ComponentPostVisitor;
import com.sun.enterprise.deployment.util.ComponentVisitor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.web.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.security.common.Role;
import org.glassfish.web.deployment.node.WebBundleNode;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.util.WebBundleTracerVisitor;
import org.glassfish.web.deployment.util.WebBundleValidator;
import org.glassfish.web.deployment.util.WebBundleVisitor;

import java.util.*;

 /**
 *
 * The concrete implementation of abstract super class com.sun.enterprise.deployment.WebBundleDescriptor.
 * TODO WebBundleDescriptor could be changed from abstract class to an interface in the future, with this
 * class as its implementation.
 */

public class WebBundleDescriptorImpl extends WebBundleDescriptor {

    private final static String DEPLOYMENT_DESCRIPTOR_DIR = "WEB-INF";

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(WebBundleDescriptor.class);

    private Set<WebComponentDescriptor> webComponentDescriptors;
    private SessionConfig sessionConfig;
    private Set<MimeMapping> mimeMappings;
    private Set<String> welcomeFiles;
    private Set<ErrorPageDescriptor> errorPageDescriptors;
    private Vector<AppListenerDescriptor> appListenerDescriptors;
    private Set<ContextParameter> contextParameters;
    private Set<EjbReference> ejbReferences;
    private Set<ResourceReferenceDescriptor> resourceReferences;
    private Set<ResourceEnvReferenceDescriptor> resourceEnvRefReferences;
    private Set<MessageDestinationReferenceDescriptor> messageDestReferences;
    private Set<ServiceReferenceDescriptor> serviceReferences;

    private Set<LifecycleCallbackDescriptor> postConstructDescs =
            new HashSet<LifecycleCallbackDescriptor>();
    private Set<LifecycleCallbackDescriptor> preDestroyDescs =
            new HashSet<LifecycleCallbackDescriptor>();

    private Set<EntityManagerFactoryReferenceDescriptor>
            entityManagerFactoryReferences =
            new HashSet<EntityManagerFactoryReferenceDescriptor>();

    private Set<EntityManagerReferenceDescriptor>
            entityManagerReferences =
            new HashSet<EntityManagerReferenceDescriptor>();

    private Boolean isDistributable;
    private boolean denyUncoveredHttpMethods = false;
    private Set<SecurityRoleDescriptor> securityRoles;
    private Set<SecurityConstraint> securityConstraints;
    private String contextRoot;
    private LoginConfiguration loginConfiguration;
    private Set<EnvironmentEntry> environmentEntries;
    private LocaleEncodingMappingListDescriptor localeEncodingMappingListDesc = null;
    private JspConfigDescriptorImpl jspConfigDescriptor = null;

    private Vector<ServletFilter> servletFilters = null;
    private Vector<ServletFilterMapping> servletFilterMappings = null;

    private AbsoluteOrderingDescriptor absOrdering = null;

    private SunWebApp sunWebApp = null;

    // An entry here, may be set to indicate additional processing.
    // This entry may be set, for example, by a Deployer.
    //
    private Map<String, String> extensionProperty = null;

    private Map<String, String> jarName2WebFragNameMap  = null;

    // this is for checking whether there are more than one servlets for a given url-pattern
    private Map<String, String> urlPattern2ServletName = null;

    private List<String> orderedLibs = new ArrayList<String>();

    private boolean showArchivedRealPathEnabled = true;

    private int servletReloadCheckSecs = 1;

    private Set<String> conflictedMimeMappingExtensions = null;

    /**
     * Constrct an empty web app [{0}].
     */
    public WebBundleDescriptorImpl() {
    }

    public WebBundleDescriptor createWebBundleDescriptor() {
        return new WebBundleDescriptorImpl();
    }

    /**
     * This method will merge the contents of webComponents.
     * @param webBundleDescriptor
     */
    public void addWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        getWelcomeFilesSet().addAll(webBundleDescriptor.getWelcomeFilesSet());
        addCommonWebBundleDescriptor(webBundleDescriptor);
    }

    public void addDefaultWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        if (getWelcomeFilesSet().size() == 0) {
            getWelcomeFilesSet().addAll(webBundleDescriptor.getWelcomeFilesSet());
        }
        addCommonWebBundleDescriptor(webBundleDescriptor);
    }

    /**
     * This method combines all except welcome file set for two webBundleDescriptors.
     */
    private void addCommonWebBundleDescriptor(WebBundleDescriptor wbd) {
        super.addBundleDescriptor(wbd);

        WebBundleDescriptorImpl webBundleDescriptor = (WebBundleDescriptorImpl) wbd;
        for (WebComponentDescriptor webComponentDesc :webBundleDescriptor.getWebComponentDescriptors())
        {
            // don't modify the original one
            WebComponentDescriptorImpl webComponentDescriptor =
                new WebComponentDescriptorImpl(webComponentDesc);
            // set web bundle to null so that the urlPattern2ServletName
            // of the others will not be changed,
            // see WebComponentDescriptor.getUrlPatternsSet()
            webComponentDescriptor.setWebBundleDescriptor(null);

            List<String> removeUrlPatterns = null;
            for (String urlPattern: webComponentDescriptor.getUrlPatternsSet()) {
                String servletName = null;
                if (urlPattern2ServletName != null) {
                    servletName = urlPattern2ServletName.get(urlPattern);
                }
                if (servletName != null &&
                        (!servletName.equals(webComponentDescriptor.getCanonicalName()))) {
                    // url pattern already exists in current bundle
                    // need to remove the url pattern in current bundle servlet
                    if (removeUrlPatterns == null) {
                        removeUrlPatterns = new ArrayList<String>();
                    }
                    removeUrlPatterns.add(urlPattern);
                }
            }

            if (removeUrlPatterns != null) {
                webComponentDescriptor.getUrlPatternsSet().removeAll(removeUrlPatterns);
            }

            addWebComponentDescriptor(webComponentDescriptor);
        }

        getContextParametersSet().addAll(webBundleDescriptor.getContextParametersSet());

        if (conflictedMimeMappingExtensions == null) {
            conflictedMimeMappingExtensions = webBundleDescriptor.getConflictedMimeMappingExtensions();
        } else {
            conflictedMimeMappingExtensions.addAll(webBundleDescriptor.getConflictedMimeMappingExtensions());
        }
        combineMimeMappings(webBundleDescriptor.getMimeMappingsSet());

        // do not call getErrorPageDescriptorsSet.addAll() as there is special overriding rule
        for (ErrorPageDescriptor errPageDesc : webBundleDescriptor.getErrorPageDescriptorsSet()) {
            addErrorPageDescriptor(errPageDesc);
        }
        getAppListeners().addAll(webBundleDescriptor.getAppListeners());

        if (webBundleDescriptor.isDenyUncoveredHttpMethods()) {
            setDenyUncoveredHttpMethods(true);
        }
        combineSecurityConstraints(getSecurityConstraintsSet(), webBundleDescriptor.getSecurityConstraintsSet());

        // ServletFilters
        combineServletFilters(webBundleDescriptor);
        combineServletFilterMappings(webBundleDescriptor);

        if (getLocaleEncodingMappingListDescriptor() == null) {
            setLocaleEncodingMappingListDescriptor(webBundleDescriptor.getLocaleEncodingMappingListDescriptor());
        }

        if (webBundleDescriptor.getJspConfigDescriptor() != null) {
            JspConfigDescriptorImpl jspConfigDesc = getJspConfigDescriptor();
            if (jspConfigDesc == null) {
                jspConfigDesc = new JspConfigDescriptorImpl();
                setJspConfigDescriptor(jspConfigDesc);
            }
            jspConfigDescriptor.add(webBundleDescriptor.getJspConfigDescriptor());
        }

        // WebServices
        WebServicesDescriptor thisWebServices = this.getWebServices();
        WebServicesDescriptor otherWebServices = webBundleDescriptor.getWebServices();
        for (WebService ws : otherWebServices.getWebServices()) {
            thisWebServices.addWebService(new WebService(ws));
        }

        if (getSessionConfig() == null) {
            setSessionConfig(webBundleDescriptor.getSessionConfig());
        }

        // combine login config with conflict resolution check
        combineLoginConfiguration(webBundleDescriptor);

        Boolean otherIsDistributable = webBundleDescriptor.isDistributable();
        if (otherIsDistributable != null) {
            if (isDistributable != null) {
                setDistributable(isDistributable && otherIsDistributable);
            } else {
                setDistributable(otherIsDistributable);
            }
        }
        combinePostConstructDescriptors(webBundleDescriptor);
        combinePreDestroyDescriptors(webBundleDescriptor);
        addJndiNameEnvironment(webBundleDescriptor);
    }

    public void addJndiNameEnvironment(JndiNameEnvironment env) {

        // combine with conflict resolution check
        combineEnvironmentEntries(env);
        combineResourceReferenceDescriptors(env);
        combineEjbReferenceDescriptors(env);
        combineServiceReferenceDescriptors(env);
        // resource-env-ref
        combineResourceEnvReferenceDescriptors(env);
        combineMessageDestinationReferenceDescriptors(env);
        // persistence-context-ref
        combineEntityManagerReferenceDescriptors(env);
        // persistence-unit-ref
        combineEntityManagerFactoryReferenceDescriptors(env);
        combineAllResourceDescriptors(env);
    }

    public boolean isEmpty() {
        return (webComponentDescriptors == null || webComponentDescriptors.isEmpty());
    }

    /**
     * @return the default version of the deployment descriptor
     *         loaded by this descriptor
     */
    public String getDefaultSpecVersion() {
        return WebBundleNode.SPEC_VERSION;
    }

    /**
     * Return the set of named descriptors that I have.
     */
    public Collection getNamedDescriptors() {
        return super.getNamedDescriptorsFrom(this);
    }

    /**
     * Return the saet of NamedReferencePairs that I have.
     */
    public Vector<NamedReferencePair> getNamedReferencePairs() {
        return super.getNamedReferencePairsFrom(this);
    }

    /**
     * return the name of my context root
     */
    public String getContextRoot() {
        if (getModuleDescriptor() != null && getModuleDescriptor().getContextRoot() != null) {
            return getModuleDescriptor().getContextRoot();
        }
        if (contextRoot == null) {
            contextRoot = "";
        }
        return contextRoot;
    }

    /**
     * Set the name of my context root.
     */
    public void setContextRoot(String contextRoot) {
        if (getModuleDescriptor() != null) {
            getModuleDescriptor().setContextRoot(contextRoot);
        }
        this.contextRoot = contextRoot;
    }

    /**
     * Return the Set of Web COmponent Descriptors (JSP or JavaServlets) in me.
     */
    public Set<WebComponentDescriptor> getWebComponentDescriptors() {
        if (webComponentDescriptors == null) {
            webComponentDescriptors = new OrderedSet<WebComponentDescriptor>();
        }
        return webComponentDescriptors;
    }

    /**
     * Adds a new Web Component Descriptor to me.
     * @param webComponentDescriptor
     */
    public void addWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        String name = webComponentDescriptor.getCanonicalName();
        webComponentDescriptor.setWebBundleDescriptor(this);

        WebComponentDescriptor resultDesc =
                combineWebComponentDescriptor(webComponentDescriptor);

        // sync up urlPattern2ServletName map
        for (String up : resultDesc.getUrlPatternsSet()) {
            String oldName = getUrlPatternToServletNameMap().put(up, name);
            if (oldName != null && (!oldName.equals(name))) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                    "web.deployment.exceptionsameurlpattern",
                    "Servlet [{0}] and Servlet [{1}] have the same url pattern: [{2}]",
                    new Object[] { oldName, name, up }));
            }
        }
    }

    /**
     * This method combines descriptor except urlPattern and add
     * to current bundle descriptor if necessary.
     * It returns the web component descriptor in the current bundle descriptor.
     * @param webComponentDescriptor the new descriptor
     *
     * @return web component descriptor in current bundle
     */
    protected WebComponentDescriptor combineWebComponentDescriptor(
            WebComponentDescriptor webComponentDescriptor) {

        WebComponentDescriptor resultDesc = null;
        String name = webComponentDescriptor.getCanonicalName();
        WebComponentDescriptor webCompDesc = getWebComponentByCanonicalName(name);

        if (webCompDesc != null) {
            // Servlet defined in web.xml
            resultDesc = webCompDesc;
            if (!webCompDesc.isConflict(webComponentDescriptor, true)) {
                // combine the contents of the given one to this one
                // except the urlPatterns
                webCompDesc.add(webComponentDescriptor, false, false);
            }

            String implFile = webCompDesc.getWebComponentImplementation();
            if (resultDesc.isConflict() &&
                    (implFile == null || implFile.length() == 0)) {

                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.exceptionconflictwebcompwithoutimpl",
                        "Two or more web fragments define the same Servlet with conflicting implementation class names that are not overridden by the web.xml"));
            } 
            if (resultDesc.getConflictedInitParameterNames().size() > 0) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.exceptionconflictwebcompinitparam",
                        "Two or more web fragments define the same Servlet with conflicting init param that are not overridden by the web.xml"));
            }
        } else {
            resultDesc = webComponentDescriptor;
            if (resultDesc.isConflict()) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.exceptionconflictwebcomp",
                        "One or more web fragments define the same Servlet in a conflicting way, and the Servlet is not defined in web.xml"));
            } else {
                this.getWebComponentDescriptors().add(resultDesc);
            }
        }

        return resultDesc;
    }

    /**
     * Remove the given web component from me.
     */

    public void removeWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        webComponentDescriptor.setWebBundleDescriptor(null);
        getWebComponentDescriptors().remove(webComponentDescriptor);
        resetUrlPatternToServletNameMap();
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    /**
     * DeploymentDescriptorNode.addNodeDescriptor(node) need this.
     */
    public void setSessionConfig(SessionConfigDescriptor sessionConfigDesc) {
        this.sessionConfig = sessionConfigDesc;
    }

    /**
     * WEB SERVICES REF APIS
     */
    public boolean hasServiceReferenceDescriptors() {
        if (serviceReferences == null)
            return false;
        return serviceReferences.size() != 0;
    }

    public Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors() {
        if (serviceReferences == null) {
            serviceReferences = new OrderedSet<ServiceReferenceDescriptor>();
        }
        return serviceReferences;
    }

    public void addServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceRef.setBundleDescriptor(this);
        getServiceReferenceDescriptors().add(serviceRef);
    }

    public void removeServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceRef.setBundleDescriptor(null);
        getServiceReferenceDescriptors().remove(serviceRef);
    }

    /**
     * Looks up an service reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
        ServiceReferenceDescriptor sr = _getServiceReferenceByName(name);
        if (sr != null) {
            return sr;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoservicerefbyname",
                "This web app [{0}] has no service reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    protected ServiceReferenceDescriptor _getServiceReferenceByName(String name) {
        for (ServiceReferenceDescriptor srd : getServiceReferenceDescriptors()) {
            if (srd.getName().equals(name)) {
                return srd;
            }
        }
        return null;
    }

    protected void combineServiceReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oserviceRef: env.getServiceReferenceDescriptors()) {
            ServiceReferenceDescriptor serviceRef =
                (ServiceReferenceDescriptor)oserviceRef;
            ServiceReferenceDescriptor sr = _getServiceReferenceByName(serviceRef.getName());
            if (sr != null) {
                combineInjectionTargets(sr, (EnvironmentProperty)serviceRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictServiceReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictserviceref",
                            "There are more than one service references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addServiceReferenceDescriptor(serviceRef);
                }
            }
        }
    }

    /**
     * @return the set of resource environment references this ejb declares.
     */
    public Set<ResourceEnvReferenceDescriptor> getResourceEnvReferenceDescriptors() {
        if (resourceEnvRefReferences == null) {
            resourceEnvRefReferences = new OrderedSet<ResourceEnvReferenceDescriptor>();
        }
        return resourceEnvRefReferences;
    }

    /**
     * adds a resource environment reference to the bundle
     */
    public void addResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference) {
        getResourceEnvReferenceDescriptors().add(resourceEnvRefReference);
    }

    /**
     * removes a existing resource environment reference from the bundle
     */
    public void removeResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference) {
        getResourceEnvReferenceDescriptors().remove(resourceEnvRefReference);
    }

    /**
     * @return a resource environment reference by the same name or throw an IllegalArgumentException.
     */
    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name) {
        ResourceEnvReferenceDescriptor jrd = _getResourceEnvReferenceByName(name);
        if (jrd != null) {
            return jrd;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoresourceenvrefbyname",
                "This web app [{0}] has no resource environment reference by the name of [{1}]", new Object[]{getName(), name}));
    }

    protected ResourceEnvReferenceDescriptor _getResourceEnvReferenceByName(String name) {
        for (ResourceEnvReferenceDescriptor jdr : getResourceEnvReferenceDescriptors()) {
            if (jdr.getName().equals(name)) {
                return jdr;
            }
        }
        return null;
    }

    protected void combineResourceEnvReferenceDescriptors(JndiNameEnvironment env) {
        for (Object ojdRef: env.getResourceEnvReferenceDescriptors()) {
            ResourceEnvReferenceDescriptor jdRef =
                (ResourceEnvReferenceDescriptor)ojdRef;
            ResourceEnvReferenceDescriptor jdr = _getResourceEnvReferenceByName(jdRef.getName());
            if (jdr != null) {
                combineInjectionTargets(jdr, jdRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictResourceEnvReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictresourceenvref",
                            "There are more than one resource env references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addResourceEnvReferenceDescriptor(jdRef);
                }

            }
        }
    }

    public Set<MimeMapping> getMimeMappingsSet() {
        if (mimeMappings == null) {
            mimeMappings = new HashSet<MimeMapping>();
        }
        return mimeMappings;
    }

    /**
     * Sets the Set of Mime Mappings for this web application.
     */
    public void setMimeMappings(Set<MimeMapping> mimeMappings) {
        this.mimeMappings = mimeMappings;
    }


    /**
     * @returns an enumeration of my mime mappings.
     */
    public Enumeration<MimeMapping> getMimeMappings() {
        return (new Vector<MimeMapping>(this.getMimeMappingsSet())).elements();
    }

    /**
     * @add the given mime mapping to my list if the given MimeType is not added
     * return the result MimeType of the MimeMapping in the resulting set of MimeMapping
     */
    public String addMimeMapping(MimeMapping mimeMapping) {
        // there should be at most one mapping per extension
        MimeMapping resultMimeMapping = null;
        for (Iterator<MimeMapping> itr = getMimeMappingsSet().iterator(); itr.hasNext();) {
            MimeMapping mm = itr.next();
            if (mm.getExtension().equals(mimeMapping.getExtension())) {
                resultMimeMapping = mm;
                break;
            }
        }
        if (resultMimeMapping == null) {
            resultMimeMapping = mimeMapping;
            getMimeMappingsSet().add(mimeMapping);
        }

        return resultMimeMapping.getMimeType();
    }

    /**
     * add the given mime mapping to my list.
     */
    public String addMimeMapping(MimeMappingDescriptor mimeMapping) {
        return addMimeMapping((MimeMapping) mimeMapping);
    }

    protected void combineMimeMappings(Set<MimeMapping> mimeMappings) {
        if (conflictedMimeMappingExtensions != null) {
            for (MimeMapping mm : getMimeMappingsSet()) {
                 conflictedMimeMappingExtensions.remove(mm.getExtension());
            }

            if (conflictedMimeMappingExtensions.size() > 0) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.exceptionconflictMimeMapping",
                        "There are more than one Mime mapping defined in web fragments with the same extension."));
            }
        }

        // do not call getMimeMappingsSet().addAll() as there is special overriding rule
        for (MimeMapping mimeMap : mimeMappings) {
            addMimeMapping(mimeMap);
        }
    }

    public Set<String> getConflictedMimeMappingExtensions() {
        if (conflictedMimeMappingExtensions == null) {
            conflictedMimeMappingExtensions = new HashSet<>();
        }
        return conflictedMimeMappingExtensions;
    }

    public LocaleEncodingMappingListDescriptor getLocaleEncodingMappingListDescriptor() {
        return localeEncodingMappingListDesc;
    }

    public void setLocaleEncodingMappingListDescriptor(LocaleEncodingMappingListDescriptor lemListDesc) {
        localeEncodingMappingListDesc = lemListDesc;
    }

    public void addLocaleEncodingMappingDescriptor(LocaleEncodingMappingDescriptor lemDesc) {
        if (localeEncodingMappingListDesc == null) {
            localeEncodingMappingListDesc = new LocaleEncodingMappingListDescriptor();
        }

        localeEncodingMappingListDesc.addLocaleEncodingMapping(lemDesc);
    }

    /**
     * Removes the given mime mapping from my list.
     */
    public void removeMimeMapping(MimeMapping mimeMapping) {
        getMimeMappingsSet().remove(mimeMapping);
    }

    /**
     * Return an enumeration of the welcome files I have..
     */
    public Enumeration<String> getWelcomeFiles() {
        return (new Vector<String>(this.getWelcomeFilesSet())).elements();
    }

    public Set<String> getWelcomeFilesSet() {
        if (welcomeFiles == null) {
            welcomeFiles = new OrderedSet<String>();
        }
        return welcomeFiles;
    }

    /**
     * Adds a new welcome file to my list.
     */
    public void addWelcomeFile(String fileUri) {
        getWelcomeFilesSet().add(fileUri);
    }

    /**
     * Removes a welcome file from my list.
     */
    public void removeWelcomeFile(String fileUri) {
        getWelcomeFilesSet().remove(fileUri);
    }

    /**
     * Sets the collection of my welcome files.
     */
    public void setWelcomeFiles(Set<String> welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
    }

    public Set<ErrorPageDescriptor> getErrorPageDescriptorsSet() {
        if (errorPageDescriptors == null) {
            errorPageDescriptors = new HashSet<ErrorPageDescriptor>();
        }
        return errorPageDescriptors;
    }

    /**
     * Returns an enumeration of the error pages I have.
     */
    public Enumeration<ErrorPageDescriptor> getErrorPageDescriptors() {
        return (new Vector<ErrorPageDescriptor>(getErrorPageDescriptorsSet())).elements();
    }

    /**
     * Adds a new error page to my list.
     */
    public void addErrorPageDescriptor(ErrorPageDescriptor errorPageDescriptor) {
        String errorSignifier = errorPageDescriptor.getErrorSignifierAsString();
        ErrorPageDescriptor errPageDesc =
            getErrorPageDescriptorBySignifier(errorSignifier);

        if (errPageDesc == null) {
            getErrorPageDescriptorsSet().add(errorPageDescriptor);
        }
    }

    /**
     * Removes the given error page from my list.
     */
    public void removeErrorPageDescriptor(ErrorPageDescriptor errorPageDescriptor) {
        getErrorPageDescriptorsSet().remove(errorPageDescriptor);
    }

    /**
     * Search my error pages for one with thei given signifier or null if there isn't one.
     */
    public ErrorPageDescriptor getErrorPageDescriptorBySignifier(String signifier) {
        for (ErrorPageDescriptor next : getErrorPageDescriptorsSet()) {
            if (next.getErrorSignifierAsString().equals(signifier)) {
                return next;
            }
        }
        return null;
    }

    /**
     * @return the Set of my Context Parameters.
     */
    public Set<ContextParameter> getContextParametersSet() {
        if (contextParameters == null) {
            contextParameters = new OrderedSet<ContextParameter>();
        }
        return contextParameters;
    }

    /**
     * @return my Context Parameters in an enumeration.
     */
    public Enumeration<ContextParameter> getContextParameters() {
        return (new Vector<ContextParameter>(getContextParametersSet())).elements();
    }

    /**
     * Adds a new context parameter to my list.
     */
    public void addContextParameter(ContextParameter contextParameter) {
        getContextParametersSet().add(contextParameter);
    }

    /**
     * Adds a new context parameter to my list.
     */
    public void addContextParameter(EnvironmentProperty contextParameter) {
        addContextParameter((ContextParameter) contextParameter);
    }

    /**
     * Removes the given context parameter from my list.
     */
    public void removeContextParameter(ContextParameter contextParameter) {
        getContextParametersSet().remove(contextParameter);
    }

    /**
     * Return true if this web app [{0}] can be distributed across different processes.
     */

    public Boolean isDistributable() {
        return isDistributable;
    }

    /**
     * Sets whether this web app [{0}] can be distributed across different processes.
     */
    public void setDistributable(Boolean isDistributable) {
        this.isDistributable = isDistributable;
    }

    /**
     * Returns the enumeration of my references to Enterprise Beans.
     */

    public Enumeration<EjbReference> getEjbReferences() {
        return (new Vector<EjbReference>(this.getEjbReferenceDescriptors())).elements();
    }

    /**
     * Returns the Set of my references to Enterprise Beans.
     */

    public Set<EjbReference> getEjbReferenceDescriptors() {
        if (ejbReferences == null) {
            ejbReferences = new OrderedSet<EjbReference>();
        }
        return ejbReferences;
    }

    /**
     * @return an Enterprise Bean with the matching name or throw.
     */

    public EjbReferenceDescriptor getEjbReferenceByName(String name) {
        return (EjbReferenceDescriptor) getEjbReference(name);
    }

    public EjbReference getEjbReference(String name) {
        EjbReference er = _getEjbReference(name);
        if (er != null) {
            return er;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoejbrefbyname",
                "This web app [{0}] has no ejb reference by the name of [{1}] ", new Object[]{getName(), name}));
    }

    protected EjbReference _getEjbReference(String name) {
        for (EjbReference er : getEjbReferenceDescriptors()) {
            if (er.getName().equals(name)) {
                return er;
            }
        }
        return null;
    }

    /**
     * @return a reource reference with the matching name or throw.
     */

    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
        ResourceReferenceDescriptor rrd = _getResourceReferenceByName(name);
        if (rrd != null) {
            return rrd;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoresourcerefbyname",
                "This web app [{0}] has no resource reference by the name of [{1}]", new Object[]{getName(), name}));
    }

    protected ResourceReferenceDescriptor _getResourceReferenceByName(String name) {
        for (ResourceReference next : getResourceReferenceDescriptors()) {
            if (next.getName().equals(name)) {
                return (ResourceReferenceDescriptor) next;
            }
        }
        return null;
    }


    /**
     * @returns my Set of references to resources.
     */

    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
        if (resourceReferences == null) {
            resourceReferences = new OrderedSet<ResourceReferenceDescriptor>();
        }
        return resourceReferences;
    }

    public Set<EntityManagerFactoryReferenceDescriptor>
    getEntityManagerFactoryReferenceDescriptors() {
        return entityManagerFactoryReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerFactoryReferenceDescriptor
    getEntityManagerFactoryReferenceByName(String name) {
        EntityManagerFactoryReferenceDescriptor emfr =
            _getEntityManagerFactoryReferenceByName(name);
        if (emfr != null) {
            return emfr;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoentitymgrfactoryrefbyname",
                "This web app [{0}] has no entity manager factory reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    protected EntityManagerFactoryReferenceDescriptor
    _getEntityManagerFactoryReferenceByName(String name) {
        for (EntityManagerFactoryReferenceDescriptor next :
                getEntityManagerFactoryReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }

    public void addEntityManagerFactoryReferenceDescriptor
            (EntityManagerFactoryReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        this.getEntityManagerFactoryReferenceDescriptors().add(reference);
    }

    protected void combineEntityManagerFactoryReferenceDescriptors(JndiNameEnvironment env) {
        for (EntityManagerFactoryReferenceDescriptor emfRef :
            env.getEntityManagerFactoryReferenceDescriptors()) {
            EntityManagerFactoryReferenceDescriptor emfr =
                _getEntityManagerFactoryReferenceByName(emfRef.getName());
            if (emfr != null) {
                combineInjectionTargets(emfr, emfRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictEntityManagerFactoryReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictpersistenceunitref",
                            "There are more than one persistence unit references defined in web fragments with the same name, but not overrided in web.xml"));

                } else {
                    addEntityManagerFactoryReferenceDescriptor(emfRef);
                }
            }
        }
    }

    public Set<EntityManagerReferenceDescriptor>
    getEntityManagerReferenceDescriptors() {
        return entityManagerReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerReferenceDescriptor
    getEntityManagerReferenceByName(String name) {
        EntityManagerReferenceDescriptor emr =
            _getEntityManagerReferenceByName(name);
        if (emr != null) {
            return emr;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoentitymgrrefbyname",
                "This web app [{0}] has no entity manager reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    protected  EntityManagerReferenceDescriptor
    _getEntityManagerReferenceByName(String name) {
        for (EntityManagerReferenceDescriptor next :
                getEntityManagerReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }


    public void addEntityManagerReferenceDescriptor
            (EntityManagerReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        getEntityManagerReferenceDescriptors().add(reference);
    }

    protected void combineEntityManagerReferenceDescriptors(JndiNameEnvironment env) {
        for (EntityManagerReferenceDescriptor emRef :
                env.getEntityManagerReferenceDescriptors()) {
            EntityManagerReferenceDescriptor emr =
                _getEntityManagerReferenceByName(emRef.getName());
            if (emr != null) {
                combineInjectionTargets(emr, emRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                    ((WebBundleDescriptor)env).isConflictEntityManagerReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictpersistencecontextref",
                            "There are more than one persistence context references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addEntityManagerReferenceDescriptor(emRef);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends PersistenceUnitDescriptor>
           findReferencedPUs() {
        Collection<PersistenceUnitDescriptor> pus =
                new HashSet<PersistenceUnitDescriptor>(
                        findReferencedPUsViaPURefs(this));
        pus.addAll(findReferencedPUsViaPCRefs(this));
        if (extensions.containsKey(EjbBundleDescriptor.class)) {
            for (RootDeploymentDescriptor extension : extensions.get(EjbBundleDescriptor.class)) {
                pus.addAll(((EjbBundleDescriptor) extension).findReferencedPUs());
            }
        }
        return pus;
    }

    /**
     * Return my set of environment properties.
     */

    public Set<EnvironmentEntry> getEnvironmentProperties() {
        return getEnvironmentEntrySet();
    }

    /**
     * Adds a new reference to an ejb.
     */

    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
        if (getEjbReferenceDescriptors().add(ejbReference)) {
            ejbReference.setReferringBundleDescriptor(this);
        }
    }

    /**
     * Adds a new reference to an ejb.
     */

    public void addEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        addEjbReferenceDescriptor((EjbReference) ejbReferenceDescriptor);
    }

    /**
     * Removes a reference to an ejb.
     */
    public void removeEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        removeEjbReferenceDescriptor((EjbReference) ejbReferenceDescriptor);
    }

    public void removeEjbReferenceDescriptor(EjbReference ejbReferenceDescriptor) {
        if(getEjbReferenceDescriptors().remove(ejbReferenceDescriptor)) {
            ejbReferenceDescriptor.setReferringBundleDescriptor(null);
        }
    }

    protected void combineEjbReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oejbRef: env.getEjbReferenceDescriptors()) {
            EjbReference ejbRef = (EjbReference)oejbRef;
            EjbReferenceDescriptor ejbRefDesc =
                    (EjbReferenceDescriptor)_getEjbReference(ejbRef.getName());
            if (ejbRefDesc != null) {
                combineInjectionTargets(ejbRefDesc, (EnvironmentProperty)ejbRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictEjbReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictejbref",
                            "There are more than one ejb references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addEjbReferenceDescriptor(ejbRef);
                }
            }
        }
    }

    /**
     * Return an enumeration of references to resources that I have.
     */
    public Enumeration<ResourceReferenceDescriptor> getResourceReferences() {
        return (new Vector<ResourceReferenceDescriptor>(getResourceReferenceDescriptors())).elements();
    }

    /**
     * adds a new reference to a resource.
     */
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        getResourceReferenceDescriptors().add(resourceReference);
    }

    /**
     * removes a reference to a resource.
     */
    public void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        getResourceReferenceDescriptors().remove(resourceReference);
    }

    protected void combineResourceReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oresRef : env.getResourceReferenceDescriptors()) {
            ResourceReferenceDescriptor resRef =
                (ResourceReferenceDescriptor)oresRef;
            ResourceReferenceDescriptor rrd = _getResourceReferenceByName(resRef.getName());
            if (rrd != null) {
                combineInjectionTargets(rrd, resRef);
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictResourceReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictresourceref",
                            "There are more than one resource references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addResourceReferenceDescriptor(resRef);
                }
            }
        }
    }

    public Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors() {
        if (messageDestReferences == null) {
            messageDestReferences = new OrderedSet<MessageDestinationReferenceDescriptor>();
        }
        return messageDestReferences;
    }

    public void addMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor messageDestRef) {
        messageDestRef.setReferringBundleDescriptor(this);
        getMessageDestinationReferenceDescriptors().add(messageDestRef);
    }

    public void removeMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor msgDestRef) {
        getMessageDestinationReferenceDescriptors().remove(msgDestRef);
    }

    /**
     * Looks up an message destination reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    public MessageDestinationReferenceDescriptor
    getMessageDestinationReferenceByName(String name) {
        MessageDestinationReferenceDescriptor mdr =
            _getMessageDestinationReferenceByName(name);
        if (mdr != null) {
            return mdr;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnomsgdestrefbyname",
                "This web app [{0}] has no message destination reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    protected MessageDestinationReferenceDescriptor _getMessageDestinationReferenceByName(String name) {
        for (MessageDestinationReferenceDescriptor mdr :
                getMessageDestinationReferenceDescriptors()) {
            if (mdr.getName().equals(name)) {
                return mdr;
            }
        }
        return null;
    }

    protected void combineMessageDestinationReferenceDescriptors(JndiNameEnvironment env) {
        for (Object omdRef : env.getMessageDestinationReferenceDescriptors()) {
            MessageDestinationReferenceDescriptor mdRef =
                (MessageDestinationReferenceDescriptor)omdRef;
            MessageDestinationReferenceDescriptor mdr =
                _getMessageDestinationReferenceByName(mdRef.getName());
            if (mdr != null) {
                combineInjectionTargets(mdr, mdRef);           
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictMessageDestinationReference()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictmessagedestinationref",
                            "There are more than one message destination references defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addMessageDestinationReferenceDescriptor(mdRef);
                }
            }
        }
    }

    public Set<LifecycleCallbackDescriptor>
    getPostConstructDescriptors() {
        return postConstructDescs;
    }

    public void addPostConstructDescriptor(LifecycleCallbackDescriptor
            postConstructDesc) {
        String className = postConstructDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
                getPostConstructDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPostConstructDescriptors().add(postConstructDesc);
        }
    }

    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        return getPostConstructDescriptorByClass(className, this);
    }

    protected void combinePostConstructDescriptors(WebBundleDescriptor webBundleDescriptor) {
        boolean isFromXml = false;
        for (LifecycleCallbackDescriptor lccd : getPostConstructDescriptors()) {
            isFromXml = (lccd.getMetadataSource() == MetadataSource.XML);
            if (isFromXml) {
                break;
            }
        }

        if (!isFromXml) {
            getPostConstructDescriptors().addAll(webBundleDescriptor.getPostConstructDescriptors());
        }
    }

    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return preDestroyDescs;
    }

    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor
            preDestroyDesc) {
        String className = preDestroyDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
                getPreDestroyDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPreDestroyDescriptors().add(preDestroyDesc);
        }
    }

    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        return getPreDestroyDescriptorByClass(className, this);
    }

    protected void combinePreDestroyDescriptors(WebBundleDescriptor webBundleDescriptor) {
        boolean isFromXml = false;
        for (LifecycleCallbackDescriptor lccd : getPreDestroyDescriptors()) {
            isFromXml = (lccd.getMetadataSource() == MetadataSource.XML);
            if (isFromXml) {
                break;
            }
        }

        if (!isFromXml) {
            getPreDestroyDescriptors().addAll(webBundleDescriptor.getPreDestroyDescriptors());
        }
    }

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

        if (((WebBundleDescriptor) jndiNameEnv).hasWebServices()) {
            // Add @Resource WebServiceContext present in endpoint impl class to the list of
            // injectable resources; We do this for servelt endpoint only because the actual 
            // endpoint impl class gets replaced by JAXWSServlet in web.xml and hence
            // will never be added as an injectable resource
            for (InjectionCapable next : getInjectableResources(this)) {
                if (next.isInjectable()) {
                    for (InjectionTarget target : next.getInjectionTargets()) {
                        Iterator<WebServiceEndpoint> epIter = getWebServices().getEndpoints().iterator();
                        while (epIter.hasNext()) {
                            String servletImplClass = epIter.next().getServletImplClass();
                            if (target.getClassName().equals(servletImplClass)) {
                                injectables.add(next);
                            }
                        }
                    }
                }
            }
        }
        return injectables;
    }

    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        return (getInjectableResourcesByClass(className, this));
    }

    public InjectionInfo getInjectionInfoByClass(Class clazz) {
        return (getInjectionInfoByClass(clazz, this));
    }

    /**
     * Returns an Enumeration of my SecurityRole objects.
     */
    public Enumeration<SecurityRoleDescriptor> getSecurityRoles() {
        Vector<SecurityRoleDescriptor> securityRoles = new Vector<SecurityRoleDescriptor>();
        for (Iterator itr = super.getRoles().iterator(); itr.hasNext();) {
            Role r = (Role) itr.next();
            SecurityRoleDescriptor srd = new SecurityRoleDescriptor(r);
            securityRoles.add(srd);
        }
        return securityRoles.elements();
    }

    /**
     * Add a new abstrct role to me.
     */
    public void addSecurityRole(SecurityRole securityRole) {
        Role r = new Role(securityRole.getName());
        r.setDescription(securityRole.getDescription());
        super.addRole(r);
    }

    /**
     * Add a new abstrct role to me.
     */
    public void addSecurityRole(SecurityRoleDescriptor securityRole) {
        addSecurityRole((SecurityRole) securityRole);
    }

    /**
     * Return all the references by a given component (by name) to the given rolename.
     */
    public SecurityRoleReference getSecurityRoleReferenceByName(String compName, String roleName) {
        for (WebComponentDescriptor comp : getWebComponentDescriptors()) {
            if (!comp.getCanonicalName().equals(compName))
                continue;

            SecurityRoleReference r = comp.getSecurityRoleReferenceByName(roleName);
            if (r != null)
                return r;
        }

        return null;
    }

    protected void combineSecurityConstraints(Set<SecurityConstraint> firstScSet,
            Set<SecurityConstraint> secondScSet) {
        Set<String> allUrlPatterns = new HashSet<String>();
        for (SecurityConstraint sc : firstScSet) {
            for (WebResourceCollection wrc : sc.getWebResourceCollections()) {
                allUrlPatterns.addAll(wrc.getUrlPatterns());
            }
        }

        for (SecurityConstraint sc : secondScSet) {
            SecurityConstraint newSc = new SecurityConstraintImpl((SecurityConstraintImpl)sc);
            boolean addSc = false;
            Iterator<WebResourceCollection> iter = newSc.getWebResourceCollections().iterator();
            while (iter.hasNext()) {
                WebResourceCollection wrc = iter.next();
                Set<String> urlPatterns = wrc.getUrlPatterns();   
                urlPatterns.removeAll(allUrlPatterns);
                boolean isEmpty = (urlPatterns.size() == 0);
                addSc = (addSc || (!isEmpty));
                if (isEmpty) {
                    iter.remove();
                }
            }

            if (addSc) {
                firstScSet.add(newSc);
            }
        }
    }

    public Set<SecurityConstraint> getSecurityConstraintsSet() {
        if (securityConstraints == null) {
            securityConstraints = new HashSet<SecurityConstraint>();
        }
        return securityConstraints;
    }

    /**
     * My list of security constraints.
     */
    public Enumeration<SecurityConstraint> getSecurityConstraints() {
        return (new Vector<SecurityConstraint>(this.getSecurityConstraintsSet())).elements();
    }

    public Collection<SecurityConstraint> getSecurityConstraintsForUrlPattern(String urlPattern) {
        Collection<SecurityConstraint> constraints = new HashSet<SecurityConstraint>();
        for (Iterator<SecurityConstraint> i = getSecurityConstraintsSet().iterator(); i.hasNext();) {
            SecurityConstraint next = i.next();
            boolean include = false;
            for (WebResourceCollection nextCol: next.getWebResourceCollections()) {
                for (String nextPattern: nextCol.getUrlPatterns()) {
                    if ((urlPattern != null) && urlPattern.equals(nextPattern)) {
                        include = true;
                        break;
                    }
                }
                if (include) {
                    break;
                }
            }
            if (include) {
                constraints.add(next);
            }
        }
        return constraints;
    }

    /**
     * Add a new security constraint.
     */
    public void addSecurityConstraint(SecurityConstraint securityConstraint) {
        getSecurityConstraintsSet().add(securityConstraint);
    }

    /**
     * Add a new security constraint.
     */
    public void addSecurityConstraint(SecurityConstraintImpl securityConstraint) {
        addSecurityConstraint((SecurityConstraint) securityConstraint);
    }

    /**
     * Remove the given security constraint.
     */
    public void removeSecurityConstraint(SecurityConstraint securityConstraint) {
        getSecurityConstraintsSet().remove(securityConstraint);
    }



    public JspConfigDescriptorImpl getJspConfigDescriptor() {
        return jspConfigDescriptor;
    }

    public void setJspConfigDescriptor(JspConfigDescriptorImpl jspC) {
        jspConfigDescriptor = jspC;
    }

    /*
     * @return my set of servlets
    */

    public Set<WebComponentDescriptor> getServletDescriptors() {
        Set<WebComponentDescriptor> servletDescriptors = new HashSet<WebComponentDescriptor>();
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.isServlet()) {
                servletDescriptors.add(next);
            }
        }
        return servletDescriptors;
    }

    /**
     * @return my Set of jsps.
     */
    public Set<WebComponentDescriptor> getJspDescriptors() {
        Set<WebComponentDescriptor> jspDescriptors = new HashSet<WebComponentDescriptor>();
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (!next.isServlet()) {
                jspDescriptors.add(next);
            }
        }
        return jspDescriptors;
    }

    public Set<EnvironmentEntry> getEnvironmentEntrySet() {
        if (environmentEntries == null) {
            environmentEntries = new OrderedSet<EnvironmentEntry>();
        }
        return environmentEntries;
    }

    /**
     * Return my set of environment properties.
     */
    public Enumeration<EnvironmentEntry> getEnvironmentEntries() {
        return (new Vector<EnvironmentEntry>(this.getEnvironmentEntrySet())).elements();
    }

    /**
     * Adds this given environment property to my list.
     */
    public void addEnvironmentEntry(EnvironmentEntry environmentEntry) {
        getEnvironmentEntrySet().add(environmentEntry);
    }

    protected EnvironmentProperty _getEnvironmentPropertyByName(String name) {
        for (EnvironmentEntry ev : getEnvironmentEntrySet()) {
            if (ev.getName().equals(name)) {
                return (EnvironmentProperty) ev;
            }
        }
        return null;
    }

    /**
     * Returns the environment property object searching on the supplied key.
     * throws an illegal argument exception if no such environment property exists.
     */
    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
        EnvironmentProperty envProp = _getEnvironmentPropertyByName(name);
        if (envProp != null) {
            return envProp;
        }

        throw new IllegalArgumentException(localStrings.getLocalString(
                "web.deployment.exceptionwebapphasnoenvpropertybyname",
                "This web app [{0}] has no environment property by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    /**
     * Removes this given environment property from my list.
     */
    public void removeEnvironmentProperty(EnvironmentProperty environmentProperty) {
        getEnvironmentEntrySet().remove(environmentProperty);
    }

    /**
     * Adds this given environment property to my list.
     */
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
        getEnvironmentEntrySet().add(environmentProperty);
    }

    /**
     * Removes this given environment property from my list.
     */
    public void removeEnvironmentEntry(EnvironmentEntry environmentEntry) {
        getEnvironmentEntrySet().remove(environmentEntry);
    }

    protected void combineEnvironmentEntries(JndiNameEnvironment env) {
        for (Object oenve: env.getEnvironmentProperties()) {
            EnvironmentEntry enve = (EnvironmentEntry)oenve;
            EnvironmentProperty envProp = _getEnvironmentPropertyByName(enve.getName());
            if (envProp != null) {
                combineInjectionTargets(envProp, (EnvironmentProperty)enve);
                EnvironmentProperty envP = (EnvironmentProperty)enve;
                if (!envProp.hasInjectionTargetFromXml() &&
                        (!envProp.isSetValueCalled()) && envP.isSetValueCalled()) {
                    envProp.setValue(enve.getValue());
                }
            } else {
                if (env instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor)env).isConflictEnvironmentEntry()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictenventry",
                            "There are more than one environment entries defined in web fragments with the same name, but not overrided in web.xml"));
                } else {
                    addEnvironmentEntry(enve);
                }
            }
        }
    }

    /**
     * Return the information about how I should log in.
     */
    public LoginConfiguration getLoginConfiguration() {
        return loginConfiguration;
    }

    /**
     * Specifies the information about how I should log in.
     */
    public void setLoginConfiguration(LoginConfiguration loginConfiguration) {
        this.loginConfiguration = loginConfiguration;
    }

    public void setLoginConfiguration(LoginConfigurationImpl loginConfiguration) {
        setLoginConfiguration((LoginConfiguration) loginConfiguration);
    }

    protected void combineLoginConfiguration(WebBundleDescriptor webBundleDescriptor) {
        if (getLoginConfiguration() == null) {
            if (webBundleDescriptor.isConflictLoginConfig()) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.exceptionconflictloginconfig",
                        "There are more than one login-config defined in web fragments with different values"));
            } else {
                setLoginConfiguration(webBundleDescriptor.getLoginConfiguration());
            }
        }
    }

    /**
     * Search for a web component that I have by name.
     */
    public WebComponentDescriptor getWebComponentByName(String name) {
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.getName().equals(name)) {
                return (WebComponentDescriptor) next;
            }
        }
        return null;
    }

    /**
     * Search for a web component that I have by name.
     */
    public WebComponentDescriptor getWebComponentByCanonicalName(String name) {
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.getCanonicalName().equals(name)) {
                return (WebComponentDescriptor) next;
            }
        }
        return null;
    }

    /**
     * @return a set of web component descriptor of given impl name.
     */
    public WebComponentDescriptor[] getWebComponentByImplName(String name) {
        ArrayList<WebComponentDescriptor> webCompList =
                new ArrayList<WebComponentDescriptor>();
        for (WebComponentDescriptor webComp : getWebComponentDescriptors()) {
            if (webComp.getWebComponentImplementation().equals(name)) {
                webCompList.add(webComp);
            }
        }
        return webCompList.toArray(new WebComponentDescriptor[webCompList.size()]);
    }

    /* ----
    */

    /**
     * @return a Vector of servlet filters that I have.
     */
    public Vector<ServletFilter> getServletFilters() {
        if (servletFilters == null) {
            servletFilters = new Vector<ServletFilter>();
        }
        return servletFilters;
    }

    /**
     * @return a Vector of servlet filters that I have.
     */
    @SuppressWarnings("unchecked")
    public Vector<ServletFilter> getServletFilterDescriptors() {
        return (Vector<ServletFilter>) getServletFilters().clone();
    }

    /**
     * Adds a servlet filter to this web component.
     */
    public void addServletFilter(ServletFilter ref) {
        String name = ref.getName();
        boolean found = false;
        for (ServletFilter servletFilter : getServletFilters()) {
            if (name.equals(servletFilter.getName())) {
                found = true;
                break;
            }
        }

        if (!found) {
            getServletFilters().addElement(ref);
        }
    }

    public void addServletFilter(ServletFilterDescriptor ref) {
        addServletFilter((ServletFilter) ref);
    }

    /**
     * Removes the given servlet filter from this web component.
     */
    public void removeServletFilter(ServletFilter ref) {
        removeVectorItem(getServletFilters(), ref);
    }

    protected void combineServletFilters(WebBundleDescriptor webBundleDescriptor) {
        for (ServletFilter servletFilter : webBundleDescriptor.getServletFilters()) {
            ServletFilterDescriptor servletFilterDesc = (ServletFilterDescriptor)servletFilter;
            String name = servletFilter.getName();
            ServletFilterDescriptor aServletFilterDesc = null;
            for (ServletFilter sf : getServletFilters()) {
                if (name.equals(sf.getName())) {
                    aServletFilterDesc = (ServletFilterDescriptor)sf;
                    break;
                }
            }

            if (aServletFilterDesc != null) {
                if (!aServletFilterDesc.isConflict(servletFilterDesc)) {
                    if (aServletFilterDesc.getClassName().length() == 0) {
                        aServletFilterDesc.setClassName(servletFilter.getClassName());
                    }
                    if (aServletFilterDesc.isAsyncSupported() == null) {
                        aServletFilterDesc.setAsyncSupported(servletFilter.isAsyncSupported());
                    }
                }

                String className = aServletFilterDesc.getClassName();
                if (servletFilterDesc.isConflict() && (className == null || className.length() == 0)) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictfilterwithoutimpl",
                            "Two or more web fragments define the same Filter with conflicting implementation class names that are not overridden by the web.xml"));
                }
            } else {
                if (servletFilterDesc.isConflict()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "web.deployment.exceptionconflictfilter",
                            "One or more web fragments define the same Filter in a conflicting way, and the Filter is not defined in web.xml"));
                } else {
                    getServletFilters().add(servletFilterDesc);
                }
            }
        }
    }

    /* ----
    */

    /**
     * Return a Vector of servlet filters that I have.
     */
    public Vector<ServletFilterMapping> getServletFilterMappings() {
        if (servletFilterMappings == null) {
            servletFilterMappings = new Vector<ServletFilterMapping>();
        }
        return servletFilterMappings;
    }

    /**
     * Return a Vector of servlet filter mappings that I have.
     */
    @SuppressWarnings("unchecked")
    public Vector<ServletFilterMapping> getServletFilterMappingDescriptors() {
        return (Vector<ServletFilterMapping>) getServletFilterMappings().clone();
    }

    /**
     * Adds a servlet filter mapping to this web component.
     */
    public void addServletFilterMapping(ServletFilterMapping ref) {
        if (!getServletFilterMappings().contains(ref)) {
            getServletFilterMappings().addElement(ref);
        }
    }

    /**
     * Adds a servlet filter mapping to this web component.
     */
    public void addServletFilterMapping(ServletFilterMappingDescriptor ref) {
        addServletFilterMapping((ServletFilterMapping) ref);
    }

    /**
     * Removes the given servlet filter mapping from this web component.
     */
    public void removeServletFilterMapping(ServletFilterMapping ref) {
        removeVectorItem(getServletFilterMappings(), ref);
    }

    /**
     * * Moves the given servlet filter mapping to a new relative location in
     * * the list
     */
    public void moveServletFilterMapping(ServletFilterMapping ref, int relPos) {
        moveVectorItem(getServletFilterMappings(), ref, relPos);
    }

    protected void combineServletFilterMappings(WebBundleDescriptor webBundleDescriptor) {
        Map<String, ServletFilterMappingInfo> map = new HashMap<String, ServletFilterMappingInfo>();
        for (ServletFilterMapping sfMapping : getServletFilterMappings()) {
            ServletFilterMappingInfo sfmInfo = map.get(sfMapping.getName());
            if (sfmInfo == null) {
                sfmInfo = new ServletFilterMappingInfo();
                sfmInfo.servletFilterMapping = sfMapping;
                map.put(sfMapping.getName(), sfmInfo);
            }
            if (!sfmInfo.hasMapping) {
                sfmInfo.hasMapping = (sfMapping.getServletNames().size() > 0 ||
                       sfMapping.getUrlPatterns().size() > 0);
            }
            if (!sfmInfo.hasDispatcher) {
                sfmInfo.hasDispatcher = (sfMapping.getDispatchers().size() > 0);
            }
        }

        for (ServletFilterMapping sfMapping : webBundleDescriptor.getServletFilterMappings()) {
            ServletFilterMappingInfo sfmInfo = map.get(sfMapping.getName());
            if (sfmInfo != null) {
                if (!sfmInfo.hasMapping) {
                    sfmInfo.servletFilterMapping.getServletNames().addAll(sfMapping.getServletNames());
                    sfmInfo.servletFilterMapping.getUrlPatterns().addAll(sfMapping.getUrlPatterns());
                }
                if (!sfmInfo.hasDispatcher) {
                    sfmInfo.servletFilterMapping.getDispatchers().addAll(sfMapping.getDispatchers());
                }
            } else {
                addServletFilterMapping(sfMapping);
            }
        }
    }

    /* ----
    */

    public Vector<AppListenerDescriptor> getAppListeners() {
        if (appListenerDescriptors == null) {
            appListenerDescriptors = new Vector<AppListenerDescriptor>();
        }
        return appListenerDescriptors;
    }

    @SuppressWarnings("unchecked")
    public Vector<AppListenerDescriptor> getAppListenerDescriptors() {
        return (Vector<AppListenerDescriptor>) getAppListeners().clone();
    }

    public void setAppListeners(Collection<? extends AppListenerDescriptor> c) {
        getAppListeners().clear();
        getAppListeners().addAll(c);
    }

    public void addAppListenerDescriptor(AppListenerDescriptor ref) {
        if (!getAppListeners().contains(ref)) {
            getAppListeners().addElement(ref);
        }
    }

     public void addAppListenerDescriptorToFirst(AppListenerDescriptor ref) {
         if (!getAppListeners().contains(ref)) {
             getAppListeners().add(0, ref);
         }
     }

     public void addAppListenerDescriptor(AppListenerDescriptorImpl ref) {
        addAppListenerDescriptor((AppListenerDescriptor) ref);
    }

    public void removeAppListenerDescriptor(AppListenerDescriptor ref) {
        removeVectorItem(getAppListeners(), ref);
    }

    public void moveAppListenerDescriptor(AppListenerDescriptor ref,
                                          int relPos) {
        this.moveVectorItem(this.getAppListeners(), ref, relPos);
    }

    public AbsoluteOrderingDescriptor getAbsoluteOrderingDescriptor() {
        return absOrdering;
    }

    public void setAbsoluteOrderingDescriptor(AbsoluteOrderingDescriptor absOrdering) {
        this.absOrdering = absOrdering;
    }

    public boolean isDenyUncoveredHttpMethods() {
        return denyUncoveredHttpMethods;
    }

    public void setDenyUncoveredHttpMethods(boolean denyUncoveredHttpMethods) {
        this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;
    }

    public boolean isShowArchivedRealPathEnabled() {
        return showArchivedRealPathEnabled;
    }

    public void setShowArchivedRealPathEnabled(boolean enabled) {
        showArchivedRealPathEnabled = enabled;
    }

    public int getServletReloadCheckSecs() {
        return servletReloadCheckSecs;
    }

    public void setServletReloadCheckSecs(int secs) {
        servletReloadCheckSecs = secs;
    }

    /**
     * @return true if this bundle descriptor defines web service clients
     */
    public boolean hasWebServiceClients() {
        return !getServiceReferenceDescriptors().isEmpty();
    }

    /**
     * End of Web-Services related API
     */

    /* ----
    */

    /**
     * remove a specific object from the given list (does not rely on 'equals')
     */
    protected boolean removeVectorItem(Vector<? extends Object> list, Object ref) {
        for (Iterator<? extends Object> i = list.iterator(); i.hasNext();) {
            if (ref == i.next()) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the given object to a new relative location in the specified list
     */
    @SuppressWarnings("unchecked")
    protected void moveVectorItem(Vector list, Object ref, int rpos) {

        /* get current position of ref */
        // 'indexOf' is not used because it is base on 'equals()' which may
        // not be unique.
        int size = list.size(), old_pos = size - 1;
        for (; old_pos >= 0; old_pos--) {
            if (ref == list.elementAt(old_pos)) {
                break;
            }
        }
        if (old_pos < 0) {
            return; // not found
        }

        /* limit up/down movement */
        int new_pos = old_pos + rpos;
        if (new_pos < 0) {
            new_pos = 0; // limit movement
        } else if (new_pos >= size) {
            new_pos = size - 1; // limit movement
        }

        /* is it really moving? */
        if (new_pos == old_pos) {
            return; // it's not moving
        }

        /* move it */
        list.removeElementAt(old_pos);
        list.insertElementAt(ref, new_pos);


    }

    /**
     * visitor API implementation
     */
    public void visit(DescriptorVisitor aVisitor) {
        if (aVisitor instanceof WebBundleVisitor || 
            aVisitor instanceof ComponentPostVisitor) {
            visit((ComponentVisitor) aVisitor);
        } else {
            super.visit(aVisitor);
        }
    }

    /**
     * visitor API implementation
     */
    public void visit(ComponentVisitor aVisitor) {
        super.visit(aVisitor);
        aVisitor.accept(this);
    }


    public void putJarNameWebFragmentNamePair(String jarName, String webFragName) {
        if (jarName2WebFragNameMap == null) {
            jarName2WebFragNameMap = new HashMap<String, String>();
        }
        jarName2WebFragNameMap.put(jarName, webFragName);
    }

    /**
     * This method return an unmodifiable version of jarName2WebFragNameMap.
     * @return unmodifiable version of jarName2WebFragNameMap
     */
    public Map<String, String> getJarNameToWebFragmentNameMap() {
        if (jarName2WebFragNameMap == null) {
            jarName2WebFragNameMap = new HashMap<String, String>();
        }
        return Collections.unmodifiableMap(jarName2WebFragNameMap);
    }

    /**
     * This method is used by WebComponentDescriptor only.
     * The returned map is supposed to be only modified by the corresponding url patterns set.
     */
    public Map<String, String> getUrlPatternToServletNameMap() {
        if (urlPattern2ServletName == null) {
            urlPattern2ServletName = new HashMap<String, String>();
            for (WebComponentDescriptor wc : getWebComponentDescriptors()) {
                String name = wc.getCanonicalName();
                for (String up : wc.getUrlPatternsSet()) {
                    String oldName = urlPattern2ServletName.put(up, name);
                    if (oldName != null && (!oldName.equals(name))) {
                        throw new RuntimeException(localStrings.getLocalString(
                                "web.deployment.exceptionsameurlpattern",
                                "Servlet [{0}] and Servlet [{1}] have the same url pattern: [{2}]",
                                new Object[] { oldName, name, up }));
                    }
                }
            }
        }

        return urlPattern2ServletName;
    }

    public void resetUrlPatternToServletNameMap() {
        urlPattern2ServletName = null;
    }

    public List<String> getOrderedLibs() {
        return orderedLibs;
    }

    public void addOrderedLib(String libName) {
        orderedLibs.add(libName);
    }

    /**
     * This method will copy the injection targets from env2 to env1.
     *
     * @param env1
     * @param env2
     */
    protected void combineInjectionTargets(EnvironmentProperty env1, EnvironmentProperty env2) {
        for (InjectionTarget injTarget: env2.getInjectionTargets()) {
            env1.addInjectionTarget(injTarget);
        }
    }

    /* ----
    */

    /**
     * Return a formatted version as a String.
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\nWeb Bundle descriptor");
        toStringBuffer.append("\n");
        printCommon(toStringBuffer);
        if (sunWebApp != null) {
            toStringBuffer.append("\n ========== Runtime Descriptors =========");
            toStringBuffer.append("\n").append(sunWebApp.toString());
        }
    }

    public void printCommon(StringBuffer toStringBuffer) {
        super.print(toStringBuffer);
        toStringBuffer.append("\n context root ").append(getContextRoot());
        if (sessionConfig != null) {
            toStringBuffer.append(sessionConfig);
        }
        String wname = getName();
        if (wname != null && wname.length() > 0) {
            toStringBuffer.append("\n name ").append(wname);
        }
        toStringBuffer.append("\n mimeMappings ").append(mimeMappings);
        toStringBuffer.append("\n welcomeFiles ").append(welcomeFiles);
        toStringBuffer.append("\n errorPageDescriptors ").append(errorPageDescriptors);
        toStringBuffer.append("\n appListenerDescriptors ").append(appListenerDescriptors);
        toStringBuffer.append("\n contextParameters ").append(contextParameters);
        toStringBuffer.append("\n ejbReferences ");
        if (ejbReferences != null)
            printDescriptorSet(ejbReferences, toStringBuffer);
        toStringBuffer.append("\n resourceEnvRefReferences ");
        if (resourceEnvRefReferences != null)
            printDescriptorSet(resourceEnvRefReferences, toStringBuffer);
        toStringBuffer.append("\n messageDestReferences ");
        if (messageDestReferences != null)
            printDescriptorSet(messageDestReferences, toStringBuffer);
        toStringBuffer.append("\n resourceReferences ");
        if (resourceReferences != null)
            printDescriptorSet(resourceReferences, toStringBuffer);
        toStringBuffer.append("\n serviceReferences ");
        if (serviceReferences != null)
            printDescriptorSet(serviceReferences, toStringBuffer);
        toStringBuffer.append("\n isDistributable ").append(isDistributable);
        toStringBuffer.append("\n denyUncoveredHttpMethods ").append(denyUncoveredHttpMethods);
        toStringBuffer.append("\n securityRoles ").append(securityRoles);
        toStringBuffer.append("\n securityConstraints ").append(securityConstraints);
        toStringBuffer.append("\n contextRoot ").append(contextRoot);
        toStringBuffer.append("\n loginConfiguration ").append(this.loginConfiguration);
        toStringBuffer.append("\n webComponentDescriptors ");
        if (webComponentDescriptors != null)
            printDescriptorSet(webComponentDescriptors, toStringBuffer);
        toStringBuffer.append("\n environmentEntries ");
        if (environmentEntries != null)
            printDescriptorSet(environmentEntries, toStringBuffer);
    }

    private void printDescriptorSet(Set descSet, StringBuffer sbuf) {
        if (descSet == null)
            return;
        for (Iterator itr = descSet.iterator(); itr.hasNext();) {
            Object obj = itr.next();
            if (obj instanceof Descriptor)
                ((Descriptor) obj).print(sbuf);
            else
                sbuf.append(obj);
        }
    }

    /**
     * @return the module type for this bundle descriptor
     */
    public ArchiveType getModuleType() {
        return DOLUtils.warType();
    }

    /**
     * @return the visitor for this bundle descriptor
     */
    public ComponentVisitor getBundleVisitor() {
        return new WebBundleValidator();
    }

    /**
     * @return the tracer visitor for this descriptor
     */
    public DescriptorVisitor getTracerVisitor() {
        return new WebBundleTracerVisitor();
    }

    /**
     * @return the deployment descriptor directory location inside
     *         the archive file
     */
    public String getDeploymentDescriptorDir() {
        return DEPLOYMENT_DESCRIPTOR_DIR;
    }

    /***********************************************************************************************
     * START
     * Deployment Consolidation to Suppport Multiple Deployment API Clients
     * Methods: setSunDescriptor, getSunDescriptor
     ***********************************************************************************************/

    /**
     * This returns the extra web sun specific info not in the RI DID.
     *
     * @return object representation of web deployment descriptor
     */
    public SunWebApp getSunDescriptor() {
        if (sunWebApp == null) {
            sunWebApp = new SunWebAppImpl();
        }
        return sunWebApp;
    }

    /**
     * This sets the extra web sun specific info not in the RI DID.
     *
     * @param webApp SunWebApp object representation of web deployment descriptor
     */
    public void setSunDescriptor(SunWebApp webApp) {
        this.sunWebApp = webApp;
    }

    /**
     * This property can be used to indicate special processing.
     * For example, a Deployer may set this property.
     */ 
    public void setExtensionProperty(String key, String value) {
        if (null == extensionProperty) {
            extensionProperty = new HashMap<String, String>();
        }
        extensionProperty.put(key, value);
    }

    /**
     * Determine if an extension property has been set.
     */
    public boolean hasExtensionProperty(String key) {
        if (null == extensionProperty || 
            extensionProperty.get(key) == null) {
            return false;
        }
        return true;
    }

    private static final class ServletFilterMappingInfo {
        private ServletFilterMapping servletFilterMapping;
        private boolean hasMapping = false;
        private boolean hasDispatcher = false;
    }

    private void combineAllResourceDescriptors(JndiNameEnvironment env) {
        for(JavaEEResourceType javaEEResourceType: JavaEEResourceType.values()) {
            combineResourceDescriptors(env,javaEEResourceType);
        }
    }

    protected void combineResourceDescriptors(JndiNameEnvironment env, JavaEEResourceType javaEEResourceType) {
	 for (ResourceDescriptor desc : env.getResourceDescriptors(javaEEResourceType)) {
	     ResourceDescriptor descriptor = getResourceDescriptor(javaEEResourceType, desc.getName());
	     if (descriptor == null) {
		 if (env instanceof WebBundleDescriptor) {
		     WebBundleDescriptor wbDesc = (WebBundleDescriptor)env;

		     if (javaEEResourceType.equals(JavaEEResourceType.AODD) &&
			     wbDesc.isConflictAdminObjectDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictadministeredobjectdefinition",
				 "There are more than one administered object definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     } else if (javaEEResourceType.equals(JavaEEResourceType.MSD) &&
			     wbDesc.isConflictMailSessionDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictmailsessiondefinition",
				 "There are more than one mail-session definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     } else if (javaEEResourceType.equals(JavaEEResourceType.DSD) &&
			     wbDesc.isConflictDataSourceDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictdatasourcedefinition",
				 "There are more than one datasource definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     } else if (javaEEResourceType.equals(JavaEEResourceType.CFD) &&
			     wbDesc.isConflictConnectionFactoryDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictconnectionfactorydefinition",
				 "There are more than one connection factory definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     } else if (javaEEResourceType.equals(JavaEEResourceType.JMSCFDD) &&
			     wbDesc.isConflictJMSConnectionFactoryDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictjmsconnectionfactorydefinition",
				 "There are more than one jms connection factory definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     } else if (javaEEResourceType.equals(JavaEEResourceType.JMSDD) &&
			     wbDesc.isConflictJMSDestinationDefinition()) {
			 throw new IllegalArgumentException(localStrings.getLocalString(
				 "web.deployment.exceptionconflictjmsdestinationdefinition",
				 "There are more than one jms destination definitions defined in web fragments with the same name, but not overrided in web.xml"));
		     }
		 }
		 if (desc.getResourceType().equals(JavaEEResourceType.DSD) ||
			 desc.getResourceType().equals(JavaEEResourceType.MSD) ||
			 desc.getResourceType().equals(JavaEEResourceType.CFD) ||
			 desc.getResourceType().equals(JavaEEResourceType.AODD) ||
			 desc.getResourceType().equals(JavaEEResourceType.JMSCFDD) ||
			 desc.getResourceType().equals(JavaEEResourceType.JMSDD)) {
		     getResourceDescriptors(javaEEResourceType).add(desc);
		 }
	     }
         }
    }

    /*******************************************************************************************
     * END
     * Deployment Consolidation to Suppport Multiple Deployment API Clients
     *******************************************************************************************/ 
}
