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
// Portions Copyright [2019] Payara Foundation and/or affiliates
package org.glassfish.deployment.client;

import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

import org.glassfish.api.admin.CommandException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployapi.ProgressObjectImpl;
import org.glassfish.deployapi.TargetImpl;
import org.glassfish.deployapi.TargetModuleIDImpl;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Provides common behavior for the local and remote deployment facilities.
 * <p>
 * Code that needs an instance of a remote deployment facility use the
 * {@link DeploymentFacilityFactory}.
 * <p>
 *
 * @author tjquinn
 * @author David Matejcek
 */
public abstract class AbstractDeploymentFacility implements DeploymentFacility, TargetOwner {

    private static final String TARGET_DOMAIN = "domain";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AbstractDeploymentFacility.class);

    private static final String LIST_SUB_COMPONENTS_COMMAND = "list-sub-components";
    private static final String GET_CLIENT_STUBS_COMMAND = "get-client-stubs";
    private static final String GET_COMMAND = "get";

    private boolean connected;
    private TargetImpl domain;
    private ServerConnectionIdentifier targetDAS;
    private final Map<String, String> targetModuleWebURLs = new HashMap<>();

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.deployment.LogMessages";

    // Reserve this range [AS-DEPLOYMENT-04001, AS-DEPLOYMENT-06000]
    // for message ids used in this deployment dol module
    @LoggerInfo(subsystem = "DEPLOYMENT", description = "Deployment logger for client module", publish = true)
    private static final String DEPLOYMENT_LOGGER = "javax.enterprise.system.tools.deployment.client";

    /** Shared deployment logger: {@value #DEPLOYMENT_LOGGER} */
    public static final Logger deplLogger = Logger.getLogger(DEPLOYMENT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(message = "Error in deleting file {0}", level="WARNING")
    private static final String FILE_DELETION_ERROR = "AS-DEPLOYMENT-04017";

    /**
     * Defines behavior implemented in the local or remote deployment facility
     * for actually executing the requested command.
     */
    public interface DFCommandRunner {

        /**
         * Runs the command.
         *
         * @return the DF deployment status reflecting the outcome of the operation
         * @throws CommandException
         */
        DFDeploymentStatus run() throws CommandException;
    }

    /**
     * Returns a command runner for the concrete implementation.
     *
     * @param commandName
     * @param commandOptions
     * @param operands
     * @return {@link DFCommandRunner}
     * @throws CommandException
     */
    protected abstract DFCommandRunner getDFCommandRunner(
            String commandName,
            DFDeploymentProperties commandOptions,
            String[] operands) throws CommandException;

    /**
     * Changes the state of an application.
     * <p>
     * Used for enable and disable.
     * @param targets targets on which the change should occur
     * @param moduleID name of the module affected
     * @param commandName enable or disable
     * @param action name enabling or disabling
     *
     * @return DFProgressObject the caller can use to monitor progress and query final status
     */
    protected DFProgressObject changeState(Target[] targets, String moduleID, String commandName, String action) {
        ensureConnected();

        targets = getTargetServers(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        if (commandName.equals("enable")) {
            po.setCommand(CommandType.START);
        } else if (commandName.equals("disable")) {
            po.setCommand(CommandType.STOP);
        }
        List<TargetModuleIDImpl> targetModuleIDList = new ArrayList<>();
        try {
            for (Target target : targets) {
                DFDeploymentProperties commandParams = new DFDeploymentProperties();
                commandParams.put(DFDeploymentProperties.TARGET, target.getName());
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, new String[]{moduleID});
                DFDeploymentStatus ds = commandRunner.run();
                DFDeploymentStatus mainStatus = ds.getMainStatus();
                String message = localStrings.getLocalString("enterprise.deployment.client.change_state",
                    "{0} of {1} in target {2}", action, moduleID, target.getName());
                if (!po.checkStatusAndAddStage((TargetImpl) target, message, mainStatus)) {
                    return po;
                }
                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target, moduleID);
                targetModuleIDList.add(targetModuleID);
            }
            TargetModuleIDImpl[] targetModuleIDs = new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = targetModuleIDList.toArray(targetModuleIDs);
            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.change_state_all",
                "{0} of application in all targets", action), (TargetImpl) targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            po.setupForAbnormalExit(
                localStrings.getLocalString("enterprise.deployment.client.state_change_failed",
                    "Attempt to change the state of the application {0} failed - {1}", moduleID, ioex.toString()),
                (TargetImpl) targets[0]);
            return po;
        }
    }

    /**
     * Performs any local- or remote-specific work related to connecting to the DAS.
     * @return true if the connection was made successfully; false otherwise
     */
    protected abstract boolean doConnect();

    /**
     * Connects the deployment facility to the DAS.
     *
     * @param targetDAS the DAS to contact
     * @return true if the connection was made successfully; false otherwise
     */
    @Override
    public boolean connect(ServerConnectionIdentifier targetDAS) {
        connected = true;
        this.targetDAS = targetDAS;
        domain = new TargetImpl(this, TARGET_DOMAIN, localStrings.getLocalString(
                "enterprise.deployment.client.administrative_domain",
                "administrative-domain"));
        return doConnect();
    }

    /**
     * Performs any local- or remote-specific work to end the connection to the DAS.
     *
     * @return true if the disconnection succeeded; false otherwise
     */
    protected abstract boolean doDisconnect();

    /**
     * Disconnects the deployment facility from the DAS.
     *
     * @return true if the disconnection was successful; false otherwise
     */
    @Override
    public boolean disconnect() {
        connected = false;
        domain = null;
        targetDAS = null;
        return doDisconnect();
    }

    @Override
    public DFProgressObject createAppRef(Target[] targets, String moduleID, Map options) {
        return changeAppRef(targets, moduleID, "create-application-ref", "Creation", options);
    }

    @Override
    public DFProgressObject deleteAppRef(Target[] targets, String moduleID, Map options) {
        return changeAppRef(targets, moduleID, "delete-application-ref", "Removal", options);
    }


    private DFProgressObject changeAppRef(Target[] targets, String moduleID, String commandName, String action,
        Map origOptions) {
        ensureConnected();
        targets = getTargetServers(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        List<TargetModuleIDImpl> targetModuleIDList = new ArrayList<>();
        try {
            final DFDeploymentProperties options = new DFDeploymentProperties();
            options.putAll(origOptions);
            for (Target target : targets) {
                options.put(DFDeploymentProperties.TARGET, target.getName());
                String[] operands = new String[] {moduleID};
                DFDeploymentStatus mainStatus = null;
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, options, operands);
                DFDeploymentStatus ds = commandRunner.run();
                mainStatus = ds.getMainStatus();
                String message = localStrings.getLocalString("enterprise.deployment.client.create_reference",
                    "Creation of reference for application in target {0}", target.getName());
                if (!po.checkStatusAndAddStage((TargetImpl) target, message, mainStatus)) {
                    return po;
                }
                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target, moduleID);
                targetModuleIDList.add(targetModuleID);
            }
            TargetModuleIDImpl[] targetModuleIDs = new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = targetModuleIDList.toArray(targetModuleIDs);
            String message = localStrings.getLocalString("enterprise.deployment.client.change_reference_application",
                "{0} of application reference in all targets", action);
            po.setupForNormalExit(message, (TargetImpl) targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            String message = localStrings.getLocalString(
                "enterprise.deployment.client.change_reference_application_failed",
                "{0} of application reference failed - {1}", action, ioex.getMessage());
            po.setupForAbnormalExit(message, (TargetImpl) targets[0]);
            return po;
        }
    }

    @Override
    public Target createTarget(String name) {
        return new TargetImpl(this, name, "");
    }

    @Override
    public Target[] createTargets(String[] targets) {
        if (targets == null) {
            targets = new String[0];
        }
        TargetImpl[] result = new TargetImpl[targets.length];
        int i = 0;
        for (String name : targets) {
            result[i++] = new TargetImpl(this, name, "");
        }
        return result;
    }

    private String createTargetsParam(Target[] targets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targets.length; i++) {
            sb.append(targets[i].getName());
            if (i != targets.length-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public DFProgressObject deploy(Target[] targets, ReadableArchive source, ReadableArchive deploymentPlan,
        Map deploymentOptions) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException();
        }
        File tempSourceFile = null;
        File tempPlanFile = null;
        if (source instanceof MemoryMappedArchive) {
            try {
                String extension = (String) deploymentOptions.remove(DFDeploymentProperties.MODULE_EXTENSION);
                tempSourceFile = writeMemoryMappedArchiveToTempFile((MemoryMappedArchive)source, extension);
                URI tempPlanURI = null;
                if (deploymentPlan != null && deploymentPlan instanceof MemoryMappedArchive) {
                    tempPlanFile = writeMemoryMappedArchiveToTempFile((MemoryMappedArchive)deploymentPlan, ".jar");
                    tempPlanURI = tempPlanFile.toURI();
                }

                return deploy(targets, tempSourceFile.toURI(), tempPlanURI, deploymentOptions);
            } finally {
                if (tempSourceFile != null) {
                    boolean isDeleted = tempSourceFile.delete();
                    if (!isDeleted) {
                        deplLogger.log(Level.WARNING, FILE_DELETION_ERROR, tempSourceFile.getAbsolutePath());
                    }
                }
                if (tempPlanFile != null) {
                    boolean isDeleted = tempPlanFile.delete();
                    if (!isDeleted) {
                        deplLogger.log(Level.WARNING, FILE_DELETION_ERROR, tempPlanFile.getAbsolutePath());
                    }
                }
            }
        } else {
            if (deploymentPlan == null) {
                return deploy(targets, source.getURI(), null, deploymentOptions);
            } else {
                return deploy(targets, source.getURI(), deploymentPlan.getURI(), deploymentOptions);
            }
        }
    }

    private File writeMemoryMappedArchiveToTempFile(MemoryMappedArchive mma, String fileSuffix) throws IOException {
        final File tempFile = File.createTempFile("jsr88-", fileSuffix);
        try (FileOutputStream bos = new FileOutputStream(tempFile)) {
            bos.write(mma.getByteArray());
        }
        return tempFile;
    }

    @Override
    public DFProgressObject deploy(final Target[] targets, final URI source, final URI deploymentPlan,
        final Map origOptions) {
        deplLogger.fine(() -> String.format("deploy(targets=%s, source=%s, deploymentPlan=%s, origOptions=%s)",
            targets, source, deploymentPlan, origOptions));

        ensureConnected();
        final Target[] targetServers = getTargetServers(targets);
        final ProgressObjectImpl po = new ProgressObjectImpl(targetServers);
        final DFDeploymentProperties deploymentOptions = new DFDeploymentProperties();
        deploymentOptions.putAll(origOptions);
        // target for deployment is always domain
        // references to targets are created after successful deployment
        deploymentOptions.put(DFDeploymentProperties.TARGET, TARGET_DOMAIN);
        final boolean isRedeploy = deploymentOptions.getRedeploy();
        deplLogger.finest(() -> "isRedeploy=" + isRedeploy);
        // redeploy is not supported command argument, must be removed, because
        // we use this instance directly
        deploymentOptions.remove(DFDeploymentProperties.REDEPLOY);
        if (isRedeploy) {
            po.setCommand(CommandType.REDEPLOY);
        } else {
            po.setCommand(CommandType.DISTRIBUTE);
        }
        List<TargetModuleIDImpl> targetModuleIDList = new ArrayList<>();

        // Make sure the file permission is correct when deploying a file
        if (source == null) {
            String msg = localStrings.getLocalString("enterprise.deployment.client.archive_not_specified",
                "Archive to be deployed is not specified at all.");
            po.setupForAbnormalExit(msg, domain);
            return po;
        }
        File tmpFile = new File(source.getSchemeSpecificPart());
        if (!tmpFile.exists()) {
            String msg = localStrings.getLocalString("enterprise.deployment.client.archive_not_in_location",
                "Unable to find the archive to be deployed in specified location.");
            po.setupForAbnormalExit(msg, domain);
            return po;
        }
        if (!tmpFile.canRead()) {
            String msg = localStrings.getLocalString("enterprise.deployment.client.archive_no_read_permission",
                "Archive to be deployed does not have read permission.");
            po.setupForAbnormalExit(msg, domain);
            return po;
        }
        try {
            if (deploymentPlan != null) {
                File dp = new File(deploymentPlan.getSchemeSpecificPart());
                if (!dp.exists()) {
                    String msg = localStrings.getLocalString("enterprise.deployment.client.plan_not_in_location",
                        "Unable to find the deployment plan in specified location.");
                    po.setupForAbnormalExit(msg, domain);
                    return po;
                }
                if (!dp.canRead()) {
                    String msg = localStrings.getLocalString("enterprise.deployment.client.plan_no_read_permission",
                        "Deployment plan does not have read permission.");
                    po.setupForAbnormalExit(msg, domain);
                    return po;
                }
                deploymentOptions.put(DFDeploymentProperties.DEPLOYMENT_PLAN, dp.getAbsolutePath());
            }

            if (isRedeploy) {
                String appName = (String) deploymentOptions.get(DFDeploymentProperties.NAME);
                if (!isTargetsMatched(appName, targetServers)) {
                    String msg = localStrings.getLocalString(
                        "enterprise.deployment.client.specifyAllTargets",
                        "Application {0} is already deployed on other targets. Please remove all references or specify"
                        + " all targets (or domain target if using asadmin command line) before attempting"
                        + " {1} operation",
                        appName, "redeploy");
                    po.setupForAbnormalExit(msg, domain);
                }
                String enabled = getAppRefEnabledAttr(TARGET_DOMAIN, appName);
                deploymentOptions.put(DFDeploymentProperties.ENABLED, enabled);
                deploymentOptions.put("isredeploy", "true");
                deploymentOptions.put("forcename", "true");
                deploymentOptions.setForce(true);
            }

            // note about the current relation of commands: redeploy uses deploy.
            final String command = isRedeploy ? "redeploy" : "deploy";
            DFCommandRunner commandRunner = getDFCommandRunner(command, deploymentOptions,
                new String[] {tmpFile.getAbsolutePath()});
            DFDeploymentStatus ds = commandRunner.run();
            DFDeploymentStatus mainStatus = ds.getMainStatus();
            String msg = localStrings.getLocalString("enterprise.deployment.client.deploy_to_first_target",
                "Deploying application to target {0}", targetServers[0].getName());
            if (!po.checkStatusAndAddStage((TargetImpl) targetServers[0], msg, mainStatus)) {
                return po;
            }
            String moduleID = mainStatus.getProperty(DFDeploymentProperties.NAME);
            deplLogger.finest("moduleID retrieved from mainStatus: " + moduleID);
            if (moduleID == null) {
                moduleID = (String) deploymentOptions.get(DFDeploymentProperties.NAME);
                deplLogger.finest("moduleID retrieved from deploymentOptions: " + moduleID);
            }
            po.setModuleID(moduleID);

            final DFDeploymentProperties createAppRefOptions = new DFDeploymentProperties();
            if (deploymentOptions.get(DFDeploymentProperties.ENABLED) != null) {
                createAppRefOptions.put(DFDeploymentProperties.ENABLED,
                    deploymentOptions.get(DFDeploymentProperties.ENABLED));
            }
            if (deploymentOptions.get(DFDeploymentProperties.VIRTUAL_SERVERS) != null) {
                createAppRefOptions.put(DFDeploymentProperties.VIRTUAL_SERVERS,
                    deploymentOptions.get(DFDeploymentProperties.VIRTUAL_SERVERS));
            }
            // then create application references to the rest of the targets
            for (Target target : targetServers) {
                createAppRefOptions.put(DFDeploymentProperties.TARGET, target.getName());
                DFCommandRunner commandRunner2 = getDFCommandRunner("create-application-ref", createAppRefOptions,
                    new String[] {moduleID});
                DFDeploymentStatus ds2 = commandRunner2.run();
                DFDeploymentStatus mainStatus2 = ds2.getMainStatus();
                if (!po.checkStatusAndAddStage((TargetImpl) target, "create app ref", mainStatus2)) {
                    deplLogger.finest("create-application-ref failed, mainStatus2: " + mainStatus2);
                    return po;
                }
            }

            // we use targetServers to populate the targetModuleIDList
            // so it takes care of the redeploy using domain target case too
            for (Target target : targetServers) {
                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target, moduleID);
                targetModuleIDList.add(targetModuleID);
            }

            TargetModuleIDImpl[] targetModuleIDs = new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = targetModuleIDList.toArray(targetModuleIDs);
            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.deploy_application",
                "Deployment of application {0}", moduleID), (TargetImpl) targetServers[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            String msg = localStrings.getLocalString("enterprise.deployment.client.deploy_application_failed",
                "Deployment of application failed - {0} ", ioex.toString());
            po.setupForAbnormalExit(msg, (TargetImpl) targetServers[0]);
            return po;
        }
    }

    /**
     * Disables an app on the specified targets.
     * @param targets the targets on which to disable the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
    @Override
    public DFProgressObject disable(Target[] targets, String moduleID) {
        return changeState(targets, moduleID, "disable", "Disable");
    }

    @Override
    public String downloadFile(File location, String moduleID, String moduleURI) throws IOException {
        throw new UnsupportedOperationException("Not supported in v3");
    }

    /**
     * Enables an app on the specified targets.
     * @param targets the targets on which to enable the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
    @Override
    public DFProgressObject enable(Target[] targets, String moduleID) {
        return changeState(targets, moduleID, "enable", "Enable");
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException(localStrings.getLocalString("enterprise.deployment.client.disconnected_state", "Not connected to the Domain Admin Server"));
        }
    }

    /**
     * Reports whether the deployment facility is connected.
     * @return true if connected, false otherwise
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<String> getSubModuleInfoForJ2EEApplication(String appName) throws IOException {
        ensureConnected();
        String commandName = LIST_SUB_COMPONENTS_COMMAND;
        String[] operands = new String[] { appName };
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<String> subModuleInfoList = new ArrayList<>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator<DFDeploymentStatus> subIter = mainStatus.getSubStages(); subIter.hasNext();) {
                    DFDeploymentStatus subStage = subIter.next();
                    if (subStage.getProperty(DeploymentProperties.MODULE_INFO) != null) {
                        subModuleInfoList.add(subStage.getProperty(DeploymentProperties.MODULE_INFO));
                    }
                }
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful. Because getContextRoot does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException("remote command execution failed on the server");
                commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
                throw commandExecutionException;
            }
            return subModuleInfoList;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    private String getAppRefEnabledAttr(String target, String moduleName) throws IOException {
        ensureConnected();
        String commandName = "show-component-status";
        DFDeploymentProperties commandParams = new DFDeploymentProperties();
        commandParams.put(DFDeploymentProperties.TARGET, target);
        String[] operands = new String[] { moduleName };
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            String enabledAttr = null;

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                Iterator<DFDeploymentStatus> subIter = mainStatus.getSubStages();
                if (subIter.hasNext()) {
                    DFDeploymentStatus subStage = subIter.next();
                    String result = subStage.getProperty(DeploymentProperties.STATE);
                    if (result.equals("enabled")) {
                        enabledAttr = "true";
                    } else {
                        enabledAttr = "false";
                    }
                }
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful. Because getContextRoot does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException("remote command execution failed on the server");
                commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
                throw commandExecutionException;
            }
            return enabledAttr;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public String getContextRoot(String moduleName) throws IOException {
        ensureConnected();
        String commandName = GET_COMMAND;
        String patternParam = "applications.application." + moduleName + ".context-root";
        String[] operands = new String[] {patternParam};
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            String contextRoot = null;

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                Iterator<DFDeploymentStatus> subIter = mainStatus.getSubStages();
                if (subIter.hasNext()) {
                    DFDeploymentStatus subStage = subIter.next();
                    String result = subStage.getStageStatusMessage();
                    contextRoot = getValueFromDottedNameGetResult(result);
                }
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because getContextRoot does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw commandExecutionException;
            }
            return contextRoot;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public ModuleType getModuleType(String moduleName) throws IOException {
        ensureConnected();
        String commandName = GET_COMMAND;
        String patternParam = "applications.application." + moduleName + ".*";
        String[] operands = new String[] { patternParam };
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<String> resultList = new ArrayList<>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator<DFDeploymentStatus> subIter = mainStatus.getSubStages(); subIter.hasNext();) {
                    DFDeploymentStatus subStage = subIter.next();
                    resultList.add(subStage.getStageStatusMessage());
                }
                return getJavaEEModuleTypeFromResult(resultList);
            }
            /*
             * We received a response from the server but the status was
             * reported as unsuccessful. Because get does not
             * return a ProgressObject which the caller could use to find
             * out about the success or failure, we must throw an exception
             * so the caller knows about the failure.
             */
            commandExecutionException = new IOException("remote command execution failed on the server");
            commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
            throw commandExecutionException;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public Target[] listTargets() throws IOException {
        return listReferencedTargets("*");
    }

    @Override
    public Target[] listReferencedTargets(String appName) throws IOException {
        ensureConnected();
        String commandName = "_get-targets";
        String[] operands = new String[] { appName };
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<Target> targets = new ArrayList<>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator<DFDeploymentStatus> subIter = mainStatus.getSubStages(); subIter.hasNext();) {
                    DFDeploymentStatus subStage = subIter.next();
                    String result = subStage.getStageStatusMessage();
                    targets.add(createTarget(result));
                }
                Target[] result = new Target[targets.size()];
                return targets.toArray(result);
            }
            /*
             * We received a response from the server but the status was
             * reported as unsuccessful. Because listTargets does not
             * return a ProgressObject which the caller could use to find
             * out about the success or failure, we must throw an exception
             * so the caller knows about the failure.
             */
            commandExecutionException = new IOException("remote command execution failed on the server");
            commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
            throw commandExecutionException;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public void getClientStubs(String location, String moduleID)
        throws IOException {
        ensureConnected();
        String commandName = GET_CLIENT_STUBS_COMMAND;
        DFDeploymentProperties commandParams = new DFDeploymentProperties();
        commandParams.put("appname", moduleID);
        String[] operands = new String[] { location };
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();

            if (mainStatus.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful. Because getClientStubs does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException("remote command execution failed on the server");
                commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
                throw commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public HostAndPort getHostAndPort(String target) throws IOException {
        return getHostAndPort(target, false);
    }

    @Override
    public HostAndPort getHostAndPort(String target, boolean securityEnabled) throws IOException {
        return getHostAndPort(target, null, securityEnabled);
    }


    @Override
    public HostAndPort getVirtualServerHostAndPort(String target, String virtualServer, boolean securityEnabled)
        throws IOException {
        return getHostAndPort(target, null, virtualServer, securityEnabled);
    }

    @Override
    public HostAndPort getHostAndPort(String target, String moduleId, boolean securityEnabled)
        throws IOException {
        return getHostAndPort(target, moduleId, null, securityEnabled);
    }

    private HostAndPort getHostAndPort(String target, String moduleId,
        String virtualServer, boolean securityEnabled) throws IOException {
        ensureConnected();
        String commandName = "_get-host-and-port";
        DFDeploymentProperties commandParams = new DFDeploymentProperties();
        commandParams.put(DFDeploymentProperties.TARGET, "server");
        if (moduleId != null) {
            commandParams.put("moduleId", moduleId);
        }
        if (virtualServer != null) {
            commandParams.put("virtualServer", virtualServer);
        }
        commandParams.put("securityEnabled", Boolean.valueOf(securityEnabled));
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, null);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();

            HostAndPort hap = null;
            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                String hostPortStr = mainStatus.getStageStatusMessage();
                if (hostPortStr != null && !hostPortStr.trim().equals("")) {
                    hap = new HostAndPort(hostPortStr);
                }
                return hap;
            }
            /*
             * We received a response from the server but the status was
             * reported as unsuccessful. Because getHostAndPort does not
             * return a ProgressObject which the caller could use to find
             * out about the success or failure, we must throw an exception
             * so the caller knows about the failure.
             */
            commandExecutionException = new IOException("remote command execution failed on the server");
            commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
            throw commandExecutionException;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    @Override
    public TargetModuleID[] listAppRefs(String[] targets) throws IOException {
        ensureConnected();
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<>();
        IOException commandExecutionException = null;
        try {
            Target[] targetImpls = getTargetServers(createTargets(targets));
            for (Target target : targetImpls) {
                String commandName = "list-application-refs";
                String[] operands = new String[] { target.getName() };
                DFDeploymentProperties commandParams = new DFDeploymentProperties();
                DFDeploymentStatus mainStatus = null;
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
                DFDeploymentStatus ds = commandRunner.run();
                mainStatus = ds.getMainStatus();

                if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                    for (Iterator<DFDeploymentStatus> appRefIter = mainStatus.getSubStages(); appRefIter.hasNext();) {
                        DFDeploymentStatus appRefSubStage = appRefIter.next();
                        String moduleID = appRefSubStage.getStageStatusMessage();
                        TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target, moduleID);
                        targetModuleIDList.add(targetModuleID);
                    }
                } else {
                    /*
                     * We received a response from the server but the status was
                     * reported as unsuccessful.  Because listAppRefs does not
                     * return a ProgressObject which the caller could use to find
                     * out about the success or failure, we must throw an exception
                     * so the caller knows about the failure.
                     */
                    commandExecutionException = new IOException("remote command execution failed on the server");
                    commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
                    throw commandExecutionException;
                }
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
        TargetModuleIDImpl[] result = new TargetModuleIDImpl[targetModuleIDList.size()];
        return targetModuleIDList.toArray(result);
    }

    @Override
    public TargetModuleID[] _listAppRefs(String[] targets) throws IOException {
        return _listAppRefs(targets, DFDeploymentProperties.ALL);
    }

    @Override
    public TargetModuleID[] _listAppRefs(String[] targets, String state) throws IOException {
        return _listAppRefs(targets, state, null);
    }

    @Override
    public TargetModuleID[] _listAppRefs(String[] targets, String state, String type) throws IOException {
        Target[] targetImpls = getTargetServers(createTargets(targets));
        return _listAppRefs(targetImpls, state, type);
    }

    @Override
    public TargetModuleID[] _listAppRefs(Target[] targets, String state, String type) throws IOException {
        ensureConnected();
        String commandName = "_list-app-refs";
        String targetsParam = createTargetsParam(targets);
        DFDeploymentProperties commandParams = new DFDeploymentProperties();
        commandParams.put(DFDeploymentProperties.TARGET, targetsParam);
        commandParams.put("state", state);
        if (type != null) {
            commandParams.put("type", type);
        }
        DFDeploymentStatus mainStatus = null;
        IOException commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, null);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<TargetModuleIDImpl> targetModuleIDList = new ArrayList<>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                /*
                 * There will be one substage for each target. And within each
                 * of those will be a substage for each module assigned to
                 * that target
                 */
                String targetName = mainStatus.getStageStatusMessage();
                /*
                 * Look for the caller-supplied target that matches this result.
                 */
                for (Target target: targets) {
                    if (target.getName().equals(targetName)) {
                        /*
                         * Each substage below the target substage is for
                         * a module deployed to that target.
                         */
                        for (Iterator<DFDeploymentStatus> appRefIter = mainStatus.getSubStages(); appRefIter.hasNext();) {
                            DFDeploymentStatus appRefSubStage = appRefIter.next();
                            String moduleID = appRefSubStage.getStageStatusMessage();
                            if (target instanceof TargetImpl) {
                                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target,
                                    moduleID);
                                targetModuleIDList.add(targetModuleID);
                            }
                        }
                    }
                }

                TargetModuleIDImpl[] result = new TargetModuleIDImpl[targetModuleIDList.size()];
                return targetModuleIDList.toArray(result);

            }
            /*
             * We received a response from the server but the status was
             * reported as unsuccessful.  Because listAppRefs does not
             * return a ProgressObject which the caller could use to find
             * out about the success or failure, we must throw an exception
             * so the caller knows about the failure.
             */
            commandExecutionException = new IOException("remote command execution failed on the server");
            commandExecutionException.initCause(new RuntimeException(mainStatus.getAllStageMessages()));
            throw commandExecutionException;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            }
            throw commandExecutionException;
        }
    }

    private Target[] getTargetServers(Target[] targets) {
        if (targets == null || targets.length == 0) {
            targets = new Target[] {targetForDefaultServer()};
        }
        return targets;
    }

    /**
     * Provides a {@link Target} object for the default target.
     *
     * @return Target for the default server
     */
    private Target targetForDefaultServer() {
        return new TargetImpl(this, DEFAULT_SERVER_NAME,
            localStrings.getLocalString("enterprise.deployment.client.default_server_description", "default server"));
    }

    /**
     * Undeploys an application from specified targets.
     * @param targets the targets from which to undeploy the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
    @Override
    public DFProgressObject undeploy(Target[] targets, String moduleID) {
        return undeploy(targets, moduleID, new DFDeploymentProperties());
    }

    /**
     * Undeploys an application from specified targets.
     *
     * @param origTargets the targets from which to undeploy the app
     * @param moduleID the app
     * @param origOptions options to control the undeployment
     * @return DFProgressObject for monitoring progress and querying status
     */
    @Override
    public DFProgressObject undeploy(final Target[] origTargets, final String moduleID, final Map origOptions) {
        deplLogger.fine(() -> String.format("undeploy(origTargets=%s, moduleID=%s, origOptions=%s)", //
            origTargets, moduleID, origOptions));
        ensureConnected();
        Target[] targets = getTargetServers(origTargets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        po.setCommand(CommandType.UNDEPLOY);
        final DFDeploymentProperties undeploymentOptions = new DFDeploymentProperties();
        undeploymentOptions.putAll(origOptions);
        List<TargetModuleIDImpl> targetModuleIDList = new ArrayList<>();
        try {
            if (!isTargetsMatched(moduleID, targets)) {
                String message = localStrings.getLocalString("enterprise.deployment.client.specifyAllTargets",
                    "Application {0} is already deployed on other targets. Please remove all references or specify"
                    + " all targets (or domain target if using asadmin command line) before attempting {1} operation",
                    moduleID, "undeploy");
                po.setupForAbnormalExit(message, domain);
            }

            // first remove the application references from targets
            DFDeploymentProperties deleteAppRefOptions = new DFDeploymentProperties();
            if (undeploymentOptions.get(DFDeploymentProperties.CASCADE) != null) {
                deleteAppRefOptions.put(DFDeploymentProperties.CASCADE,
                    undeploymentOptions.get(DFDeploymentProperties.CASCADE));
            }
            for (Target target : targets) {
                deleteAppRefOptions.put(DFDeploymentProperties.TARGET, target.getName());
                DFCommandRunner commandRunner = getDFCommandRunner("delete-application-ref", deleteAppRefOptions,
                    new String[] {moduleID});
                DFDeploymentStatus ds = commandRunner.run();
                DFDeploymentStatus mainStatus = ds.getMainStatus();
                String message = localStrings.getLocalString("enterprise.deployment.client.undeploy_remove_ref",
                    "While undeploying, trying to remove reference for application in target {0}", target.getName());
                if (!po.checkStatusAndAddStage((TargetImpl) target, message, mainStatus)) {
                    return po;
                }
                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl) target, moduleID);
                targetModuleIDList.add(targetModuleID);
            }

            // then undeploy from domain
            undeploymentOptions.put(DFDeploymentProperties.TARGET, TARGET_DOMAIN);
            DFCommandRunner commandRunner2 = getDFCommandRunner("undeploy", undeploymentOptions,
                new String[] {moduleID});
            DFDeploymentStatus ds2 = commandRunner2.run();
            DFDeploymentStatus mainStatus2 = ds2.getMainStatus();
            if (!po.checkStatusAndAddStage(domain,
                localStrings.getLocalString("enterprise.deployment.client.undeploy_from_target",
                    "Trying to undeploy application from target {0}", domain.getName()),
                mainStatus2)) {
                return po;
            }

            TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl(domain, moduleID);
            targetModuleIDList.add(targetModuleID);

            TargetModuleIDImpl[] targetModuleIDs = new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = targetModuleIDList.toArray(targetModuleIDs);

            String message = localStrings.getLocalString("enterprise.deployment.client.undeploy_application",
                "Undeployment of application {0}", moduleID);
            po.setupForNormalExit(message, (TargetImpl) targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            String msg = localStrings.getLocalString("enterprise.deployment.client.undeploy_application_failed",
                "Undeployment failed - {0}", ioex.toString());
            po.setupForAbnormalExit(msg, (TargetImpl) targets[0]);
            return po;
        }
    }

    /**
     *  Exports the Client stub jars to the given location.
     *  @param appName The name of the application or module.
     *  @param destDir The directory into which the stub jar file
     *  should be exported.
     *  @return the absolute location to the main jar file.
     */
    @Override
    public String exportClientStubs(String appName, String destDir)
        throws IOException {
        getClientStubs(destDir, appName);
        return (destDir + appName + "Client.jar");
    }

    /**
     * Convenient method to wait for the operation monitored by the progress
     * object to complete, returning the final operation status.
     * @param po DFProgressObject for the operation of interestt
     * @return DFDeploymentStatus final status for the operation
     */
    @Override
    public DFDeploymentStatus waitFor(DFProgressObject po) {
        return po.waitFor();
    }

    @Override
    public String getWebURL(TargetModuleID tmid) {
        return targetModuleWebURLs.get(tmid.getModuleID());
    }

    @Override
    public void setWebURL(TargetModuleID tmid, String webURL) {
        targetModuleWebURLs.put(tmid.getModuleID(), webURL);
    }

    protected ServerConnectionIdentifier getTargetDAS() {
        return targetDAS;
    }

    private String getValueFromDottedNameGetResult(String result) {
        if (result == null) {
            return null;
        }
        int index = result.lastIndexOf('=');
        return result.substring(index+1);
    }

    private ModuleType getJavaEEModuleTypeFromResult(List<String> resultList) {
        List<String> sniffersFound = new ArrayList<>();
        for (String result : resultList) {
            if (result.endsWith("property.isComposite=true")) {
                return ModuleType.EAR;
            } else if (result.endsWith("engine.web.sniffer=web")) {
                sniffersFound.add("web");
            } else if (result.endsWith("engine.ejb.sniffer=ejb")) {
                sniffersFound.add("ejb");
            } else if (result.endsWith("engine.connector.sniffer=connector")) {
                sniffersFound.add("rar");
            } else if (result.endsWith("engine.appclient.sniffer=appclient")) {
                sniffersFound.add("car");
            }
        }

        // if we are here, it's not ear
        // note, we check for web sniffer before ejb, as in ejb in war case
        // we will return war.
        if (sniffersFound.contains("web")) {
            return ModuleType.WAR;
        }
        if (sniffersFound.contains("ejb")) {
            return ModuleType.EJB;
        }
        if (sniffersFound.contains("rar")) {
            return ModuleType.RAR;
        }
        if (sniffersFound.contains("car")) {
            return ModuleType.CAR;
        }

        return null;
    }

    private boolean isTargetsMatched(String appName, Target[] targets)
        throws IOException {
        Target[] referencedTargets = listReferencedTargets(appName);
        if (targets.length != referencedTargets.length) {
            return false;
        }
        List<String> referencedTargetNames = new ArrayList<>();
        for (Target target : referencedTargets) {
            referencedTargetNames.add(target.getName());
        }
        for (Target target: targets) {
            referencedTargetNames.remove(target.getName());
        }
        if (referencedTargetNames.size() == 0) {
            return true;
        }
        return false;
    }
}
