/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.*;
import com.sun.enterprise.deployment.util.ComponentVisitor;
import com.sun.enterprise.deployment.web.*;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.event.EventTypes;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.deployment.common.JavaEEResourceType;

import java.util.*;

/**
 * This class represents all the deployment information about a web application.
 *
 * @author Danny Coward
 */
public abstract class WebBundleDescriptor extends CommonResourceBundleDescriptor implements WritableJndiNameEnvironment, ResourceReferenceContainer, ResourceEnvReferenceContainer, EjbReferenceContainer, MessageDestinationReferenceContainer, ServiceReferenceContainer {

    public static final EventTypes<WebBundleDescriptor> AFTER_SERVLET_CONTEXT_INITIALIZED_EVENT =
            EventTypes.create("After_Servlet_Context_Initialized", WebBundleDescriptor.class);

    protected boolean conflictLoginConfig = false;
    protected boolean conflictDataSourceDefinition = false;
    protected boolean conflictMailSessionDefinition = false;
    protected boolean conflictConnectionFactoryDefinition = false;
    protected boolean conflictAdminObjectDefinition = false;
    protected boolean conflictJMSConnectionFactoryDefinition = false;
    protected boolean conflictJMSDestinationDefinition = false;
    protected boolean conflictEnvironmentEntry = false;
    protected boolean conflictEjbReference = false;
    protected boolean conflictServiceReference = false;
    protected boolean conflictResourceReference = false;
    protected boolean conflictResourceEnvReference = false;
    protected boolean conflictMessageDestinationReference = false;
    protected boolean conflictEntityManagerReference = false;
    protected boolean conflictEntityManagerFactoryReference = false;

