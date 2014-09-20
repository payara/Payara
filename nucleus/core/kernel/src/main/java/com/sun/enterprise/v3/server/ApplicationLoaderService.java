/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.common.HTMLActionReporter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.glassfish.api.ActionReport;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.ApplicationConfigInfo;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.deployment.monitor.DeploymentLifecycleStatsProvider;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.deployment.*;
import org.glassfish.kernel.KernelLoggerInfo;
import org.glassfish.security.services.impl.AuthenticationServiceImpl;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * This service is responsible for loading all deployed applications...
 *
 * @author Jerome Dochez
 */
//@Priority(8) // low priority , should be started last
@Service(name="ApplicationLoaderService")
@RunLevel( value=StartupRunLevel.VAL, mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
public class ApplicationLoaderService implements org.glassfish.hk2.api.PreDestroy, org.glassfish.hk2.api.PostConstruct {
//public class ApplicationLoaderService implements Startup, org.glassfish.hk2.api.PreDestroy, org.glassfish.hk2.api.PostConstruct {

    final Logger logger = KernelLoggerInfo.getLogger();

    // During the authentication service's PostConstruct the javax.security.auth.login.Configuration class is constructed.
    // During the Configuration initialization a static variable is set to the current thread's context class loader.
    // When applications are loaded via this (ApplicationLoaderService) the current thread's context class loader
    // is temporarily set.  When an application is loaded it will access the authentication service.  If the
    // authentication service gets initialized at this time then the Configuration construction will happen and its
    // static variable will be set to the thread's temporarily set context class loader.  Therefore by making
    // a reference to the authentication service here we guarantee that it will be created before this
    // (ApplicationLoaderService) service is created.
    @Inject @Optional
    private AuthenticationServiceImpl authenticationService;

    @Inject
    Deployment deployment;

    @Inject
    Provider<ArchiveFactory> archiveFactoryProvider;

    @Inject
    SnifferManager snifferManager;

    @Inject
    ContainerRegistry containerRegistry;

    @Inject
    ApplicationRegistry appRegistry;

    @Inject
    Events events;

    @Inject
    protected Applications applications;

    protected SystemApplications systemApplications;

    protected Domain domain;

    @Inject @Named( ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    @Inject
    ServerEnvironment env;

    @Inject
    ServiceLocator habitat;

    private String deploymentTracingEnabled = null;

    private Map<String,Integer> appOrderInfoMap = new HashMap<String, Integer>();
    private int appOrder = 0;

    /**
     * Starts the application loader service.
     *
     * Look at the list of applications installed in our local repository
     * Get a Deployer capable for each application found
     * Invoke the deployer load() method for each application.
     */
    public void postConstruct() {
        
        assert env!=null;
        try{
            logger.fine("Satisfying Optional Packages dependencies...");
            InstalledLibrariesResolver.initializeInstalledLibRegistry(env.getLibPath().getAbsolutePath());
        }catch(Exception e){
            logger.log(Level.WARNING, KernelLoggerInfo.exceptionOptionalDepend, e);
        }

        DeploymentLifecycleStatsProvider dlsp = new DeploymentLifecycleStatsProvider();
        StatsProviderManager.register("deployment", PluginPoint.SERVER,
            "deployment/lifecycle", dlsp);

        deploymentTracingEnabled = System.getProperty(
            "org.glassfish.deployment.trace");

        domain = habitat.getService(Domain.class);

        /*
         * Build a map that associates an application with its
         * order in domain.xml.  If the deployment-order attribute
         * is not used for any application, then the applications
         * are loaded in the order they occur in domain.xml.  Also, for
         * applications with the same deployment-order attribute,
         * the applications are loaded in the order they occur in domain.xml.
         * Otherwise, applications are loaded according to the
         * deploynment-order attribute.
         */
        systemApplications = domain.getSystemApplications();
        for (Application systemApp : systemApplications.getApplications()) {
          appOrderInfoMap.put(systemApp.getName(), Integer.valueOf(appOrder++));
        }
        List<Application> standaloneAdapters =
            applications.getApplicationsWithSnifferType(ServerTags.CONNECTOR, true);
        for (Application standaloneAdapter : standaloneAdapters) {
          appOrderInfoMap.put(standaloneAdapter.getName(), Integer.valueOf(appOrder++));
        }
        List<Application> allApplications = applications.getApplications();
        for (Application app : allApplications) {
          appOrderInfoMap.put(app.getName(), Integer.valueOf(appOrder++));
        }
        
        for (Application systemApp : systemApplications.getApplications()) {
            // check to see if we need to load up this system application
            if (Boolean.valueOf(systemApp.getDeployProperties().getProperty
                (ServerTags.LOAD_SYSTEM_APP_ON_STARTUP))) {
                if (deployment.isAppEnabled(systemApp) || loadAppOnDAS(systemApp.getName())) {
                  Integer order = appOrderInfoMap.get(systemApp.getName());
                  ApplicationOrderInfo info = new ApplicationOrderInfo(systemApp, order);
                  DeploymentOrder.addApplicationDeployment(info);
                }
            }
        }

        // load standalone resource adapters first
        for (Application standaloneAdapter : standaloneAdapters) {
            // load the referenced enabled applications on this instance 
            // and always (partially) load on DAS when application is 
            // referenced by non-DAS target so the application
            // information is available on DAS
            if (deployment.isAppEnabled(standaloneAdapter) || loadAppOnDAS(standaloneAdapter.getName())) {
              DeploymentOrder.addApplicationDeployment(new ApplicationOrderInfo(standaloneAdapter, appOrderInfoMap.get(standaloneAdapter.getName()).intValue()));
            }
        }

        // then the rest of the applications
        for (Application app : allApplications) {
            if (app.isStandaloneModule() && 
                app.containsSnifferType(ServerTags.CONNECTOR)) {
                continue;
            }
            // load the referenced enabled applications on this instance 
            // and always (partially) load on DAS when application is 
            // referenced by non-DAS target so the application
            // information is available on DAS
            if (deployment.isAppEnabled(app) || loadAppOnDAS(app.getName())) {
              DeploymentOrder.addApplicationDeployment(new ApplicationOrderInfo(app, appOrderInfoMap.get(app.getName()).intValue()));
            }
        }

        Iterator iter = DeploymentOrder.getApplicationDeployments();
        while (iter.hasNext()) {
          Application app = (Application)iter.next();
          ApplicationRef appRef = server.getApplicationRef(app.getName());
          processApplication(app, appRef);
        }

        // does the user want us to run a particular application
        String defaultParam = env.getStartupContext().getArguments().getProperty("default");
        if (defaultParam!=null) {

            initializeRuntimeDependencies();
            
            File sourceFile;
            if (defaultParam.equals(".")) {
                sourceFile = new File(System.getProperty("user.dir"));
            } else {
                sourceFile = new File(defaultParam);
            }


            if (sourceFile.exists()) {
                sourceFile = sourceFile.getAbsoluteFile();
                ReadableArchive sourceArchive=null;
                try {
                    sourceArchive = archiveFactoryProvider.get().openArchive(sourceFile);

                    DeployCommandParameters parameters = new DeployCommandParameters(sourceFile);
                    parameters.name = sourceFile.getName();
                    parameters.enabled = Boolean.TRUE;
                    parameters.origin = DeployCommandParameters.Origin.deploy;

                    ActionReport report = new HTMLActionReporter();

                    if (!sourceFile.isDirectory()) {

                    // ok we need to explode the directory somwhere and remember to delete it on shutdown
                        final File tmpFile = File.createTempFile(sourceFile.getName(),"");
                        final String path = tmpFile.getAbsolutePath();
                        if (!tmpFile.delete()) {
                            logger.log(Level.WARNING, KernelLoggerInfo.cantDeleteTempFile, path);
                        }
                        File tmpDir = new File(path);
                        tmpDir.deleteOnExit();
                        events.register(new org.glassfish.api.event.EventListener() {
                            public void event(Event event) {
                                if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                                    if (tmpFile.exists()) {
                                        FileUtils.whack(tmpFile);
                                    }
                                }
                            }
                        });
                        if (tmpDir.mkdirs()) {
                            ArchiveHandler handler = deployment.getArchiveHandler(sourceArchive);
                            final String appName = handler.getDefaultApplicationName(sourceArchive);
                            DeploymentContextImpl dummyContext = new DeploymentContextImpl(report, logger, sourceArchive, parameters, env);
                            handler.expand(sourceArchive, archiveFactoryProvider.get().createArchive(tmpDir), dummyContext);
                            sourceArchive =
                                    archiveFactoryProvider.get().openArchive(tmpDir);
                            logger.log(Level.INFO, KernelLoggerInfo.sourceNotDirectory, tmpDir.getAbsolutePath());
                            parameters.name = appName;
                        }
                    }
                    ExtendedDeploymentContext depContext = deployment.getBuilder(logger, parameters, report).source(sourceArchive).build();
                    
                    ApplicationInfo appInfo = deployment.deploy(depContext);
                    if (appInfo==null) {

                        logger.log(Level.SEVERE, KernelLoggerInfo.cantFindApplicationInfo, sourceFile.getAbsolutePath());
                    }
                } catch(RuntimeException e) {
                    logger.log(Level.SEVERE, KernelLoggerInfo.deployException, e);
                } catch(IOException ioe) {
                    logger.log(Level.SEVERE, KernelLoggerInfo.deployException, ioe);                    
                } finally {
                    if (sourceArchive!=null) {
                        try {
                            sourceArchive.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }
            }
        }
        events.send(new Event<DeploymentContext>(Deployment.ALL_APPLICATIONS_PROCESSED, null));

    }

    private void initializeRuntimeDependencies() {
        // ApplicationLoaderService needs to be initialized after
        // ManagedBeanManagerImpl. By injecting ManagedBeanManagerImpl,
        // we guarantee the initialization order.
        habitat.getAllServices(BuilderHelper.createNameFilter("ManagedBeanManagerImpl"));

        // ApplicationLoaderService needs to be initialized after
        // ResourceManager. By injecting ResourceManager, we guarantee the
        // initialization order.
        // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=7179
        habitat.getAllServices(BuilderHelper.createNameFilter("ResourceManager"));

        // Application scoped resource is loaded after ResourceManager
        // http://java.net/jira/browse/GLASSFISH-19161
        habitat.getAllServices(BuilderHelper.createNameFilter("ApplicationScopedResourcesManager"));

    }


    public void processApplication(Application app, ApplicationRef appRef) {

        long operationStartTime = Calendar.getInstance().getTimeInMillis();

        initializeRuntimeDependencies();        

        String source = app.getLocation();
        final String appName = app.getName();

        // lifecycle modules are loaded separately
        if (Boolean.valueOf(app.getDeployProperties().getProperty
            (ServerTags.IS_LIFECYCLE))) {
            return;
        }

        URI uri;
        try {
            uri = new URI(source);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.cantDetermineLocation, e.getLocalizedMessage());
            return;
        }
        File sourceFile = new File(uri);
        if (sourceFile.exists()) {
            try {
                ReadableArchive archive = null;
                try {

                    DeploymentTracing tracing = null;
                    if (deploymentTracingEnabled != null) {
                        tracing = new DeploymentTracing();
                    }
                    DeployCommandParameters deploymentParams =
                        app.getDeployParameters(appRef);
                    deploymentParams.target = server.getName();
                    deploymentParams.origin = DeployCommandParameters.Origin.load;
                    deploymentParams.command = DeployCommandParameters.Command.startup_server;
                    if (domain.isAppReferencedByPaaSTarget(appName)) {
                        if (server.isDas()) {
                            // for loading PaaS application on DAS
                            // we set it to the real PaaS target
                            deploymentParams.target = deployment.getDefaultTarget(appName, deploymentParams.origin, deploymentParams._classicstyle);
                        }
                    }

                    archive = archiveFactoryProvider.get().openArchive(sourceFile, deploymentParams);

                    ActionReport report = new HTMLActionReporter();
                    ExtendedDeploymentContext depContext = deployment.getBuilder(logger, deploymentParams, report).source(archive).build();
                    if (tracing!=null) {
                        depContext.addModuleMetaData(tracing);
                    }

                    depContext.getAppProps().putAll(app.getDeployProperties());
                    depContext.setModulePropsMap(app.getModulePropertiesMap());

                    new ApplicationConfigInfo(app).store(depContext.getAppProps());

                    deployment.deploy(deployment.getSniffersFromApp(app), depContext);
                    loadApplicationForTenants(app, appRef, report);
                    if (report.getActionExitCode().equals(ActionReport.ExitCode.SUCCESS)) {
                        if (tracing!=null) {
                            tracing.print(System.out);
                        }
                        logger.log(Level.INFO, KernelLoggerInfo.loadingApplicationTime, new Object[] {
                                appName, (Calendar.getInstance().getTimeInMillis() - operationStartTime)});
                    } else {
                        logger.log(Level.SEVERE, KernelLoggerInfo.deployFail, report.getMessage());
                    }
                } finally {
                    if (archive!=null) {
                        try {
                            archive.close();
                        } catch(IOException e) {
                            logger.log(Level.FINE, KernelLoggerInfo.deployException, e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, KernelLoggerInfo.exceptionOpenArtifact, e);

            }

        } else {
            logger.log(Level.SEVERE, KernelLoggerInfo.notFoundInOriginalLocation, source);
        }
    }


    public String toString() {
        return "Application Loader";
    }

    /**
     * Stopped all loaded applications
     */
    public void preDestroy() {


        // stop all running applications including user and system applications
        // which are registered in the domain.xml
        List<Application> allApplications = new ArrayList<Application>();

        List<Application> standaloneAdapters =
            applications.getApplicationsWithSnifferType(ServerTags.CONNECTOR, true);

        allApplications.addAll(applications.getApplications());
        allApplications.addAll(systemApplications.getApplications());

        //stop applications that are not of type "standalone" connectors
        for (Application app : allApplications) {
            if (app.isStandaloneModule() &&
                app.containsSnifferType(ServerTags.CONNECTOR)) {
                continue;
            }
            ApplicationInfo appInfo = deployment.get(app.getName());
            stopApplication(app, appInfo);
        }

        //stop applications that are "standalone" connectors
        for (Application app : standaloneAdapters) {
            ApplicationInfo appInfo = deployment.get(app.getName());
            stopApplication(app, appInfo);
        }

        // now stop the applications which are not registered in the 
        // domain.xml like timer service application
        Set<String> allAppNames = new HashSet<String>();
        allAppNames.addAll(appRegistry.getAllApplicationNames());
        for (String appName : allAppNames) {
            ApplicationInfo appInfo = appRegistry.get(appName);
            stopApplication(null, appInfo);
        }

        // stop all the containers
        for (EngineInfo engineInfo : containerRegistry.getContainers()) {
            engineInfo.stop(logger);
        }
    }

    private void stopApplication(Application app, ApplicationInfo appInfo) {
        final ActionReport dummy = new HTMLActionReporter();
        if (appInfo!=null) {
            UndeployCommandParameters parameters = new UndeployCommandParameters(appInfo.getName());
            parameters.origin = UndeployCommandParameters.Origin.unload;
            parameters.command = UndeployCommandParameters.Command.shutdown_server;

            try {
                deployment.disable(parameters, app, appInfo, dummy, logger);
            } catch (Exception e) {
                logger.log(Level.SEVERE, KernelLoggerInfo.loadingApplicationErrorDisable, e);
            }
            unloadApplicationForTenants(app, dummy);
            appRegistry.remove(appInfo.getName());
        }
    }

    private void unloadApplicationForTenants(Application app, ActionReport report) {
        if (app == null || app.getAppTenants() == null) {
            return;
        }

        for (AppTenant tenant : app.getAppTenants().getAppTenant()) {
            UndeployCommandParameters parameters = new UndeployCommandParameters();
            parameters.name = DeploymentUtils.getInternalNameForTenant(app.getName(), tenant.getTenant());
            parameters.origin = UndeployCommandParameters.Origin.unload;
            parameters.target = server.getName();
            ApplicationInfo appInfo = deployment.get(parameters.name);
            if (appInfo == null) {
                continue;
            }

            ActionReport subReport = report.addSubActionsReport();

            try {
                ExtendedDeploymentContext deploymentContext = deployment.getBuilder(KernelLoggerInfo.getLogger(), parameters, subReport).source(appInfo.getSource()).build();

                deploymentContext.getAppProps().putAll(
                    app.getDeployProperties());
                deploymentContext.getAppProps().putAll(
                    tenant.getDeployProperties());
                deploymentContext.setModulePropsMap(
                    app.getModulePropertiesMap());

                deploymentContext.setTenant(tenant.getTenant(), app.getName());

                deployment.unload(appInfo, deploymentContext);

            } catch(Throwable e) {
               subReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
               subReport.setMessage(e.getMessage());
               subReport.setFailureCause(e);
            }
            appRegistry.remove(appInfo.getName());
        }
    }

    private void loadApplicationForTenants(Application app, ApplicationRef appRef, ActionReport report) {
        if (app.getAppTenants() == null) {
            return;
        }
        for (AppTenant tenant : app.getAppTenants().getAppTenant()) {
            DeployCommandParameters commandParams = app.getDeployParameters(appRef);
            commandParams.contextroot = tenant.getContextRoot();
            commandParams.target = server.getName();
            commandParams.name = DeploymentUtils.getInternalNameForTenant(app.getName(), tenant.getTenant());
            commandParams.enabled = Boolean.TRUE;
            commandParams.origin = DeployCommandParameters.Origin.load;

            ActionReport subReport = report.addSubActionsReport();
            ReadableArchive archive = null;

            try {
                URI uri = new URI(app.getLocation());
                File file = new File(uri);

                if (file.exists()) {
                    archive = archiveFactoryProvider.get().openArchive(file);

                    ExtendedDeploymentContext deploymentContext =
                        deployment.getBuilder(KernelLoggerInfo.getLogger(), commandParams, subReport).source(archive).build();

                    deploymentContext.getAppProps().putAll(app.getDeployProperties());
                    deploymentContext.getAppProps().putAll(tenant.getDeployProperties());
                    deploymentContext.setModulePropsMap(app.getModulePropertiesMap());
                    deploymentContext.setTenant(tenant.getTenant(), app.getName());
                    deployment.deploy(deployment.getSniffersFromApp(app), deploymentContext);
                } else {
                    logger.log(Level.SEVERE, KernelLoggerInfo.notFoundInOriginalLocation, app.getLocation());
                }
            } catch(Throwable e) {
               subReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
               subReport.setMessage(e.getMessage());
               subReport.setFailureCause(e);
            } finally {
                try {
                    if (archive != null) {
                        archive.close();
                    }
                } catch(IOException e) {
                    // ignore
                }
            }
        }
    }

    private boolean loadAppOnDAS(String appName) {
        if (server.isDas()) {
            List<String> targets = domain.getAllReferencedTargetsForApplication(appName);
            for (String target : targets) {
                if (!DeploymentUtils.isDASTarget(target)) {
                    // if application is referenced by any non-DAS target 
                    // we need to partially load it on DAS
                    return true;
                }
            }
        } 
        return false;
    }
}
