/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.SystemApplications;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.common.HTMLActionReporter;
import com.sun.logging.LogDomains;
import com.sun.hk2.component.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Startup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.internal.deployment.DeploymentTracing;
import org.glassfish.internal.api.*;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.deployment.monitor.DeploymentLifecycleStatsProvider;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.deployment.common.ApplicationConfigInfo;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.DeploymentProperties;

/**
 * This service is responsible for loading all deployed applications...
 *
 * @author Jerome Dochez
 */
@Priority(8) // low priority , should be started last
@Service(name="ApplicationLoaderService")
public class ApplicationLoaderService implements Startup, PreDestroy, PostConstruct {

    final Logger logger = LogDomains.getLogger(AppServerStartup.class, LogDomains.CORE_LOGGER);

    @Inject
    Deployment deployment;

    @Inject
    Holder<ArchiveFactory> archiveFactory;

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

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    @Inject
    ServerEnvironment env;

    @Inject
    Habitat habitat;

    private String deploymentTracingEnabled = null;

    /**
     * Retuns the lifecyle of the service.
     * Once the applications are loaded, this service does not need to remain
     * available
     */
    public Startup.Lifecycle getLifecycle() {
        return Startup.Lifecycle.SERVER;
    }

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
            logger.fine("satisfy.optionalpkg.dependency");
            InstalledLibrariesResolver.initializeInstalledLibRegistry(env.getLibPath().getAbsolutePath());
        }catch(Exception e){
            logger.log(Level.WARNING, "optionalpkg.error", e);
        }

        DeploymentLifecycleStatsProvider dlsp = new DeploymentLifecycleStatsProvider();
        StatsProviderManager.register("deployment", PluginPoint.SERVER,
            "deployment/lifecycle", dlsp);

        deploymentTracingEnabled = System.getProperty(
            "org.glassfish.deployment.trace");

        Domain domain = habitat.getComponent(Domain.class);
        systemApplications = domain.getSystemApplications();
        
        for (Application systemApp : systemApplications.getApplications()) {
            // check to see if we need to load up this system application
            if (Boolean.valueOf(systemApp.getDeployProperties().getProperty
                (ServerTags.LOAD_SYSTEM_APP_ON_STARTUP))) {
                if (deployment.isAppEnabled(systemApp) || server.isDas()) {
                    ApplicationRef appRef = server.getApplicationRef(systemApp.getName());
                    processApplication(systemApp, appRef, logger);
                }
            }
        }

        List<Application> allApplications = applications.getApplications();

        List<Application> standaloneAdapters =
            applications.getApplicationsWithSnifferType(Application.CONNECTOR_SNIFFER_TYPE, true);

        // load standalone resource adapters first
        for (Application standaloneAdapter : standaloneAdapters) {
            // load the referenced enabled applications on this instance 
            // and always (partially) load on DAS so the application
            // information is available on DAS
            if (deployment.isAppEnabled(standaloneAdapter) || server.isDas()) {
                ApplicationRef appRef = server.getApplicationRef(standaloneAdapter.getName());
                processApplication(standaloneAdapter, appRef, logger);
            }
        }

        // then the rest of the applications
        for (Application app : allApplications) {
            if (app.isStandaloneModule() && 
                app.containsSnifferType(Application.CONNECTOR_SNIFFER_TYPE)) {
                continue;
            }
            // load the referenced enabled applications on this instance 
            // and always (partially) load on DAS so the application
            // information is available on DAS
            if (deployment.isAppEnabled(app) || server.isDas()) {
                ApplicationRef appRef = server.getApplicationRef(app.getName());
                processApplication(app, appRef, logger);
            }
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
                    sourceArchive = archiveFactory.get().openArchive(sourceFile);

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
                            logger.log(Level.WARNING, "cannot.delete.temp.file", new Object[] {path});
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
                            handler.expand(sourceArchive, archiveFactory.get().createArchive(tmpDir), dummyContext);
                            sourceArchive = 
                                archiveFactory.get().openArchive(tmpDir);
                            logger.log(Level.INFO, "source.not.directory", new Object[] {tmpDir.getAbsolutePath()});
                            parameters.name = appName;
                        }
                    }
                    ExtendedDeploymentContext depContext = deployment.getBuilder(logger, parameters, report).source(sourceArchive).build();
                    
                    ApplicationInfo appInfo = deployment.deploy(depContext);
                    if (appInfo==null) {

                        logger.log(Level.SEVERE, "cannot.find.applicationinfo", new Object[] {sourceFile.getAbsolutePath()});
                    }
                } catch(RuntimeException e) {
                    logger.log(Level.SEVERE, "exception.while.deploying", e);
                } catch(IOException ioe) {
                    logger.log(Level.SEVERE, "ioexception.while.deploying", ioe);                    
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
        habitat.getComponent(PostStartup.class, "ManagedBeanManagerImpl");

        // ApplicationLoaderService needs to be initialized after
        // ResourceManager. By injecting ResourceManager, we guarantee the
        // initialization order.
        // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=7179
        habitat.getComponent(PostStartup.class, "ResourceManager");

    }


    public void processApplication(Application app, ApplicationRef appRef, 
        final Logger log) {

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
            logger.log(Level.SEVERE, "cannot.determine.location", new Object[] {e.getMessage()});
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
                    if (app.containsSnifferType(Application.OSGI_SNIFFER_TYPE)) {
                        deploymentParams.type = DeploymentProperties.OSGI;
                    }

                    archive = archiveFactory.get().openArchive(sourceFile, deploymentParams);

                    ActionReport report = new HTMLActionReporter();
                    ExtendedDeploymentContext depContext = deployment.getBuilder(logger, deploymentParams, report).source(archive).build();
                    if (tracing!=null) {
                        depContext.addModuleMetaData(tracing);
                    }

                    depContext.getAppProps().putAll(app.getDeployProperties());
                    depContext.setModulePropsMap(app.getModulePropertiesMap());

                    new ApplicationConfigInfo(app).store(depContext.getAppProps());

                    deployment.deploy(deployment.getSniffersFromApp(app), depContext);
                    if (report.getActionExitCode().equals(ActionReport.ExitCode.SUCCESS)) {
                        if (tracing!=null) {
                            tracing.print(System.out);
                        }
                        logger.log(Level.INFO, "loading.application.time", new Object[] {
                                appName, (Calendar.getInstance().getTimeInMillis() - operationStartTime)});
                    } else {
                        logger.severe(report.getMessage());
                    }
                } finally {
                    if (archive!=null) {
                        try {
                            archive.close();
                        } catch(IOException e) {
                            logger.log(Level.FINE, e.getMessage(), e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "exception.open.artifact", e);

            }

        } else {
            logger.log(Level.SEVERE, "not.found.in.original.location", new Object[] {source});
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
        allApplications.addAll(applications.getApplications());
        allApplications.addAll(systemApplications.getApplications());
        for (Application app : allApplications) {
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

            // send this disable event on the DAS when the application  
            // is not loaded on DAS
            if (!appInfo.isLoaded() && env.isDas()) {
                events.send(new Event<ApplicationInfo>(Deployment.APPLICATION_DISABLED, appInfo));
            }

            UndeployCommandParameters parameters = new UndeployCommandParameters(appInfo.getName());
            parameters.origin = UndeployCommandParameters.Origin.unload;

            try {
                deployment.disable(parameters, app, appInfo, dummy, logger);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            appRegistry.remove(appInfo.getName());
        }
    }
}
