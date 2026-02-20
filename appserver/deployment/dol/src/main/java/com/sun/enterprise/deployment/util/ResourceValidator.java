/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.deployment.Application;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;
import org.glassfish.internal.api.PostStartupRunLevel;

/**
 * Checks that resources referenced in an application actually exist
 * @author Krishna Deepak on 6/9/17.
 * @since 4.1.2.174
 */
@Service
@RunLevel(value = PostStartupRunLevel.VAL, mode = RunLevel.RUNLEVEL_MODE_VALIDATING)
public class ResourceValidator implements EventListener, ResourceValidatorVisitor {

    public static final Logger deplLogger = com.sun.enterprise.deployment.util.DOLUtils.deplLogger;

    @LogMessageInfo(
            message = "JNDI lookup failed for the resource: Name: {0}, Lookup: {1}, Type: {2}.",
            level = "SEVERE",
            cause = "JNDI lookup for the specified resource failed.",
            action = "Configure the required resources before deploying the application.",
            comment = "For the method validateJNDIRefs of com.sun.enterprise.deployment.util.ResourceValidator."
    )
    private static final String RESOURCE_REF_JNDI_LOOKUP_FAILED = "AS-DEPLOYMENT-00026";
    
    @LogMessageInfo(message = "Skipping resource validation")
    private static final String SKIP_RESOURCE_VALIDATION = "AS-DEPLOYMENT-00028";

    @LogMessageInfo(
            message = "Resource Adapter not present: RA Name: {0}, Type: {1}.",
            level = "SEVERE",
            cause = "Resource apapter specified is invalid.",
            action = "Configure the required resource adapter."
    )
    private static final String RESOURCE_REF_INVALID_RA = "AS-DEPLOYMENT-00027";

    private String target;

    @Inject
    private Events events;

    @Inject
    private Domain domain;
    
    @Inject
    private JavaEEContextUtil contextUtil;