    public abstract void addWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor);

    public abstract void addDefaultWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor);

    public abstract void addJndiNameEnvironment(JndiNameEnvironment env);

    @Override
    public abstract boolean isEmpty();

    @Override
    public abstract String getDefaultSpecVersion();

    public abstract Collection getNamedDescriptors();

    public abstract Vector<NamedReferencePair> getNamedReferencePairs();

    public abstract String getContextRoot();

    public abstract void setContextRoot(String contextRoot);

    public abstract String getRequestCharacterEncoding();

    public abstract void setRequestCharacterEncoding(String requestCharacterEncoding);

    public abstract String getResponseCharacterEncoding();

    public abstract void setResponseCharacterEncoding(String responseCharacterEncoding);

    public abstract Set<WebComponentDescriptor> getWebComponentDescriptors();

    public abstract void addWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor);

    protected abstract WebComponentDescriptor combineWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor);

    public abstract void removeWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor);

    public abstract SessionConfig getSessionConfig();

    public abstract void setSessionConfig(SessionConfig sessionConfig);

    public abstract boolean hasServiceReferenceDescriptors();

    @Override
    public abstract Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors();

    @Override
    public abstract void addServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef);

    @Override
    public abstract void removeServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef);

    @Override
    public abstract ServiceReferenceDescriptor getServiceReferenceByName(String name);

    protected abstract ServiceReferenceDescriptor _getServiceReferenceByName(String name);

    protected abstract void combineServiceReferenceDescriptors(JndiNameEnvironment env);

    @Override
    public abstract Set<ResourceEnvReferenceDescriptor> getResourceEnvReferenceDescriptors();

    @Override
    public abstract void addResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference);

    @Override
    public abstract void removeResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference);

    @Override
    public abstract ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name);

    protected abstract ResourceEnvReferenceDescriptor _getResourceEnvReferenceByName(String name);

    protected abstract void combineResourceEnvReferenceDescriptors(JndiNameEnvironment env);

    protected abstract void combineResourceDescriptors(JndiNameEnvironment env, JavaEEResourceType javaEEResourceType);

    public abstract Set<MimeMapping> getMimeMappingsSet();

    public abstract void setMimeMappings(Set<MimeMapping> mimeMappings);

    public abstract Enumeration<MimeMapping> getMimeMappings();

    public abstract String addMimeMapping(MimeMapping mimeMapping);

    public abstract LocaleEncodingMappingListDescriptor getLocaleEncodingMappingListDescriptor();

    public abstract void setLocaleEncodingMappingListDescriptor(LocaleEncodingMappingListDescriptor lemDesc);

    public abstract void removeMimeMapping(MimeMapping mimeMapping);

    public abstract Enumeration<String> getWelcomeFiles();

    public abstract Set<String> getWelcomeFilesSet();

    public abstract void addWelcomeFile(String fileUri);

    public abstract void removeWelcomeFile(String fileUri);

    public abstract void setWelcomeFiles(Set<String> welcomeFiles);

    public abstract Set<ContextParameter> getContextParametersSet();

    public abstract Enumeration<ContextParameter> getContextParameters();

    public abstract void addContextParameter(ContextParameter contextParameter);

    public abstract void addContextParameter(EnvironmentProperty contextParameter);

    public abstract void removeContextParameter(ContextParameter contextParameter);

    public abstract boolean isDistributable();

    public abstract void setDistributable(boolean isDistributable);

    public abstract Enumeration<EjbReference> getEjbReferences();

    @Override
    public abstract Set<EjbReference> getEjbReferenceDescriptors();

    public abstract EjbReferenceDescriptor getEjbReferenceByName(String name);

    @Override
    public abstract EjbReference getEjbReference(String name);

    protected abstract EjbReference _getEjbReference(String name);

    @Override
    public abstract ResourceReferenceDescriptor getResourceReferenceByName(String name);

    protected abstract ResourceReferenceDescriptor _getResourceReferenceByName(String name);

    @Override
    public abstract Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors();

    @Override
    public abstract Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors();

    @Override
    public abstract EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name);

    protected abstract EntityManagerFactoryReferenceDescriptor _getEntityManagerFactoryReferenceByName(String name);

    @Override
    public abstract void addEntityManagerFactoryReferenceDescriptor(EntityManagerFactoryReferenceDescriptor reference);

    protected abstract void combineEntityManagerFactoryReferenceDescriptors(JndiNameEnvironment env);

    @Override
    public abstract Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors();

    @Override
    public abstract EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name);

    protected abstract EntityManagerReferenceDescriptor _getEntityManagerReferenceByName(String name);

    @Override
    public abstract void addEntityManagerReferenceDescriptor(EntityManagerReferenceDescriptor reference);

    protected abstract void combineEntityManagerReferenceDescriptors(JndiNameEnvironment env);

    @Override
    public abstract Collection<? extends PersistenceUnitDescriptor> findReferencedPUs();

    @Override
    public abstract Set<EnvironmentEntry> getEnvironmentProperties();

    @Override
    public abstract void addEjbReferenceDescriptor(EjbReference ejbReference);

    public abstract void addEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor);

    public abstract void removeEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor);

    @Override
    public abstract void removeEjbReferenceDescriptor(EjbReference ejbReferenceDescriptor);

    protected abstract void combineEjbReferenceDescriptors(JndiNameEnvironment env);

    public abstract Enumeration<ResourceReferenceDescriptor> getResourceReferences();

    @Override
    public abstract void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference);

    @Override
    public abstract void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference);

    protected abstract void combineResourceReferenceDescriptors(JndiNameEnvironment env);

    @Override
    public abstract Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors();

    @Override
    public abstract void addMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor messageDestRef);

    @Override
    public abstract void removeMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor msgDestRef);

    @Override
    public abstract MessageDestinationReferenceDescriptor getMessageDestinationReferenceByName(String name);

    protected abstract MessageDestinationReferenceDescriptor _getMessageDestinationReferenceByName(String name);

    protected abstract void combineMessageDestinationReferenceDescriptors(JndiNameEnvironment env);

    @Override
    public abstract Set<LifecycleCallbackDescriptor> getPostConstructDescriptors();

    @Override
    public abstract void addPostConstructDescriptor(LifecycleCallbackDescriptor postConstructDesc);

    @Override
    public abstract LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className);

    protected abstract void combinePostConstructDescriptors(WebBundleDescriptor webBundleDescriptor);

    @Override
    public abstract Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors();

    @Override
    public abstract void addPreDestroyDescriptor(LifecycleCallbackDescriptor preDestroyDesc);

    @Override
    public abstract LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className);

    protected abstract void combinePreDestroyDescriptors(WebBundleDescriptor webBundleDescriptor);

    @Override
    protected abstract List<InjectionCapable> getInjectableResourcesByClass(String className, JndiNameEnvironment jndiNameEnv);

    @Override
    public abstract List<InjectionCapable> getInjectableResourcesByClass(String className);

    @Override
    public abstract InjectionInfo getInjectionInfoByClass(Class clazz);

    public abstract Enumeration<SecurityRoleDescriptor> getSecurityRoles();

    public abstract void addSecurityRole(SecurityRole securityRole);

    public abstract void addSecurityRole(SecurityRoleDescriptor securityRole);

    public abstract SecurityRoleReference getSecurityRoleReferenceByName(String compName, String roleName);

    public abstract boolean isDenyUncoveredHttpMethods();

    protected abstract void combineSecurityConstraints(Set<SecurityConstraint> firstScSet, Set<SecurityConstraint> secondScSet);

    public abstract Set<SecurityConstraint> getSecurityConstraintsSet();

    public abstract Enumeration<SecurityConstraint> getSecurityConstraints();

    public abstract Collection<SecurityConstraint> getSecurityConstraintsForUrlPattern(String urlPattern);

    public abstract void addSecurityConstraint(SecurityConstraint securityConstraint);

    public abstract void removeSecurityConstraint(SecurityConstraint securityConstraint);

    public abstract Set<WebComponentDescriptor> getServletDescriptors();

    public abstract Set<WebComponentDescriptor> getJspDescriptors();

    public abstract Set<EnvironmentEntry> getEnvironmentEntrySet();

    public abstract Enumeration<EnvironmentEntry> getEnvironmentEntries();

    public abstract void addEnvironmentEntry(EnvironmentEntry environmentEntry);

    protected abstract EnvironmentProperty _getEnvironmentPropertyByName(String name);

    @Override
    public abstract EnvironmentProperty getEnvironmentPropertyByName(String name);

    @Override
    public abstract void removeEnvironmentProperty(EnvironmentProperty environmentProperty);

    @Override
    public abstract void addEnvironmentProperty(EnvironmentProperty environmentProperty);

    public abstract void removeEnvironmentEntry(EnvironmentEntry environmentEntry);

    protected abstract void combineEnvironmentEntries(JndiNameEnvironment env);

    public abstract LoginConfiguration getLoginConfiguration();

    public abstract void setLoginConfiguration(LoginConfiguration loginConfiguration);

    protected abstract void combineLoginConfiguration(WebBundleDescriptor webBundleDescriptor);

    public abstract WebComponentDescriptor getWebComponentByName(String name);

    public abstract WebComponentDescriptor getWebComponentByCanonicalName(String name);

    public abstract WebComponentDescriptor[] getWebComponentByImplName(String name);

    public abstract Vector<ServletFilter> getServletFilters();

    public abstract Vector<ServletFilter> getServletFilterDescriptors();

    public abstract void addServletFilter(ServletFilter ref);

    public abstract void removeServletFilter(ServletFilter ref);

    protected abstract void combineServletFilters(WebBundleDescriptor webBundleDescriptor);

    public abstract Vector<ServletFilterMapping> getServletFilterMappings();

    public abstract Vector<ServletFilterMapping> getServletFilterMappingDescriptors();

    public abstract void addServletFilterMapping(ServletFilterMapping ref);

    public abstract void removeServletFilterMapping(ServletFilterMapping ref);

    public abstract void moveServletFilterMapping(ServletFilterMapping ref, int relPos);

    protected abstract void combineServletFilterMappings(WebBundleDescriptor webBundleDescriptor);

    public abstract Vector<AppListenerDescriptor> getAppListeners();

    public abstract Vector<AppListenerDescriptor> getAppListenerDescriptors();

    public abstract void setAppListeners(Collection<? extends AppListenerDescriptor> c);

    public abstract void addAppListenerDescriptor(AppListenerDescriptor ref);

    public abstract void addAppListenerDescriptorToFirst(AppListenerDescriptor ref);

    public abstract void removeAppListenerDescriptor(AppListenerDescriptor ref);

    public abstract void moveAppListenerDescriptor(AppListenerDescriptor ref, int relPos);

    public abstract boolean isShowArchivedRealPathEnabled();

    public abstract void setShowArchivedRealPathEnabled(boolean enabled);

    public abstract int getServletReloadCheckSecs();

    public abstract void setServletReloadCheckSecs(int secs);

    @Override
    public abstract boolean hasWebServiceClients();

    protected abstract boolean removeVectorItem(Vector<? extends Object> list, Object ref);

    protected abstract void moveVectorItem(Vector list, Object ref, int rpos);

    public abstract void putJarNameWebFragmentNamePair(String jarName, String webFragName);

    public abstract Map<String, String> getJarNameToWebFragmentNameMap();

    public abstract Map<String, String> getUrlPatternToServletNameMap();

    public abstract void resetUrlPatternToServletNameMap();

    public abstract List<String> getOrderedLibs();

    public abstract void addOrderedLib(String libName);

    protected abstract void combineInjectionTargets(EnvironmentProperty env1, EnvironmentProperty env2);

    public abstract void printCommon(StringBuilder toStringBuilder);

    @Override
    public abstract ArchiveType getModuleType();

    @Override
    public abstract ComponentVisitor getBundleVisitor();

    @Override
    public abstract DescriptorVisitor getTracerVisitor();

    @Override
    public abstract String getDeploymentDescriptorDir();

    public abstract SunWebApp getSunDescriptor();

    public abstract void setSunDescriptor(SunWebApp webApp);

    public abstract void setExtensionProperty(String key, String value);

    public abstract boolean hasExtensionProperty(String key);

    public abstract boolean getServletInitializersEnabled();

    public abstract void setServletInitializersEnabled(boolean tf);

    public abstract boolean isJaxrsRolesAllowedEnabled();

    public abstract void setJaxrsRolesAllowedEnabled(boolean jaxrsRolesAllowedEnabled);

    public abstract String getAppContextId();

    public abstract void setAppContextId(String appContextId);

    public boolean isConflictLoginConfig() {
        return conflictLoginConfig;
    }

    public boolean isConflictDataSourceDefinition() {
        return conflictDataSourceDefinition;
    }

    public boolean isConflictMailSessionDefinition() {
        return conflictMailSessionDefinition;
    }

    public boolean isConflictConnectionFactoryDefinition() {
        return conflictConnectionFactoryDefinition;
    }

    public boolean isConflictAdminObjectDefinition() {
        return conflictAdminObjectDefinition;
    }

    public boolean isConflictJMSConnectionFactoryDefinition() {
        return conflictJMSConnectionFactoryDefinition;
    }

    public boolean isConflictJMSDestinationDefinition() {
        return conflictJMSDestinationDefinition;
    }

    public boolean isConflictEnvironmentEntry() {
        return conflictEnvironmentEntry;
    }

    public boolean isConflictEjbReference() {
        return conflictEjbReference;
    }

    public boolean isConflictServiceReference() {
        return conflictServiceReference;
    }

    public boolean isConflictResourceReference() {
        return conflictResourceReference;
    }

    public boolean isConflictResourceEnvReference() {
        return conflictResourceEnvReference;
    }

    public boolean isConflictMessageDestinationReference() {
        return conflictMessageDestinationReference;
    }

    public boolean isConflictEntityManagerReference() {
        return conflictEntityManagerReference;
    }

    public boolean isConflictEntityManagerFactoryReference() {
        return conflictEntityManagerFactoryReference;
    }

    public abstract Set<String> getConflictedMimeMappingExtensions();
}
