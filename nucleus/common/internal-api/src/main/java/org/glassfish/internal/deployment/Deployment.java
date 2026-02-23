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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package org.glassfish.internal.deployment;

import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.event.EventTypes;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.data.ProgressTracker;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.RetryableException;
import org.jvnet.hk2.config.Transaction;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import org.glassfish.api.deployment.ApplicationMetaDataProvider;

/**
 * Deployment facility
 *
 * @author Jerome Dochez
 */
@Contract
public interface Deployment {
    /**
     * This synchronous event is sent right after initial deployment context is created
     */
    EventTypes<DeploymentContext> INITIAL_CONTEXT_CREATED = EventTypes.create("Initial_Context_Created", DeploymentContext.class);
    /**
     * This synchronous event is sent when a new deployment or loading of an already deployed application start. It is invoked
     * once before any sniffer is invoked.
     */
    EventTypes<DeploymentContext> DEPLOYMENT_START = EventTypes.create("Deployment_Start", DeploymentContext.class);
    
    /**
     * The name of the Deployment Failure event
     */
    String DEPLOYMENT_FAILURE_NAME = "Deployment_Failed";
    /**
     * This asynchronous event is sent when a deployment activity (first time deploy or loading of an already deployed application)
     * failed.
     */
    EventTypes<DeploymentContext> DEPLOYMENT_FAILURE = EventTypes.create(DEPLOYMENT_FAILURE_NAME, DeploymentContext.class);
    /**
     * This synchronous event is sent after creation of deployment classloader. 
     */
    EventTypes<DeploymentContext> AFTER_DEPLOYMENT_CLASSLOADER_CREATION =
            EventTypes.create("After_Deployment_ClassLoader_Creation", DeploymentContext.class);
    /**
     * This synchronous event is sent before prepare phase of deployment. 
     */
    EventTypes<DeploymentContext> DEPLOYMENT_BEFORE_CLASSLOADER_CREATION =
            EventTypes.create("Deployment_ClassLoader_Creation", DeploymentContext.class);
    /**
     * This synchronous event is sent after creation of application classloader. 
     */
    EventTypes<DeploymentContext> AFTER_APPLICATION_CLASSLOADER_CREATION =
            EventTypes.create("After_Application_ClassLoader_Creation", DeploymentContext.class);

    /**
     * This asynchronous event is sent when a deployment activity (first time deploy or loading of an already deployed application)
     * succeeded.
     */
    EventTypes<ApplicationInfo> DEPLOYMENT_SUCCESS = EventTypes.create("Deployment_Success", ApplicationInfo.class);

    /**
     * This synchronous event is sent at the end of deployment command process
     */
    EventTypes<ApplicationInfo> DEPLOYMENT_COMMAND_FINISH = EventTypes.create("Deployment_Command_Finish", ApplicationInfo.class);


    /**
     * This asynchronous event is sent when a new deployment or loading of an already deployed application start. It is invoked
     * once before any sniffer is invoked.
     */
    EventTypes<ApplicationInfo> UNDEPLOYMENT_START = EventTypes.create("Undeployment_Start", ApplicationInfo.class);
    /**
     * This asynchronous event is sent when a deployment activity (first time deploy or loading of an already deployed application)
     * failed.
     */
    EventTypes<DeploymentContext> UNDEPLOYMENT_FAILURE = EventTypes.create("Undeployment_Failed", DeploymentContext.class);

    /**
     * This asynchronous event is sent when a deployment activity (first time deploy or loading of an already deployed application)
     * succeeded.
     */
    EventTypes<DeploymentContext> UNDEPLOYMENT_SUCCESS = EventTypes.create("Undeployment_Success", DeploymentContext.class);

