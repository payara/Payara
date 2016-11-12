/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
 */package com.sun.enterprise.glassfish.web;

import com.google.common.collect.ImmutableSet;
import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerFactoryReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.LocaleEncodingMappingListDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.NamedReferencePair;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.SecurityRoleDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.ComponentVisitor;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.deployment.web.ContextParameter;
import com.sun.enterprise.deployment.web.EnvironmentEntry;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.MimeMapping;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.SecurityRole;
import com.sun.enterprise.deployment.web.SecurityRoleReference;
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import com.sun.enterprise.deployment.web.SessionConfig;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.deployment.common.JavaEEResourceType;

/**
 * WebBundleDescriptor mock object
 *
 * @author lprimak
 */
public class DummyWebBundleDescriptor extends WebBundleDescriptor {
    private final Set<String> whitelistPackages = new HashSet<>();


    @Override
    public void addWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addDefaultWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addJndiNameEnvironment(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDefaultSpecVersion() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection getNamedDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<NamedReferencePair> getNamedReferencePairs() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getContextRoot() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContextRoot(String contextRoot) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<WebComponentDescriptor> getWebComponentDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected WebComponentDescriptor combineWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SessionConfig getSessionConfig() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSessionConfig(SessionConfig sessionConfig) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasServiceReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeServiceReferenceDescriptor(ServiceReferenceDescriptor serviceRef) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ServiceReferenceDescriptor _getServiceReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineServiceReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<ResourceEnvReferenceDescriptor> getResourceEnvReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeResourceEnvReferenceDescriptor(ResourceEnvReferenceDescriptor resourceEnvRefReference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceEnvReferenceDescriptor getResourceEnvReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ResourceEnvReferenceDescriptor _getResourceEnvReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineResourceEnvReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineResourceDescriptors(JndiNameEnvironment env, JavaEEResourceType javaEEResourceType) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<MimeMapping> getMimeMappingsSet() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMimeMappings(Set<MimeMapping> mimeMappings) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<MimeMapping> getMimeMappings() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String addMimeMapping(MimeMapping mimeMapping) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocaleEncodingMappingListDescriptor getLocaleEncodingMappingListDescriptor() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLocaleEncodingMappingListDescriptor(LocaleEncodingMappingListDescriptor lemDesc) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeMimeMapping(MimeMapping mimeMapping) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<String> getWelcomeFiles() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<String> getWelcomeFilesSet() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addWelcomeFile(String fileUri) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeWelcomeFile(String fileUri) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setWelcomeFiles(Set<String> welcomeFiles) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<ContextParameter> getContextParametersSet() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<ContextParameter> getContextParameters() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addContextParameter(ContextParameter contextParameter) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addContextParameter(EnvironmentProperty contextParameter) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeContextParameter(ContextParameter contextParameter) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boolean isDistributable() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDistributable(Boolean isDistributable) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<EjbReference> getEjbReferences() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EjbReference> getEjbReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EjbReferenceDescriptor getEjbReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EjbReference getEjbReference(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected EjbReference _getEjbReference(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ResourceReferenceDescriptor _getResourceReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected EntityManagerFactoryReferenceDescriptor _getEntityManagerFactoryReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEntityManagerFactoryReferenceDescriptor(EntityManagerFactoryReferenceDescriptor reference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineEntityManagerFactoryReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected EntityManagerReferenceDescriptor _getEntityManagerReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEntityManagerReferenceDescriptor(EntityManagerReferenceDescriptor reference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineEntityManagerReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<? extends PersistenceUnitDescriptor> findReferencedPUs() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EnvironmentEntry> getEnvironmentProperties() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeEjbReferenceDescriptor(EjbReference ejbReferenceDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineEjbReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<ResourceReferenceDescriptor> getResourceReferences() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineResourceReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor messageDestRef) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor msgDestRef) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MessageDestinationReferenceDescriptor getMessageDestinationReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected MessageDestinationReferenceDescriptor _getMessageDestinationReferenceByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineMessageDestinationReferenceDescriptors(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPostConstructDescriptor(LifecycleCallbackDescriptor postConstructDesc) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combinePostConstructDescriptors(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor preDestroyDesc) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combinePreDestroyDescriptors(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected List<InjectionCapable> getInjectableResourcesByClass(String className, JndiNameEnvironment jndiNameEnv) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InjectionInfo getInjectionInfoByClass(Class clazz) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<SecurityRoleDescriptor> getSecurityRoles() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSecurityRole(SecurityRole securityRole) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSecurityRole(SecurityRoleDescriptor securityRole) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SecurityRoleReference getSecurityRoleReferenceByName(String compName, String roleName) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isDenyUncoveredHttpMethods() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineSecurityConstraints(Set<SecurityConstraint> firstScSet, Set<SecurityConstraint> secondScSet) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<SecurityConstraint> getSecurityConstraintsSet() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<SecurityConstraint> getSecurityConstraints() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SecurityConstraint> getSecurityConstraintsForUrlPattern(String urlPattern) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSecurityConstraint(SecurityConstraint securityConstraint) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeSecurityConstraint(SecurityConstraint securityConstraint) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<WebComponentDescriptor> getServletDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<WebComponentDescriptor> getJspDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<EnvironmentEntry> getEnvironmentEntrySet() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<EnvironmentEntry> getEnvironmentEntries() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEnvironmentEntry(EnvironmentEntry environmentEntry) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected EnvironmentProperty _getEnvironmentPropertyByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeEnvironmentProperty(EnvironmentProperty environmentProperty) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeEnvironmentEntry(EnvironmentEntry environmentEntry) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineEnvironmentEntries(JndiNameEnvironment env) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LoginConfiguration getLoginConfiguration() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLoginConfiguration(LoginConfiguration loginConfiguration) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineLoginConfiguration(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WebComponentDescriptor getWebComponentByName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WebComponentDescriptor getWebComponentByCanonicalName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WebComponentDescriptor[] getWebComponentByImplName(String name) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<ServletFilter> getServletFilters() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<ServletFilter> getServletFilterDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addServletFilter(ServletFilter ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeServletFilter(ServletFilter ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineServletFilters(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<ServletFilterMapping> getServletFilterMappings() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<ServletFilterMapping> getServletFilterMappingDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addServletFilterMapping(ServletFilterMapping ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeServletFilterMapping(ServletFilterMapping ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void moveServletFilterMapping(ServletFilterMapping ref, int relPos) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineServletFilterMappings(WebBundleDescriptor webBundleDescriptor) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<AppListenerDescriptor> getAppListeners() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<AppListenerDescriptor> getAppListenerDescriptors() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAppListeners(Collection<? extends AppListenerDescriptor> c) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAppListenerDescriptor(AppListenerDescriptor ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAppListenerDescriptorToFirst(AppListenerDescriptor ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAppListenerDescriptor(AppListenerDescriptor ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void moveAppListenerDescriptor(AppListenerDescriptor ref, int relPos) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isShowArchivedRealPathEnabled() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setShowArchivedRealPathEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getServletReloadCheckSecs() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setServletReloadCheckSecs(int secs) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasWebServiceClients() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean removeVectorItem(Vector<? extends Object> list, Object ref) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void moveVectorItem(Vector list, Object ref, int rpos) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putJarNameWebFragmentNamePair(String jarName, String webFragName) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> getJarNameToWebFragmentNameMap() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> getUrlPatternToServletNameMap() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resetUrlPatternToServletNameMap() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getOrderedLibs() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addOrderedLib(String libName) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void combineInjectionTargets(EnvironmentProperty env1, EnvironmentProperty env2) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void printCommon(StringBuffer toStringBuffer) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArchiveType getModuleType() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ComponentVisitor getBundleVisitor() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DescriptorVisitor getTracerVisitor() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDeploymentDescriptorDir() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SunWebApp getSunDescriptor() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSunDescriptor(SunWebApp webApp) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setExtensionProperty(String key, String value) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasExtensionProperty(String key) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getServletInitializersEnabled() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setServletInitializersEnabled(boolean tf) {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isWhitelistEnabled() {
        return !whitelistPackages.isEmpty();
    }

    @Override
    public Set<String> getWhitelistPackages() {
        return ImmutableSet.copyOf(whitelistPackages);
    }

    @Override
    public void addWhitelistPackage(String aPackage) {
        whitelistPackages.add(aPackage);
    }

    @Override
    public Set<String> getConflictedMimeMappingExtensions() {
        throw new UnsupportedOperationException("Invalid Call"); //To change body of generated methods, choose Tools | Templates.
    }
}