    private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ResourceValidator.class);

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Server server;

    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.AFTER_APPLICATION_CLASSLOADER_CREATION)) {
            DeploymentContext deploymentContext = (DeploymentContext) event.hook();
            Application application = deploymentContext.getModuleMetaData(Application.class);
            DeployCommandParameters commandParams = deploymentContext.getCommandParameters(DeployCommandParameters.class);
            target = commandParams.target;
            if (System.getProperty("deployment.resource.validation", "true").equals("false")) {
                deplLogger.log(Level.INFO, SKIP_RESOURCE_VALIDATION);
                return;
            }
            if (application == null) {
                return;
            }
            AppResources appResources = new AppResources();
            // Puts all resources found in the application via annotation or xml into appResources
            parseResources(deploymentContext, application, appResources);

            // Ensure we have a valid component invocation before triggering lookups
            try (Context ctx = contextUtil.empty().pushContext()) {
                validateResources(deploymentContext, application, appResources);
            }
        }
    }

    /**
     * Store all the resources before starting the validation.
     */
    private void parseResources(DeploymentContext deploymentContext, Application application, AppResources appResources) {
        parseResourcesBd(application, appResources);
        for (BundleDescriptor bd : application.getBundleDescriptors()) {
            if (bd instanceof WebBundleDescriptor || bd instanceof ApplicationClientDescriptor) {
                parseResourcesBd(bd, appResources);
            }
            if (bd instanceof EjbBundleDescriptor) {
                // Resources from Java files in the ejb.jar which are neither an EJB nor a managed bean are stored here.
                // Skip validation for them, validate only Managed Beans.
                for (ManagedBeanDescriptor mbd : bd.getManagedBeans()) {
                    parseResources(mbd, (JndiNameEnvironment) bd, appResources);
                }
                EjbBundleDescriptor ebd = (EjbBundleDescriptor) bd;
                for (EjbDescriptor ejb : ebd.getEjbs()) {
                    parseEJB(ejb, appResources);
                }
            }
        }

        parseManagedBeans(application, appResources);

        // Parse AppScoped resources
        String appName = DOLUtils.getApplicationName(application);
        Map<String, List<String>> resourcesList
                = (Map<String, List<String>>) deploymentContext.getTransientAppMetadata().get(ResourceConstants.APP_SCOPED_RESOURCES_JNDI_NAMES);
        appResources.storeAppScopedResources(resourcesList, appName);
    }

    /**
     * Code logic from BaseContainer.java. Store portable and non-portable JNDI
     * names in our namespace. Internal JNDI names not processed as they will
     * not be called from an application.
     *
     * @param ejb
     */
    private void parseEJB(EjbDescriptor ejb, AppResources appResources) {
        String javaGlobalName = getJavaGlobalJndiNamePrefix(ejb);

        boolean disableNonPortableJndiName = false;
        // TODO: Need to get the value of system-property server.ejb-container.property.disable-nonportable-jndi-names
        Boolean disableInDD = ejb.getEjbBundleDescriptor().getDisableNonportableJndiNames();
        if (disableInDD != null) {  // explicitly set in glassfish-ejb-jar.xml
            disableNonPortableJndiName = disableInDD;
        }

        String glassfishSpecificJndiName = null;
        if (!disableNonPortableJndiName) {
            glassfishSpecificJndiName = ejb.getJndiName();
        }
        if ((glassfishSpecificJndiName != null)
                && (glassfishSpecificJndiName.equals("")
                || glassfishSpecificJndiName.equals(javaGlobalName))) {
            glassfishSpecificJndiName = null;
        }

        // used to decide whether the javaGlobalName needs to be stored
        int countPortableJndiNames = 0;

        // interfaces now
        if (ejb.isRemoteInterfacesSupported()) {
            String intf = ejb.getHomeClassName();
            String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
            appResources.storeInNamespace(fullyQualifiedJavaGlobalName, ejb);
            countPortableJndiNames++;
            // non-portable
            if (glassfishSpecificJndiName != null) {
                appResources.storeInNamespace(glassfishSpecificJndiName, ejb);
            }
        }

        if (ejb.isRemoteBusinessInterfacesSupported()) {
            int count = 0;
            for (String intf : ejb.getRemoteBusinessClassNames()) {
                count++;
                String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
                appResources.storeInNamespace(fullyQualifiedJavaGlobalName, ejb);
                countPortableJndiNames++;
                // non-portable - interface specific
                if (glassfishSpecificJndiName != null) {
                    String remoteJndiName = getRemoteEjbJndiName(true, intf, glassfishSpecificJndiName);
                    appResources.storeInNamespace(remoteJndiName, ejb);
                }
            }
            // non-portable - if only one remote business interface exists and no remote home interfaces exist,
            // then by default this can be used to lookup the remote interface.
            if (glassfishSpecificJndiName != null && !ejb.isRemoteInterfacesSupported() && count == 1) {
                appResources.storeInNamespace(glassfishSpecificJndiName, ejb);
            }
        }

        if (ejb.isLocalInterfacesSupported()) {
            String intf = ejb.getLocalHomeClassName();
            String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
            appResources.storeInNamespace(fullyQualifiedJavaGlobalName, ejb);
            countPortableJndiNames++;
        }

        if (ejb.isLocalBusinessInterfacesSupported()) {
            for (String intf : ejb.getLocalBusinessClassNames()) {
                String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
                appResources.storeInNamespace(fullyQualifiedJavaGlobalName, ejb);
                countPortableJndiNames++;
            }
        }

        if (ejb.isLocalBean()) {
            String intf = ejb.getEjbClassName();
            String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
            appResources.storeInNamespace(fullyQualifiedJavaGlobalName, ejb);
            countPortableJndiNames++;
        }

        if (countPortableJndiNames == 1) {
            appResources.storeInNamespace(javaGlobalName, ejb);
        }
        parseResources(ejb, appResources);
    }

    private String getJavaGlobalJndiNamePrefix(EjbDescriptor ejbDescriptor) {

        String appName = null;

        Application app = ejbDescriptor.getApplication();
        if (!app.isVirtual()) {
            appName = ejbDescriptor.getApplication().getAppName();
        }

        EjbBundleDescriptor ejbBundle = ejbDescriptor.getEjbBundleDescriptor();
        String modName = ejbBundle.getModuleDescriptor().getModuleName();

        String ejbName = ejbDescriptor.getName();

        StringBuilder javaGlobalPrefix = new StringBuilder("java:global/");

        if (appName != null) {
            javaGlobalPrefix.append(appName);
            javaGlobalPrefix.append("/");
        }

        javaGlobalPrefix.append(modName);
        javaGlobalPrefix.append("/");
        javaGlobalPrefix.append(ejbName);

        return javaGlobalPrefix.toString();
    }

    private String getRemoteEjbJndiName(EjbReferenceDescriptor refDesc) {

        String intf = refDesc.isEJB30ClientView()
                ? refDesc.getEjbInterface() : refDesc.getHomeClassName();

        return getRemoteEjbJndiName(refDesc.isEJB30ClientView(), intf, refDesc.getJndiName());
    }

    private String getRemoteEjbJndiName(boolean businessView, String interfaceName, String jndiName) {
        String returnValue = jndiName;

        String portableFullyQualifiedPortion = "!" + interfaceName;
        String glassfishFullyQualifiedPortion = "#" + interfaceName;

        if (businessView) {
            if (!jndiName.startsWith("corbaname:")) {
                if (jndiName.startsWith(ResourceConstants.JAVA_GLOBAL_SCOPE_PREFIX)) {
                    returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
                } else {
                    returnValue = checkFullyQualifiedJndiName(jndiName, glassfishFullyQualifiedPortion);
                }
            }
        } else {
            // Only in the portable global case, convert to a fully-qualified name
            if (jndiName.startsWith(ResourceConstants.JAVA_GLOBAL_SCOPE_PREFIX)) {
                returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
            }
        }

        return returnValue;
    }

    private static String checkFullyQualifiedJndiName(String origJndiName, String fullyQualifiedPortion) {
        String returnValue = origJndiName;
        if (!origJndiName.endsWith(fullyQualifiedPortion)) {
            returnValue = origJndiName + fullyQualifiedPortion;
        }
        return returnValue;
    }

    private void parseManagedBeans(Application application, AppResources appResources) {
        for (BundleDescriptor bd : application.getBundleDescriptors()) {
            for (ManagedBeanDescriptor managedBean : bd.getManagedBeans()) {
                appResources.storeInNamespace(managedBean.getGlobalJndiName(), (JndiNameEnvironment) bd);
            }
        }
    }

    private void parseResourcesBd(BundleDescriptor bd, AppResources appResources) {
        if (!(bd instanceof JndiNameEnvironment)) {
            return;
        }
        JndiNameEnvironment env = (JndiNameEnvironment) bd;
        for (Object next : env.getResourceReferenceDescriptors()) {
            parseResources((ResourceReferenceDescriptor) next, env, appResources);
        }

        for (Object next : env.getResourceEnvReferenceDescriptors()) {
            parseResources((ResourceEnvReferenceDescriptor) next, env, appResources);
        }

        for (Object next : env.getMessageDestinationReferenceDescriptors()) {
            parseResources((MessageDestinationReferenceDescriptor) next, env, appResources);
        }

        for (Object next : env.getEnvironmentProperties()) {
            parseResources((EnvironmentProperty) next, env, appResources);
        }

        for (Object next : env.getAllResourcesDescriptors()) {
            parseResources((ResourceDescriptor) next, env, appResources);
        }

        for (Object next : env.getEntityManagerReferenceDescriptors()) {
            storeInNamespace(((EntityManagerReferenceDescriptor) next).getName(), env, appResources);
        }

        for (Object next : env.getEntityManagerFactoryReferenceDescriptors()) {
            storeInNamespace(((EntityManagerFactoryReferenceDescriptor) next).getName(), env, appResources);
        }

        for (Object next : env.getEjbReferenceDescriptors()) {
            parseResources((EjbReferenceDescriptor) next, env, appResources);
        }

        for (Object next : env.getServiceReferenceDescriptors()) {
            parseResources((ServiceReferenceDescriptor) next, env, appResources);
        }

        for (PersistenceUnitsDescriptor pus : bd.getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            for (PersistenceUnitDescriptor pu : pus.getPersistenceUnitDescriptors()) {
                parseResources(pu, env, appResources);
            }
        }

        for (ManagedBeanDescriptor mbd : bd.getManagedBeans()) {
            parseResources(mbd, env, appResources);
        }
    }

    /**
     * Store resources in ResourceRefDescriptor.
     */
    private void parseResources(ResourceReferenceDescriptor resRef, JndiNameEnvironment env, AppResources appResources) {
        resRef.checkType();
        String name = getLogicalJNDIName(resRef.getName(), env);
        String type = resRef.getType();
        String jndiName = resRef.getJndiName();
        AppResource resRefResource = new AppResource(name, jndiName, type, env, true);

        if (resRef.isURLResource()) {
            if (jndiName != null && !(jndiName.startsWith(ResourceConstants.JAVA_SCOPE_PREFIX))) {
                try {
                    // for jndi-name like "http://localhost:8080/index.html"
                    new java.net.URL(jndiName);
                    resRefResource.noValidation();
                } catch (MalformedURLException e) {
                    // If jndi-name is not an actual url, we might want to lookup the name
                }
            }
        }
        if (resRef.isWebServiceContext()) {
            resRefResource.noValidation();
        }

        appResources.store(resRefResource);
    }

    /**
     * Store resources in ResourceEnvRefDescriptor.
     */
    private void parseResources(ResourceEnvReferenceDescriptor resEnvRef, JndiNameEnvironment env, AppResources appResources) {
        resEnvRef.checkType();
        String name = getLogicalJNDIName(resEnvRef.getName(), env);
        String type = resEnvRef.getType();
        String jndiName = resEnvRef.getJndiName();
        AppResource resEnvRefResource = new AppResource(name, jndiName, type, env, true);

        if (resEnvRef.isEJBContext() || resEnvRef.isValidator() || resEnvRef.isValidatorFactory() || resEnvRef.isCDIBeanManager()) {
            resEnvRefResource.noValidation();
        }

        appResources.store(resEnvRefResource);
    }

    /**
     * If the message destination ref is linked to a message destination, fetch
     * the linked destination and validate it. We might be duplicating our
     * validation efforts since we are already validating message destination
     * separately.
     */
    private void parseResources(MessageDestinationReferenceDescriptor msgDestRef, JndiNameEnvironment env, AppResources appResources) {
        String name = getLogicalJNDIName(msgDestRef.getName(), env);
        String jndiName;
        if (msgDestRef.isLinkedToMessageDestination()) {
            jndiName = msgDestRef.getMessageDestination().getJndiName();
        } else {
            jndiName = msgDestRef.getJndiName();
        }
        appResources.store(new AppResource(name, jndiName, msgDestRef.getType(), env, true));
    }

    /**
     * Store references to environment entries. Also validate custom resources
     * of primitive data types.
     */
    private void parseResources(EnvironmentProperty envProp, JndiNameEnvironment env, AppResources appResources) {
        String name = getLogicalJNDIName(envProp.getName(), env);
        String jndiName = "";
        if (envProp.hasLookupName()) {
            jndiName = envProp.getLookupName();
        } // error handling for mapped name null case done in getMappedName
        else if (envProp.getMappedName().length() > 0) {
            jndiName = envProp.getMappedName();
        }

        AppResource envPropResource = new AppResource(name, jndiName, envProp.getType(), env, true);
        // If lookup/mapped name is not present, then we do not need to validate.
        if (jndiName.length() == 0) {
            envPropResource.noValidation();
        }

        appResources.store(envPropResource);

        // Store EnvProps even if they do not have a valid lookup element
        appResources.storeInNamespace(name, env);
    }

    /**
     * Logic from EjbNamingReferenceManagerImpl.java - Here EJB references get
     * resolved
     */
    private void parseResources(Application application, EjbReferenceDescriptor ejbRef, JndiNameEnvironment env, AppResources appResources) {
        String name = getLogicalJNDIName(ejbRef.getName(), env);
        // we only need to worry about those references which are not linked yet
        if (ejbRef.getEjbDescriptor() != null) {
            appResources.storeInNamespace(name, env);
            return;
        }

        String jndiName = "";
        // Should we use an inverse approach i.e., skip validation only in special cases?
        // Not sure if that is required as the below approach works fine while resolving EJB references
        boolean validationRequired = false;

        // local
        if (ejbRef.isLocal()) {
            // mapped name has no meaning for local ejb-ref as non-portable JNDI names don't have any meaning in this case?
            if (ejbRef.hasLookupName()) {
                jndiName = ejbRef.getLookupName();
                validationRequired = true;
            }
        } // remote
        else {
            // mapped-name takes precedence over lookup name
            if (!ejbRef.hasJndiName() && ejbRef.hasLookupName()) {
                jndiName = ejbRef.getLookupName();
                validationRequired = true;
            } // TODO: A case skipped from EjbNamingRefManager
            else if (ejbRef.hasJndiName()
                    && ejbRef.getJndiName().startsWith("java:app/")
                    && !ejbRef.getJndiName().startsWith("java:app/env/")) {
                // Why does the below logic exist in the EjbNamingRefMan code?
                // Intentionally or not, this resolves the java:app mapped names
                // Seems suspicious as the corresponding java:global case is handled in the getRemoteEjbJndiName function call
                String remoteJndiName = ejbRef.getJndiName();

                String appName = DOLUtils.getApplicationName(application);;
                String newPrefix = "java:global/" + appName + "/";

                int javaAppLength = "java:app/".length();
                jndiName = newPrefix + remoteJndiName.substring(javaAppLength);
                validationRequired = true;
            } else {
                String remoteJndiName = getRemoteEjbJndiName(ejbRef);
                // TODO: CORBA case
                if (!remoteJndiName.startsWith("corbaname:")) {
                    validationRequired = true;
                    jndiName = remoteJndiName;
                }
            }
        }

        appResources.store(new AppResource(name, jndiName, ejbRef.getType(), env, validationRequired));
    }

    private void parseResources(ServiceReferenceDescriptor serviceRef, JndiNameEnvironment env, AppResources appResources) {
        String name = getLogicalJNDIName(serviceRef.getName(), env);
        if (serviceRef.hasLookupName()) {
            appResources.store(new AppResource(name, serviceRef.getLookupName(), serviceRef.getType(), env, true));
        } else {
            appResources.storeInNamespace(name, env);
        }
    }

    /**
     * Store the resource definitions in our namespace. CFD and AODD are not
     * valid in an AppClient. O/w need to validate the ra-name in them.
     * <p>
     * CFD = Connection Factory Definiton, AODD = AdministeredObjectDefinitionDescriptor
     */
    private void parseResources(ResourceDescriptor resourceDescriptor, JndiNameEnvironment env, AppResources appResources) {
        JavaEEResourceType type = resourceDescriptor.getResourceType();
        if (type.equals(JavaEEResourceType.CFD) || type.equals(JavaEEResourceType.AODD)) {
            if (env instanceof ApplicationClientDescriptor) {
                return;
            }
            // No need to type check as CFD and AODD extend from AbstractConnectorResourceDescriptor
            AbstractConnectorResourceDescriptor acrd = (AbstractConnectorResourceDescriptor) resourceDescriptor;
            appResources.store(new AppResource(resourceDescriptor.getName(), acrd.getResourceAdapter(), type.toString(), env, true));
        } else {
            // nothing to validate here. store the definitions in our namespace.
            storeInNamespace(resourceDescriptor.getName(), env, appResources);
        }
    }

    /**
     * Record the Data Source specified in PUD.
     */
    private void parseResources(PersistenceUnitDescriptor pu, JndiNameEnvironment env, AppResources appResources) {
        String jtaDataSourceName = pu.getJtaDataSource();
        String nonJtaDataSourceName = pu.getNonJtaDataSource();

        if (jtaDataSourceName != null && jtaDataSourceName.length() > 0) {
            appResources.store(new AppResource(pu.getName(), jtaDataSourceName, "javax.sql.DataSource", env, true));
        }
        if (nonJtaDataSourceName != null && nonJtaDataSourceName.length() > 0) {
            appResources.store(new AppResource(pu.getName(), nonJtaDataSourceName, "javax.sql.DataSource", env, true));
        }
    }

    private void parseResources(ManagedBeanDescriptor managedBean, JndiNameEnvironment env, AppResources appResources) {
        for (Object next : managedBean.getResourceReferenceDescriptors()) {
            parseResources((ResourceReferenceDescriptor) next, env, appResources);
        }

        for (Object next : managedBean.getResourceEnvReferenceDescriptors()) {
            parseResources((ResourceEnvReferenceDescriptor) next, env, appResources);
        }

        for (Object next : managedBean.getMessageDestinationReferenceDescriptors()) {
            parseResources((MessageDestinationReferenceDescriptor) next, env, appResources);
        }

        for (Object next : managedBean.getEjbReferenceDescriptors()) {
            parseResources((EjbReferenceDescriptor) next, env, appResources);
        }

        for (Object next : managedBean.getEnvironmentProperties()) {
            parseResources((EnvironmentProperty) next, env, appResources);
        }

        for (Object next : env.getAllResourcesDescriptors()) {
            parseResources((ResourceDescriptor) next, env, appResources);
        }
    }

    private void parseResources(EjbDescriptor ejb, AppResources appResources) {
        for (Object next : ejb.getResourceReferenceDescriptors()) {
            parseResources((ResourceReferenceDescriptor) next, ejb, appResources);
        }

        for (Object next : ejb.getResourceEnvReferenceDescriptors()) {
            parseResources((ResourceEnvReferenceDescriptor) next, ejb, appResources);
        }

        for (Object next : ejb.getMessageDestinationReferenceDescriptors()) {
            parseResources((MessageDestinationReferenceDescriptor) next, ejb, appResources);
        }

        for (Object next : ejb.getEnvironmentProperties()) {
            parseResources((EnvironmentProperty) next, ejb, appResources);
        }

        for (Object next : ejb.getEjbReferenceDescriptors()) {
            parseResources((EjbReferenceDescriptor) next, ejb, appResources);
        }

        for (Object next : ejb.getAllResourcesDescriptors()) {
            parseResources((ResourceDescriptor) next, ejb, appResources);
        }
    }

    private void storeInNamespace(String name, JndiNameEnvironment env, AppResources appResources) {
        String logicalJNDIName = getLogicalJNDIName(name, env);
        appResources.storeInNamespace(logicalJNDIName, env);
    }

    /**
     * @param rawName to be converted
     * @return The logical JNDI name which has a java: prefix
     */
    private String getLogicalJNDIName(String rawName, JndiNameEnvironment env) {
        String logicalJndiName = rawNameToLogicalJndiName(rawName);
        boolean treatComponentAsModule = DOLUtils.getTreatComponentAsModule(env);
        if (treatComponentAsModule && logicalJndiName.startsWith(ResourceConstants.JAVA_COMP_SCOPE_PREFIX)) {
            logicalJndiName = logicalCompJndiNameToModule(logicalJndiName);
        }
        return logicalJndiName;
    }

    /**
     * Convert name from java:comp/xxx to java:module/xxx.
     */
    private String logicalCompJndiNameToModule(String logicalCompName) {
        String tail = logicalCompName.substring(ResourceConstants.JAVA_COMP_SCOPE_PREFIX.length());
        return ResourceConstants.JAVA_MODULE_SCOPE_PREFIX + tail;
    }

    /**
     * Attach default prefix - java:comp/env/.
     */
    private String rawNameToLogicalJndiName(String rawName) {
        return (rawName.startsWith(ResourceConstants.JAVA_SCOPE_PREFIX))
                ? rawName : ResourceConstants.JAVA_COMP_ENV_SCOPE_PREFIX + rawName;
    }

    /**
     * Convert JNDI names beginning with java:module and java:app to their
     * corresponding java:global names.
     *
     * @return the converted name with java:global JNDI prefix.
     */
    private String convertModuleOrAppJNDIName(Application application, String jndiName, JndiNameEnvironment env) {
        BundleDescriptor bd = null;
        if (env instanceof EjbDescriptor) {
            bd = ((EjbDescriptor) env).getEjbBundleDescriptor();
        } else if (env instanceof BundleDescriptor) {
            bd = (BundleDescriptor) env;
        }

        if (jndiName == null) {
            return null;
        }

        if (bd != null) {
            String appName = null;
            if (!application.isVirtual()) {
                appName = application.getAppName();
            }
            String moduleName = bd.getModuleDescriptor().getModuleName();
            StringBuilder javaGlobalName = new StringBuilder("java:global/");
            if (jndiName.startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX)) {
                if (appName != null) {
                    javaGlobalName.append(appName);
                    javaGlobalName.append("/");
                }

                // Replace java:app/ with the fully-qualified global portion
                int javaAppLength = ResourceConstants.JAVA_APP_SCOPE_PREFIX.length();
                javaGlobalName.append(jndiName.substring(javaAppLength));
            } else if (jndiName.startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX)) {
                if (appName != null) {
                    javaGlobalName.append(appName);
                    javaGlobalName.append("/");
                }

                javaGlobalName.append(moduleName);
                javaGlobalName.append("/");

                // Replace java:module/ with the fully-qualified global portion
                int javaModuleLength = ResourceConstants.JAVA_MODULE_SCOPE_PREFIX.length();
                javaGlobalName.append(jndiName.substring(javaModuleLength));
            } else {
                return "";
            }
            return javaGlobalName.toString();
        }
        return "";
    }

    /**
     * Start of validation logic.
     */
    private void validateResources(DeploymentContext deploymentContext, Application application, AppResources appResources) {
        for (AppResource resource : appResources.myResources) {
            if (!resource.validate) {
                continue;
            }
            if (resource.getType().equals("CFD") || resource.getType().equals("AODD")) {
                validateRAName(application, resource);
            } else {
                validateJNDIRefs(deploymentContext, application, resource, appResources.myNamespace);
            }
        }
        // Validate the ra-names of app scoped resources
        // RA-name and the type of this resource are stored
        List<Map.Entry<String, String>> raNames = 
                (List<Map.Entry<String, String>>) deploymentContext.getTransientAppMetadata().get(ResourceConstants.APP_SCOPED_RESOURCES_RA_NAMES);
        if (raNames == null) {
            return;
        }
        for (Map.Entry<String, String> entry : raNames) {
            validateRAName(application, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Validate the resource adapter names of @CFD, @AODD.
     */
    private void validateRAName(Application application, AppResource resource) {
        validateRAName(application, resource.getJndiName(), resource.getType());
    }

    /**
     * Strategy to validate the resource adapter name:
     *
     * 1) In case of stand-alone RA, look in the domain.xml and for default
     * system RA's 2) In case of embedded RA, compare it with names of RAR
     * descriptors
     *
     * In case of null ra name, we fail the deployment.
     */
    private void validateRAName(Application application, String raname, String type) {
        // No ra-name specified
        if (raname == null || raname.length() == 0) {
            deplLogger.log(Level.SEVERE, RESOURCE_REF_INVALID_RA,
                    new Object[]{null, type});
            throw new DeploymentException(localStrings.getLocalString("enterprise.deployment.util.ra.validation",
                    "Resource Adapter not present: RA Name: {0}, Type: {1}.",
                    null, type));
        }
        int poundIndex = raname.indexOf("#");

        // Pound not present: check for app named raname in domain.xml, check for system ra's
        if (poundIndex < 0) {
            if (domain.getApplications().getApplication(raname) != null) {
                return;
            }
            // System RA's - Copied from ConnectorConstants.java
            if (raname.equals("jmsra") || raname.equals("__ds_jdbc_ra") || raname.equals("jaxr-ra")
                    || raname.equals("__cp_jdbc_ra") || raname.equals("__xa_jdbc_ra") || raname.equals("__dm_jdbc_ra")) {
                return;
            }
            if (isEmbedded(application, raname)) {
                return;
            }
        } // Embedded RA
        // In case the app name does not match, we fail the deployment
        else if (raname.substring(0, poundIndex).equals(application.getAppName())) {
            raname = raname.substring(poundIndex + 1);
            if (isEmbedded(application, raname)) {
                return;
            }
        }
        deplLogger.log(Level.SEVERE, RESOURCE_REF_INVALID_RA,
                new Object[]{raname, type});
        throw new DeploymentException(localStrings.getLocalString(
                "enterprise.deployment.util.ra.validation",
                "Resource Adapter not present: RA Name: {0}, Type: {1}.",
                raname, type));
    }

    private boolean isEmbedded(Application application, String raname) {
        String ranameWithRAR = raname + ".rar";
        // check for rar named this
        for (BundleDescriptor bd : application.getBundleDescriptors(ConnectorDescriptor.class)) {
            if (raname.equals(bd.getModuleName()) || ranameWithRAR.equals(bd.getModuleName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Strategy for validating a given jndi name 1) Check in domain.xml 2) Check
     * in the resources defined within the app. These have not been binded to
     * the namespace yet. 3) Check for resources defined by an earlier
     * application.
     *
     * In case a null jndi name is passed, we fail the deployment.
     *
     * @param resource the resource to be validated.
     */
    private void validateJNDIRefs(DeploymentContext deploymentContext, Application application, AppResource resource, JNDINamespace namespace) {
        // In case lookup is not present, check if another resource with the same name exists
        if (!resource.hasLookup() && !namespace.find(resource.getName(), resource.getEnv())) {
            deplLogger.log(Level.SEVERE, RESOURCE_REF_JNDI_LOOKUP_FAILED,
                    new Object[]{resource.getName(), null, resource.getType()});
            throw new DeploymentException(localStrings.getLocalString("enterprise.deployment.util.resource.validation",
                    "JNDI lookup failed for the resource: Name: {0}, Lookup: {1}, Type: {2}",
                    resource.getName(), null, resource.getType()));
        }
        String jndiName = TranslatedConfigView.expandConfigValue(resource.getJndiName());
        if (jndiName == null) {
            // there's no mapping in this resource, but it exists in JNDI namespace, so it's validated by other ref.
            return;
        }

        JndiNameEnvironment env = resource.getEnv();

        if (isResourceInDomainXML(jndiName) || isDefaultResource(jndiName)) {
            return;
        }
       

        // Managed Bean & EJB portable JNDI names
        if (jndiName.startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX) || jndiName.startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX)) {
            // first look with the name
            if (namespace.find(jndiName, env)) {
                return;
            }
            
            String newName = convertModuleOrAppJNDIName(application, jndiName, resource.getEnv());
            if (namespace.find(newName, env)) {
                return;
            }
            // try actual lookup
            try {
                InitialContext ctx = new InitialContext();
                ctx.lookup(newName);
                return;
            } catch(NamingException ne) {
                
            }
        }

        // EJB Non-portable JNDI names
        if (!jndiName.startsWith(ResourceConstants.JAVA_SCOPE_PREFIX)) {
            if (namespace.find(jndiName, env)) {
                return;
            }
        }

        // convert comp to module if req
        String convertedJndiName = getLogicalJNDIName(jndiName, env);
        if (namespace.find(convertedJndiName, env)) {
            return;
        }

        try {
            if (loadOnCurrentInstance(deploymentContext)) {
                InitialContext ctx = new InitialContext();
                ctx.lookup(jndiName);
            }
        } catch (NamingException e) {
            deplLogger.log(Level.SEVERE, RESOURCE_REF_JNDI_LOOKUP_FAILED,
                    new Object[]{resource.getName(), jndiName, resource.getType()});
            throw new DeploymentException(localStrings.getLocalString(
                    "enterprise.deployment.util.resource.validation",
                    "JNDI lookup failed for the resource: Name: {0}, Lookup: {1}, Type: {2}",
                    resource.getName(), jndiName, resource.getType()), e);
        }
    }

    /**
     * Validate the given resource in the corresponding target using domain.xml
     * server beans. For resources defined outside the application.
     *
     * @param jndiName to be validated
     * @return True if resource is present in domain.xml in the corresponding
     * target. False otherwise.
     */
    private boolean isResourceInDomainXML(String jndiName) {
        if (jndiName == null) {
            return false;
        }

        Server svr = domain.getServerNamed(target);
        if (svr != null) {
            return svr.isResourceRefExists(jndiName);
        }

        return false;
    }

    /**
     * Default resources provided by GF.
     */
    private boolean isDefaultResource(String jndiName) {
        return (jndiName != null
                && (jndiName.equals("java:comp/DefaultDataSource")
                || jndiName.equals("java:comp/DefaultJMSConnectionFactory")
                || jndiName.equals("java:comp/ORB")
                || jndiName.equals("java:comp/DefaultManagedExecutorService")
                || jndiName.equals("java:comp/DefaultManagedScheduledExecutorService")
                || jndiName.equals("java:comp/DefaultManagedThreadFactory")
                || jndiName.equals("java:comp/DefaultContextService")
                || jndiName.equals("java:comp/UserTransaction")
                || jndiName.equals("java:comp/TransactionSynchronizationRegistry")
                || jndiName.equals("java:comp/BeanManager")
                || jndiName.equals("java:comp/ValidatorFactory")
                || jndiName.equals("java:comp/Validator")
                || jndiName.equals("java:module/ModuleName")
                || jndiName.equals("java:app/AppName")
                || jndiName.equals("java:comp/InAppClientContainer")));
    }

    /**
     * Copy from ApplicationLifeCycle.java
     */
    private boolean loadOnCurrentInstance(DeploymentContext deploymentContext) {
        final DeployCommandParameters commandParams = deploymentContext.getCommandParameters(DeployCommandParameters.class);
        final Properties appProps = deploymentContext.getAppProps();
        if (commandParams.enabled) {
            // if the current instance match with the target
            if (domain.isCurrentInstanceMatchingTarget(commandParams.target, commandParams.name(), server.getName(),
                    deploymentContext.getTransientAppMetaData(DeploymentProperties.PREVIOUS_TARGETS, List.class))) {
                return true;
            }
            if (server.isDas()) {
                String objectType
                        = appProps.getProperty(ServerTags.OBJECT_TYPE);
                if (objectType != null) {
                    // if it's a system application needs to be loaded on DAS
                    if (objectType.equals(DeploymentProperties.SYSTEM_ADMIN)
                            || objectType.equals(DeploymentProperties.SYSTEM_ALL)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static class AppResource {

        private final String name;

        private final String lookup;

        private final String type;

        private final JndiNameEnvironment env;

        boolean validate;

        private AppResource(String name, String lookup, String type, JndiNameEnvironment env, boolean validate) {
            this.name = name;
            this.lookup = lookup;
            this.type = type;
            this.env = env;
            this.validate = validate;
        }

        private String getJndiName() {
            return lookup;
        }

        private JndiNameEnvironment getEnv() {
            return env;
        }

        private String getName() {
            return name;
        }

        private String getType() {
            return type;
        }

        private boolean hasLookup() {
            return lookup != null && lookup.length() > 0;
        }

        /**
         * 
         * @return false
         */
        private void noValidation() {
            validate = false;
        }
    }

    private static class AppResources {

        private final List<AppResource> myResources;

        private final JNDINamespace myNamespace;

        private AppResources() {
            myResources = new ArrayList<>();
            myNamespace = new JNDINamespace();
        }

        /**
         * Store in namespace only if it has a valid lookup value. This is
         * because we do not want to store invalid resources in our namespace.
         */
        private void store(AppResource resource) {
            myResources.add(resource);
            if (resource.hasLookup()) {
                myNamespace.store(resource.name, resource.env);
            }
        }

        /**
         * If we know that the name points to a valid resource, directly store
         * in namespace.
         */
        private void storeInNamespace(String name, JndiNameEnvironment env) {
            myNamespace.store(name, env);
        }

        private void storeAppScopedResources(Map<String, List<String>> resourcesList, String appName) {
            myNamespace.storeAppScopedResources(resourcesList, appName);
        }
    }

    /**
     * A class to record all the logical JNDI names of resources defined in the
     * application in the appropriate scopes. App scoped resources, Resource
     * Definitions are also stored in this data structure.
     */
    private static class JNDINamespace {

        private final Map<String, List<String>> componentNamespaces;

        private final Map<String, List<String>> moduleNamespaces;

        private final List<String> appNamespace;

        private final List<String> globalNameSpace;

        private final List<String> nonPortableJndiNames;

        private JNDINamespace() {
            componentNamespaces = new HashMap<>();
            moduleNamespaces = new HashMap<>();
            appNamespace = new ArrayList<>();
            globalNameSpace = new ArrayList<>();
            nonPortableJndiNames = new ArrayList<>();
        }

        /**
         * Store app scoped resources in this namespace to facilitate lookup
         * during validation.
         *
         * @param resources - App scoped resources
         * @param appName - Application name
         */
        private void storeAppScopedResources(Map<String, List<String>> resources, String appName) {
            if (resources == null) {
                return;
            }
            List<String> appLevelResources = resources.get(appName);
            appNamespace.addAll(appLevelResources);
            for (Map.Entry<String, List<String>> entry : resources.entrySet()) {
                if (!entry.getKey().equals(appName)) {
                    String moduleName = getActualModuleName(entry.getKey());
                    List<String> jndiNames = moduleNamespaces.get(moduleName);
                    if (jndiNames == null) {
                        jndiNames = new ArrayList<>();
                        jndiNames.addAll(entry.getValue());
                        moduleNamespaces.put(moduleName, jndiNames);
                    } else {
                        jndiNames.addAll(entry.getValue());
                    }
                }
            }
        }

        /**
         * Store the jndi name in the correct scope. Will be stored only if jndi
         * name is javaURL.
         */
        public void store(String jndiName, JndiNameEnvironment env) {
            if (jndiName.startsWith(ResourceConstants.JAVA_COMP_SCOPE_PREFIX)) {
                String componentId = DOLUtils.getComponentEnvId(env);
                List<String> jndiNames = componentNamespaces.get(componentId);
                if (jndiNames == null) {
                    jndiNames = new ArrayList<>();
                    jndiNames.add(jndiName);
                    componentNamespaces.put(componentId, jndiNames);
                } else {
                    jndiNames.add(jndiName);
                }
            } else if (jndiName.startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX)) {
                String moduleName = getActualModuleName(DOLUtils.getModuleName(env));
                List<String> jndiNames = moduleNamespaces.get(moduleName);
                if (jndiNames == null) {
                    jndiNames = new ArrayList<>();
                    jndiNames.add(jndiName);
                    moduleNamespaces.put(moduleName, jndiNames);
                } else {
                    jndiNames.add(jndiName);
                }
            } else if (jndiName.startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX)) {
                appNamespace.add(jndiName);
            } else if (jndiName.startsWith(ResourceConstants.JAVA_GLOBAL_SCOPE_PREFIX)) {
                globalNameSpace.add(jndiName);
            } else {
                nonPortableJndiNames.add(jndiName);
            }
        }

        /**
         * Find the jndi name in our namespace.
         *
         * @return True if the jndi name is found in the namespace. False
         * otherwise.
         */
        public boolean find(String jndiName, JndiNameEnvironment env) {
            if (jndiName == null) {
                return false;
            }

            if (jndiName.startsWith(ResourceConstants.JAVA_COMP_SCOPE_PREFIX)) {
                String componentId = DOLUtils.getComponentEnvId(env);
                List jndiNames = componentNamespaces.get(componentId);
                return jndiNames != null && jndiNames.contains(jndiName);
            } else if (jndiName.startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX)) {
                String moduleName = getActualModuleName(DOLUtils.getModuleName(env));
                List jndiNames = moduleNamespaces.get(moduleName);
                return jndiNames != null && jndiNames.contains(jndiName);
            } else if (jndiName.startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX)) {
                return appNamespace.contains(jndiName);
            } else if (jndiName.startsWith(ResourceConstants.JAVA_GLOBAL_SCOPE_PREFIX)) {
                return globalNameSpace.contains(jndiName);
            } else {
                return nonPortableJndiNames.contains(jndiName);
            }
        }
       
        /**
         * Remove suffix from the module name.
         */
        private String getActualModuleName(String moduleName) {
            if (moduleName != null) {
                if (moduleName.endsWith(".jar") || moduleName.endsWith(".war") || moduleName.endsWith(".rar")) {
                    moduleName = moduleName.substring(0, moduleName.length() - 4);
                }
            }
            return moduleName;
        }
    }

}