    /**
     * The following synchronous events are sent after each change in a module state.
     */
    EventTypes<DeploymentContext> MODULE_PREPARED = EventTypes.create("Module_Prepared", DeploymentContext.class);
    EventTypes<ModuleInfo> MODULE_LOADED = EventTypes.create("Module_Loaded", ModuleInfo.class);
    EventTypes<ModuleInfo> MODULE_STARTED = EventTypes.create("Module_Running", ModuleInfo.class);
    EventTypes<ModuleInfo> MODULE_STOPPED = EventTypes.create("Module_Stopped", ModuleInfo.class);
    EventTypes<ModuleInfo> MODULE_UNLOADED = EventTypes.create("Module_Unloaded", ModuleInfo.class);
    EventTypes<DeploymentContext> MODULE_CLEANED= EventTypes.create("Module_Cleaned", DeploymentContext.class);

    /**
     * The following synchronous events are sent after each change in an application stated (An application contains
     * 1 to many modules)
     */
    EventTypes<DeploymentContext> APPLICATION_PREPARED = EventTypes.create("Application_Prepared", DeploymentContext.class);
    EventTypes<ApplicationInfo> APPLICATION_LOADED = EventTypes.create("Application_Loaded", ApplicationInfo.class);
    EventTypes<ApplicationInfo> APPLICATION_STARTED = EventTypes.create("Application_Running", ApplicationInfo.class);
    EventTypes<ApplicationInfo> APPLICATION_STOPPED = EventTypes.create("Application_Stopped", ApplicationInfo.class);
    EventTypes<ApplicationInfo> APPLICATION_UNLOADED = EventTypes.create("Application_Unloaded", ApplicationInfo.class);
    EventTypes<DeploymentContext> APPLICATION_CLEANED= EventTypes.create("Application_Cleaned", DeploymentContext.class);
    EventTypes<ApplicationInfo> APPLICATION_DISABLED = EventTypes.create("Application_Disabled", ApplicationInfo.class);


    /**
     * The following synchronous event is sent before the application is 
     * undeployed so various listeners could validate the undeploy operation
     * and decide whether to abort undeployment
     */
    EventTypes<DeploymentContext> UNDEPLOYMENT_VALIDATION = EventTypes.create("Undeployment_Validation", DeploymentContext.class);

    /**
     * This event is thrown before the STOP deployment phase, notably from the disable and undeploy asadmin commands.
     * It is used to mark the context unavailable before stopping the application and its engines, in cases where
     * implementing the ApplicationLifecycleInterceptor interface will cause a circular dependency
     */
    EventTypes<ApplicationInfo> DISABLE_START = EventTypes.create("Disable_Start", ApplicationInfo.class);


    interface DeploymentContextBuilder {

        DeploymentContextBuilder source(File source);
        DeploymentContextBuilder source(ReadableArchive archive);
        File sourceAsFile();
        ReadableArchive sourceAsArchive();
        ArchiveHandler archiveHandler();
        DeploymentContextBuilder archiveHandler(ArchiveHandler handler);

        Logger logger();
        ActionReport report();
        OpsParams params();
        
        ExtendedDeploymentContext build() throws IOException;

        ExtendedDeploymentContext build(ExtendedDeploymentContext initialContext)
                throws IOException;

    }

    class ApplicationDeployment {
        public ApplicationDeployment(ApplicationInfo appInfo, ExtendedDeploymentContext context) {
            this.appInfo = appInfo;
            this.context = context;
        }

        public final ApplicationInfo appInfo;
        public final ExtendedDeploymentContext context;
    }

    /**
     * triggered when all applications are loaded, but not yet initialized
     * Useful to find out when all classes are available in the class loader
     */
    EventTypes<DeploymentContext> ALL_APPLICATIONS_LOADED = EventTypes.create("All_Applications_Loaded", DeploymentContext.class);

    /**
     * The following asynchronous event is sent after all applications are 
     * started in server start up.
     */
    EventTypes<DeploymentContext> ALL_APPLICATIONS_PROCESSED= EventTypes.create("All_Applications_Processed", DeploymentContext.class);

    /**
     * All applications are now stopped / unloaded in the process of server shutdown
     */
    EventTypes<DeploymentContext> ALL_APPLICATIONS_STOPPED = EventTypes.create("All_Applications_Stopped", DeploymentContext.class);

    DeploymentContextBuilder getBuilder(Logger loggger, OpsParams params, ActionReport report);

