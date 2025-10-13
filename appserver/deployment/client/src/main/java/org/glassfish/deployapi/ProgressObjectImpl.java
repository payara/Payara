/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployapi;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.TargetModuleID;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DFProgressObject;

/**
 * Implementation of the Progress Object
 *
 * @author  dochez
 * @author  tjquinn
 * @author David Matejcek
 */
public class ProgressObjectImpl extends DFProgressObject {

    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ProgressObjectImpl.class);

    private final static String MODULE_ID = DFDeploymentStatus.MODULE_ID;
    private final static String MODULE_TYPE = DFDeploymentStatus.MODULE_TYPE;
    private final static String KEY_SEPARATOR = DFDeploymentStatus.KEY_SEPARATOR;
    private final static String SUBMODULE_COUNT = DFDeploymentStatus.SUBMODULE_COUNT;
    private final static String CONTEXT_ROOT = DFDeploymentStatus.CONTEXT_ROOT;
    private final static String WARNING_PREFIX = "WARNING: ";

    private final TargetImpl target;
    private final Vector<ProgressListener> listeners = new Vector<>();
    private final Vector<ProgressEvent> deliveredEvents = new Vector<>();
    private CommandType commandType;
    private String moduleID;
    private ModuleType moduleType;
    private TargetModuleID[] targetModuleIDs;
    private final DeploymentStatusImpl deploymentStatus;
    private final DFDeploymentStatus finalDeploymentStatus;
    private boolean deployActionCompleted;
    private String warningMessages;


    /** Creates a new instance of ProgressObjectImpl */
    public ProgressObjectImpl(TargetImpl target) {
        this.target = target;
        deploymentStatus = new DeploymentStatusImpl(this);
        deploymentStatus.setState(StateType.RELEASED);
        finalDeploymentStatus = new DFDeploymentStatus();
        deployActionCompleted = false;
    }

    public ProgressObjectImpl(TargetImpl[] targets) {
        this.target = targets[0];
        deploymentStatus = new DeploymentStatusImpl(this);
        deploymentStatus.setState(StateType.RELEASED);
        finalDeploymentStatus = new DFDeploymentStatus();
        deployActionCompleted = false;
    }

    public ProgressObjectImpl(Target[] targets) {
        this(toTargetImpl(targets));
    }

    public ProgressObjectImpl(Target target) {
        this(toTargetImpl(target));
    }

    public static TargetImpl toTargetImpl(Target target) {
        if (target instanceof TargetImpl) {
            return (TargetImpl) target;
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployapi.spi.wrongImpl",
                "Expected Target implementation class of {0} but found instance of {1} instead",
                TargetImpl.class.getName(),
                target.getClass().getName()));
    }

    private static TargetImpl[] toTargetImpl(Target[] targets) {
        TargetImpl[] result = new TargetImpl[targets.length];
        int i = 0;
        for (Target t : targets) {
            result[i++] = toTargetImpl(t);
        }
        return result;
    }


    @Override
    public void addProgressListener(ProgressListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
            if (!deliveredEvents.isEmpty()) {
                for (ProgressEvent progressEvent : deliveredEvents) {
                    listener.handleProgressEvent(progressEvent);
                }
            }
        }
    }

    /**
     * @return {@link CommandType}, may be null.
     */
    public CommandType getCommandType() {
        return commandType;
    }

    /**
     * Sets the command type.
     *
     * @param commandType
     * @see CommandType
     */
    public void setCommand(CommandType commandType) {
        this.commandType = commandType;
    }

    /**
     * Sets the ID of the module.
     *
     * @param moduleId
     */
    public void setModuleID(String moduleId) {
        this.moduleID = moduleId;
    }

    /**
     * Sets the module type for this deployed module
     * @param moduleType the module type
     */
    public void setModuleType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }

    /**
     * @return the module type of this deployed module
     */
    public ModuleType getModuleType() {
        return moduleType;
    }

    /**
     * Always throws {@link OperationUnsupportedException}
     */
    @Override
    public void cancel() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("cancel not supported");
    }


    /**
     * @return always null.
     */
    @Override
    public ClientConfiguration getClientConfiguration(TargetModuleID id) {
        return null;
    }

    @Override
    public DeploymentStatus getDeploymentStatus() {
        DeploymentStatusImpl result = new DeploymentStatusImpl(this);
        result.setState(deploymentStatus.getState());
        result.setMessage(deploymentStatus.getMessage());

        return result;
    }

    /**
     * Returns null until final deployment status available.
     */
    @Override
    public DFDeploymentStatus getCompletedStatus() {
        if (deployActionCompleted) {
            return finalDeploymentStatus;
        }
        return null;
    }

    @Override
    public TargetModuleID[] getResultTargetModuleIDs() {
        if (targetModuleIDs == null && target != null) {
            initializeTargetModuleIDs(moduleID);
        }
        return targetModuleIDs;
    }

    /**
     * Initializes the target module IDs with the passed application moduleID
     *
     * @param moduleID
     */
    private void initializeTargetModuleIDs(String moduleID) {
        TargetModuleIDImpl parentTargetModuleID = new TargetModuleIDImpl(target, moduleID);
        targetModuleIDs = new TargetModuleIDImpl[1];
        targetModuleIDs[0] = parentTargetModuleID;
    }

    /**
     * Always returns false
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isCancelSupported() {
        return false;
    }

    /**
     * Always returns true
     *
     * @return <code>true</code>
     */
    @Override
    public boolean isStopSupported() {
        return false;
    }

    /**
     * Remove a ProgressObject listener.
     *
     * @param listener the listener being removed
     * @see ProgressEvent
     */
    @Override
    public void removeProgressListener(ProgressListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Always throws OperationUnsupportedException
     *
     * @throws OperationUnsupportedException this optional command
     *         is not supported by this implementation.
     */
    @Override
    public void stop() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("stop not supported");
    }

    /**
     * Notifies all listeners that have registered interest for ProgressEvent notification.
     *
     * @param progressEvent {@link ProgressEvent}
     */
    protected void fireProgressEvent(ProgressEvent progressEvent) {
        // Bug 4977764
        // Iteration failed due to concurrent modification of the vector. Even though the add,
        // remove, and fire methods synchronize on the listeners vector, a listener could
        // conceivably invoke add or remove recursively, thereby triggering the concurrent
        // modification exception.
        // Fix: clone the listeners vector and iterate through the clone.
        //
        final Vector<ProgressListener> currentListeners;
        synchronized (listeners) {
            currentListeners = (Vector<ProgressListener>) listeners.clone();
            // The following add must remain inside the synchronized block. Otherwise, there will be
            // a small window in which a new listener's registration could interleave with
            // fireProgressEvent, registering itself after the listeners vector had been cloned
            // (thus excluding the new listener from the iteration a few lines below) but before
            // the list of previously-delivered events had been updated.
            // This would cause the new listener to miss the event that was firing.
            // Keeping the following add inside the synchronized block ensures that updates to
            // the listeners vector by addProgressListener and to deliveredEvents
            // by fireProgressEvent do not interleave and therefore all listeners will receive all
            // events.
            deliveredEvents.add(progressEvent);
        }

        for (ProgressListener element : currentListeners) {
            element.handleProgressEvent(progressEvent);
        }
    }

    /**
     * Notifies all listeners that have registered interest for ProgressEvent notification.
     *
     * @param state
     * @param message
     * @param aTarget
     */
    private void fireProgressEvent(StateType state, String message, TargetImpl aTarget) {

        StateType stateToBroadcast = state == null ? deploymentStatus.getState() : state;

        /* new copy of DeploymentStatus */
        DeploymentStatusImpl depStatus = new DeploymentStatusImpl(this);
        depStatus.setState(stateToBroadcast);
        depStatus.setMessage(message);

        /*
         * Update this progress object's status before notifying listeners.
         */
        if (state != null) {
            deploymentStatus.setMessage(message);
            deploymentStatus.setState(state); // retain current state
        }

        /* send notification */
        TargetModuleIDImpl tmi = new TargetModuleIDImpl(aTarget, moduleID);
        fireProgressEvent(new ProgressEvent(this, tmi, depStatus));
    }

    /**
     * Parse the DeploymentStatus to get the status message within
     */
    private String getDeploymentStatusMessage(DFDeploymentStatus status) {
        if (status == null) {
            return null;
        }
        // if stage status is success, return as it is
        if (DFDeploymentStatus.Status.SUCCESS.isWorseThanOrEqual(status.getStatus())) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        DFDeploymentStatus.parseDeploymentStatus(status, pw);
        byte[] statusBytes = bos.toByteArray();
        String statusString = new String(statusBytes);
        // if stage status is WARNING, collect the warning messages
        if (status.getStatus() == DFDeploymentStatus.Status.WARNING) {
            if (warningMessages == null) {
                warningMessages = WARNING_PREFIX + statusString;
            } else {
                warningMessages += statusString;
            }
            return null;
        }
        // Failed stage; return the failure message
        return statusString;
    }


    /**
     * Notifies listeners about an action success (even with warning).
     *
     * @param message
     * @param aTarget
     * @param tmids
     */
    public void setupForNormalExit(String message, TargetImpl aTarget, TargetModuleIDImpl[] tmids) {
        String i18nmsg;
        // If we ever got some warning during any of the stages, the the final status is warning; else status=success
        if (warningMessages == null) {
            i18nmsg = localStrings.getLocalString(
                    "enterprise.deployment.client.action_completed",
                    "{0} completed successfully",
                    message);
            finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.SUCCESS);
        } else {
            i18nmsg = localStrings.getLocalString(
                    "enterprise.deployment.client.action_completed_with_warning",
                    "Action completed with warning message: {0}",
                    warningMessages);
            finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.WARNING);
        }
        finalDeploymentStatus.setStageStatusMessage(i18nmsg);
        deployActionCompleted = true;
        targetModuleIDs = tmids;
        fireProgressEvent(StateType.COMPLETED, i18nmsg, aTarget);
        for (TargetModuleIDImpl tmid : tmids) {
            // initialize moduleID so the event can be populated with
            // the proper moduleID.
            moduleID = tmid.getModuleID();
            fireProgressEvent(StateType.COMPLETED, message, tmid.getTargetImpl());
        }
    }

    /**
     * Notifies listeners about the action failure.
     *
     * @param errorMsg
     * @param aTarget
     */
    public void setupForAbnormalExit(String errorMsg, TargetImpl aTarget) {
        String i18nmsg = localStrings.getLocalString(
                "enterprise.deployment.client.action_failed",
                "Action failed {0}",
                 errorMsg);
        finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.FAILURE);
        finalDeploymentStatus.setStageStatusMessage(i18nmsg);

        deployActionCompleted = true;
        fireProgressEvent(StateType.FAILED, i18nmsg, aTarget);
        return;
    }

    /**
     * Given a Deployment status, this checks if the status is success.
     * If not, calls {@link #setupForAbnormalExit(String, TargetImpl)}
     *
     * @param aTarget
     * @param action
     * @param currentStatus
     * @return true if the currentStatus is success
     */
    public boolean checkStatusAndAddStage(TargetImpl aTarget, String action, DFDeploymentStatus currentStatus) {
        String statusMsg = getDeploymentStatusMessage(currentStatus);
        finalDeploymentStatus.addSubStage(currentStatus);
        if (statusMsg == null) {
            fireProgressEvent(StateType.RUNNING, localStrings.getLocalString(
                "enterprise.deployment.client.action_completed", "Action {0} completed", action), aTarget);
            return true;
        }
        setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.action_failed_with_message",
            "Action {0} failed - {1}", action, statusMsg), aTarget);
        return false;
    }
}
