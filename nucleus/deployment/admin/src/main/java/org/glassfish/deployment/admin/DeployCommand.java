/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import fish.payara.nucleus.hotdeploy.HotDeployService;
import fish.payara.nucleus.hotdeploy.ApplicationState;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.common.*;
import org.glassfish.deployment.versioning.VersioningService;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.*;
import org.glassfish.internal.deployment.analysis.DeploymentSpan;
import org.glassfish.internal.deployment.analysis.SpanSequence;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Transaction;

/**
 * Deploy command
 *
 * @author Jerome Dochez
 */
@Service(name = "deploy")
@I18n("deploy.command")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.POST, path = "deploy"),
    @RestEndpoint(configBean = Cluster.class, opType = RestEndpoint.OpType.POST, path = "deploy", params = {
        @RestParam(name = "target", value = "$parent")
    }),
    @RestEndpoint(configBean = DeploymentGroup.class, opType = RestEndpoint.OpType.POST, path = "deploy", params = {
        @RestParam(name = "target", value = "$parent")
    }),
    @RestEndpoint(configBean = Server.class, opType = RestEndpoint.OpType.POST, path = "deploy", params = {
        @RestParam(name = "target", value = "$parent")
    })
})
public class DeployCommand extends DeployCommandParameters implements AdminCommand, EventListener,
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeployCommand.class);

    @Inject
    Applications apps;

    @Inject
    ServerEnvironment env;

    @Inject
    ServiceLocator habitat;

    @Inject
    CommandRunner commandRunner;

    @Inject
    Deployment deployment;

    @Inject
    SnifferManager snifferManager;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    Domain domain;

    @Inject
    Events events;

    @Inject
    VersioningService versioningService;

    @Inject
    private HotDeployService hotDeployService;

    private File safeCopyOfApp = null;
    private File safeCopyOfDeploymentPlan = null;
    private File safeCopyOfAltDD = null;
    private File safeCopyOfRuntimeAltDD = null;
    private File originalPathValue;
    private List<String> previousTargets = new ArrayList<>();
    private final Properties previousVirtualServers = new Properties();
    private final Properties previousEnabledAttributes = new Properties();
    private Logger logger;
    private ExtendedDeploymentContext initialContext;
    private ExtendedDeploymentContext deploymentContext;
    private ArchiveHandler archiveHandler;
    private File expansionDir;
    private ReadableArchive archive;
    private DeploymentTracing timing;
    private transient DeployCommandSupplementalInfo suppInfo;
    private static final String EJB_JAR_XML = "META-INF/ejb-jar.xml";
    private static final String SUN_EJB_JAR_XML = "META-INF/sun-ejb-jar.xml";
    private static final String GF_EJB_JAR_XML = "META-INF/glassfish-ejb-jar.xml";

    private static final String APPLICATION_XML = "META-INF/application.xml";
    private static final String SUN_APPLICATION_XML = "META-INF/sun-application.xml";
    private static final String GF_APPLICATION_XML  = "META-INF/glassfish-application.xml";

    private static final String RA_XML  = "META-INF/ra.xml";

    private static final String APPLICATION_CLIENT_XML = "META-INF/application-client.xml";
    private static final String SUN_APPLICATION_CLIENT_XML = "META-INF/sun-application-client.xml";
    private static final String GF_APPLICATION_CLIENT_XML = "META-INF/glassfish-application-client.xml";
    private StructuredDeploymentTracing structuredTracing;

    public DeployCommand() {
        origin = Origin.deploy;
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        logger = context.getLogger();
        events.register(this);

        suppInfo
                = new DeployCommandSupplementalInfo();
        context.getActionReport().
                setResultType(DeployCommandSupplementalInfo.class, suppInfo);

        structuredTracing = System.getProperty("org.glassfish.deployment.trace") != null
                ? StructuredDeploymentTracing.create(path.getName())
                : StructuredDeploymentTracing.createDisabled(path.getName());

        timing = new DeploymentTracing(structuredTracing);

        final ActionReport report = context.getActionReport();

        originalPathValue = path;
        if (!path.exists()) {
            report.setMessage(localStrings.getLocalString("fnf", "File not found", path.getAbsolutePath()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        if (!path.canRead()) {
            report.setMessage(localStrings.getLocalString("fnr", "File {0} does not have read permission", path.getAbsolutePath()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }

        if (snifferManager.hasNoSniffers()) {
            String msg = localStrings.getLocalString("nocontainer", "No container services registered, done...");
            report.failure(logger, msg);
            return false;
        }

        try (DeploymentSpan span = structuredTracing.startSpan(DeploymentTracing.AppStage.OPENING_ARCHIVE)) {
            archive = archiveFactory.openArchive(path, this);
        } catch (IOException e) {
            final String msg = localStrings.getLocalString("deploy.errOpeningArtifact",
                    "deploy.errOpeningArtifact", path.getAbsolutePath());
            if (logReportedErrors) {
                report.failure(logger, msg, e);
            } else {
                report.setMessage(msg + path.getAbsolutePath() + e.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            return false;
        }

        if (altdd != null) {
            archive.addArchiveMetaData(DeploymentProperties.ALT_DD, altdd);
        }

        if (runtimealtdd != null) {
            archive.addArchiveMetaData(DeploymentProperties.RUNTIME_ALT_DD,
                    runtimealtdd);
        }

        expansionDir = null;
        deploymentContext = null;
        try(SpanSequence span = structuredTracing.startSequence(DeploymentTracing.AppStage.VALIDATE_TARGET, "command")) {

            deployment.validateSpecifiedTarget(target);

            span.start(DeploymentTracing.AppStage.OPENING_ARCHIVE, "ArchiveHandler");

            archiveHandler = deployment.getArchiveHandler(archive, type);

            if (archiveHandler == null) {
                report.failure(logger, localStrings.getLocalString("deploy.unknownarchivetype", "Archive type of {0} was not recognized", path));
                return false;
            }

            span.start(DeploymentTracing.AppStage.CREATE_DEPLOYMENT_CONTEXT, "Initial");

            Optional<ApplicationState> appState = hotDeployService.getApplicationState(path);
            boolean hotswap = hotDeploy 
                    && !metadataChanged 
                    && appState.map(ApplicationState::isHotswap).orElse(false);
            if (!hotswap) {
                // create an initial  context
                initialContext = new DeploymentContextImpl(report, archive, this, env);
            } else {
               initialContext = hotDeployService.getApplicationState(path)
                        .map(ApplicationState::getDeploymentContext)
                        .orElseThrow(() -> new RuntimeException());
            }
            initialContext.setArchiveHandler(archiveHandler);

            if (hotDeploy && !metadataChanged && appState.isPresent()) {
                if(!appState.get().start(this, initialContext, events)){
                    appState.get().close();
                    return false;
                }
            } else {
                hotDeployService.removeApplicationState(path);
            }

            structuredTracing.register(initialContext);

            span.finish();

            span.start(DeploymentTracing.AppStage.PROCESS_EVENTS, Deployment.INITIAL_CONTEXT_CREATED.type());
            events.send(new Event<DeploymentContext>(Deployment.INITIAL_CONTEXT_CREATED, initialContext), false);

            span.start(DeploymentTracing.AppStage.DETERMINE_APP_NAME);

            if (!forceName) {
                boolean isModuleDescriptorAvailable = false;
                if (archiveHandler.getArchiveType().equals("ejb")
                        && (archive.exists(EJB_JAR_XML)
                        || archive.exists(SUN_EJB_JAR_XML)
                        || archive.exists(GF_EJB_JAR_XML))) {
                    isModuleDescriptorAvailable = true;
                } else if (archiveHandler.getArchiveType().equals("ear")
                        && (archive.exists(APPLICATION_XML)
                        || archive.exists(SUN_APPLICATION_XML)
                        || archive.exists(GF_APPLICATION_XML))) {
                    isModuleDescriptorAvailable = true;

                } else if (archiveHandler.getArchiveType().equals("car")
                        && (archive.exists(APPLICATION_CLIENT_XML)
                        || archive.exists(SUN_APPLICATION_CLIENT_XML)
                        || archive.exists(GF_APPLICATION_CLIENT_XML))) {
                    isModuleDescriptorAvailable = true;
                } else if (archiveHandler.getArchiveType().equals("rar")
                        && (archive.exists(RA_XML))) {
                    isModuleDescriptorAvailable = true;
                }

                if (isModuleDescriptorAvailable) {
                    name = archiveHandler.getDefaultApplicationName(initialContext.getSource(), initialContext, name);
                }
            }

            if (name == null) {
                name = archiveHandler.getDefaultApplicationName(initialContext.getSource(), initialContext);
            } else {
                DeploymentUtils.validateApplicationName(name);
            }

            boolean isUntagged = VersioningUtils.isUntagged(name);
            // no GlassFish versioning support for OSGi budles
            if (name != null && !isUntagged && type != null && type.equals("osgi")) {
                ActionReport.MessagePart msgPart = context.getActionReport().getTopMessagePart();
                msgPart.setChildrenType("WARNING");
                ActionReport.MessagePart childPart = msgPart.addChild();
                childPart.setMessage(VersioningUtils.LOCALSTRINGS.getLocalString(
                        "versioning.deployment.osgi.warning",
                        "OSGi bundles will not use the GlassFish versioning, any version information embedded as part of the name option will be ignored"));
                name = VersioningUtils.getUntaggedName(name);
            }

            // if no version information embedded as part of application name
            // we try to retrieve the version-identifier element's value from DD
            if (isUntagged) {
                String versionIdentifier = archiveHandler.getVersionIdentifier(initialContext.getSource());

                if (versionIdentifier != null && !versionIdentifier.isEmpty()) {
                    name = name + VersioningUtils.EXPRESSION_SEPARATOR + versionIdentifier;
                }
            }

            if (target == null) {
                target = deployment.getDefaultTarget(name, origin, _classicstyle);
            }

            boolean isRegistered = deployment.isRegistered(name);
            isredeploy = isRegistered && force;
            return true;
        } catch (Exception ex) {
            events.unregister(this);
            if (initialContext != null && initialContext.getSource() != null) {
                try {
                    initialContext.getSource().close();
                } catch (IOException ioex) {
                    throw new RuntimeException(ioex);
                }
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<>();
        accessChecks.add(new AccessCheck(DeploymentCommandUtils.getResourceNameForApps(domain), "create"));
        accessChecks.add(new AccessCheck(DeploymentCommandUtils.getTargetResourceNameForNewAppRef(domain, target), "create"));

        /*
         * If this app is already deployed then this operation also represents
         * an undeployment - a delete - of that app.
         */
        if (isredeploy) {
            final String appResource = DeploymentCommandUtils.getResourceNameForNewApp(domain, name);
            accessChecks.add(new AccessCheck(appResource, "delete"));
            final String appRefResource = DeploymentCommandUtils.getTargetResourceNameForNewAppRef(domain, target, name);
            accessChecks.add(new AccessCheck(appRefResource, "delete"));
        }

        return accessChecks;
    }

    /**
     * Entry point from the framework into the command execution
     *
     * @param context context for the command.
     */
    @Override
    public void execute(AdminCommandContext context) {
        long timeTakenToDeploy = 0;
        long deploymentTimeMillis = 0;
        Optional<ApplicationState> appState = Optional.empty();
        final ActionReport report = context.getActionReport();
        try (SpanSequence span = structuredTracing.startSequence(DeploymentTracing.AppStage.VALIDATE_TARGET, "registry")) {

            if (!hotDeploy) {
                hotDeployService.removeApplicationState(initialContext.getSourceDir());
            } else if (!(appState = hotDeployService.getApplicationState(initialContext.getSourceDir())).isPresent()) {
                ApplicationState applicationState = new ApplicationState(name, path, initialContext);
                applicationState.setTarget(target);
                appState = Optional.of(applicationState);
            }

            // needs to be fixed in hk2, we don't generate the right innerclass index. it should use $
            Collection<Interceptor> interceptors = habitat.getAllServices(Interceptor.class);
            if (interceptors != null) {
                for (Interceptor interceptor : interceptors) {
                    interceptor.intercept(this, initialContext);
                }
            }

            deployment.validateDeploymentTarget(target, name, isredeploy);

            ActionReport.MessagePart part = report.getTopMessagePart();
            part.addProperty(DeploymentProperties.NAME, name);

            ApplicationConfigInfo savedAppConfig
                    = new ApplicationConfigInfo(apps.getModule(Application.class, name));
            Properties undeployProps = null;
            if (appState.map(ApplicationState::isInactive).orElse(true)) {
                undeployProps = handleRedeploy(name, report, context);
            }
            appState.filter(ApplicationState::isInactive)
                    .ifPresent(hotDeployService::addApplicationState);

            if (enabled == null) {
                enabled = Boolean.TRUE;
            }

            // clean up any left over repository files
            if (!keepreposdir) {
                span.start(DeploymentTracing.AppStage.CLEANUP, "applications");
                final File reposDir = new File(env.getApplicationRepositoryPath(), VersioningUtils.getRepositoryName(name));
                if (reposDir.exists()) {
                    for (int i = 0; i < domain.getApplications().getApplications().size(); i++) {
                        File existrepos = new File(new URI(domain.getApplications().getApplications().get(i).getLocation()));
                        String appname = domain.getApplications().getApplications().get(i).getName();
                        if (!appname.equals(name) && existrepos.getAbsoluteFile().equals(reposDir.getAbsoluteFile())) {
                            report.failure(logger, localStrings.getLocalString("deploy.dupdeployment", "Application {0} is trying to use the same repository directory as application {1}, please choose a different application name to deploy", name, appname));
                            return;
                        }
                    }
                    /*
                     * Delete the repository directory as an archive to allow
                     * any special processing (such as stale file handling)
                     * to run.
                     */
                    final FileArchive arch = DeploymentUtils.openAsFileArchive(reposDir, archiveFactory);
                    arch.delete();
                }
                span.finish();
            }

            if (!DeploymentUtils.isDomainTarget(target) && enabled) {
                // try to disable the enabled version, if exist
                try (SpanSequence innerSpan = span.start(DeploymentTracing.AppStage.SWITCH_VERSIONS)) {
                    versioningService.handleDisable(name, target, report, context.getSubject());
                } catch (VersioningSyntaxException e) {
                    report.failure(logger, e.getMessage());
                    return;
                }
            }

            File source = new File(archive.getURI().getSchemeSpecificPart());
            boolean isDirectoryDeployed = true;
            if (!source.isDirectory()) {
                isDirectoryDeployed = false;
                expansionDir = new File(domain.getApplicationRoot(), VersioningUtils.getRepositoryName(name));
                path = expansionDir;
            } else {
                // test if a version is already directory deployed from this dir
                String versionFromSameDir
                        = versioningService.getVersionFromSameDir(source);
                if (!force && versionFromSameDir != null) {
                    report.failure(logger,
                            VersioningUtils.LOCALSTRINGS.getLocalString(
                                    "versioning.deployment.dual.inplace",
                                    "GlassFish do not support versioning for directory deployment when using the same directory. The directory {0} is already assigned to the version {1}.",
                                    source.getPath(),
                                    versionFromSameDir));
                    return;
                }
            }

            span.start(DeploymentTracing.AppStage.CREATE_DEPLOYMENT_CONTEXT, "Full");
            // create the parent class loader
            deploymentContext
                    = deployment.getBuilder(logger, this, report).
                            source(initialContext.getSource())
                            .archiveHandler(archiveHandler)
                            .build(initialContext);

            // reset the properties (might be null) set by the deployers when undeploying.
            if (undeployProps != null) {
                deploymentContext.getAppProps().putAll(undeployProps);
            }

            if (properties != null || property != null) {
                // if one of them is not null, let's merge them
                // to properties so we don't need to always
                // check for both
                if (properties == null) {
                    properties = new Properties();
                }
                if (property != null) {
                    properties.putAll(property);
                }
            }

            if (properties != null) {
                deploymentContext.getAppProps().putAll(properties);
                validateDeploymentProperties(properties, deploymentContext);
            }

            span.start(DeploymentTracing.AppStage.CLEANUP, "generated");
            // clean up any generated files
            deploymentContext.clean();

            span.start(DeploymentTracing.AppStage.PREPARE, "ServerConfig");
            Properties appProps = deploymentContext.getAppProps();
            /*
             * If the app's location is within the domain's directory then
             * express it in the config as ${com.sun.aas.instanceRootURI}/rest-of-path
             * so users can relocate the entire installation without having
             * to modify the app locations.  Leave the location alone if
             * it does not fall within the domain directory.
             */
            String appLocation = DeploymentUtils.relativizeWithinDomainIfPossible(deploymentContext.getSource().getURI());

            appProps.setProperty(ServerTags.LOCATION, appLocation);
            // set to default "user", deployers can override it
            // during processing
            appProps.setProperty(ServerTags.OBJECT_TYPE, "user");
            if (contextroot != null) {
                appProps.setProperty(ServerTags.CONTEXT_ROOT, contextroot);
            }
            appProps.setProperty(ServerTags.DIRECTORY_DEPLOYED, String.valueOf(isDirectoryDeployed));
            if (type == null) {
                type = archiveHandler.getArchiveType();
            }
            appProps.setProperty(Application.ARCHIVE_TYPE_PROP_NAME, type);
            if (useWarLibs != null) {
                appProps.setProperty(DeploymentProperties.WARLIBS, useWarLibs.toString());
            }
            savedAppConfig.store(appProps);

            deploymentContext.addTransientAppMetaData(DeploymentProperties.PREVIOUS_TARGETS, previousTargets);
            deploymentContext.addTransientAppMetaData(DeploymentProperties.PREVIOUS_VIRTUAL_SERVERS, previousVirtualServers);
            deploymentContext.addTransientAppMetaData(DeploymentProperties.PREVIOUS_ENABLED_ATTRIBUTES, previousEnabledAttributes);
            Transaction tx = deployment.prepareAppConfigChanges(deploymentContext);
            span.finish(); // next phase is launched by prepare
            Deployment.ApplicationDeployment deplResult = deployment.prepare(null, deploymentContext);
            if (deplResult != null && !loadOnly) {
                appState.ifPresent(s -> s.storeMetaData(deploymentContext));
                // initialize makes its own phase as well
                deployment.initialize(deplResult.appInfo, deplResult.appInfo.getSniffers(), deplResult.context);
            }
            ApplicationInfo appInfo = deplResult != null ? deplResult.appInfo : null;

            /*
             * Various deployers might have added to the downloadable or
             * generated artifacts.  Extract them and, if the command succeeded,
             * persist both into the app properties (which will be recorded
             * in domain.xml).
             */
            final Artifacts downloadableArtifacts
                    = DeploymentUtils.downloadableArtifacts(deploymentContext);
            final Artifacts generatedArtifacts
                    = DeploymentUtils.generatedArtifacts(deploymentContext);

            if (report.getActionExitCode() == ActionReport.ExitCode.SUCCESS) {
                try (SpanSequence innerSpan = span.start(DeploymentTracing.AppStage.REGISTRATION)){
                    moveAppFilesToPermanentLocation(
                            deploymentContext, logger);
                    recordFileLocations(appProps);

                    downloadableArtifacts.record(appProps);
                    generatedArtifacts.record(appProps);

                    timeTakenToDeploy = timing.elapsed();
                    deploymentTimeMillis = System.currentTimeMillis();
                    if (tx != null) {
                        Application application = deploymentContext.getTransientAppMetaData("application", Application.class);
                        // Set the application deploy time
                        application.setDeploymentTime(Long.toString(timeTakenToDeploy));
                        application.setTimeDeployed(Long.toString(deploymentTimeMillis));

                        // register application information in domain.xml
                        deployment.registerAppInDomainXML(appInfo, deploymentContext, tx);

                    }
                    if (retrieve != null) {
                        retrieveArtifacts(context, downloadableArtifacts.getArtifacts(), retrieve, false, name);
                    }
                    suppInfo.setDeploymentContext(deploymentContext);
                    // send new event to notify the deployment process is finish
                    events.send(new Event<ApplicationInfo>(Deployment.DEPLOYMENT_COMMAND_FINISH, appInfo), false);

                    //Fix for issue 14442
                    //We want to report the worst subreport value.
                    ActionReport.ExitCode worstExitCode = ExitCode.SUCCESS;
                    for (ActionReport subReport : report.getSubActionsReport()) {
                        ActionReport.ExitCode actionExitCode = subReport.getActionExitCode();

                        if (actionExitCode.isWorse(worstExitCode)) {
                            worstExitCode = actionExitCode;
                        }
                    }
                    report.setActionExitCode(worstExitCode);
                    report.setResultType(String.class, name);

                } catch (Exception e) {
                    // roll back the deployment and re-throw the exception
                    deployment.undeploy(name, deploymentContext);
                    deploymentContext.clean();
                    throw e;
                }
            }
        } catch (Throwable e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            if(e.getMessage() != null) {
                report.setMessage(e.getMessage());
            }
        } finally {
            events.unregister(this);
            try {
                archive.close();
            } catch (IOException e) {
                logger.log(Level.FINE, localStrings.getLocalString(
                        "errClosingArtifact",
                        "Error while closing deployable artifact : ",
                        path.getAbsolutePath()), e);
            }

            if (structuredTracing.isEnabled()) {
                structuredTracing.print(System.out);
            }

            if (report.getActionExitCode().equals(ActionReport.ExitCode.SUCCESS)) {
                // Set the app name in the result so that embedded deployer can retrieve it.
                report.setResultType(String.class, name);
                report.setMessage(localStrings.getLocalString("deploy.command.success", "Application deployed with name {0}", name));

                logger.info(localStrings.getLocalString(
                        "deploy.done",
                        "Deployment of {0} done is {1} ms at {2}",
                        name,
                        timeTakenToDeploy, DateFormat.getDateInstance().format(new Date(deploymentTimeMillis))));
            } else if (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                String errorMessage = report.getMessage();
                Throwable cause = report.getFailureCause();
                if (cause != null) {
                    String causeMessage = cause.getMessage();
                    if (causeMessage != null
                            && !causeMessage.equals(errorMessage)) {
                        errorMessage = errorMessage + " : " + cause.getMessage();
                    }
                    logger.log(Level.SEVERE, errorMessage, cause.getCause());
                }
                report.setMessage(localStrings.getLocalString("deploy.errDuringDepl", "Error occur during deployment: {0}.", errorMessage));
                // reset the failure cause so command framework will not try
                // to print the same message again
                report.setFailureCause(null);
                if (expansionDir != null) {
                    final FileArchive arch;
                    try {
                        /*
                         * Open and then delete the expansion directory as
                         * a file archive so stale file handling can run.
                         */
                        arch = DeploymentUtils.openAsFileArchive(expansionDir, archiveFactory);
                        arch.delete();
                    } catch (IOException ex) {
                        final String msg = localStrings.getLocalString(
                                "deploy.errDelRepos",
                                "Error deleting repository directory {0}",
                                expansionDir.getAbsolutePath());
                        report.failure(logger, msg, ex);
                    }

                }
                appState.map(ApplicationState::getPath)
                        .ifPresent(hotDeployService::removeApplicationState);
            }
            if (deploymentContext != null && !loadOnly) {
                deploymentContext.postDeployClean(true);
            }
            appState.ifPresent(ApplicationState::close);
        }
    }

    /**
     * Makes safe copies of the archive, deployment plan, alternate dd, runtime
     * alternate dd for later use during instance sync activity.
     * <p>
     * We rename any uploaded files from the temp directory to the permanent
     * place, and we copy any archive files that were not uploaded. This
     * prevents any confusion that could result if the developer modified the
     * archive file - changing its lastModified value - before redeploying it.
     *
     * @param deploymentContext
     * @param logger logger
     * @throws IOException
     */
    private void moveAppFilesToPermanentLocation(
            final ExtendedDeploymentContext deploymentContext,
            final Logger logger) throws IOException {
        final File finalUploadDir = deploymentContext.getAppInternalDir();
        final File finalAltDDDir = deploymentContext.getAppAltDDDir();

        if (!finalUploadDir.mkdirs()) {
            logger.log(Level.FINE, " Attempting to create upload directory {0} was reported as failed; attempting to continue",
                    new Object[]{finalUploadDir.getAbsolutePath()});
        }

        // PAYAYRA-444 GLASSFISH-21371
        if (!finalAltDDDir.mkdirs()) {
            logger.log(Level.FINE, " Attempting to create altdd directory {0} was reported as failed; attempting to continue",
                    new Object[]{finalAltDDDir.getAbsolutePath()});
        }

        safeCopyOfApp = DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(finalUploadDir, originalPathValue, logger, env);
        safeCopyOfDeploymentPlan = DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(finalUploadDir, deploymentplan, logger, env);
        safeCopyOfAltDD = DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(finalAltDDDir, altdd, logger, env);
        safeCopyOfRuntimeAltDD = DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(finalAltDDDir, runtimealtdd, logger, env);
    }

    private void recordFileLocations(
            final Properties appProps) throws URISyntaxException {
        /*
         * Setting the properties in the appProps now will cause them to be
         * stored in the domain.xml elements along with other properties when
         * the entire config is saved.
         */
        if (safeCopyOfApp != null) {
            appProps.setProperty(Application.APP_LOCATION_PROP_NAME,
                    DeploymentUtils.relativizeWithinDomainIfPossible(
                            safeCopyOfApp.toURI()));
        }
        if (safeCopyOfDeploymentPlan != null) {
            appProps.setProperty(Application.DEPLOYMENT_PLAN_LOCATION_PROP_NAME,
                    DeploymentUtils.relativizeWithinDomainIfPossible(
                            safeCopyOfDeploymentPlan.toURI()));
        }
        if (safeCopyOfAltDD != null) {
            appProps.setProperty(Application.ALT_DD_LOCATION_PROP_NAME,
                    DeploymentUtils.relativizeWithinDomainIfPossible(
                            safeCopyOfAltDD.toURI()));
        }
        if (safeCopyOfRuntimeAltDD != null) {
            appProps.setProperty(
                    Application.RUNTIME_ALT_DD_LOCATION_PROP_NAME,
                    DeploymentUtils.relativizeWithinDomainIfPossible(
                            safeCopyOfRuntimeAltDD.toURI()));
        }
    }

    /**
     * Check if the application is deployed or not. If force option is true and
     * appInfo is not null, then undeploy the application and return false. This
     * will force deployment if there's already a running application deployed.
     *
     * @param name application name
     * @param report ActionReport, report object to send back to client.
     * @return context properties that might have been set by the deployers
     * while undeploying the application
     *
     */
    private Properties handleRedeploy(final String name, final ActionReport report, final AdminCommandContext context)
            throws Exception {
        if (isredeploy) {
            //preserve settings first before undeploy
            Application app = apps.getModule(Application.class, name);

            // we save some of the old registration information in our deployment parameters
            settingsFromDomainXML(app);

            //if application is already deployed and force=true,
            //then undeploy the application first.
            // Use ParameterMap till we have a better way
            // to invoke a command on both DAS and instance with the
            // replication framework
            final ParameterMap parameters = new ParameterMap();
            parameters.add("DEFAULT", name);
            parameters.add(DeploymentProperties.TARGET, target);
            parameters.add(DeploymentProperties.KEEP_REPOSITORY_DIRECTORY, keepreposdir.toString());
            parameters.add(DeploymentProperties.IS_REDEPLOY, isredeploy.toString());
            if (dropandcreatetables != null) {
                parameters.add(DeploymentProperties.DROP_TABLES, dropandcreatetables.toString());
            }
            parameters.add(DeploymentProperties.IGNORE_CASCADE, force.toString());
            if (keepstate != null) {
                parameters.add(DeploymentProperties.KEEP_STATE, keepstate.toString());
            }

            ActionReport subReport = report.addSubActionsReport();
            subReport.setExtraProperties(new Properties());

            List<String> propertyNames = new ArrayList<String>();
            propertyNames.add(DeploymentProperties.KEEP_SESSIONS);
            propertyNames.add(DeploymentProperties.PRESERVE_APP_SCOPED_RESOURCES);
            populatePropertiesToParameterMap(parameters, propertyNames);

            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("undeploy", subReport,
                    context.getSubject());

            inv.parameters(parameters).execute();
            return subReport.getExtraProperties();
        }
        return null;
    }

    private void populatePropertiesToParameterMap(ParameterMap parameters, List<String> propertyNamesList) {

        Properties props = new Properties();
        if (properties != null) {
            for (String propertyName : propertyNamesList) {
                if (properties.containsKey(propertyName)) {
                    props.put(propertyName, properties.getProperty(propertyName));
                }
            }
        }
        parameters.add("properties", DeploymentUtils.propertiesValue(props, ':'));
    }

    /**
     * Places into the outgoing payload the downloadable artifacts for an
     * application.
     *
     * @param context the admin command context for the command requesting the
     * artifacts download
     * @param app the application of interest
     * @param targetLocalDir the client-specified local directory to receive the
     * downloaded files
     */
    public static void retrieveArtifacts(final AdminCommandContext context,
            final Application app,
            final String targetLocalDir) {
        retrieveArtifacts(context, app, targetLocalDir, true);
    }

    /**
     * Places into the outgoing payload the downloadable artifacts for an
     * application.
     *
     * @param context the admin command context for the command currently
     * running
     * @param app the application of interest
     * @param targetLocalDir the client-specified local directory to receive the
     * downloaded files
     * @param reportErrorsInTopReport whether to include error indications in
     * the report's top-level
     */
    public static void retrieveArtifacts(final AdminCommandContext context,
            final Application app,
            final String targetLocalDir,
            final boolean reportErrorsInTopReport) {
        retrieveArtifacts(context,
                DeploymentUtils.downloadableArtifacts(app).getArtifacts(),
                targetLocalDir,
                reportErrorsInTopReport,
                app.getName());
    }

    private static void retrieveArtifacts(final AdminCommandContext context,
            final Collection<Artifacts.FullAndPartURIs> artifactInfo,
            final String targetLocalDir,
            final boolean reportErrorsInTopReport,
            final String appname) {

        if (artifactInfo.isEmpty()) {
            final ActionReport report = context.getActionReport();
            final ActionReport subReport = report.addSubActionsReport();
            subReport.setMessage(localStrings.getLocalString(
                    DeployCommand.class,
                    "get-client-stubs.noStubApp",
                    "There are no files to retrieve for application {0}",
                    new Object[]{appname}));
            subReport.setActionExitCode(ExitCode.SUCCESS);
            return;
        }

        Logger logger = context.getLogger();
        FileOutputStream targetStream = null;
        try {
            Payload.Outbound outboundPayload = context.getOutboundPayload();
            // GLASSFISH-17554: pass to DownloadServlet
            boolean retrieveArtifacts = false;
            if (outboundPayload == null) {
                outboundPayload = PayloadImpl.Outbound.newInstance();
                retrieveArtifacts = true;
            }

            Properties props = new Properties();
            /*
             * file-xfer-root is used as a URI, so convert backslashes.
             */
            props.setProperty("file-xfer-root", targetLocalDir.replace('\\', '/'));
            for (Artifacts.FullAndPartURIs uriPair : artifactInfo) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "About to download artifact {0}", uriPair.getFull());
                }
                outboundPayload.attachFile("application/octet-stream",
                        uriPair.getPart(), "files", props,
                        new File(uriPair.getFull().getSchemeSpecificPart()));
            }
            if (retrieveArtifacts) {
                File targetLocalFile = new File(targetLocalDir); // CAUTION: file instead of dir
                if (targetLocalFile.exists()) {
                    final String msg = localStrings.getLocalString("download.errFileExists",
                            "Unable to generate files. File [{0}] already exists.", targetLocalFile.getAbsolutePath());
                    throw new Exception(msg);
                }

                if (!targetLocalFile.getParentFile().exists()) {
                    final String msg = localStrings.getLocalString("download.errParentFileMissing",
                            "Unable to generate files. Directory [{0}] does not exist.", targetLocalFile.getParent());
                    throw new Exception(msg);
                }
                targetStream = new FileOutputStream(targetLocalFile);
                outboundPayload.writeTo(targetStream);
                targetStream.flush();
            }
        } catch (Exception e) {
            handleRetrieveException(e, context, reportErrorsInTopReport);
        } finally {
            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException ex) {
                    handleRetrieveException(ex, context, reportErrorsInTopReport);
                }
            }
        }
    }

    private static void handleRetrieveException(final Exception e,
            final AdminCommandContext context, final boolean reportErrorsInTopReport) {
        final String errorMsg = localStrings.getLocalString(
                "download.errDownloading", "Error while downloading generated files");
        final Logger logger = context.getLogger();
        logger.log(Level.SEVERE, errorMsg, e);
        ActionReport report = context.getActionReport();
        if (!reportErrorsInTopReport) {
            report = report.addSubActionsReport();
            report.setActionExitCode(ExitCode.WARNING);
        } else {
            report.setActionExitCode(ExitCode.FAILURE);
        }
        report.setMessage(errorMsg);
        report.setFailureCause(e);
    }

    /**
     * Get settings from domain.xml and preserve the values. This is a private
     * api and its invoked when --force=true and if the app is registered.
     *
     * @param app is the registration information about the previously deployed
     * application
     *
     */
    private void settingsFromDomainXML(Application app) {
        //if name is null then cannot get the application's setting from domain.xml
        if (name != null) {
            if (contextroot == null) {
                if (app.getContextRoot() != null) {
                    this.previousContextRoot = app.getContextRoot();
                }
            }
            if (libraries == null) {
                libraries = app.getLibraries();
            }

            previousTargets = domain.getAllReferencedTargetsForApplication(name);
            if (virtualservers == null) {
                if (DeploymentUtils.isDomainTarget(target)) {
                    for (String tgt : previousTargets) {
                        String vs = domain.getVirtualServersForApplication(tgt, name);
                        if (vs != null) {
                            previousVirtualServers.put(tgt, vs);
                        }
                    }
                } else {
                    virtualservers = domain.getVirtualServersForApplication(
                            target, name);
                }
            }

            if (enabled == null) {
                if (DeploymentUtils.isDomainTarget(target)) {
                    // save the enable attributes of the application-ref
                    for (String tgt : previousTargets) {
                        previousEnabledAttributes.put(tgt, domain.getEnabledForApplication(tgt, name));
                    }
                    // save the enable attribute of the application
                    previousEnabledAttributes.put(DeploymentUtils.DOMAIN_TARGET_NAME, app.getEnabled());
                    // set the enable command param for DAS
                    enabled = deployment.isAppEnabled(app);
                } else {
                    enabled = Boolean.valueOf(domain.getEnabledForApplication(
                            target, name));
                }
            }

            String compatProp = app.getDeployProperties().getProperty(
                    DeploymentProperties.COMPATIBILITY);
            if (compatProp != null) {
                if (properties == null) {
                    properties = new Properties();
                }
                // if user does not specify the compatibility flag
                // explictly in this deployment, set it to the old value
                if (properties.getProperty(DeploymentProperties.COMPATIBILITY) == null) {
                    properties.setProperty(DeploymentProperties.COMPATIBILITY, compatProp);
                }
            }

        }
    }

    @Override
    public void event(Event event) {
        if (event.is(Deployment.DEPLOYMENT_BEFORE_CLASSLOADER_CREATION)) {
            // this is where we have processed metadata and
            // haven't created the application classloader yet
            DeploymentContext context = (DeploymentContext) event.hook();
            if (verify) {
                Verifier verifier = habitat.getService(Verifier.class);
                if (verifier != null) {
                    verifier.verify(context);
                } else {
                    context.getLogger().warning("Verifier is not installed yet. Install verifier module.");
                }
            }
        }
    }

    private void validateDeploymentProperties(Properties properties,
            DeploymentContext context) {
        String compatProp = properties.getProperty(
                DeploymentProperties.COMPATIBILITY);
        if (compatProp != null && !compatProp.equals("v2")) {
            // this only allowed value for property compatibility is v2
            String warningMsg = localStrings.getLocalString("compat.value.not.supported", "{0} is not a supported value for compatibility property.", compatProp);
            ActionReport subReport = context.getActionReport().addSubActionsReport();
            subReport.setActionExitCode(ActionReport.ExitCode.WARNING);
            subReport.setMessage(warningMsg);
            context.getLogger().log(Level.WARNING, warningMsg);
        }
    }

    /**
     * Crude interception mechanisms for deploy comamnd execution
     */
    @Contract
    public interface Interceptor {

        /**
         * Called by the deployment command to intercept a deployment activity.
         *
         * @param self the deployment command in flight.
         * @param context of the deployment
         */
        void intercept(DeployCommand self, DeploymentContext context);
    }
}
