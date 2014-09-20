/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.module;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.*;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resources.admin.cli.ResourcesXMLParser;
import org.glassfish.resources.admin.cli.SunResourcesXML;
import org.glassfish.resources.api.*;
import org.glassfish.resourcebase.resources.util.ResourceManagerFactory;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resources.util.ResourceUtil;
import org.glassfish.resourcebase.resources.api.ResourcesBinder;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.*;

import org.glassfish.api.event.EventListener;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.resource.ResourceException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;
import static org.glassfish.resources.api.Resource.*;
import static org.glassfish.resourcebase.resources.api.ResourceConstants.*;

/**
 * ResourcesDeployer to handle "glassfish-resources.xml(s)" bundled in the application.
 *
 * @author Jagadish Ramu
 */
@Service
public class ResourcesDeployer extends JavaEEDeployer<ResourcesContainer, ResourcesApplication>
        implements PostConstruct, PreDestroy, EventListener {
    private final org.glassfish.resources.admin.cli.ResourceFactory resourceFactory;

    @Inject
    private Domain domain;

    @Inject
    private ServerContext context;
    
    @Inject
    private Provider<ResourcesApplication> resourcesApplicationProvider;

    private final ApplicationRegistry appRegistry;

    private final Provider<ResourceManagerFactory> resourceManagerFactoryProvider;

    private final ResourcesBinder resourcesBinder;

    @Inject
    private ConfigSupport configSupport;

    @Inject
    private Events events;

    @Inject
    private ServiceLocator locator; 

    private final Applications applications;

    private static Map<String, Application> preservedApps = new HashMap<String, Application>();

    private final static Logger _logger = LogDomains.getLogger(ResourcesDeployer.class, LogDomains.RSR_LOGGER);

    private static final String RESOURCES_XML_META_INF = "META-INF/glassfish-resources.xml";
    private static final String RESOURCES_XML_WEB_INF = "WEB-INF/glassfish-resources.xml";

    @Inject
    public ResourcesDeployer(org.glassfish.resources.admin.cli.ResourceFactory resourceFactoryParam,
            ApplicationRegistry appRegistryParam,
            Provider<ResourceManagerFactory> resourceManagerFactoryProviderParam,
            ResourcesBinder resourcesBinderParam,
            Applications applicationsParam) {
        resourceFactory = resourceFactoryParam;
        appRegistry = appRegistryParam;
        resourceManagerFactoryProvider = resourceManagerFactoryProviderParam;
        resourcesBinder = resourcesBinderParam;
        applications = applicationsParam;
    }

    public void postConstruct() {
        events.register(this);
    }

    public void preDestroy(){
        events.unregister(this);
    }

    /**
     * Loads a previously prepared application in its execution environment and
     * return a ContractProvider instance that will identify this environment in
     * future communications with the application's container runtime.
     *
     * @param container in which the application will reside
     * @param context   of the deployment
     * @return an ApplicationContainer instance identifying the running application
     */
    @Override
    public ResourcesApplication load(ResourcesContainer container, DeploymentContext context) {
        super.load(container, context);
        debug("App-Scoped-Resources ResourcesDeployer.load()");
        ResourcesApplication application = resourcesApplicationProvider.get();
        application.setApplicationName(getAppNameFromDeployCmdParams(context));
        return application;
    }

    public void unload(ResourcesApplication appContainer, DeploymentContext context) {
        //TODO unregistering resources, removing resources configuration.
        debug("Resources-Deployer :unload() called");
    }

    /**
     * Retrieve connector and non-connector resources from the archive.
     *
     * @param archive Archieve from which the resources to be retrieved.
     * @param appName Name of the application
     * @param connectorResources Connector resources will be added to this list.
     * @param nonConnectorResources Non connector resources will be added to this list.
     * @param resourceXmlParsers Resource xml parsers corresponding to both connector and non connector resources will be stored in this.
     */
    public void getResources(ReadableArchive archive, String appName,
                                             List<org.glassfish.resources.api.Resource> connectorResources,
                                             List<org.glassfish.resources.api.Resource> nonConnectorResources,
                                             Map<org.glassfish.resources.api.Resource, ResourcesXMLParser> resourceXmlParsers) {
        try {
            if (ResourceUtil.hasResourcesXML(archive, locator)) {
                Map<String, Map<String, List>> appScopedResources = new HashMap<String, Map<String, List>>();
                Map<String, String> fileNames = new HashMap<String, String>();
                //using appName as it is possible that "deploy --name=APPNAME" will
                //be different than the archive name.
                retrieveAllResourcesXMLs(fileNames, archive, appName);

                for (Map.Entry<String, String> entry : fileNames.entrySet()) {
                    String moduleName = entry.getKey();
                    String fileName = entry.getValue();
                    debug("GlassFish Resources XML : " + fileName);

                    moduleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleNameWithExtension(moduleName);
                    String scope;
                    if (appName.equals(moduleName)) {
                        scope = JAVA_APP_SCOPE_PREFIX;
                    } else {
                        scope = JAVA_MODULE_SCOPE_PREFIX;
                    }

                    File file = new File(fileName);
                    ResourcesXMLParser parser = new ResourcesXMLParser(file, scope);

                    validateResourcesXML(file, parser);

                    List<org.glassfish.resources.api.Resource> resources = parser.getResourcesList();

                    if (nonConnectorResources != null) {
                        nonConnectorResources.addAll(ResourcesXMLParser.
                                getNonConnectorResourcesList(resources, false, true));
                    }

                    if (connectorResources != null) {
                        connectorResources.addAll(ResourcesXMLParser.
                                getConnectorResourcesList(resources, false, true));
                    }

                    if (resourceXmlParsers != null) {
                        for (org.glassfish.resources.api.Resource res : resources) {
                            resourceXmlParsers.put(res, parser);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // only DeploymentExceptions are propagated and result in deployment failure
            // in the event notification infrastructure
            throw new DeploymentException("Failue while processing glassfish-resources.xml(s) in the archive ", e);
        }
    }
    
    private void processArchive(DeploymentContext dc) {

        try {
            ReadableArchive archive = dc.getSource();

            if (ResourceUtil.hasResourcesXML(archive, locator)) {

                Map<String,Map<String, List>> appScopedResources = new HashMap<String,Map<String,List>>();
                Map<String, String> fileNames = new HashMap<String, String>();

                String appName = getAppNameFromDeployCmdParams(dc);
                //using appName as it is possible that "deploy --name=APPNAME" will
                //be different than the archive name.
                retrieveAllResourcesXMLs(fileNames, archive, appName);

                for (Map.Entry<String, String> entry: fileNames.entrySet()) {
                    String moduleName = entry.getKey();
                    String fileName = entry.getValue();
                    debug("Sun Resources XML : " + fileName);

                    moduleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleNameWithExtension(moduleName);
                    String scope ;
                    if(appName.equals(moduleName)){
                        scope = JAVA_APP_SCOPE_PREFIX;
                    }else{
                        scope = JAVA_MODULE_SCOPE_PREFIX;
                    }

                    File file = new File(fileName);
                    ResourcesXMLParser parser = new ResourcesXMLParser(file, scope);

                    validateResourcesXML(file, parser);

                    List list = parser.getResourcesList();

                    Map<String, List> resourcesList = new HashMap<String, List>();
                    List<org.glassfish.resources.api.Resource> nonConnectorResources =
                            ResourcesXMLParser.getNonConnectorResourcesList(list, false, true);
                    resourcesList.put(NON_CONNECTOR_RESOURCES, nonConnectorResources);
                    
                    List<org.glassfish.resources.api.Resource> connectorResources =
                            ResourcesXMLParser.getConnectorResourcesList(list, false, true);
                    resourcesList.put(CONNECTOR_RESOURCES, connectorResources);

                    appScopedResources.put(moduleName, resourcesList);
                }
                dc.addTransientAppMetaData(APP_SCOPED_RESOURCES_MAP, appScopedResources);
                ApplicationInfo appInfo = appRegistry.get(appName);
                if(appInfo != null){
                    Application app = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
                    appInfo.addTransientAppMetaData(ServerTags.APPLICATION, app);
                }
            }
        } catch (Exception e) {
            // only DeploymentExceptions are propagated and result in deployment failure
            // in the event notification infrastructure
            throw new DeploymentException("Failue while processing glassfish-resources.xml(s) in the archive ", e);
        }
    }

    private static void validateResourcesXML(File file, ResourcesXMLParser parser) throws ResourceConflictException {
        String filePath = file.getPath();
        SunResourcesXML sunResourcesXML = new SunResourcesXML(filePath, parser.getResourcesList());
        List<SunResourcesXML> resourcesXMLList = new ArrayList<SunResourcesXML>();
        resourcesXMLList.add(sunResourcesXML);
        ResourceUtilities.resolveResourceDuplicatesConflictsWithinArchive(resourcesXMLList);
    }

    /**
     * retain old resource configuration for the new archive being deployed.
     * @param dc DeploymentContext
     * @param allResources all resources (app scoped, module scoped) of old application
     * @throws Exception when unable to retain old resource configuration.
     */
    public void retainResourceConfig(DeploymentContext dc, Map<String, Resources> allResources) throws Exception {
        String appName = getAppNameFromDeployCmdParams(dc);
        Application application = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
        Resources appScopedResources = allResources.get(appName);

        if(appScopedResources != null){
            application.setResources(appScopedResources);
        }

        if(DeploymentUtils.isArchiveOfType(dc.getSource(), DOLUtils.earType(), locator)){
            List<Module> modules = application.getModule();
            if(modules != null){
                for(Module module : modules){
                    Resources moduleScopedResources = allResources.get(module.getName());
                    if(moduleScopedResources != null){
                        module.setResources(moduleScopedResources);
                    }
                }
            }
        }
    }

    /**
     * During "load()" event (eg: app/app-ref enable, server start),
     * populate resource-config in app-info so that it can be used for
     * constructing connector-classloader for the application.
     * @param dc DeploymentContext
     */
    public void populateResourceConfigInAppInfo(DeploymentContext dc){
        String appName = getAppNameFromDeployCmdParams(dc);
        Application application = applications.getApplication(appName);
        ApplicationInfo appInfo = appRegistry.get(appName);
        if(application != null && appInfo != null){
            Resources appScopedResources = application.getResources();
            if(appScopedResources != null){
                appInfo.addTransientAppMetaData(ServerTags.APPLICATION, application);
                appInfo.addTransientAppMetaData(application.getName()+"-resources", appScopedResources);
            }

            List<Module> modules = application.getModule();
            if(modules != null){
                for(Module module : modules){
                    Resources moduleScopedResources = module.getResources();
                    if(moduleScopedResources != null){
                        appInfo.addTransientAppMetaData(module.getName()+"-resources", moduleScopedResources);
                    }
                }
            }
        }
    }

    public void createResources(DeploymentContext dc, boolean embedded, boolean deployResources) throws ResourceException {
        String appName = getAppNameFromDeployCmdParams(dc);
        Application app = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
        Map<String, Map<String, List>> resourcesList =
                (Map<String, Map<String, List>>)dc.getTransientAppMetadata().get(APP_SCOPED_RESOURCES_MAP);

        if (resourcesList != null) {
            Map<String, List> appLevelResources = resourcesList.get(appName);
            if (appLevelResources != null) {
                List<org.glassfish.resources.api.Resource> connectorResources =
                        appLevelResources.get(CONNECTOR_RESOURCES);

                createAppScopedResources(app, connectorResources, dc, embedded);

                List<org.glassfish.resources.api.Resource> nonConnectorResources =
                        appLevelResources.get(NON_CONNECTOR_RESOURCES);

                createAppScopedResources(app, nonConnectorResources, dc, embedded);

            }
            List<Module> modules = app.getModule();
            if (modules != null) {
                for (Module module : modules) {
                    String actualModuleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleNameWithExtension(module.getName());
                    //create resources for modules, ignore standalone applications where
                    //module name will be the same as app name
                    if(!appName.equals(actualModuleName)){
                        Map<String, List> moduleResources = resourcesList.get(actualModuleName);
                        if (moduleResources != null) {
                            List<org.glassfish.resources.api.Resource> connectorResources =
                                    moduleResources.get(CONNECTOR_RESOURCES);
                            createModuleScopedResources(app, module, connectorResources, dc, embedded);

                            List<org.glassfish.resources.api.Resource> nonConnectorResources =
                                    moduleResources.get(NON_CONNECTOR_RESOURCES);
                            createModuleScopedResources(app, module, nonConnectorResources, dc, embedded);
                        }
                    }
                }
            }
        }
    }

    private Collection<Resource>
    createConfig(Resources resources, Collection<org.glassfish.resources.api.Resource> resourcesToRegister,
                 boolean embedded)
    throws ResourceException {
        List<Resource> resourceConfigs =
                new ArrayList<Resource>();
        for (org.glassfish.resources.api.Resource resource : resourcesToRegister) {
            final HashMap attrList = resource.getAttributes();
            final Properties props = resource.getProperties();
            String desc = resource.getDescription();
            if (desc != null) {
                attrList.put("description", desc);
            }

            try {
                final ResourceManager rm = resourceFactory.getResourceManager(resource);
                if(embedded && isEmbeddedResource(resource, resourcesToRegister)){
                    Resource configBeanResource =
                            rm.createConfigBean(resources, attrList, props, false);
                    resources.getResources().add(configBeanResource);
                    resourceConfigs.add(configBeanResource);
                }else if(!embedded && !isEmbeddedResource(resource, resourcesToRegister)){
                    com.sun.enterprise.config.serverbeans.Resource configBeanResource =
                            rm.createConfigBean(resources, attrList, props, true);
                    resources.getResources().add(configBeanResource);
                    resourceConfigs.add(configBeanResource);
                }
            } catch (Exception e) {
                throw new ResourceException(e);
            }
        }
        return resourceConfigs;
    }

    private static boolean isConnectorResource(org.glassfish.resources.api.Resource resource){
        if(resource.getType().equals(ADMIN_OBJECT_RESOURCE) ||
           resource.getType().equals(CONNECTOR_CONNECTION_POOL) ||
                resource.getType().equals(CONNECTOR_RESOURCE) ||
                resource.getType().equals(RESOURCE_ADAPTER_CONFIG) ||
                resource.getType().equals(CONNECTOR_WORK_SECURITY_MAP)){
            return true;
        }else{
            return false;
        }
    }

    private static boolean isEmbeddedResource(org.glassfish.resources.api.Resource resource,
                                              Collection<org.glassfish.resources.api.Resource> resources){
        boolean result = false;
        if(isConnectorResource(resource)){
            String attributeName = null;
            if(resource.getType().equals(ADMIN_OBJECT_RESOURCE)){
                attributeName = RES_ADAPTER;
            } else if(resource.getType().equals(CONNECTOR_CONNECTION_POOL)){
                attributeName = RES_ADAPTER_NAME;
            } else if(resource.getType().equals(CONNECTOR_RESOURCE)){
                String poolName = (String)resource.getAttributes().get(POOL_NAME);
                if(poolName != null){
                    org.glassfish.resources.api.Resource poolResource = getPoolResource(poolName, resources);
                    //point to poolResource
                    resource = poolResource;
                    attributeName = RES_ADAPTER_NAME;
                }
            }/* else if(resource.getType().equals(org.glassfish.resources.api.Resource.RESOURCE_ADAPTER_CONFIG)){
                attributeName = ResourceConstants.RES_ADAPTER_NAME;
            } */else if(resource.getType().equals(CONNECTOR_WORK_SECURITY_MAP)){
                attributeName = RES_ADAPTER_NAME;
            }
            if(attributeName != null && resource != null){
                result = isEmbeddedRar(resource, attributeName);
            }
        }
        return result;
    }

    private static org.glassfish.resources.api.Resource getPoolResource(
            String poolName, Collection<org.glassfish.resources.api.Resource> resources){
        org.glassfish.resources.api.Resource result = null;
        for(org.glassfish.resources.api.Resource resource : resources){
            if(resource.getType().equals(CONNECTOR_CONNECTION_POOL)){
                String cpName = (String)resource.getAttributes().get(CONNECTION_POOL_NAME);
                if(poolName.equals(cpName)){
                    result = resource;
                    break;
                }
            }
        }
        return result;
    }

    private static boolean isEmbeddedRar(org.glassfish.resources.api.Resource resource, String attributeName) {
        boolean result = false;
        String raName = (String)resource.getAttributes().get(attributeName);
        if(raName != null && raName.contains(ResourceConstants.EMBEDDEDRAR_NAME_DELIMITER)){
            result = true;
        }
        return result;
    }


    private void createAppScopedResources(Application app, List<org.glassfish.resources.api.Resource> resources,
                                                 DeploymentContext dc, boolean embedded)
            throws ResourceException {
        try {
            if (resources != null) {
                Application application = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
                Resources asc = dc.getTransientAppMetaData(APP_META_DATA_RESOURCES, Resources.class);
                if (asc == null) {
                    asc = application.createChild(Resources.class);
                    application.setResources(asc);
                    dc.addTransientAppMetaData(APP_META_DATA_RESOURCES, asc);
                    ApplicationInfo appInfo = appRegistry.get(app.getName());
                    if(appInfo != null){
                        appInfo.addTransientAppMetaData(app.getName()+"-resources", asc);
                    }
                }

                createConfig(asc, resources, embedded);
                String appName = app.getName();
                preserveResources(asc, appName, appName);
            }
        } catch (Exception e) {
            Object params[] = new Object[]{app.getName(), e};
            _logger.log(Level.SEVERE, "gf.resources.app.scope.deployment.failure", params);
            throw new ResourceException(e);
        }
    }

    /**
     * preserve the resources such that they can be undeployed during deployment failure.
     * @param resources resources
     * @param appName application-name
     * @param moduleName module-name
     */
    private static void preserveResources(Resources resources, String appName, String moduleName) {
        Map<String, Resources> allResources = ResourcesRegistry.getResources(appName);
        if(allResources != null){
            allResources.put(moduleName, resources);
        }else{
            allResources = new HashMap<String, Resources>();
            allResources.put(moduleName, resources);
            ResourcesRegistry.putResources(appName, allResources);
        }
    }
                            
    private void createModuleScopedResources(Application app, Module module,
                                                    List<org.glassfish.resources.api.Resource> resources,
                                                    DeploymentContext dc, boolean embedded)
            throws ResourceException {
        try {
            if (resources != null) {
                Resources msc = dc.getTransientAppMetaData(module.getName()+"-resources", Resources.class);
                if (msc == null) {
                    msc = module.createChild(Resources.class);
                    module.setResources(msc);
                    dc.addTransientAppMetaData(module.getName()+"-resources", msc);
                    ApplicationInfo appInfo = appRegistry.get(app.getName());
                    if(appInfo != null){
                        appInfo.addTransientAppMetaData(module.getName()+"-resources", msc);
                    }
                }

                createConfig(msc, resources, embedded);
                preserveResources(msc, app.getName(), module.getName());
            }
        } catch (Exception e) {
            Object params[] = new Object[]{module.getName(),app.getName(), e};
            _logger.log(Level.SEVERE, "gf.resources.module.scope.deployment.failure", params);
            throw new ResourceException(e);
        }
    }

    public void  deployResourcesFromConfiguration(String appName, boolean embedded) throws Exception {
        Application application = applications.getApplication(appName);
        ApplicationInfo appInfo = appRegistry.get(appName);
        if(application != null && appInfo != null){
            Resources appScopedResources = application.getResources();
            if(appScopedResources != null){
                deployResources(appName, null, appScopedResources.getResources(), embedded);
            }

            List<Module> modules = application.getModule();
            if(modules != null){
                for(Module module : modules){
                    Resources moduleScopedResources = module.getResources();
                    String moduleName = module.getName();
                    if(moduleScopedResources != null){
                        deployResources(appName, moduleName, moduleScopedResources.getResources(), embedded);
                    }
                }
            }
        }
    }

    public void  deployResources(String applicationName, boolean postDeployPhase) throws Exception {
        Map<String, Resources> allResources = ResourcesRegistry.getResources(applicationName);
        if(allResources != null){
            for(Map.Entry<String, Resources> entry : allResources.entrySet()){
                String moduleName = entry.getKey();
                Resources resources = entry.getValue();
                if(applicationName.equals(moduleName)){
                    deployResources(applicationName, null, resources.getResources(), postDeployPhase);
                }else{
                    deployResources(applicationName, moduleName, resources.getResources(), postDeployPhase);
                }
            }
        }
    }
    public void deployResources(String applicationName, String moduleName,
                                Collection<com.sun.enterprise.config.serverbeans.Resource> resources,
                                boolean postDeployPhase) throws Exception {
        for(Resource resource : resources){
            if(resource instanceof BindableResource) {
                BindableResource bindableResource = (BindableResource)resource;
                ResourceInfo resourceInfo = new ResourceInfo(bindableResource.getJndiName(), applicationName, moduleName);
                if(getResourceDeployer(bindableResource).canDeploy(postDeployPhase, resources, bindableResource)){
                    resourcesBinder.deployResource(resourceInfo, bindableResource);
                }
            } else{
                if(getResourceDeployer(resource).canDeploy(postDeployPhase, resources, resource)){
                    getResourceDeployer(resource).deployResource(resource, applicationName, moduleName);
                }
            }
        }
    }

    private static String getAppNameFromDeployCmdParams(DeploymentContext dc) {
        final DeployCommandParameters commandParams = dc.getCommandParameters(DeployCommandParameters.class);
        return commandParams.name();
    }

    public void retrieveAllResourcesXMLs(Map<String, String> fileNames, ReadableArchive archive,
                                         String actualArchiveName) throws IOException {

        if(DeploymentUtils.isArchiveOfType(archive, DOLUtils.earType(), locator)){
            //Look for top-level META-INF/glassfish-resources.xml
            if(archive.exists(RESOURCES_XML_META_INF)){
                String archivePath = archive.getURI().getPath();
                String fileName = archivePath + RESOURCES_XML_META_INF;
                if(_logger.isLoggable(Level.FINEST)){
                    _logger.finest("GlassFish-Resources Deployer - fileName : " + fileName +
                            " - parent : " + archive.getName());
                }
                fileNames.put(actualArchiveName, fileName);
            }

            //Lok for sub-module level META-INF/glassfish-resources.xml and WEB-INF/glassfish-resources.xml
            Enumeration<String> entries = archive.entries();
            while(entries.hasMoreElements()){
                String element = entries.nextElement();
                if(element.endsWith(".jar") || element.endsWith(".war") || element.endsWith(".rar") ||
                        element.endsWith("_jar") || element.endsWith("_war") || element.endsWith("_rar")){
                    ReadableArchive subArchive = archive.getSubArchive(element);
                    if(subArchive != null ){
                        retrieveResourcesXMLFromArchive(fileNames, subArchive, subArchive.getName());
                    }
                }
            }
        }else{
            //Look for standalone archive's META-INF/glassfish-resources.xml and WEB-INF/glassfish-resources.xml
            retrieveResourcesXMLFromArchive(fileNames, archive, actualArchiveName);
        }
    }

    private void retrieveResourcesXMLFromArchive(Map<String, String> fileNames, ReadableArchive archive,
                                                 String actualArchiveName) {
        if(ResourceUtil.hasResourcesXML(archive, locator)){
            String archivePath = archive.getURI().getPath();
            String fileName ;
            if(DeploymentUtils.isArchiveOfType(archive, DOLUtils.warType(), locator)){
                fileName = archivePath +  RESOURCES_XML_WEB_INF;
            }else{
                fileName = archivePath + RESOURCES_XML_META_INF;
            }
            if(_logger.isLoggable(Level.FINEST)){
                _logger.finest("GlassFish-Resources Deployer - fileName : " + fileName +
                        " - parent : " + archive.getName());
            }

            fileNames.put(actualArchiveName, fileName);
        }
    }

    /**
     * Given a <i>resource</i> instance, appropriate deployer will be provided
     *
     * @param resource resource instance
     * @return ResourceDeployer
     */
    private ResourceDeployer getResourceDeployer(Object resource){
        return resourceManagerFactoryProvider.get().getResourceDeployer(resource);
    }

    /**
     * Event listener to listen to </code>application undeploy validation</code> and
     * if <i>preserveResources</i> flag is set, cache the &lt;resources&gt;
     * config for persisting it in domain.xml
     */
    public void event(Event event) {
        if (event.is(Deployment.DEPLOYMENT_BEFORE_CLASSLOADER_CREATION)) {
            DeploymentContext dc = (DeploymentContext) event.hook();
            final DeployCommandParameters deployParams = dc.getCommandParameters(DeployCommandParameters.class);
            processResources(dc, deployParams);
        }else if(event.is(Deployment.UNDEPLOYMENT_VALIDATION)){
            DeploymentContext dc = (DeploymentContext) event.hook();
            final UndeployCommandParameters undeployCommandParameters =
                    dc.getCommandParameters(UndeployCommandParameters.class);
            preserveResources(dc, undeployCommandParameters);
        }else if(Deployment.UNDEPLOYMENT_FAILURE.equals(event.type())){
            DeploymentContext dc = (DeploymentContext) event.hook();
            cleanupPreservedResources(dc, event);
        }else if(Deployment.DEPLOYMENT_FAILURE.equals(event.type())){
            DeploymentContext dc = (DeploymentContext) event.hook();
            String appName = getAppNameFromDeployCmdParams(dc);
            cleanupResources(appName, dc.getCommandParameters(DeployCommandParameters.class).origin);
            //TODO ASR call this only when the flag is on ? --properties preserveAppScopedResources=true
            cleanupPreservedResources(dc, event);
        }else if(Deployment.DEPLOYMENT_SUCCESS.equals(event.type())){
            ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
            String appName = applicationInfo.getName();
            ResourcesRegistry.remove(appName);
        }
    }

    private void processResources(DeploymentContext dc, DeployCommandParameters deployParams) {
        try{
            if (deployParams.origin == OpsParams.Origin.deploy || deployParams.origin == OpsParams.Origin.deploy_instance
                    /*|| (deployParams.origin == OpsParams.Origin.create_application_ref && env.isInstance())*/) {
                Properties properties = deployParams.properties;
                if(properties != null){
                    //handle if "preserveAppScopedResources" property is set (during deploy --force=true or redeploy)
                    String preserve = properties.getProperty(DeploymentProperties.PRESERVE_APP_SCOPED_RESOURCES);
                    if (preserve != null && Boolean.valueOf(preserve)) {
                        Boolean redeploy = false;
                        redeploy = deployParams.force;

                        if (redeploy) {
                            String appName = getAppNameFromDeployCmdParams(dc);
                            Map<String, Resources> allResources = ResourcesRegistry.remove(appName);
                            Application oldApp = preservedApps.remove(appName);
                            if (allResources != null && oldApp != null) {
                                Application application = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
                                validatePreservedResources(allResources, oldApp, application);
                                retainResourceConfig(dc, allResources);
                            }
                           return;
                        }
                    }
                }
                Application app = dc.getTransientAppMetaData(ServerTags.APPLICATION, Application.class);
                String applicationName = getAppNameFromDeployCmdParams(dc);
                if(app != null){
                    //application is stored in transient meta data only during deployment.
                    processArchive(dc);
                    createResources(dc, false, true);
                    createResources(dc, true, false);
                    deployResources(applicationName, false);
                }else{
                    //application config is already present. Use the same.
                    deployResourcesFromConfiguration(applicationName, false);
                    populateResourceConfigInAppInfo(dc);
                }
            }else if(deployParams.origin == OpsParams.Origin.load){
                //during load event (ie., app/app-ref enable or server start, resource configuration
                //is present in domain.xml. Use the configuration.
                populateResourceConfigInAppInfo(dc);
            }
        }catch(Exception e){
            // only DeploymentExceptions are propagated and result in deployment failure
            // in the event notification infrastructure
            if(_logger.isLoggable(Level.FINEST)){
                _logger.log(Level.FINEST, "Exception while processing archive of application" +
                    " [ "+getAppNameFromDeployCmdParams(dc)+" ] for resources definitions : " + e.getCause());
            }
            throw new DeploymentException(e);
        }
    }

    /**
     * Validates the old resource configuration against new archive's modules.
     * @param allResources all resources (app scoped, module scoped)
     * @param oldApp Old Application config
     * @param newApp New Applicatoin config
     * @throws org.glassfish.resourcebase.resources.api.ResourceConflictException when it is not possible to map any of the resource(s) to
     * new application/its modules
     */
    private void validatePreservedResources(Map<String, Resources> allResources, Application oldApp,
                                               Application newApp) throws ResourceConflictException {

        String appName = newApp.getName();
        Resources appScopedResources = allResources.get(appName);
        if (appScopedResources != null) {
            for(Resource resource : appScopedResources.getResources()){
                getResourceDeployer(resource).validatePreservedResource(oldApp, newApp, resource, appScopedResources);
            }
        }

        List<Module> newModules = newApp.getModule();
        for(Module newModule : newModules){
            Module oldModule = oldApp.getModule(newModule.getName());
            if(oldModule != null){
                Resources oldModuleResources = oldModule.getResources();
                if(oldModuleResources != null){
                    for(Resource resource : oldModuleResources.getResources()){
                        getResourceDeployer(resource).validatePreservedResource(oldApp, newApp, resource, oldModuleResources);
                    }
                }
            }//else its a new module in the archive being redeployed.
        }
    }

    /**
     * clean up resources due to deployment failure.
     * @param appName application-name
     * @param deploymentPhase deployment-phase (deploy/load etc.,)
     */
    private void cleanupResources(String appName, OpsParams.Origin deploymentPhase){
        try{
            if (deploymentPhase == OpsParams.Origin.deploy || deploymentPhase == OpsParams.Origin.deploy_instance
                    || deploymentPhase == OpsParams.Origin.create_application_ref) {
                Map<String, Resources> allResources = ResourcesRegistry.remove(appName);
                if(allResources != null){
                    for(Map.Entry<String, Resources> entry : allResources.entrySet()){
                        String moduleName = entry.getKey();
                        Resources resources = entry.getValue();
                        String actualModuleName = moduleName;
                        //for app-scoped-resources, no module name is needed
                        if(appName.equals(moduleName)){
                            actualModuleName = null;
                        }
                        undeployResources(resources, appName, actualModuleName );
                    }
                }
            }else if(deploymentPhase == OpsParams.Origin.load){

                Application application = applications.getApplication(appName);
                if(application != null){
                    Resources appScopedResources = application.getResources();
                    undeployResources(appScopedResources, appName, null);

                    List<Module> modules = application.getModule();
                    if(modules != null){
                        for(Module module : modules){
                            Resources moduleScopedResources = module.getResources();
                            undeployResources(moduleScopedResources, appName, module.getName());
                        }
                    }
                }
            }
        }catch(Exception e){
            _logger.log(Level.WARNING, "Exception while cleaning-up resources during deployment failure", e);
        }
    }

    private void undeployResources(Resources resources, String appName, String moduleName) {

        if(resources != null){
            for(Resource resource : resources.getResources()){
                try{
                    //delete pools after resources
                    if(!(resource instanceof ResourcePool)){
                        getResourceDeployer(resource).undeployResource(resource, appName, moduleName);
                    }
                }catch(Exception e){
                    //ignore as this is cleanup
                }
            }
            Collection<ResourcePool> pools= resources.getResources(ResourcePool.class);
            for(ResourcePool pool : pools){
                try{
                    getResourceDeployer(pool).undeployResource(pool, appName, moduleName);
                }catch(Exception e){
                    //ignore as this is cleanup
                }
            }
        }
    }

    private void cleanupPreservedResources(DeploymentContext dc, Event event) {
        if (Deployment.DEPLOYMENT_FAILURE.equals(event.type())) {
            final DeployCommandParameters deployCommandParameters =
                    dc.getCommandParameters(DeployCommandParameters.class);
            if (deployCommandParameters.origin == OpsParams.Origin.deploy ||
                    deployCommandParameters.origin == OpsParams.Origin.deploy_instance || 
                    deployCommandParameters.origin == OpsParams.Origin.create_application_ref) {
                Properties properties = deployCommandParameters.properties;
                String appName = deployCommandParameters.name();
                cleanupPreservedResources(appName, properties);
            }
        } else if (Deployment.UNDEPLOYMENT_FAILURE.equals(event.type())) {
            final UndeployCommandParameters undeployCommandParameters =
                    dc.getCommandParameters(UndeployCommandParameters.class);
            if (undeployCommandParameters.origin == OpsParams.Origin.undeploy) {
                Properties properties = undeployCommandParameters.properties;
                String appName = undeployCommandParameters.name();
                cleanupPreservedResources(appName, properties);
            }
        }
    }

    private void cleanupPreservedResources(String appName, Properties properties) {
        if(properties != null){
            String preserve = properties.getProperty(DeploymentProperties.PRESERVE_APP_SCOPED_RESOURCES);
            if(preserve != null && Boolean.valueOf(preserve)){
                ResourcesRegistry.remove(appName);
                preservedApps.remove(appName);
            }
        }
    }


    /**
     * preserve the old application's resources so that they can be registered during deploy.
     * @param dc DeploymentContext
     * @param undeployCommandParameters undeploy command parameters
     */
    private void preserveResources(DeploymentContext dc, UndeployCommandParameters undeployCommandParameters) {
        try{
            if (undeployCommandParameters.origin == OpsParams.Origin.undeploy) {
                Properties properties = undeployCommandParameters.properties;
                if(properties != null){
                    String preserve = properties.getProperty(DeploymentProperties.PRESERVE_APP_SCOPED_RESOURCES);
                    if(preserve != null && Boolean.valueOf(preserve)){
                        debug("Preserve app scoped resources enabled");
                        final UndeployCommandParameters commandParams =
                                dc.getCommandParameters(UndeployCommandParameters.class);
                        String appName = commandParams.name();
                        Application app = applications.getApplication(appName);
                        preserveResources(app);
                        //store application info (for module information ie., sniffer type)
                        preservedApps.put(appName, app);
                    }
                }
            }
        }catch(Exception e){
            // only DeploymentExceptions are propagated and result in deployment failure
            // in the event notification infrastructure
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private void preserveResources(Application app) {
        String appName = app.getName();
        Map<String, Resources> allResources = new HashMap<String, Resources>();
        Resources appScopedResources = app.getResources();
        if(appScopedResources != null){
            allResources.put(appName, appScopedResources);
        }
        List<Module> modules = app.getModule();
        if(modules != null){
            for(Module module : modules){
                Resources moduleScopedResources = module.getResources();
                if(moduleScopedResources != null){
                    allResources.put(module.getName(), moduleScopedResources);
                }
            }
        }
        //store the resource-configuration
        ResourcesRegistry.putResources(appName, allResources);
    }

    private static void debug(String message){
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("[ASR] ResourceDeployer : " + message);
        }
    }
}