    ArchiveHandler getArchiveHandler(ReadableArchive archive) throws IOException;

    ArchiveHandler getArchiveHandler(ReadableArchive archive, String type) throws IOException;

    ModuleInfo prepareModule(
        List<EngineInfo> sortedEngineInfos, String moduleName,
        DeploymentContext context,
        ProgressTracker tracker) throws Exception;

    ApplicationDeployment prepare(final Collection<? extends Sniffer> sniffers, final ExtendedDeploymentContext context);
    void initialize(ApplicationInfo appInfo, final Collection<? extends Sniffer> sniffers, final ExtendedDeploymentContext context);

    ApplicationInfo deploy(final ExtendedDeploymentContext context);
    ApplicationInfo deploy(final Collection<? extends Sniffer> sniffers, final ExtendedDeploymentContext context);

    void undeploy(String appName, ExtendedDeploymentContext context);

    Transaction prepareAppConfigChanges(final DeploymentContext context)
        throws TransactionFailure;

    void registerAppInDomainXML(final ApplicationInfo
        applicationInfo, final DeploymentContext context, Transaction t) 
        throws TransactionFailure;

    void unregisterAppFromDomainXML(final String appName, 
        final String target)
        throws TransactionFailure;

    void registerAppInDomainXML(final ApplicationInfo
        applicationInfo, final DeploymentContext context, Transaction t,
        boolean appRefOnly)
        throws TransactionFailure;

    void unregisterAppFromDomainXML(final String appName,
        final String target, boolean appRefOnly)
        throws TransactionFailure;

    void registerTenantWithAppInDomainXML(final String appName, final ExtendedDeploymentContext context)
            throws TransactionFailure;

    void unregisterTenantWithAppInDomainXML(final String appName, final String tenantName)
            throws TransactionFailure, RetryableException;

    void updateAppEnabledAttributeInDomainXML(final String appName,
        final String target, final boolean enabled) throws TransactionFailure;

    List<EngineInfo> setupContainerInfos(
            DeploymentContext context) throws Exception;

    List<EngineInfo> setupContainerInfos(final ArchiveHandler handler,
            Collection<? extends Sniffer> sniffers, DeploymentContext context)
             throws Exception;
    
    Map<Class, ApplicationMetaDataProvider> getTypeByProvider();

    boolean isRegistered(String appName);

    ApplicationInfo get(String appName);

    ParameterMap prepareInstanceDeployParamMap(DeploymentContext dc) throws Exception;

    void validateDeploymentTarget(String target, String name,
        boolean isRedeploy);

    void validateUndeploymentTarget(String target, String name);

    boolean isAppEnabled(Application app);

    ApplicationInfo unload(ApplicationInfo appInfo,
        ExtendedDeploymentContext context);

    DeploymentContext disable(UndeployCommandParameters commandParams, 
        Application app, ApplicationInfo appInfo, ActionReport report, 
        Logger logger) throws Exception;

    DeploymentContext enable(String target, Application app, ApplicationRef appRef,
        ActionReport report, Logger logger) throws Exception;

    /**
     * Scans the source of the deployment operation for all types
     * and store the result in the deployment context.
     * Subsequent calls will return the cached copy from the context
     *
     * @param context deployment context
     * @return the types information from the deployment artifacts
     * @throws IOException if the scanning fails due to an I/O exception
     */
    Types getDeployableTypes(DeploymentContext context) throws IOException;

    List<Sniffer> getSniffersFromApp(Application app);

    Collection<? extends Sniffer> getSniffers(ArchiveHandler archiveHandler, Collection<? extends Sniffer> sniffers, DeploymentContext context);

    // sets the default target when the target is not specified
    String getDefaultTarget(String appName, OpsParams.Origin origin, Boolean isClassicStyle);

    // gets the default target when no target is specified for non-paas case
    String getDefaultTarget(Boolean isClassicStyle);

    /**
     * Returns thread-local deployment, which is the currently-executing deployment context,
     * or null if none
     * 
     * @return Currently-executing deployment context
     */
    ExtendedDeploymentContext getCurrentDeploymentContext();
}
