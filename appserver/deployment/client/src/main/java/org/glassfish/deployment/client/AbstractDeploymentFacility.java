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

package org.glassfish.deployment.client;

import org.glassfish.api.admin.CommandException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import org.glassfish.deployapi.ProgressObjectImpl;
import org.glassfish.deployapi.TargetImpl;
import org.glassfish.deployapi.TargetModuleIDImpl;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentProperties;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 * Provides common behavior for the local and remote deployment facilities.
 * <p>
 * Code that needs an instance of a remote deployment facility use the
 * {@link DeploymentFacilityFactory}.
 * <p>
 *
 * @author tjquinn
 */
public abstract class AbstractDeploymentFacility implements DeploymentFacility, TargetOwner {
    private static final String DEFAULT_SERVER_NAME = "server";
    protected static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AbstractDeploymentFacility.class);

    private static final String LIST_COMMAND = "list";
    private static final String LIST_SUB_COMPONENTS_COMMAND = "list-sub-components";
    private static final String GET_CLIENT_STUBS_COMMAND = "get-client-stubs";
    private static final String GET_COMMAND = "get";

    private boolean connected;
    private TargetImpl domain;
    private ServerConnectionIdentifier targetDAS;
    private Map<String, String> targetModuleWebURLs = 
        new HashMap<String, String>();

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.deployment.LogMessages";

    // Reserve this range [AS-DEPLOYMENT-04001, AS-DEPLOYMENT-06000]
    // for message ids used in this deployment dol module
    @LoggerInfo(subsystem = "DEPLOYMENT", description="Deployment logger for client module", publish=true)
    private static final String DEPLOYMENT_LOGGER = "javax.enterprise.system.tools.deployment.client";

    public static final Logger deplLogger =
        Logger.getLogger(DEPLOYMENT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

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
         * @throws com.sun.enterprise.cli.framework.CommandException
         */
        DFDeploymentStatus run() throws CommandException;
    }

    /**
     * Returns a command runner for the concrete implementation.
     * 
     * @param commandName
     * @param commandOptions
     * @param operands
     * @return
     * @throws com.sun.enterprise.cli.framework.CommandException
     */
    protected abstract DFCommandRunner getDFCommandRunner(
            String commandName,
            Map<String,Object> commandOptions,
            String[] operands) throws CommandException;

    /**
     * Changes the state of an application.
     * <p>
     * Used for enable and disable.
     * @param targets targets on which the change should occur
     * @param moduleID name of the module affected
     * @param commandName enable or disable
     * @param action name enabling or disabling
     * @return DFProgressObject the caller can use to monitor progress and query final status
     */
    protected DFProgressObject changeState(Target[] targets, String moduleID, String commandName, String action) {
        ensureConnected();
        targets = prepareTargets(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
	if(commandName.equals("enable")) {
            po.setCommand(CommandType.START, null);
        } else if(commandName.equals("disable")) {
            po.setCommand(CommandType.STOP, null);
        }
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<TargetModuleIDImpl>();
        try {
            for (Target target : targets) {
                Map commandParams = new HashMap();
                commandParams.put(DFDeploymentProperties.TARGET, target.getName());
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, new String[]{moduleID});
                DFDeploymentStatus ds = commandRunner.run();
                DFDeploymentStatus mainStatus = ds.getMainStatus();
                if(!po.checkStatusAndAddStage((TargetImpl)target, localStrings.getLocalString("enterprise.deployment.client.change_state", "{0} of {1} in target {2}", action, moduleID, target.getName()), mainStatus)) {
                    return po;
                } else {
                    TargetModuleIDImpl targetModuleID =
                        new TargetModuleIDImpl((TargetImpl)target, moduleID);
                    targetModuleIDList.add(targetModuleID);
                }
            }
            TargetModuleIDImpl[] targetModuleIDs =
                new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = (TargetModuleIDImpl[])targetModuleIDList.toArray(targetModuleIDs);
            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.change_state_all", "{0} of application in all targets", action), (TargetImpl)targets[0],  targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.state_change_failed", "Attempt to change the state of the application {0} failed - {1}", moduleID, ioex.toString()), (TargetImpl)targets[0]);
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
     * @param targetDAS the DAS to contact
     * @return true if the connection was made successfully; false otherwise
     */
    public boolean connect(ServerConnectionIdentifier targetDAS) {
        connected = true;
        this.targetDAS = targetDAS;
        domain = new TargetImpl(this, "domain", localStrings.getLocalString(
                "enterprise.deployment.client.administrative_domain",
                "administrative-domain"));
        return doConnect();
    }

    /**
     * Performs any local- or remote-specific work to end the connection to the DAS.
     * @return true if the disconnection succeeded; false otherwise
     */
    protected abstract boolean doDisconnect();

    /**
     * Disconnects the deployment facility from the DAS.
     * @return true if the disconnection was successful; false otherwise
     */
    public boolean disconnect() {
        connected = false;
        domain = null;
        targetDAS = null;
        return doDisconnect();
    }

    public DFProgressObject createAppRef(Target[] targets, String moduleID, Map options) {
        return changeAppRef(targets, moduleID, "create-application-ref", "Creation", options);
    }

    public DFProgressObject deleteAppRef(Target[] targets, String moduleID, Map options) {
        return changeAppRef(targets, moduleID, "delete-application-ref", "Removal", options);
    }

    protected DFProgressObject changeAppRef(Target[] targets, String moduleID, String commandName, String action, Map options) {
        ensureConnected();
        Throwable commandExecutionException = null;
        targets = prepareTargets(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<TargetModuleIDImpl>();
        try {
            for (Target target : targets) {
                options.put(DFDeploymentProperties.TARGET, target.getName());
                String[] operands = new String[] {moduleID};
                DFDeploymentStatus mainStatus = null;
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, options, operands);
                DFDeploymentStatus ds = commandRunner.run();
                mainStatus = ds.getMainStatus();
                if(!po.checkStatusAndAddStage((TargetImpl)target, localStrings.getLocalString("enterprise.deployment.client.create_reference", "Creation of reference for application in target {0}", target.getName()),  mainStatus)) {
                    return po;
                } else {
                    TargetModuleIDImpl targetModuleID =
                        new TargetModuleIDImpl((TargetImpl)target, moduleID);
                    targetModuleIDList.add(targetModuleID);
                }
            }
            TargetModuleIDImpl[] targetModuleIDs =
                new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = (TargetModuleIDImpl[])targetModuleIDList.toArray(targetModuleIDs);
            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.change_reference_application", "{0} of application reference in all targets", action), (TargetImpl)targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
           po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.change_reference_application_failed", "{0} of application reference failed - {1}", action, ioex.getMessage()), (TargetImpl)targets[0]);
            return po;
        }
    }

    public Target createTarget(String name) {
        return new TargetImpl(this, name, "");
    }

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

    protected String createTargetsParam(Target[] targets) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < targets.length; i++) {
            sb.append(targets[i].getName());
            if (i != targets.length-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public DFProgressObject deploy(Target[] targets, ReadableArchive source, ReadableArchive deploymentPlan, Map deploymentOptions) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException();
        }
        File tempSourceFile = null; 
        File tempPlanFile = null;
        if (source instanceof MemoryMappedArchive) {
            try {
                String type = (String)deploymentOptions.remove("type");
                tempSourceFile = writeMemoryMappedArchiveToTempFile((MemoryMappedArchive)source, getSuffixFromType(type));
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
                        deplLogger.log(Level.WARNING,
                                       FILE_DELETION_ERROR,
                                       tempSourceFile.getAbsolutePath());
                    }
                }
                if (tempPlanFile != null) {
                    boolean isDeleted =tempPlanFile.delete();
                    if (!isDeleted) {
                        deplLogger.log(Level.WARNING,
                                       FILE_DELETION_ERROR,
                                       tempPlanFile.getAbsolutePath());
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
        File tempFile = File.createTempFile("jsr88-", fileSuffix);
        BufferedOutputStream bos = null; 
        BufferedInputStream bis = null;
        int chunkSize = 32 * 1024;
        long remaining = mma.getArchiveSize();
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tempFile));
            bis = new BufferedInputStream(
                new ByteArrayInputStream(mma.getByteArray()));
            while(remaining != 0) {
                int actual = (remaining < chunkSize) ? (int) remaining : chunkSize;
                byte[] bytes = new byte[actual];
                try {
                    for (int totalCount = 0, count = 0; 
                        count != -1 && totalCount < actual; 
                        totalCount += (count = bis.read(bytes, totalCount, actual - totalCount)));
                    bos.write(bytes);
                } catch (EOFException eof) {
                    break;
                }
                remaining -= actual;
            }
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                } finally {
                   bos.close();
                }
            }
            if (bis != null) {
                bis.close(); 
            }
        }
        return tempFile;
    } 

    /**
     * Deploys the application (with optional deployment plan) to the specified
     * targets with the indicated options.
     * @param targets targets to which to deploy the application
     * @param source the app
     * @param deploymentPlan the deployment plan (null if not specified)
     * @param deploymentOptions options to be applied to the deployment
     * @return DFProgressObject the caller can use to monitor progress and query status
     */
    public DFProgressObject deploy(Target[] targets, URI source, URI deploymentPlan, Map deploymentOptions) {
        ensureConnected();
        targets = prepareTargets(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        if(deploymentOptions instanceof DFDeploymentProperties) {
            if (((DFDeploymentProperties) deploymentOptions).getRedeploy())
                po.setCommand(CommandType.REDEPLOY, null);
            else
                po.setCommand(CommandType.DISTRIBUTE, null);
        }
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<TargetModuleIDImpl>();

        //Make sure the file permission is correct when deploying a file
        if (source == null) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.archive_not_specified", "Archive to be deployed is not specified at all."), (TargetImpl)targets[0]);
            return po;
        }
        File tmpFile = new File(source.getSchemeSpecificPart());
        if (!tmpFile.exists()) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.archive_not_in_location", "Unable to find the archive to be deployed in specified location."), domain);
            return po;
        }
        if (!tmpFile.canRead()) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.archive_no_read_permission", "Archive to be deployed does not have read permission."), domain);
            return po;
        }
        try {
            if (deploymentPlan != null) {
                File dp = new File(deploymentPlan.getSchemeSpecificPart());
                if (!dp.exists()) {
                    po.setupForAbnormalExit(localStrings.getLocalString(
                            "enterprise.deployment.client.plan_not_in_location",
                            "Unable to find the deployment plan in specified location."), domain);
                    return po;
                }
                if (!dp.canRead()) {
                    po.setupForAbnormalExit(localStrings.getLocalString(
                            "enterprise.deployment.client.plan_no_read_permission",
                            "Deployment plan does not have read permission."), domain);
                    return po;
                }
                deploymentOptions.put(DFDeploymentProperties.DEPLOYMENT_PLAN, dp.getAbsolutePath());
            }

            // it's redeploy, set the enable attribute accordingly
            boolean isRedeploy = Boolean.valueOf((String)deploymentOptions.remove(DFDeploymentProperties.REDEPLOY));
            if (isRedeploy) {
                String appName = (String)deploymentOptions.get(
                    DFDeploymentProperties.NAME);
                if (!isTargetsMatched(appName, targets)) {
                    po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.specifyAllTargets", "Application {0} is already deployed on other targets. Please remove all references or specify all targets (or domain target if using asadmin command line) before attempting {1} operation", appName, "redeploy"), domain);
                }
                // set the enable attribute accordingly
                String enabledAttr = getAppRefEnabledAttr(
                    targets[0].getName(), appName);
                deploymentOptions.put(DFDeploymentProperties.ENABLED, 
                    enabledAttr);
            }

            Target[] origTargets = targets;

            // first deploy to first target
            if (isRedeploy && targets.length > 1) {
                // if it's redeploy and having more than one target, 
                // we should just redeploy with the special domain target
                targets = createTargets(new String[] {"domain"});
            }
            deploymentOptions.put(DFDeploymentProperties.TARGET, targets[0].getName());    
            DFCommandRunner commandRunner = getDFCommandRunner(
                "deploy", deploymentOptions, new String[]{tmpFile.getAbsolutePath()});
            DFDeploymentStatus ds = commandRunner.run();
            DFDeploymentStatus mainStatus = ds.getMainStatus();
            String moduleID;
            if (!po.checkStatusAndAddStage((TargetImpl)targets[0], localStrings.getLocalString("enterprise.deployment.client.deploy_to_first_target", "Deploying application to target {0}", targets[0].getName()),  mainStatus)) {
                return po;
            } else {
                moduleID = mainStatus.getProperty(DFDeploymentProperties.NAME);
                if (moduleID == null) {
                    moduleID = (String)deploymentOptions.get(DFDeploymentProperties.NAME);
                }
                po.setModuleID(moduleID);
            }

            Map createAppRefOptions = new HashMap();
            if (deploymentOptions.get(DFDeploymentProperties.ENABLED) != null) {
                createAppRefOptions.put(DFDeploymentProperties.ENABLED, deploymentOptions.get(DFDeploymentProperties.ENABLED)); 
            }
            if (deploymentOptions.get(DFDeploymentProperties.VIRTUAL_SERVERS) != null) {
                createAppRefOptions.put(DFDeploymentProperties.VIRTUAL_SERVERS, deploymentOptions.get(DFDeploymentProperties.VIRTUAL_SERVERS)); 
            }
            // then create application references to the rest of the targets
            for (int i = 1; i < targets.length; i++) {
                createAppRefOptions.put(DFDeploymentProperties.TARGET, targets[i].getName());    
                DFCommandRunner commandRunner2 = getDFCommandRunner(
                    "create-application-ref", createAppRefOptions, new String[] {moduleID});
                DFDeploymentStatus ds2 = commandRunner2.run();
                DFDeploymentStatus mainStatus2 = ds2.getMainStatus();
                if (!po.checkStatusAndAddStage((TargetImpl)targets[i], 
"create app ref", mainStatus2)) {
                    return po;
                } 
            }
 
            // we use origTargets to populate the targetModuleIDList 
            // so it takes care of the redeploy using domain target case too
            for (int i = 0; i < origTargets.length; i++) {
                TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl((TargetImpl)origTargets[i], moduleID);
                targetModuleIDList.add(targetModuleID);
            }

            TargetModuleIDImpl[] targetModuleIDs =
                new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = (TargetModuleIDImpl[])targetModuleIDList.toArray(targetModuleIDs);
            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.deploy_application", "Deployment of application {0}", moduleID), (TargetImpl)targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.deploy_application_failed", "Deployment of application failed - {0} ", ioex.toString()), (TargetImpl)targets[0]);
            return po;
        }
    }

    /**
     * Disables an app on the specified targets.
     * @param targets the targets on which to disable the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
    public DFProgressObject disable(Target[] targets, String moduleID) {
        return changeState(targets, moduleID, "disable", "Disable");
    }

    public String downloadFile(File location, String moduleID, String moduleURI) throws IOException {
        throw new UnsupportedOperationException("Not supported in v3");
    }

    /**
     * Enables an app on the specified targets.
     * @param targets the targets on which to enable the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
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
    public boolean isConnected() {
        return connected;
    }

    public List<String> getSubModuleInfoForJ2EEApplication(String appName) throws IOException {
        ensureConnected();
        String commandName = LIST_SUB_COMPONENTS_COMMAND;
        String[] operands = new String[] { appName };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<String> subModuleInfoList = new ArrayList<String>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator subIter = mainStatus.getSubStages(); 
                    subIter.hasNext();) {
                    DFDeploymentStatus subStage =
                        (DFDeploymentStatus) subIter.next();
                    if (subStage.getProperty(DeploymentProperties.MODULE_INFO) != null) {
                        subModuleInfoList.add(
                            subStage.getProperty(DeploymentProperties.MODULE_INFO));
                    }
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
                throw (IOException)commandExecutionException;
            }
            return subModuleInfoList;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    private String getAppRefEnabledAttr(String target, String moduleName) throws IOException {
        ensureConnected();
        String commandName = "show-component-status";
        Map commandParams = new HashMap();
        commandParams.put(DFDeploymentProperties.TARGET, target);
        String[] operands = new String[] { moduleName };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            String enabledAttr = null;

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                Iterator subIter = mainStatus.getSubStages(); 
                if (subIter.hasNext()) {
                    DFDeploymentStatus subStage =
                        (DFDeploymentStatus) subIter.next();
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
                 * reported as unsuccessful.  Because getContextRoot does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
            return enabledAttr;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public String getContextRoot(String moduleName) throws IOException {
        ensureConnected();
        String commandName = GET_COMMAND;
        String patternParam = "applications.application." + moduleName + 
            ".context-root";
        String[] operands = new String[] { patternParam };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            String contextRoot = null;

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                Iterator subIter = mainStatus.getSubStages(); 
                if (subIter.hasNext()) {
                    DFDeploymentStatus subStage =
                        (DFDeploymentStatus) subIter.next();
                    String result = subStage.getStageStatusMessage();
                    contextRoot = 
                        getValueFromDottedNameGetResult(result);
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
                throw (IOException)commandExecutionException;
            }
            return contextRoot;
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public ModuleType getModuleType(String moduleName) throws IOException {
        ensureConnected();
        String commandName = GET_COMMAND;
        String patternParam = "applications.application." + moduleName + ".*";
        String[] operands = new String[] { patternParam };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<String> resultList = new ArrayList<String>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator subIter = mainStatus.getSubStages(); 
                    subIter.hasNext();) {
                    DFDeploymentStatus subStage =
                        (DFDeploymentStatus) subIter.next();
                    resultList.add(subStage.getStageStatusMessage());
                }
                return getJavaEEModuleTypeFromResult(resultList);
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because get does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public Target[] listTargets() throws IOException {
        return listReferencedTargets("*");
    }

    public Target[] listReferencedTargets(String appName) throws IOException {
        ensureConnected();
        String commandName = "_get-targets";
        String[] operands = new String[] { appName };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, null, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<Target> targets = new ArrayList<Target>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                for (Iterator subIter = mainStatus.getSubStages(); 
                    subIter.hasNext();) {
                    DFDeploymentStatus subStage =
                        (DFDeploymentStatus) subIter.next();
                    String result = subStage.getStageStatusMessage();
                    targets.add(createTarget(result));
                }
                Target[] result =
                    new Target[targets.size()];
                return (Target[]) targets.toArray(result);
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because listTargets does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public void getClientStubs(String location, String moduleID)
        throws IOException {
        ensureConnected();
        String commandName = GET_CLIENT_STUBS_COMMAND;
        Map commandParams = new HashMap();
        commandParams.put("appname", moduleID);
        String[] operands = new String[] { location };
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();

            if (mainStatus.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because getClientStubs does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public HostAndPort getHostAndPort(String target) throws IOException {
        return getHostAndPort(target, false);
    }

    public HostAndPort getHostAndPort(String target, boolean securityEnabled) 
        throws IOException {
        return getHostAndPort(target, null, securityEnabled);
    }

    public HostAndPort getVirtualServerHostAndPort(String target, String virtualServer, boolean securityEnabled) throws IOException {
        return getHostAndPort(target, null, virtualServer, securityEnabled);
    }

    public HostAndPort getHostAndPort(String target, String moduleId, boolean securityEnabled) 
        throws IOException {
        return getHostAndPort(target, moduleId, null, securityEnabled);
    }

    private HostAndPort getHostAndPort(String target, String moduleId, 
        String virtualServer, boolean securityEnabled) throws IOException {
        ensureConnected();
        String commandName = "_get-host-and-port";
        Map commandParams = new HashMap();
        commandParams.put(DFDeploymentProperties.TARGET, "server");
        if (moduleId != null) {
            commandParams.put("moduleId", moduleId);
        }
        if (virtualServer != null) {
            commandParams.put("virtualServer", virtualServer);
        }
        commandParams.put("securityEnabled", Boolean.valueOf(securityEnabled));
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
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
            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because getHostAndPort does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    public TargetModuleID[] listAppRefs(String[] targets) throws IOException {
        ensureConnected();
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<TargetModuleIDImpl>();
        Throwable commandExecutionException = null;
        try {
            Target[] targetImpls = prepareTargets(createTargets(targets));
            for (Target target : targetImpls) {
                String commandName = "list-application-refs";
                String[] operands = new String[] { target.getName() };
                Map commandParams = new HashMap();
                DFDeploymentStatus mainStatus = null;
                DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, operands);
                DFDeploymentStatus ds = commandRunner.run();
                mainStatus = ds.getMainStatus();

                if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                    for (Iterator appRefIter = mainStatus.getSubStages(); appRefIter.hasNext();) {
                        DFDeploymentStatus appRefSubStage = (DFDeploymentStatus) appRefIter.next();
                        String moduleID = appRefSubStage.getStageStatusMessage();
                        TargetModuleIDImpl targetModuleID =
                            new TargetModuleIDImpl((TargetImpl)target, moduleID);
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
                    commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                    commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                    throw (IOException)commandExecutionException;
                }
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
        TargetModuleIDImpl[] result =
            new TargetModuleIDImpl[targetModuleIDList.size()];
        return (TargetModuleIDImpl[]) targetModuleIDList.toArray(result);
    }

    public TargetModuleID[] _listAppRefs(String[] targets) throws IOException {
        return _listAppRefs(targets, DFDeploymentProperties.ALL);
    }

    public TargetModuleID[] _listAppRefs(String[] targets, String state) throws IOException {
        return _listAppRefs(targets, state, null);
    }

    public TargetModuleID[] _listAppRefs(String[] targets, String state, String type) throws IOException {
        Target[] targetImpls = prepareTargets(createTargets(targets));
        return _listAppRefs(targetImpls, state, type);
    }

    public TargetModuleID[] _listAppRefs(Target[] targets, String state, String type) throws IOException {
        ensureConnected();
        String commandName = "_list-app-refs";
        String targetsParam = createTargetsParam(targets);
        Map commandParams = new HashMap();
        commandParams.put(DFDeploymentProperties.TARGET, targetsParam);
        commandParams.put("state", state);
        if (type != null) {
            commandParams.put("type", type);
        }
        DFDeploymentStatus mainStatus = null;
        Throwable commandExecutionException = null;
        try {
            DFCommandRunner commandRunner = getDFCommandRunner(commandName, commandParams, null);
            DFDeploymentStatus ds = commandRunner.run();
            mainStatus = ds.getMainStatus();
            List<TargetModuleIDImpl> targetModuleIDList =
                new ArrayList<TargetModuleIDImpl>();

            if (mainStatus.getStatus() != DFDeploymentStatus.Status.FAILURE) {
                /*
                 * There will be one substage for each target.  And within each
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
                        for (Iterator appRefIter = mainStatus.getSubStages(); appRefIter.hasNext();) {
                            DFDeploymentStatus appRefSubStage = (DFDeploymentStatus) appRefIter.next();
                            String moduleID = appRefSubStage.getStageStatusMessage();
                            if (target instanceof TargetImpl) {
                                TargetModuleIDImpl targetModuleID =
                                    new TargetModuleIDImpl((TargetImpl)target, moduleID);
                                targetModuleIDList.add(targetModuleID);
                            }
                        }
                    }
                }

                TargetModuleIDImpl[] result =
                    new TargetModuleIDImpl[targetModuleIDList.size()];
                return (TargetModuleIDImpl[]) targetModuleIDList.toArray(result);

            } else {
                /*
                 * We received a response from the server but the status was
                 * reported as unsuccessful.  Because listAppRefs does not
                 * return a ProgressObject which the caller could use to find
                 * out about the success or failure, we must throw an exception
                 * so the caller knows about the failure.
                 */
                commandExecutionException = new IOException(
                        "remote command execution failed on the server");
                commandExecutionException.initCause(
                        new RuntimeException(mainStatus.getAllStageMessages()));
                throw (IOException)commandExecutionException;
            }
        } catch (Throwable ex) {
            if (commandExecutionException == null) {
                throw new RuntimeException("error submitting remote command", ex);
            } else {
                throw (IOException)commandExecutionException;
            }
        }
    }

    private Target[] prepareTargets(Target[] targets) {
        if (targets == null || targets.length == 0) {
            targets = new Target[]{targetForDefaultServer()};
        } 

        return targets;
    }

    /**
     * Provides a {@link Target} object for the default target.
     *
     * @return Target for the default server
     */
    private Target targetForDefaultServer() {
        TargetImpl t = new TargetImpl(this, DEFAULT_SERVER_NAME, localStrings.getLocalString("enterprise.deployment.client.default_server_description", "default server"));
        return t;
    }

    /**
     * Undeploys an application from specified targets.
     * @param targets the targets from which to undeploy the app
     * @param moduleID the app
     * @return DFProgressObject for monitoring progress and querying status
     */
    public DFProgressObject undeploy(Target[] targets, String moduleID) {
        return undeploy(targets, moduleID, new HashMap());
    }

    /**
     * Undeploys an application from specified targets.
     * @param targets the targets from which to undeploy the app
     * @param moduleID the app
     * @param undeploymentOptions options to control the undeployment
     * @return DFProgressObject for monitoring progress and querying status
     */
    public DFProgressObject undeploy(Target[] targets, String moduleID, Map undeploymentOptions) {
        ensureConnected();
        targets = prepareTargets(targets);
        ProgressObjectImpl po = new ProgressObjectImpl(targets);
        po.setCommand(CommandType.UNDEPLOY,null);
        List<TargetModuleIDImpl> targetModuleIDList =
            new ArrayList<TargetModuleIDImpl>();
        try {
            if (!isTargetsMatched(moduleID, targets)) {
                po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.specifyAllTargets", "Application {0} is already deployed on other targets. Please remove all references or specify all targets (or domain target if using asadmin command line) before attempting {1} operation", moduleID, "undeploy"), domain);
            }

            // first remove the application references from targets except
            // last one
            Map deleteAppRefOptions = new HashMap();
            if (undeploymentOptions.get(DFDeploymentProperties.CASCADE) != null) {
                deleteAppRefOptions.put(DFDeploymentProperties.CASCADE, undeploymentOptions.get(DFDeploymentProperties.CASCADE));
            }
            for (int i = 0 ; i < targets.length - 1 ; i++) {
                deleteAppRefOptions.put(DFDeploymentProperties.TARGET, targets[i].getName());
                DFCommandRunner commandRunner = getDFCommandRunner(
                    "delete-application-ref", deleteAppRefOptions, new String[] {moduleID});
                DFDeploymentStatus ds = commandRunner.run();
                DFDeploymentStatus mainStatus = ds.getMainStatus();
                if (!po.checkStatusAndAddStage((TargetImpl)targets[i], localStrings.getLocalString("enterprise.deployment.client.undeploy_remove_ref", "While undeploying, trying to remove reference for application in target {0}", targets[i].getName()), mainStatus)) {
                    return po;
                } else {
                    TargetModuleIDImpl targetModuleID =
                       new TargetModuleIDImpl((TargetImpl)targets[i], moduleID);
                    targetModuleIDList.add(targetModuleID);
                }
            }

            // then undeploy from last target
            Target lastTarget = targets[targets.length -1];
            undeploymentOptions.put(DFDeploymentProperties.TARGET, lastTarget.getName());
            DFCommandRunner commandRunner2 = getDFCommandRunner(
                    "undeploy",
                    undeploymentOptions,
                    new String[]{moduleID});
            DFDeploymentStatus ds2 = commandRunner2.run();
            DFDeploymentStatus mainStatus2 = ds2.getMainStatus();
            if (!po.checkStatusAndAddStage((TargetImpl)lastTarget, localStrings.getLocalString("enterprise.deployment.client.undeploy_from_target", "Trying to undeploy application from target {0}", lastTarget.getName()), mainStatus2)) {
                return po;
            }

            TargetModuleIDImpl targetModuleID =
                new TargetModuleIDImpl((TargetImpl)lastTarget, moduleID);
            targetModuleIDList.add(targetModuleID);

            TargetModuleIDImpl[] targetModuleIDs =
                new TargetModuleIDImpl[targetModuleIDList.size()];
            targetModuleIDs = (TargetModuleIDImpl[])targetModuleIDList.toArray(targetModuleIDs);

            po.setupForNormalExit(localStrings.getLocalString("enterprise.deployment.client.undeploy_application", "Undeployment of application {0}", moduleID), (TargetImpl)targets[0], targetModuleIDs);
            return po;
        } catch (Throwable ioex) {
            po.setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.undeploy_application_failed", "Undeployment failed - {0}", ioex.toString()), (TargetImpl)targets[0]);
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
    public DFDeploymentStatus waitFor(DFProgressObject po) {
        return po.waitFor();
    }


    public String getWebURL(TargetModuleID tmid) {
        return targetModuleWebURLs.get(tmid.getModuleID());
    }

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
        int index = result.lastIndexOf("=");
        return result.substring(index+1);
    }


    private ModuleType getJavaEEModuleTypeFromResult(List<String> resultList) {
        List<String> sniffersFound = new ArrayList<String>();
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
 
    private String getSuffixFromType(String type) {
        if (type == null) { 
            return null;
        }
        if (type.equals("war")) {
            return ".war";
        } else if (type.equals("ejb")) {
            return ".jar";
        } else if (type.equals("car")) {
            return ".car";
        } else if (type.equals("rar")) {
            return ".rar";
        } else if (type.equals("ear")) {
            return ".ear";
        }
        return null;
    }

    private boolean isTargetsMatched(String appName, Target[] targets) 
        throws IOException {
        Target[] referencedTargets = listReferencedTargets(appName);
        if (targets.length != referencedTargets.length) {
            return false;
        }
        List<String> referencedTargetNames = new ArrayList<String>();
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
